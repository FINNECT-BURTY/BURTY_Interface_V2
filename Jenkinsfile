pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        choice(
            name: 'SPRING_PROFILE',
            choices: ['prod', 'dev'],
            description: 'Spring Boot profile (이 값이 .env 의 SPRING_PROFILES_ACTIVE 를 덮어씁니다)'
        )
    }

    environment {
        SERVER_HOST = '44.194.3.230'
        DOMAIN      = 'burty.co.kr'
        CONTAINER   = 'burty'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test & Format') {
            steps {
                sh '''
                    set -e
                    chmod +x gradlew
                    ./gradlew spotlessCheck test --no-daemon
                '''
            }
        }

        stage('Inject .env') {
            steps {
                withCredentials([file(credentialsId: 'BURTY_ENV_FILE', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e
                        install -m 600 "$ENV_FILE" .env
                        printf "\nSPRING_PROFILES_ACTIVE=%s\n" "${SPRING_PROFILE}" >> .env
                    '''
                }
            }
        }

        stage('Verify ingress') {
            steps {
                sh '''
                    set -e
                    if ! docker network inspect global-nginx >/dev/null 2>&1; then
                      echo "❌ global-nginx 네트워크 없음. ingress 스택을 먼저 기동하세요."
                      exit 1
                    fi
                    if docker ps --format '{{.Names}}' | grep -Eq '^(global-nginx|nginx-proxy)$'; then
                      echo "✅ ingress 컨테이너 실행 중"
                    else
                      echo "⚠️  ingress 컨테이너가 실행 중이 아닙니다."
                    fi
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    docker compose \
                      -f docker-compose.yml \
                      -f docker-compose.prod.yml \
                      up -d --build --remove-orphans
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    set -e
                    CODE=000
                    for i in $(seq 1 20); do
                      CODE=$(docker exec "${CONTAINER}" curl -s -o /dev/null -w "%{http_code}" \
                        http://localhost:8080/health || echo 000)
                      [ "$CODE" = "200" ] && break
                      sleep 3
                    done
                    echo "burty internal /health → HTTP ${CODE}"
                    if [ "$CODE" != "200" ]; then
                      docker logs --tail 80 "${CONTAINER}" || true
                      exit 1
                    fi

                    EXT=000
                    for i in $(seq 1 20); do
                      EXT=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 https://${DOMAIN}/health || echo 000)
                      [ "$EXT" = "200" ] && break
                      sleep 3
                    done
                    echo "external https://${DOMAIN}/health → HTTP ${EXT}"
                    if [ "$EXT" != "200" ]; then
                      docker logs --tail 120 nginx-proxy 2>/dev/null || true
                      exit 1
                    fi
                '''
            }
        }
    }

    post {
        always {
            sh 'rm -f .env || true'
        }
        success {
            echo """
            배포 완료
            - API:     https://burty.co.kr/api/v1
            - Health:  https://burty.co.kr/health
            - Jenkins: http://${SERVER_HOST}:8081/
            """
        }
        failure {
            sh "docker logs --tail 120 ${CONTAINER} 2>/dev/null || true"
        }
    }
}

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

        stage('Inject .env') {
            steps {
                withCredentials([file(credentialsId: 'BURTY_ENV_FILE', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e
                        install -m 600 "$ENV_FILE" .env
                        # 파라미터로 받은 SPRING_PROFILE 을 마지막에 append → docker-compose 변수 보간 시 우선
                        printf "\nSPRING_PROFILES_ACTIVE=%s\n" "${SPRING_PROFILE}" >> .env
                    '''
                }
            }
        }

        stage('Verify global-nginx') {
            steps {
                sh '''
                    set -e
                    # global-nginx 네트워크가 미리 만들어져 있어야 burty 가 가입 가능.
                    # infra/global-nginx 스택 (별도 운영) 이 떠 있는지 확인.
                    if ! docker network inspect global-nginx >/dev/null 2>&1; then
                      echo "❌ global-nginx 네트워크 없음. infra/global-nginx 스택을 먼저 기동하세요."
                      echo "   cd infra/global-nginx && ./init-letsencrypt.sh"
                      exit 1
                    fi
                    if ! docker ps --format '{{.Names}}' | grep -q '^global-nginx$'; then
                      echo "⚠️  global-nginx 컨테이너가 실행 중이 아닙니다. 배포는 계속 진행하지만 외부 접근은 안 됩니다."
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
                    # burty 컨테이너 내부 health (global-nginx 와 무관)
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

                    # 외부 도메인 (global-nginx 경유 HTTPS) — 실패해도 경고만
                    EXT=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 https://${DOMAIN}/health || echo 000)
                    echo "external https://${DOMAIN}/health → HTTP ${EXT}"
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
            - Swagger: https://burty.co.kr/api/v1/swagger-ui/index.html
            - Health:  https://burty.co.kr/health
            - Jenkins: http://${SERVER_HOST}:8081/
            """
        }
        failure {
            sh "docker logs --tail 120 ${CONTAINER} 2>/dev/null || true"
        }
    }
}
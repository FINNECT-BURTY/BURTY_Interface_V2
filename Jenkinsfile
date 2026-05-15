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
        PROXY_HOST  = 'nginx-proxy'
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

        stage('Health Check') {
            steps {
                sh '''
                    set -e
                    echo "burty 컨테이너 healthcheck 대기..."
                    STATUS=starting
                    for i in $(seq 1 36); do
                      STATUS=$(docker inspect -f "{{.State.Health.Status}}" "${CONTAINER}" 2>/dev/null || echo starting)
                      echo "[$i/36] status=$STATUS"
                      [ "$STATUS" = "healthy" ] && break
                      sleep 5
                    done
                    if [ "$STATUS" != "healthy" ]; then
                      echo "기동 실패 — 최근 로그:"
                      docker logs --tail 100 "${CONTAINER}" || true
                      exit 1
                    fi
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    set -e
                    CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                      -H "Host: ${SERVER_HOST}" \
                      "http://${PROXY_HOST}/health" || echo 000)
                    echo "GET http://${PROXY_HOST}/health (Host: ${SERVER_HOST}) -> HTTP ${CODE}"
                    if [ "$CODE" != "200" ]; then
                      docker logs --tail 50 "${CONTAINER}" || true
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
            - API:     http://${SERVER_HOST}/api/burty
            - Health:  http://${SERVER_HOST}/health
            - Jenkins: http://${SERVER_HOST}:8081/
            """
        }
        failure {
            sh "docker logs --tail 120 ${CONTAINER} 2>/dev/null || true"
        }
    }
}
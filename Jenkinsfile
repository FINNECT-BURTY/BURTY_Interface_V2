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

        stage('Smoke Test') {
            steps {
                sh '''
                    set -e
                    CODE=000
                    for i in $(seq 1 20); do
                      CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                        -H "Host: ${SERVER_HOST}" \
                        "http://${PROXY_HOST}/health" || echo 000)
                      [ "$CODE" = "200" ] && break
                      sleep 3
                    done
                    echo "GET http://${PROXY_HOST}/health (Host: ${SERVER_HOST}) -> HTTP ${CODE}"
                    if [ "$CODE" != "200" ]; then
                      docker logs --tail 80 "${CONTAINER}" || true
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
pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    environment {
        IMAGE_NAME = "berty"
        DOCKER = "/usr/bin/docker"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build Docker Image') {
            steps {
                sh '${DOCKER} build --network host -t ${IMAGE_NAME}:latest .'
            }
        }
        stage('Deploy') {
            steps {
                sh 'mkdir -p /var/jenkins_home/ws/berty/uploads'
                sh 'chmod -R 777 /var/jenkins_home/ws/berty/uploads || true'
                sh 'mkdir -p /var/jenkins_home/ws/berty/secrets'
                sh 'mkdir -p /var/jenkins_home/ws/berty/logs'
                sh 'chmod -R 777 /var/jenkins_home/ws/berty/logs || true'
                sh '''
                    ${DOCKER} stop ${IMAGE_NAME} || true
                    ${DOCKER} rm -f ${IMAGE_NAME} || true
                    CONTAINER_ID=$(${DOCKER} ps -aq -f name=^/${IMAGE_NAME}$)
                    if [ -n "$CONTAINER_ID" ]; then
                        ${DOCKER} rm -f $CONTAINER_ID
                    fi
                '''
                withCredentials([
                    file(credentialsId: 'BERTY_ENV_FILE', variable: 'ENV_FILE'),
                    file(credentialsId: 'gcp-keys-json', variable: 'GCP_KEYS_FILE')
                ]) {
                    sh '''
                        rm -rf /var/jenkins_home/ws/berty/secrets/keys.json
                        cp "${GCP_KEYS_FILE}" /var/jenkins_home/ws/berty/secrets/keys.json
                        chmod 644 /var/jenkins_home/ws/berty/secrets/keys.json
                        echo "=== keys.json 상태 확인 ==="
                        ls -la /var/jenkins_home/ws/berty/secrets/keys.json
                        [ -f /var/jenkins_home/ws/berty/secrets/keys.json ] || (echo "ERROR: keys.json이 파일이 아닙니다" && exit 1)
                        echo "=== keys.json 정상 생성 완료 ==="
                    '''
                    sh '''
                        ${DOCKER} run -d \
                        --name ${IMAGE_NAME} \
                        --network global-proxy \
                        --add-host host.docker.internal:host-gateway \
                        --env-file ${ENV_FILE} \
                        -e SPRING_PROFILES_ACTIVE=prod \
                        -e DB_HOST=host.docker.internal \
                        -e DB_PORT=3306 \
                        -e DB_NAME=berty \
                        -e DB_USER=root \
                        -e GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/keys.json \
                        -e VIRTUAL_HOST=api.berty.kr \
                        -e VIRTUAL_PORT=8080 \
                        -e FRONTEND_URL=https://berty.kr \
                        -e SERVER_URL=https://berty.kr/api/v1 \
                        -e COOKIE_DOMAIN=berty.kr \
                        -e COOKIE_SECURE=true \
                        -v /home/ubuntu/jenkins_home/ws/berty/uploads:/app/uploads \
                        -v /home/ubuntu/jenkins_home/ws/berty/logs:/app/logs \
                        -v /home/ubuntu/jenkins_home/ws/berty/secrets/keys.json:/app/secrets/keys.json:ro \
                        -e UPLOAD_PATH=/app/uploads/ \
                        -e LOG_PATH=/app/logs \
                        --restart unless-stopped \
                        ${IMAGE_NAME}:latest
                    '''
                }
            }
        }
    }
}

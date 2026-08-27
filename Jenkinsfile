// BURTY — Kubernetes / GitOps 배포 파이프라인
//
// 역할이 바뀌었다. 예전 Jenkinsfile(Jenkinsfile.compose 로 보존)은 EC2 에 SSH 없이 직접
// `docker compose up -d` 를 때리는 **배포 실행자**였다. 지금 Jenkins 는 배포하지 않는다.
//
//   Jenkins  : 검증 → 이미지 빌드/스캔/푸시 → GitOps 레포에 이미지 태그 커밋
//   ArgoCD   : 그 커밋을 클러스터에 반영 (배포의 단일 진실 공급원)
//
// 매니페스트는 이 레포에 없다. RosieOh/BURTY-GitOps 가 소유한다.
//
// 이렇게 나누면 "지금 클러스터에 뭐가 떠 있나"의 답이 항상 Git 에 있다. 롤백은 revert 다.
//
// 필요한 Jenkins Credentials:
//   ghcr-credentials  : Username/Password (또는 Secret file 로 docker config.json)
//   github-token      : Username/Password — 매니페스트 커밋 push 용 (PAT)
//   argocd-token      : Secret text — ArgoCD API 토큰
//
// EC2 Jenkins 를 그대로 쓴다면 아래 `agent { kubernetes ... }` 를 `agent any` 로 바꾸고
// kaniko 단계를 `docker buildx build --push` 로 교체하면 된다.

pipeline {
    agent {
        kubernetes {
            defaultContainer 'jnlp'
            yaml '''
apiVersion: v1
kind: Pod
spec:
  # 파드 레벨에서 runAsUser 를 고정하지 않는다 — kaniko 는 root 로만 빌드된다.
  # 컨테이너마다 필요한 UID 를 따로 준다.
  securityContext:
    fsGroup: 1000
  containers:
    - name: gradle
      image: gradle:9.4.1-jdk21-alpine
      command: ["cat"]
      tty: true
      securityContext:
        runAsUser: 1000          # gradle 이미지의 gradle 계정
      resources:
        requests: { cpu: "1", memory: "2Gi" }
        limits: { memory: "4Gi" }
      volumeMounts:
        - name: gradle-cache
          mountPath: /home/gradle/.gradle
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      command: ["/busybox/cat"]
      tty: true
      securityContext:
        # kaniko 는 파일시스템 스냅샷을 뜨려면 root 가 필요하다.
        # 이것 때문에 jenkins 네임스페이스는 PSA restricted 로 올릴 수 없다.
        # 곤란하면 buildah rootless 또는 원격 BuildKit 데몬으로 교체할 것.
        runAsUser: 0
      resources:
        requests: { cpu: "500m", memory: "2Gi" }
        limits: { memory: "4Gi" }
      volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
    - name: trivy
      image: aquasec/trivy:0.55.0
      command: ["cat"]
      tty: true
      resources:
        requests: { cpu: "200m", memory: "1Gi" }
        limits: { memory: "2Gi" }
      volumeMounts:
        - name: docker-config
          mountPath: /root/.docker
    - name: argocd
      image: quay.io/argoproj/argocd:v2.12.3
      command: ["cat"]
      tty: true
    - name: tools
      # kubectl / kustomize / helm / git / curl / jq
      image: alpine/k8s:1.31.0
      command: ["cat"]
      tty: true
  volumes:
    - name: docker-config
      secret:
        secretName: ghcr-docker-config
        items:
          - key: .dockerconfigjson
            path: config.json
    - name: gradle-cache
      persistentVolumeClaim:
        claimName: jenkins-gradle-cache
'''
        }
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
        skipDefaultCheckout(true)
    }

    environment {
        REGISTRY   = 'ghcr.io'
        // 패키지 네임스페이스는 앱 레포 소유자와 같아야 GHCR 이 패키지를 레포에 연결하고
        // 패키지 권한이 레포 권한을 따라간다. (Dockerfile 의 image.source 라벨과 한 쌍)
        IMAGE_REPO = 'ghcr.io/finnect-burty/burty-api'
        // 매니페스트는 별도 GitOps 레포가 소유한다. 이 레포는 앱 코드만 갖는다.
        GITOPS_REPO   = 'github.com/RosieOh/BURTY-GitOps.git'
        GITOPS_BRANCH = 'main'
        ARGOCD_SERVER = 'argocd.burty.co.kr'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHA   = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                    env.BRANCH    = env.BRANCH_NAME ?: 'main'
                    // 브랜치 → 환경 매핑. 그 외 브랜치는 빌드만 하고 배포하지 않는다.
                    env.TARGET_ENV = (env.BRANCH == 'main')    ? 'prod'
                                   : (env.BRANCH == 'develop') ? 'dev'
                                   : ''
                    env.IMAGE_TAG = "${env.BRANCH.replaceAll('[^a-zA-Z0-9._-]', '-')}-${env.GIT_SHA}"
                    env.IMAGE     = "${env.IMAGE_REPO}:${env.IMAGE_TAG}"
                    echo "branch=${env.BRANCH} env=${env.TARGET_ENV ?: '(배포 없음)'} image=${env.IMAGE}"
                }
            }
        }

        stage('Verify & Test') {
            steps {
                container('gradle') {
                    sh '''
                        set -eu
                        chmod +x gradlew
                        # Testcontainers 는 이 파드에 도커 데몬이 없으면 뜨지 않는다.
                        # DinD 대신 원격 도커(테스트 전용 노드)를 DOCKER_HOST 로 주입하거나,
                        # Testcontainers Cloud 를 쓴다. 아래 변수는 Jenkins 전역 환경에서 온다.
                        ./gradlew spotlessCheck test jacocoTestCoverageVerification --no-daemon
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Schema drift gate') {
            steps {
                sh '''
                    set -eu
                    # 기존 CI 게이트를 그대로 유지한다. Flyway 마이그레이션이 실제 MariaDB 를
                    # 상대로 돌지 않으면 ddl-auto=validate 인 파드가 기동에 실패한다.
                    grep -q 'SchemaMigrationDriftTests' build/test-results/test/*.xml \
                      || { echo "스키마 드리프트 테스트가 실행되지 않았습니다 (Docker 미검출)"; exit 1; }
                '''
            }
        }

        stage('Build & Push image') {
            steps {
                container('kaniko') {
                    sh """
                        set -eu
                        /kaniko/executor \
                          --context=dir://\$(pwd) \
                          --dockerfile=Dockerfile \
                          --destination=${IMAGE} \
                          --build-arg=GIT_SHA=${GIT_SHA} \
                          --build-arg=IMAGE_VERSION=${IMAGE_TAG} \
                          --build-arg=BUILD_DATE=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                          --cache=true \
                          --cache-repo=${IMAGE_REPO}-cache \
                          --cache-ttl=168h \
                          --snapshot-mode=redo
                    """
                }
            }
        }

        stage('Image scan') {
            steps {
                container('trivy') {
                    sh """
                        set -eu
                        # CI 와 동일한 게이트. HIGH/CRITICAL 이면 배포로 넘어가지 않는다.
                        trivy image --exit-code 1 --severity CRITICAL,HIGH \
                          --ignore-unfixed --timeout 10m ${IMAGE}
                    """
                }
            }
        }

        stage('Promote (GitOps commit)') {
            when { expression { return env.TARGET_ENV } }
            steps {
                container('tools') {
                    withCredentials([usernamePassword(
                        credentialsId: 'github-token',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN')]) {
                        sh '''
                            set -eu
                            rm -rf .gitops
                            # 얕은 클론이면 충분하다. 히스토리가 필요한 건 ArgoCD 가 아니라 사람이다.
                            git clone --depth 1 --branch "${GITOPS_BRANCH}" \
                              "https://${GIT_USER}:${GIT_TOKEN}@${GITOPS_REPO}" .gitops

                            cd .gitops
                            git config user.email "jenkins@burty.co.kr"
                            git config user.name  "burty-ci"

                            cd "k8s/overlays/${TARGET_ENV}"
                            kustomize edit set image "${IMAGE_REPO}=${IMAGE}"
                            cd - >/dev/null

                            if git diff --quiet; then
                              echo "이미지 태그 변경 없음 — 커밋 생략"
                              exit 0
                            fi

                            git add k8s/
                            git commit -m "CHORE: ${TARGET_ENV} 이미지 태그를 ${IMAGE_TAG} 로 갱신

무엇: overlays/${TARGET_ENV} 의 burty-api 이미지 태그 갱신
왜: ${BRANCH}@${GIT_SHA} 빌드 산출물을 ${TARGET_ENV} 에 배포하기 위함"

                            # GitOps 레포는 앱 CI 를 돌리지 않는다. 매니페스트 검증만 트리거된다.
                            git push origin "HEAD:${GITOPS_BRANCH}"
                        '''
                    }
                }
            }
        }

        stage('Approve prod deploy') {
            when { expression { return env.TARGET_ENV == 'prod' } }
            options { timeout(time: 30, unit: 'MINUTES') }
            steps {
                // prod 는 사람이 한 번 본다. ArgoCD Application 의 automated sync 를 끈 이유가 이것이다.
                input message: "운영에 ${env.IMAGE_TAG} 를 배포합니다. 진행할까요?", ok: '배포'
            }
        }

        stage('Sync & wait (ArgoCD)') {
            when { expression { return env.TARGET_ENV } }
            steps {
                container('argocd') {
                    withCredentials([string(credentialsId: 'argocd-token', variable: 'ARGOCD_AUTH_TOKEN')]) {
                        sh '''
                            set -eu
                            APP="burty-${TARGET_ENV}"

                            argocd app sync "$APP" \
                              --server "$ARGOCD_SERVER" --grpc-web \
                              --timeout 300

                            # Healthy + Synced 가 될 때까지 대기. 롤아웃 실패를 여기서 잡는다.
                            argocd app wait "$APP" \
                              --server "$ARGOCD_SERVER" --grpc-web \
                              --health --sync --operation --timeout 600
                        '''
                    }
                }
            }
        }

        stage('Smoke test') {
            when { expression { return env.TARGET_ENV } }
            steps {
                container('tools') {
                    sh '''
                        set -eu
                        HOST=$([ "$TARGET_ENV" = "prod" ] && echo "burty.co.kr" || echo "dev.burty.co.kr")

                        CODE=000
                        for i in $(seq 1 20); do
                          CODE=$(curl -s -o /tmp/health.json -w "%{http_code}" --max-time 5 "https://${HOST}/health" || echo 000)
                          [ "$CODE" = "200" ] && break
                          sleep 5
                        done
                        echo "https://${HOST}/health → ${CODE}"
                        cat /tmp/health.json 2>/dev/null || true
                        [ "$CODE" = "200" ] || exit 1

                        # 상태만 200 이고 내부 컴포넌트가 DOWN 인 경우를 잡는다.
                        grep -q '"status":"UP"' /tmp/health.json || {
                          echo "health 응답이 UP 이 아닙니다"; exit 1; }

                        # /actuator 가 외부로 새지 않는지 확인 (VirtualService directResponse 404)
                        ACT=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "https://${HOST}/actuator/prometheus" || echo 000)
                        echo "https://${HOST}/actuator/prometheus → ${ACT}"
                        [ "$ACT" = "404" ] || { echo "actuator 가 외부에 노출되고 있습니다"; exit 1; }
                    '''
                }
            }
        }
    }

    post {
        always {
            // clone 한 GitOps 워크스페이스에는 토큰이 박힌 remote URL 이 남는다.
            // 워크스페이스가 다음 빌드까지 살아 있으므로 반드시 지운다.
            sh 'rm -rf .gitops || true'
        }
        failure {
            script {
                if (env.TARGET_ENV) {
                    // 실패 원인을 로그에 남긴다. 실제 롤백은 ArgoCD 히스토리로 한다:
                    //   argocd app rollback burty-prod <REVISION>
                    container('tools') {
                        sh '''
                            kubectl -n "burty-${TARGET_ENV}" get pods -o wide || true
                            kubectl -n "burty-${TARGET_ENV}" describe deploy/burty-api || true
                            kubectl -n "burty-${TARGET_ENV}" logs -l app.kubernetes.io/name=burty-api \
                              --tail=120 -c burty-api || true
                        '''
                    }
                    // argocd 바이너리는 argocd 컨테이너에만 있다.
                    container('argocd') {
                        withCredentials([string(credentialsId: 'argocd-token', variable: 'ARGOCD_AUTH_TOKEN')]) {
                            sh '''
                                argocd app history "burty-${TARGET_ENV}" \
                                  --server "$ARGOCD_SERVER" --grpc-web || true
                            '''
                        }
                    }
                }
            }
        }
        success {
            script {
                if (env.TARGET_ENV) {
                    echo """
                    배포 완료 (${env.TARGET_ENV})
                    - image  : ${env.IMAGE}
                    - ArgoCD : https://${env.ARGOCD_SERVER}/applications/burty-${env.TARGET_ENV}
                    """
                }
            }
        }
    }
}

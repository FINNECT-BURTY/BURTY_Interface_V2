# syntax=docker/dockerfile:1

# ── Build ──────────────────────────────────────────────────────────────────────
FROM gradle:9.7.1-jdk21-alpine AS build
WORKDIR /app

# 의존성 레이어를 소스와 분리한다.
# 예전에는 COPY src 가 빌드보다 앞에 있어서, 소스 한 줄만 바꿔도 전체 의존성을 다시 받았다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet || true

COPY src ./src
# 테스트는 CI 가 Testcontainers 로 별도 실행한다. 이미지 빌드에서는 아티팩트만 만든다.
RUN ./gradlew bootJar --no-daemon -x test

# ── Runtime ────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 베이스 이미지에 남아 있는 OS 패키지 취약점(libcrypto3 등)을 빌드 시점에 올린다.
RUN apk upgrade --no-cache && \
    apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    addgroup -g 10001 -S spring && adduser -u 10001 -S spring -G spring

# OCI 표준 라벨. org.opencontainers.image.source 가 있어야 GHCR 패키지가 이 레포에
# 자동으로 연결되고(패키지 페이지 → 소스 링크), 패키지 권한이 레포 권한을 따라간다.
# source 의 소유자와 패키지 네임스페이스(ghcr.io/<owner>)가 같아야 연결된다.
LABEL org.opencontainers.image.source="https://github.com/FINNECT-BURTY/BURTY_Interface_V2" \
      org.opencontainers.image.title="BURTY API" \
      org.opencontainers.image.description="BURTY 청년 자산관리 백엔드 API" \
      org.opencontainers.image.vendor="FINNECT-BURTY" \
      org.opencontainers.image.licenses="UNLICENSED"

# 빌드 시점에 주입되는 값. 이미지만 보고 어느 커밋인지 알 수 있어야 롤백 판단이 된다.
ARG GIT_SHA=unknown
ARG BUILD_DATE=unknown
ARG IMAGE_VERSION=unknown
LABEL org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${IMAGE_VERSION}"

COPY --from=build /app/build/libs/*.jar app.jar

RUN mkdir -p /app/logs /app/uploads /app/secrets && \
    chown -R spring:spring /app

# UID 를 숫자로 고정한다. Kubernetes 는 securityContext.runAsNonRoot 를 검증할 때
# 이미지의 USER 가 이름이면 root 여부를 판정하지 못해 파드를 기동시키지 않는다
# (CreateContainerConfigError: "container has runAsNonRoot and image has non-numeric user").
USER 10001:10001

EXPOSE 8080

# 고정 Xmx 대신 컨테이너 메모리 비율을 쓴다. 메모리 한도를 바꿔도 이미지를 다시 만들 필요가 없다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://localhost:8080/health | grep -q '"status":"UP"' || exit 1

# exec 로 java 를 PID 1 로 만든다. 그래야 docker stop 의 SIGTERM 이 JVM 에 전달되고
# server.shutdown=graceful 이 실제로 동작한다. (없으면 배포마다 진행 중인 이체가 잘린다.)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

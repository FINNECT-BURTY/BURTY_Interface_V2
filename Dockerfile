# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Timezone 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 보안을 위한 non-root 사용자
RUN addgroup -S spring && adduser -S spring -G spring

# Build stage에서 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

RUN mkdir -p /app/logs && chown -R spring:spring /app
USER spring

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -Duser.timezone=Asia/Seoul -Dio.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

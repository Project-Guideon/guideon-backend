# guideon-core Dockerfile
# Core Service (Internal API - 내부망 전용)

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 복사
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐싱을 위해 build.gradle 먼저 복사
COPY build.gradle settings.gradle ./
COPY guideon-common/build.gradle guideon-common/
COPY guideon-core/build.gradle guideon-core/
COPY guideon-admin-bff/build.gradle guideon-admin-bff/

# 의존성 다운로드 (캐싱 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY guideon-common guideon-common
COPY guideon-core guideon-core

# Core 모듈 빌드
RUN ./gradlew :guideon-core:bootJar --no-daemon -x test

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/guideon-core/build/libs/guideon-core.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# guideon-kiosk-bff Dockerfile
# Kiosk BFF Service (외부 노출 - Unity Kiosk 전용)

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
COPY guideon-kiosk-bff/build.gradle guideon-kiosk-bff/

# 의존성 다운로드 (캐싱 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY guideon-common guideon-common
COPY guideon-core guideon-core
COPY guideon-kiosk-bff guideon-kiosk-bff

# Kiosk BFF 모듈 빌드
RUN ./gradlew :guideon-kiosk-bff:bootJar --no-daemon -x test

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine

# non-root user 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 빌드된 JAR 복사 및 권한 설정
COPY --from=builder --chown=appuser:appgroup /app/guideon-kiosk-bff/build/libs/guideon-kiosk-bff.jar app.jar

# non-root user로 전환
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/health || exit 1

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx384m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m} -jar app.jar"]

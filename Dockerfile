# =========================================================================
# Multi-Stage Dockerfile for THE JOURNAL (Spring Boot 3.3.4 + Java 21)
# =========================================================================

# -------------------------------------------------------------------------
# Stage 1: Build Stage
# -------------------------------------------------------------------------
FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /home/gradle/project

# Gradle 캐시 극대화를 위해 의존성 정의 파일 먼저 복사
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle/ ./gradle/
COPY --chown=gradle:gradle gradlew ./

# 의존성 다운로드 캐싱
RUN ./gradlew dependencies --no-daemon || true

# 전체 소스 복사 및 테스트 제외 빌드 (단위/통합 테스트는 CI 파이프라인에서 선행)
COPY --chown=gradle:gradle src ./src
RUN ./gradlew bootJar -x test --no-daemon

# -------------------------------------------------------------------------
# Stage 2: Production Runtime Stage (경량 JRE 21 Alpine)
# -------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="journal-dev"
LABEL description="The Journal - Editorial Magazine & Blog Production Image"

# 보안 강화를 위한 비-루트(non-root) 유저 생성 및 curl(헬스체크용) 설치
RUN apk add --no-cache curl tzdata \
    && addgroup -S appgroup && adduser -S appuser -G appgroup

# 한국 시간대(KST) 설정
ENV TZ=Asia/Seoul
RUN cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 업로드 디렉토리 생성 및 권한 부여
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Stage 1에서 빌드된 실행 가능한 fat JAR 복사
COPY --from=builder --chown=appuser:appgroup /home/gradle/project/build/libs/*-SNAPSHOT.jar app.jar

# 비-루트 유저로 전환
USER appuser

# 컨테이너 포트 (기본 8090)
EXPOSE 8090

# JVM 튜닝 옵션 (컨테이너 메모리 인식 및 가비지 컬렉터 최적화)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

# 헬스체크 설정
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8090/articles || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

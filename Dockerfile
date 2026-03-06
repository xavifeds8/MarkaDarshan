# ============================================================
# Multi-stage build for Markdown Preview Server
# ============================================================

# Stage 1: Build
FROM gradle:8.8-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle fatJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/build/libs/*-all.jar app.jar

# Default directory to serve (can be overridden with -v)
RUN mkdir -p /data && chown appuser:appgroup /data
VOLUME /data

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "/data"]

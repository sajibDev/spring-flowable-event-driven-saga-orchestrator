# Multi-stage build for all services
ARG SERVICE_NAME=saga-orchestrator

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
ARG SERVICE_NAME
WORKDIR /build

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Copy all service source code
COPY common-events common-events
COPY saga-orchestrator saga-orchestrator
COPY order-service order-service
COPY inventory-service inventory-service
COPY payment-service payment-service
COPY shipping-service shipping-service

# Build the specific service
RUN chmod +x gradlew && \
    ./gradlew :${SERVICE_NAME}:bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_NAME
WORKDIR /app

# Copy the built jar from builder
COPY --from=builder /build/${SERVICE_NAME}/build/libs/${SERVICE_NAME}-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

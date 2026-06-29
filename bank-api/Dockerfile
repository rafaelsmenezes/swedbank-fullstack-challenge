# Stage 1: Builder
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Gradle wrapper and build files first for better layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Make the wrapper script executable (executable bit can be lost in some Docker/git contexts)
RUN chmod +x gradlew

# Download dependencies (cached unless build files change)
RUN ./gradlew --no-daemon dependencies

# Copy source code and build the application
COPY src src
RUN ./gradlew --no-daemon bootJar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install curl for healthchecks (required by docker-compose)
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

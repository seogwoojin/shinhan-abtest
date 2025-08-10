# Backend Dockerfile for Spring Boot Kotlin application
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test

# Expose the port the application runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "build/libs/finsync-0.0.1-SNAPSHOT.jar"]
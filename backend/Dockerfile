# Stage 1: Use the official OpenJDK 21 image to build the project
# This ensures that the build environment matches your development environment.
FROM openjdk:21-jdk AS builder

# Set the working directory
WORKDIR /workspace/app

# === FIX ===
# The base image is Oracle Linux, which uses 'microdnf', not 'apt-get'.
# Install the 'findutils' package which provides the 'xargs' command.
RUN microdnf install -y findutils
# ===========

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle gradle

# Copy the build.gradle.kts and settings.gradle.kts files
COPY build.gradle.kts settings.gradle.kts ./

# Copy the source code
COPY src src

# Make the gradlew script executable
RUN chmod +x ./gradlew

# Build the application, skipping tests to make the Docker build faster.
# The output will be in build/libs
RUN ./gradlew build -x test --no-daemon

# Stage 2: Create the final, smaller runtime image
# The openjdk:21-slim image is Debian-based, so it is fine as is.
FROM openjdk:21-slim

# Set the working directory
WORKDIR /app

# Copy ONLY the built application JAR from the 'builder' stage
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# The command to run the application
ENTRYPOINT ["java","-jar","app.jar"]
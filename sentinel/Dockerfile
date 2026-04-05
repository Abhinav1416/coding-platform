# Stage 1: Build
FROM --platform=linux/amd64 eclipse-temurin:21-jdk AS builder
WORKDIR /workspace/app

# Install findutils using apt-get (Debian/Ubuntu standard)
RUN apt-get update && apt-get install -y findutils

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime (Using JRE to keep it small)
FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
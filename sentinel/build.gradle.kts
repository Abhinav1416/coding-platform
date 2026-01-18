plugins {
    java
    id("org.springframework.boot") version "3.3.4" // MATCHED BACKEND
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.coding-platform"
version = "0.0.1-SNAPSHOT"
description = "Sentinel Worker Service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// We use the same Bill of Materials (BOM) as your backend
dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.2.0")
    }
}

dependencies {
    // 1. Core Web & Actuator
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 2. Redis (State Management)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // 3. AWS SQS (Version managed by BOM above)
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

    // 4. Resilience4j (Circuit Breaker) - Explicit version for Boot 3
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // 5. Utilities & Testing
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
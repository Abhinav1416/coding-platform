plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.linkedin"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

//dependencies {
//	implementation("org.springframework.boot:spring-boot-starter-web")
//	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation("org.springframework.boot:spring-boot-starter-validation")
//	implementation("org.springframework.boot:spring-boot-starter-mail")
//	implementation("mysql:mysql-connector-java:8.0.33")
//
//	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
//	implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
//	implementation(("io.jsonwebtoken:jjwt-jackson:0.12.6"))
//
//	developmentOnly("org.springframework.boot:spring-boot-devtools")
//	testImplementation("org.springframework.boot:spring-boot-starter-test")
//	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//}


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("mysql:mysql-connector-java:8.0.33")
	implementation("org.postgresql:postgresql:42.6.0")  // <-- Add this

	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
	implementation(("io.jsonwebtoken:jjwt-jackson:0.12.6"))

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



tasks.withType<Test> {
	useJUnitPlatform()
}

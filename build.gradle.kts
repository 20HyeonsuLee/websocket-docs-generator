plugins {
    id("java")
    id("org.springframework.boot") version "3.5.3" // 이미 쓰는 버전
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
}

group = "coffeeshout"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.async-websocket"     // 네 그룹 아이디
            artifactId = "docs-generator"       // 라이브러리 이름
            version = "1.0.0"           // 버전
        }
    }
}

dependencies {
    // ✅ Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")

    // ✅ Reflections (리플렉션 스캐닝)
    implementation("org.reflections:reflections:0.10.2")

    // ✅ victools JSON Schema Generator
    implementation("com.github.victools:jsonschema-generator:4.37.0")

    // ✅ Spring 메시징 어노테이션 (@MessageMapping 등)
    implementation("org.springframework:spring-messaging:6.2.8")

    // ✅ Spring WebSocket (WebsocketMessage 스캔하려고 필요)
    implementation("org.springframework:spring-websocket:6.2.8")

    // ✅ Lombok (있으면 편의용)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // ✅ 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.yaml:snakeyaml:2.0") // asyncapi.yml 파싱
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0") // Jackson YAML 바인딩
}
tasks.test {
    useJUnitPlatform()
}

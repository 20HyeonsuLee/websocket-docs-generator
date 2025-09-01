import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("java-library")
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.vanniktech.maven.publish") version "0.28.0"
    `maven-publish`
    signing
}
group = "io.github.20hyeonsulee"
version = "1.0.6"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    
    // 사용자 프로젝트에서 제공될 것으로 기대하는 의존성
    compileOnly("org.springframework:spring-messaging")
    compileOnly("org.springframework:spring-websocket") 
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // 내부 구현용 - 버전을 고정하되 사용자에게 노출되지 않음
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.github.victools:jsonschema-generator:4.37.0")
    implementation("org.yaml:snakeyaml:2.0")
}

// 라이브러리이므로 bootJar 비활성화
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
    archiveClassifier = ""
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    coordinates("io.github.20hyeonsulee", "websocket-docs-generator", "1.0.6")

    pom {
        name = "WebSocket Docs Generator"
        description = "A Java library for generating WebSocket API documentation"
        inceptionYear = "2025"
        url = "https://github.com/20HyeonsuLee/websocket-docs-generator"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "20HyeonsuLee"
                name = "Hyeonsu Lee"
                url = "https://github.com/20HyeonsuLee"
            }
        }

        scm {
            url = "https://github.com/20HyeonsuLee/websocket-docs-generator"
            connection = "scm:git:git://github.com/20HyeonsuLee/websocket-docs-generator.git"
            developerConnection = "scm:git:ssh://git@github.com/20HyeonsuLee/websocket-docs-generator.git"
        }
    }
}

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("java")
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.vanniktech.maven.publish") version "0.28.0"
    `maven-publish`
    signing
}
group = "io.github.20hyeonsulee"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.34")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.github.victools:jsonschema-generator:4.37.0")
    implementation("org.springframework:spring-messaging:6.2.8")
    implementation("org.springframework:spring-websocket:6.2.8")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.yaml:snakeyaml:2.0")
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    coordinates("io.github.20hyeonsulee", "websocket-docs-generator", "1.0.2")

    pom {
        name = "WebSocket Docs Generator"
        description = "A Kotlin library for generating WebSocket API documentation"
        inceptionYear = "2024"
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

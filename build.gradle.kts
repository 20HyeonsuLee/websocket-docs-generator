import com.vanniktech.maven.publish.SonatypeHost
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
    // This is a library: the Boot plugin is only referenced for its BOM coordinates, never applied.
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("com.vanniktech.maven.publish") version "0.28.0"
    signing
}

group = "io.github.20hyeonsulee"
version = "2.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Provided by the consuming Spring Boot application; no versions are forced on it.
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-messaging")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.yaml:snakeyaml")

    // Internal implementation detail; none of its types appear in the public API.
    implementation("com.github.victools:jsonschema-generator:4.37.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.37.0")

    // Generates META-INF/spring-configuration-metadata.json for IDE completion of websocket.docs.* keys.
    // Must come after Lombok so the generated getters/setters are visible to it.
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Lets the fixture controllers' parameter names be read reflectively (for @DestinationVariable without a value).
    options.compilerArgs.add("-parameters")
}

// Boots the fixture app so the docs UI can be inspected at http://localhost:8080/ws-docs.
tasks.register<JavaExec>("runTestApp") {
    group = "application"
    description = "Runs the fixture Spring Boot app from src/test so the docs UI can be opened in a browser."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "io.github.hyeonsulee.wsdocs.fixture.TestWsApp"
    standardInput = System.`in`
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.hyeonsulee.wsdocs")
    }
}

tasks.test {
    useJUnitPlatform()
    // Refresh the snapshot with: ./gradlew test -Dwsdocs.updateSnapshot=true
    systemProperty("wsdocs.updateSnapshot", System.getProperty("wsdocs.updateSnapshot", "false"))
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Sign only when a Maven Central publish task (publishToMavenCentral*) was requested or an in-memory key is
// supplied, so build / publishToMavenLocal work without a GPG key.
val signingRequested = gradle.startParameter.taskNames.any { it.contains("MavenCentral") }
        || project.hasProperty("signingInMemoryKey")

if (signingRequested) {
    signing {
        // CI uses the signingInMemoryKey* properties; local builds use signing.gnupg.* with the gpg command.
        if (project.hasProperty("signing.gnupg.keyName") && !project.hasProperty("signingInMemoryKey")) {
            useGpgCmd()
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (signingRequested) {
        signAllPublications()
    }

    coordinates(group.toString(), "websocket-docs-generator", version.toString())

    pom {
        name = "WebSocket Docs Generator"
        description = "AsyncAPI 3.0 documentation generator for Spring Boot STOMP/WebSocket applications"
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

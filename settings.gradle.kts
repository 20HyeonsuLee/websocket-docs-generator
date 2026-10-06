pluginManagement {
    plugins {
        // The CI matrix overrides this with -PspringBootVersion=3.x.y to build and test against several Boot lines.
        id("org.springframework.boot") version providers.gradleProperty("springBootVersion").getOrElse("3.5.3")
    }
}

rootProject.name = "websocket-docs-generator"

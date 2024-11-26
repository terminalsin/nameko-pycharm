import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.namecheap"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        pycharmCommunity("2024.3")
        bundledPlugin("PythonCore")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }

    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

intellijPlatform {
    // Plugin configuration
    pluginConfiguration {
        id.set("com.namecheap.nameko.rpc")
        name.set("Nameko RPC Support")
        version.set("1.0.0")
        description.set("""
            Provides code completion and type inference support for Nameko RPC services.
            - Code completion for RPC methods
            - Type inference for method parameters
            - Service name detection
        """.trimIndent())
    }
}

intellijPlatformTesting {
  runIde
  testIde
  testIdeUi
  testIdePerformance
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    runIde {
        // Enable debug logging
        jvmArgs(
            "-Didea.log.debug=true",
            "-Didea.log.trace=true",
            "-Didea.log.debug.categories=#com.namecheap.nameko.debug",
        )
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
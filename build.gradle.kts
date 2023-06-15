plugins {
    kotlin("jvm")
}

group = "dev.luna5ama"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.test {
    useJUnitPlatform()
}
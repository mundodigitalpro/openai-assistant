plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.josejordan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.aallam.openai:openai-client:3.7.0")
    implementation ("io.ktor:ktor-client-cio:2.3.8")
    implementation ("io.ktor:ktor-client-core:2.3.8")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
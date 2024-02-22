plugins {
    kotlin("jvm") version "1.9.22"
    application
}
application {
    var mainClassName = "com.josejordan.MainKt"
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
    implementation ("org.slf4j:slf4j-api:1.7.30")
    implementation ("ch.qos.logback:logback-classic:1.4.14")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.josejordan.MainKt"
        )
    }
    // Incluir todas las dependencias en el JAR si es necesario
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(17)
}
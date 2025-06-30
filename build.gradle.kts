val kotlinxVersion = "0.6.0"
val ktorVersion = "3.2.0"
val logbackVersion = "1.5.17"
val mockitoVersion = "5.4.0"

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxVersion")

    // Test dependencies
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxVersion")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

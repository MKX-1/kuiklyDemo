/**
 * 后端是一个**独立的 Gradle 工程**（不塞进根工程）。
 * 原因：它的交付物（一个 JVM 服务）和客户端的交付物（一个 APK）完全不同，
 * 生命周期、依赖、部署方式都不一样，拆开各自演进、各自构建更清爽。
 */
plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    application
}

group = "com.example.aistock"
version = "1.0.0"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.12")
    // Ktor 的日志走 SLF4J，缺实现时是空实现 —— 装了才能看到请求日志
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // 测试：Ktor 的测试宿主 + kotlin-test（后端走 TDD，客户端侧没有测试源集）
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.12")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.aistock.backend.ApplicationKt")
}

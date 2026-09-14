pluginManagement {
    repositories {
        // 阿里云镜像加速（国内网络友好）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        gradlePluginPortal()
        mavenCentral()
        // Kuikly 的依赖只发布在腾讯源，必须显式添加
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
    }
}

rootProject.name = "ai-stock-demo"

include(":shared")       // 跨平台业务库（UI + 逻辑）
include(":androidApp")   // Android 宿主壳工程
// backend/ 是独立的 Gradle 工程，不在本工程内，见 backend/README

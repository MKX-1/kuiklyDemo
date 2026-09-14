/**
 * 跨平台业务库：所有业务代码（UI + 逻辑 + 数据）都写在这里。
 *
 * 本模块只声明了 androidTarget（Android 端），因此可以在 Windows 上完整构建。
 * 要接 iOS / 鸿蒙 / H5，在下方 kotlin {} 里补 iosArm64() / ohosArm64() / js(IR) 目标即可，
 * 业务代码不用改——这正是 KMP「一套代码多端」的含义。
 *
 * 注：Kuikly 官方模板还会应用 `com.tencent.kuikly-open.kuikly` 插件，但那个插件是用来
 * 给 H5 / 小程序打包 JS 产物的，它硬性要求存在 js 目标。纯 Android 工程用不上，
 * 强行应用会报 "Task jsBrowserDevelopmentExecutableDistribution not found"。
 */
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.google.devtools.ksp")
    kotlin("plugin.compose")
}

val KEY_PAGE_NAME = "pageName"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kuikly 核心：跨端 UI 框架、布局、Bridge 通信
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                // @Page 等注解
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
                // Compose DSL：让 Jetpack Compose 那套写法能跑在 Kuikly 上
                implementation("com.tencent.kuikly-open:compose:${Version.getKuiklyVersion()}")
            }
        }
        val androidMain by getting {
            dependencies {
                // Android 平台的渲染器实现（原生 View 渲染）
                api("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
            }
        }
    }
}

// KSP 参数：pageName 为空 = 全部页面都打进产物（开发期用）
ksp {
    arg(KEY_PAGE_NAME, getPageName())
}

dependencies {
    // Kuikly 的注解处理器：编译期扫描 @Page，自动生成页面注册表 KuiklyCoreEntry
    compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyVersion()}") {
        add("kspAndroid", this)
    }
}

android {
    namespace = "com.example.aistock.shared"
    compileSdk = 35
    buildToolsVersion = "35.0.1"
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}

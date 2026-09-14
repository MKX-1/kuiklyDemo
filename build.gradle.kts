plugins {
    // 版本统一在这里声明，子模块只引用不带版本，避免多处版本漂移
    id("com.android.application").version("8.7.3").apply(false)
    id("com.android.library").version("8.7.3").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
    // Compose 编译器插件：Kuikly 的 Compose DSL 需要它把 @Composable 编译成跨端代码
    kotlin("plugin.compose") version "2.1.21" apply false
    id("com.google.devtools.ksp").version("2.1.21-2.0.1").apply(false)
}

// 说明：官方模板还会在这里通过 buildscript 引入 Kuikly 的 Gradle 插件
// （com.tencent.kuikly-open:core-gradle-plugin）。那个插件负责 JS / 小程序的产物打包，
// 硬性要求模块里存在 js 目标。本工程是纯 Android 演示，用不上，故不引入。
// 将来要出 H5 产物时，把 classpath(BuildPlugin.kuikly) 加回来并在 shared 里补 js(IR) 目标即可。

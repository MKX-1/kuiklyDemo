/**
 * buildSrc：Gradle 的"构建用 Kotlin 代码"。
 *
 * 放在这里的东西会被 Gradle 先编译，然后所有模块的构建脚本都能直接引用。
 * 目的是把版本号收敛成"单一事实源"——改版本只改这一个文件。
 */
object Version {

    /** Kuikly 框架版本（short version）。 */
    private const val KUIKLY_VERSION = "2.27.0"

    /** Kotlin 版本。Kuikly 的坐标里带 Kotlin 版本后缀，两者必须严格对应。 */
    private const val KOTLIN_VERSION = "2.1.21"

    /**
     * Kuikly 依赖版本号规则：`${框架版本}-${Kotlin版本}`。
     * 适用于 core / core-ksp / core-annotations / core-render-android / compose / core-gradle-plugin。
     */
    fun getKuiklyVersion(): String = "$KUIKLY_VERSION-$KOTLIN_VERSION"
}

object BuildPlugin {
    /** Kuikly 的 Gradle 插件坐标。 */
    val kuikly: String by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}

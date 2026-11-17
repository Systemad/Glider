plugins {
    id("com.android.application") version "9.5.1" apply false
    id("com.google.devtools.ksp") version "2.3.14" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}

val ktlint = configurations.create("ktlint")

dependencies { ktlint("com.pinterest.ktlint:ktlint-cli:1.8.0:all") }

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**", "!bootstrap/**")
}
tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", "**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**", "!bootstrap/**")
}

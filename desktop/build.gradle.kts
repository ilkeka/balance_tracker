import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinJvm)
    application
}

group = "me.ilker.balance_tracker"
version = "1.0.0"

application {
    mainClass.set("me.ilker.balance_tracker.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.shared)

        implementation(libs.jetbrains.compose.desktop.jvm.linux.x64)
        implementation(libs.jetbrains.compose.ui)
        implementation(libs.koin.compose)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.core)
        implementation(projects.resources)

        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.lifecycle.runtime.compose)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(libs.kotlin.test)
    }
}

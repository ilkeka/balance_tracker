import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
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
        implementation(platform(libs.androidx.compose.bom))

        implementation(project(":core"))
        implementation(project(":resources"))

        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.lifecycle.runtime.compose)
        implementation(libs.kotlinx.coroutines)
        testImplementation(libs.kotlin.test)
    }
}

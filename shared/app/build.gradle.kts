import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
        }

        namespace = "me.ilker.balance_tracker.shared.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()
    
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.shared.common)
        implementation(projects.shared.core)
        implementation(projects.shared.home)
        implementation(projects.shared.resources)
        implementation(projects.shared.transaction)

        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.jetbrains.compose.materialicons.core)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.navigation)
        implementation(libs.jetbrains.compose.navigationevent)
        implementation(libs.jetbrains.compose.lifecycle.runtime)
        implementation(libs.koin.compose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(libs.kotlin.test)
    }
}

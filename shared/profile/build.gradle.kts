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

        namespace = "me.ilker.balance_tracker.shared.profile"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.shared.common)
        implementation(projects.shared.core)
        implementation(projects.shared.resources)

        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.jetbrains.compose.materialicons.core)
        implementation(libs.jetbrains.compose.materialicons.extended)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.navigationevent)
        implementation(libs.jetbrains.compose.lifecycle.runtime)
        implementation(libs.qrose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(libs.kotlin.test)
    }
}

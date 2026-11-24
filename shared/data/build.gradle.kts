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
            jvmTarget.set(JvmTarget.JVM_21)
        }

        namespace = "me.ilker.balance_tracker.shared.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            // Compose requires an executable binary on every wasm target, even for a library.
            testTask {
                enabled = false
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
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
        implementation(libs.jetbrains.compose.lifecycle.runtime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
    }
}

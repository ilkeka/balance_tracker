import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        namespace = "me.ilker.balance_tracker.shared.core"
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
        implementation(libs.kotlinx.coroutines.core)

        testImplementation(libs.kotlin.test)
    }
}

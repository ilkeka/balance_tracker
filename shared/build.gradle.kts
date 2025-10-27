import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

//    androidLibrary {
//        androidResources.enable = true
//    }
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-kclass-fqn")
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(platform(libs.androidx.compose.bom))

        implementation(project(":core"))
        implementation(project(":transaction"))

        implementation(libs.androidx.navigation)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.kotlinx.coroutines)
        testImplementation(libs.kotlin.test)
    }
}

android {
    namespace = "me.ilker.balance_tracker.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

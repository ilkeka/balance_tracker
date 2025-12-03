import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        namespace = "me.ilker.balance_tracker.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    sourceSets {
        commonMain {
            dependencies {

            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.compose)
                implementation(libs.sqldelight.android.driver)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
    }

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
        implementation(projects.core)
        implementation(projects.resources)
        implementation(projects.transaction)

        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.navigation)
        implementation(libs.jetbrains.compose.navigationevent)
        implementation(libs.koin.compose)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.sqldelight.coroutines.extensions)
        implementation(libs.sqldelight.runtime)

        testImplementation(libs.kotlin.test)
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("me.ilker.balance_tracker")
        }
    }
}

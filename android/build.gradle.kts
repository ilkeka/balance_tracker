import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

kotlin {
    dependencies {
        implementation(projects.shared.app)
        implementation(projects.shared.common)
        implementation(projects.shared.resources)

        implementation(libs.androidx.activity.compose)
        implementation(libs.koin.compose)
        implementation(libs.jetbrains.compose.ui)
    }

    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
        }
    }
}

android {
    namespace = "me.ilker.balance_tracker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.ilker.balance_tracker"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 11
        versionName = "1.0.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            defaultConfig.applicationId = defaultConfig.applicationId.plus(".debug")
        }

        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}

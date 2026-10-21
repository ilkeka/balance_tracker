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
        implementation(libs.google.material)
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
        versionCode = 15
        versionName = "1.0.14"
    }
    testBuildType = "devDebug"
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        // The dev variants install alongside the main app (separate package and database), so a
        // test build can be used without replacing the existing installation.
        create("devDebug") {
            isDebuggable = true
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
        create("devRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // Local test builds need a key; use the debug key until a release keystore is configured.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}

// The default debug variant is replaced by devDebug, so Android Studio lists exactly devDebug,
// devRelease and release.
androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
        variantBuilder.enable = false
    }
}

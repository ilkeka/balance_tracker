import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

// Signing credentials live in the git-ignored local.properties so CI and fresh checkouts still
// build unsigned release APKs.
val localProperties = Properties().apply {
    // providers.fileContents keeps the configuration cache aware of local.properties changes.
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull
        ?.reader()
        ?.use { load(it) }
}
val releaseStoreFile = localProperties.getProperty("signing.storeFile")?.takeIf { it.isNotBlank() }

fun Properties.signingPassword(key: String): String? {
    val value = getProperty(key) ?: return null
    // Older local.properties stored credentials as "<keyring-account>@<password>".
    return if (value.startsWith("KEY_") && '@' in value) value.substringAfterLast('@') else value
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
        versionCode = 16
        versionName = "1.0.15"
    }
    testBuildType = "devDebug"
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        releaseStoreFile?.let { storeFilePath ->
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = localProperties.signingPassword("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.signingPassword("signing.keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    sourceSets {
        // Shared greyscale launcher icon for both dev variants.
        getByName("devDebug").res.srcDir("src/dev/res")
        getByName("devRelease").res.srcDir("src/dev/res")
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

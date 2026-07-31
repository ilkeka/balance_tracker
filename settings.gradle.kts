rootProject.name = "balance_tracker"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://dl.google.com/android/maven2/") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://dl.google.com/android/maven2/") }
    }
}

include(":android")
include(":desktop")
include(":server")
include(":web")

include(":shared:app")
include(":shared:auth")
include(":shared:common")
include(":shared:core")
include(":shared:home")
include(":shared:resources")
include(":shared:transaction")

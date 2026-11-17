rootProject.name = "balance_tracker"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        google()
    }
}

include(":android")
include(":desktop")
include(":web")

include(":shared:app")
include(":shared:common")
include(":shared:core")
include(":shared:home")
include(":shared:resources")
include(":shared:sync")
include(":shared:transaction")

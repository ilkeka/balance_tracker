import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

val serverUrl: String = run {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        val prefix = "server.url="
        f.readLines().find { it.startsWith(prefix) }?.substringAfter(prefix)?.takeIf { it.isNotEmpty() }
            ?: "http://localhost:9090"
    } else {
        "http://localhost:9090"
    }
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
        }

        namespace = "me.ilker.balance_tracker.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "BalanceTracker"
        }
    }

    @OptIn(ExperimentalSwiftExportDsl::class)
    swiftExport {
        // Set the root module name
        moduleName = "Shared"

        // Set the collapse rule
        // Removes package prefix from generated Swift code
        flattenPackage = "me.ilker.balance_tracker"

        // Provide compiler arguments to link tasks
        configure {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                enabled = false
            }
            commonWebpackConfig {
                outputFileName = "web.js"
            }
        }

        compilerOptions {
            freeCompilerArgs.add("-Xwasm-kclass-fqn")
        }

        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/serverUrl"))
        }

        androidMain.dependencies {
            implementation(libs.koin.compose)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
        }

        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.driver)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.webPackPlugin.get()))
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.shared.core)
        implementation(projects.shared.resources)

        implementation(libs.jetbrains.compose.component.resources)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.navigation)
        implementation(libs.jetbrains.compose.navigationevent)
        implementation(libs.jetbrains.compose.lifecycle.runtime)
        implementation(libs.koin.compose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.cio)
        implementation(libs.ktor.client.core)
        implementation(libs.sqldelight.coroutines.extensions)
        implementation(libs.sqldelight.runtime)

        testImplementation(libs.kotlin.test)
    }

    compilerOptions {
        freeCompilerArgs.add("-Xreturn-value-checker=full")
    }
}

sqldelight {
    databases {
        create("Database") {
            generateAsync = true
            packageName.set("me.ilker.balance_tracker")
        }
    }
}

val generateServerUrl = tasks.register("generateServerUrl") {
    val outputDir = layout.buildDirectory.dir("generated/serverUrl")
    outputs.dir(outputDir)
    inputs.property("serverUrl", serverUrl)
    val url = serverUrl
    doLast {
        val dir = outputDir.get().asFile.resolve("me/ilker/balance_tracker")
        dir.mkdirs()
        dir.resolve("Config.kt").writeText("""
            package me.ilker.balance_tracker

            const val serverUrl: String = "$url"
        """.trimIndent() + "\n")
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn(generateServerUrl)
}

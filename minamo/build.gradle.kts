import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

group = "xyz.quaver.minamo"
version = "1.0-SNAPSHOT"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting
        val androidMain by getting
        val desktopMain by getting
    }
}

android {
    namespace = "xyz.quaver.minamo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        ndk {
            abiFilters.apply {
                add("x86_64")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.create<Exec>("buildNative") {
    group = "build"

    inputs.dir("../native")
    outputs.dir("../native/build")

    workingDir = file("../native")
    commandLine("./build-native.sh")
}

tasks.create<Exec>("buildAndroidNative") {
    dependsOn("buildNative")
    group = "build"

    inputs.dir("../native")
    outputs.dirs(
        "../native/build-ndk-aarch64",
        "../native/build-ndk-armv7a",
        "../native/build-ndk-i686",
        "../native/build-ndk-x86_64"
    )

    workingDir = file("../native")
    commandLine("./build-ndk.sh")
}

afterEvaluate {
    tasks.named("preBuild") {
        dependsOn("buildNative")
        dependsOn("buildAndroidNative")
    }
}


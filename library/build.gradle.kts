import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
}

group = "xyz.quaver.subsampledimage"
version = "1.0-SNAPSHOT"

kotlin {
    androidTarget()
    jvm("desktop") {
        jvmToolchain(17)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                api(compose.preview)
            }
        }
        val desktopTest by getting {
        }
    }
}

android {
    compileSdk = 34
    namespace = "xyz.quaver.subsampledimage"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24

        ndk {
            abiFilters.apply {
                add("x86_64")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

tasks.withType<KotlinJvmTest> {
    systemProperties("java.library.path" to rootDir.resolve("native/build/fakeroot/lib").absolutePath)
    environment("LD_LIBRARY_PATH", rootDir.resolve("native/build/fakeroot/lib").absolutePath)
}

afterEvaluate {
    tasks.named("preBuild") {
        dependsOn("buildNative")
        dependsOn("buildAndroidNative")
    }
}


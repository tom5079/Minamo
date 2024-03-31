import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRun

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "xyz.quaver.subsampledimage"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":library"))
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SubSampledImage"
            packageVersion = "1.0.0"
        }
    }
}

@OptIn(InternalKotlinGradlePluginApi::class)
tasks.withType<KotlinJvmRun> {
    val libraryPath = rootDir.resolve("native/build/fakeroot/lib").absolutePath
    environment("LD_LIBRARY_PATH", libraryPath)
    systemProperty("java.library.path", libraryPath)
}
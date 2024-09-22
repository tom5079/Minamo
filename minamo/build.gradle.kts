import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
}

group = "xyz.quaver.minamo"
version = libs.versions.minamo.get()

val ossrhUsername: String by project
val ossrhPassword: String by project

kotlin {
    androidTarget {
        publishLibraryVariants("release")
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

publishing {
    repositories.maven {
        val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
        val snapshotRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")

        url = if (version.toString().endsWith("SNAPSHOT")) snapshotRepoUrl else releasesRepoUrl

        credentials {
            username = ossrhUsername
            password = ossrhPassword
        }
    }

    publications.withType<MavenPublication> {
        pom {
            name.set("minamo")
            description.set("Image processing for Kotlin Multiplatform")
            url.set("https://github.com/tom5079/minamo")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("tom5079")
                    email.set("7948651+tom5079@users.noreply.github.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/tom5079/minamo.git")
                url.set("https://github.com/tom5079/minamo")
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
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

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {

        }
        commonMain.dependencies {
            api(project(":minamo"))
            implementation(libs.kotlinx.coroutines)
        }
        desktopMain.dependencies {

        }
    }
}

android {
    namespace = "xyz.quaver.minamo.aqua"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {

    }
}

val ossrhUsername: String by project
val ossrhPassword: String by project

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
            name.set("minamo-aqua")
            description.set("ImageView for Kotlin Multiplatform")
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

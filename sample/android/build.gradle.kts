plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = "xyz.quaver.subsampledimage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))
    implementation("androidx.activity:activity-compose:1.8.2")
}

android {
    compileSdk = 34
    namespace = "xyz.quaver.subsampledimage.android"
    defaultConfig {
        applicationId = "xyz.quaver.subsampledimage.android"
        minSdk = 24
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
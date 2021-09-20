plugins {
    id("com.android.library")
    id("kotlin-android")
}

group = "xyz.quaver"
version = "0.0.1-alpha01"

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 16
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    kotlinOptions {
        moduleName = "xyz.quaver.graphics.subsampledimage"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.0.2"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.6.0")

    implementation("androidx.compose.ui:ui:1.0.2")
    implementation("androidx.compose.ui:ui-tooling:1.0.2")
    implementation("androidx.compose.foundation:foundation:1.0.2")
    implementation("androidx.compose.material:material:1.0.2")
    implementation("androidx.compose.material:material-icons-core:1.0.2")
    implementation("androidx.compose.material:material-icons-extended:1.0.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.0.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
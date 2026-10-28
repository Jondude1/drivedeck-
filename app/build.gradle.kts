plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version numbers come from CI so every push produces a strictly newer build.
val runNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.jondude1.drivedeck"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jondude1.drivedeck"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    // One permanent key, used for both debug and release builds, so updates
    // always install over the existing app. Sideloaded app, not Play Store.
    signingConfigs {
        create("drivedeck") {
            storeFile = rootProject.file("keystore/drivedeck.jks")
            storePassword = "drivedeck"
            keyAlias = "drivedeck"
            keyPassword = "drivedeck"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("drivedeck")
        }
        debug {
            signingConfig = signingConfigs.getByName("drivedeck")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
}

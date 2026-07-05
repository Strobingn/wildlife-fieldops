plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.strobingn.wildlifefieldops"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.strobingn.wildlifefieldops"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.1-rockstar-full"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // Previous deps + new
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // TensorFlow Lite / MediaPipe for on-device AI species ID
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("com.google.mlkit:vision-object-detection:17.0.0")
    // MediaPipe pose or custom model for species (stub ready to load)

    // Firebase/Supabase for realtime collab (Supabase preferred for your backend)
    implementation("io.github.jan.supabase:supabase-kt:2.0.0") // or Firebase

    // PDF advanced
    implementation("com.itextpdf:itext7-core:8.0.2") // For rich PDFs

    // Signature for e-sign
    // Canvas + save

    // Existing Room, WorkManager, Hilt, etc.
}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.strobingn.wildlifefieldops"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.strobingn.wildlifefieldops"
        minSdk = 29
        targetSdk = 35
        versionCode = 15
        versionName = "2.2.0-heavy-ai"

        val llmKey = System.getenv("XAI_API_KEY") ?: System.getenv("LLM_API_KEY") ?: ""
        val llmBase = System.getenv("LLM_BASE_URL") ?: "https://api.x.ai/v1"
        val llmModel = System.getenv("LLM_MODEL") ?: "grok-4.5"

        buildConfigField("String", "LLM_API_KEY", "\"${llmKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "LLM_BASE_URL", "\"$llmBase\"")
        buildConfigField("String", "LLM_MODEL", "\"$llmModel\"")

        val mapsKey = System.getenv("GOOGLE_MAPS_API_KEY") ?: ""
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsKey
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Kotlin & Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // --- AndroidX Core ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- Compose BOM ---
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // --- Hilt (Dependency Injection) ---
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // --- Room (Database) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- DataStore (Preferences) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Coil (Image Loading) ---
    implementation("io.coil-kt:coil-compose:2.5.0")

    // --- Ktor (HTTP Client) ---
    implementation("io.ktor:ktor-client-android:2.3.12")

    // --- Gson (JSON) ---
    implementation("com.google.code.gson:gson:2.10.1")

    // --- ML Kit (On-device AI) ---
    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.mlkit:object-detection:16.1.0")

    // --- AR Core ---
    implementation("com.google.ar:core:1.46.0")

    // --- Google Play Services (Maps) ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // --- Debug ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

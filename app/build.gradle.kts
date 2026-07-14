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
    }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.mlkit:object-detection:16.1.0")
    implementation("com.google.ar:core:1.46.0")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("com.google.code.gson:gson:2.10.1")
}

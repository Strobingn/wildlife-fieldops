# Wildlife FieldOps ProGuard Rules

# Keep Room entities
-keep class com.strobingn.wildlifefieldops.data.model.** { *; }
-keep class com.strobingn.wildlifefieldops.data.local.** { *; }

# Keep BuildConfig (but obfuscate field names)
-keep class com.strobingn.wildlifefieldops.BuildConfig { *; }

# Keep Hilt
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Keep Supabase serialization
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep Gson converters
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# iText 7 PDF generation
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.bouncycastlefips.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**

# General
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

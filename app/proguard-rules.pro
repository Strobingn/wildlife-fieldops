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

# kotlinx.serialization — canonical R8 rules (official README). Without these,
# R8 strips/renames the generated $$serializer classes and @Serializable DTOs
# fail to decode in release builds only ("screen opens but renders nothing").
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.strobingn.wildlifefieldops.**$$serializer { *; }
-keepclassmembers class com.strobingn.wildlifefieldops.** {
    *** Companion;
}
-keepclasseswithmembers class com.strobingn.wildlifefieldops.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App DTOs decoded at runtime (Supabase Postgrest + xAI JSON over ktor).
# data.remote was NOT covered by the data.model/data.local keeps above.
-keep class com.strobingn.wildlifefieldops.data.remote.** { *; }

# ktor client (Supabase transport)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Hilt / javax.inject (generated components and injection points)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep ARCore
-keep class com.google.ar.core.** { *; }
-keepclassmembers class * {
    @com.google.ar.core.** <methods>;
}
-dontwarn com.google.ar.core.**

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
# iText references desktop-only AWT/ImageIO APIs (never executed on Android)
-dontwarn java.awt.**
-dontwarn javax.imageio.**
# Jackson (used internally by iText JsonUtil)
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# General
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Wildlife FieldOps ProGuard Rules

# Keep entity classes for Room
-keep class com.strobingn.wildlifefieldops.data.model.** { *; }
-keepclassmembers class com.strobingn.wildlifefieldops.data.model.** { *; }

# Keep Room database and DAOs
-keep class com.strobingn.wildlifefieldops.data.local.** { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class * extends android.app.Application { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# Keep serialization for Supabase/Room
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# Google Maps
-keep class com.google.android.gms.** { *; }
-keep class com.google.maps.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# General Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.ContentProvider

# Don't warn about missing classes that are safely unavailable at runtime
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.servlet.**
-dontwarn org.jetbrains.kotlin.**

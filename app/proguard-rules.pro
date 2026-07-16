# VoxLyra ProGuard Rules
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Gemini API
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# JSON
-keep class org.json.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# App
-keep class com.voxlyra.** { *; }
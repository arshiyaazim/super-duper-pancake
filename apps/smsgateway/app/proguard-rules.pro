# SMS Gateway Proguard Rules
-keep class com.locationwhere.smsgateway.data.** { *; }
-keep class androidx.room.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Timber
-dontwarn org.jetbrains.annotations.**

# Keep data classes used in JSON parsing
-keep class org.json.** { *; }

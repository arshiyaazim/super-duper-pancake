# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses

# GSON
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.example.location_where.** { *; }

# Room
-keep class androidx.room.** { *; }

# OkHttp
-keep class okhttp3.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

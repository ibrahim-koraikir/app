# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================
# Optimization Settings
# ================================
# Enable aggressive optimization with 5 passes
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-repackageclasses ''

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ================================
# Remove Debug Logging
# ================================
# Remove all android.util.Log calls
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove Timber logging (if used)
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ================================
# Jetpack Compose
# ================================
# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep stable/immutable annotations
-keepattributes *Annotation*
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *

-dontwarn androidx.compose.**

# ================================
# Room Database
# ================================
# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback

# Keep all Room entities
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    *;
}

# Keep all Room DAOs
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Dao class * {
    *;
}

# Keep Room database implementations
-keep class com.entertainmentbrowser.data.local.database.** { *; }
-keep class com.entertainmentbrowser.data.local.entity.** { *; }
-keep class com.entertainmentbrowser.data.local.dao.** { *; }

-dontwarn androidx.room.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Fetch
-keep class com.tonyofrancis.fetch2.** { *; }
-dontwarn com.tonyofrancis.fetch2.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.entertainmentbrowser.**$$serializer { *; }
-keepclassmembers class com.entertainmentbrowser.** {
    *** Companion;
}
-keepclasseswithmembers class com.entertainmentbrowser.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ================================
# Kotlin & Coroutines
# ================================
# Keep Kotlin metadata
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Kotlin intrinsics
-keep class kotlin.jvm.internal.** { *; }

-dontwarn kotlin.**
-dontwarn kotlinx.**

# ================================
# WebView JavaScript Interface
# ================================
# Generic rule for any @JavascriptInterface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Specific rule for our WebView JavaScript bridge
-keep class com.entertainmentbrowser.presentation.webview.WebViewJsBridge {
    <init>(...);
}
-keepclassmembers class com.entertainmentbrowser.presentation.webview.WebViewJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# DataStore
-keep class androidx.datastore.*.** { *; }
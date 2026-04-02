# ==================== VenCA - DEX Protection Rules ====================
# MyAlbum v4.0.1 - MT Studio

# ==================== Core Protection ====================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Obfuscation settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ==================== Kotlin Metadata & Intrinsics (CRITICAL) ====================
# KEEP kotlin.jvm.internal - removing these causes IMMEDIATE crash
-keep class kotlin.jvm.internal.** { *; }
-keepclassmembers class kotlin.jvm.internal.** { *; }

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.internal.** { *; }
-dontwarn kotlinx.coroutines.**

# ==================== AndroidX ====================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ==================== Compose ====================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== Material3 ====================
-keep class com.google.android.material.** { *; }
-keepclassmembers class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ==================== Media3/ExoPlayer ====================
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ==================== Coil ====================
-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }
-dontwarn coil.**

# ==================== Navigation ====================
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ==================== DataStore ====================
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ==================== VenCA Security ====================
-keep class com.myalbum.app.security.VencaApplication { *; }
-keep class com.myalbum.app.security.Venca { *; }
-keepclassmembers class com.myalbum.app.security.Venca {
    public *;
}

# ==================== App Models ====================
-keep class com.myalbum.app.data.MediaItem { *; }
-keep class com.myalbum.app.data.AlbumInfo { *; }
-keep class com.myalbum.app.data.MediaStoreHelper$MediaType { *; }
-keep class com.myalbum.app.data.MediaStoreHelper$SortOrder { *; }

# ==================== App Enums ====================
-keepclassmembers enum com.myalbum.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ==================== ViewModels ====================
-keep class com.myalbum.app.viewmodel.** { *; }
-keepclassmembers class com.myalbum.app.viewmodel.** { *; }

# ==================== Navigation Routes ====================
-keep class com.myalbum.app.ui.navigation.Screen { *; }
-keepclassmembers class com.myalbum.app.ui.navigation.Screen$* { *; }

# ==================== Compose @Composable Functions ====================
# Keep all Composable functions in the app
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ==================== Serialization ====================
-keepclassmembers class * {
    ** serialization;
}

# ==================== Enum (Global) ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== Prevent Reflection ====================
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature

# ==================== Remove Logging in Release ====================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ==================== WebView (if used) ====================
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

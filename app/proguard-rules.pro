# ==================== VenCA - DEX Protection Rules ====================
# MyAlbum v4.0.0 - MT Studio

# ==================== Core Protection ====================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Obfuscation settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

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

# ==================== Kotlin Coroutines ====================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

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

# ==================== Models ====================
-keep class com.myalbum.app.data.MediaItem { *; }
-keep class com.myalbum.app.data.AlbumInfo { *; }

# ==================== Serialization ====================
-keepclassmembers class * {
    ** serialization;
}

# ==================== Enum ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== VenCA String Encryption ====================
-assumenosideeffects class kotlin.jvm.internal.** {
    public <methods>;
}

# ==================== Remove Logging in Release ====================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ==================== Prevent Reflection ====================
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature

# ==================== WebView (if used) ====================
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

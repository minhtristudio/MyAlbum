# ==================== MyAlbum v5.0.0 - ProGuard Rules ====================

# ==================== Core ====================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ==================== Kotlin (CRITICAL - do not remove) ====================
-keep class kotlin.jvm.internal.** { *; }
-keepclassmembers class kotlin.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepclassmembers class kotlinx.coroutines.internal.** { *; }
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

# ==================== App Models ====================
-keep class com.myalbum.app.data.MediaItem { *; }
-keep class com.myalbum.app.data.AlbumInfo { *; }
-keep class com.myalbum.app.data.MediaStoreHelper$MediaType { *; }
-keep class com.myalbum.app.data.MediaStoreHelper$SortOrder { *; }

# ==================== App ViewModels ====================
-keep class com.myalbum.app.viewmodel.** { *; }
-keepclassmembers class com.myalbum.app.viewmodel.** { *; }

# ==================== Navigation Routes ====================
-keep class com.myalbum.app.ui.navigation.Screen { *; }
-keepclassmembers class com.myalbum.app.ui.navigation.Screen$* { *; }

# ==================== @Composable Functions ====================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ==================== App Enums ====================
-keepclassmembers enum com.myalbum.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ==================== Global Enum ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== Reflection Safety ====================
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature

# ==================== Remove Logging in Release ====================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

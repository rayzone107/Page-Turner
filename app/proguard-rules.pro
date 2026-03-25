# Keep source file names and line numbers in stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit — keep service interface methods (accessed via java.lang.reflect.Proxy).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn kotlin.Unit

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi — generated adapters are compiled code; keep data class fields they reference.
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }
-keep @com.squareup.moshi.JsonClass class *
# KotlinJsonAdapterFactory uses reflection as a fallback for non-annotated types.
-keepnames class kotlin.reflect.jvm.internal.**
-dontwarn com.squareup.moshi.**

# Hilt — generated component classes.
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-dontwarn dagger.**

# Room — DAO and database implementations are generated at compile time; R8 handles them.
# Keep entity classes so Room can access their fields at runtime.
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization / reflection
-dontwarn kotlin.**
-keepattributes *Annotation*

# Add project specific ProGuard rules here.

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ============= CRITICAL: GSON @SerializedName RULES =============
# These rules are ESSENTIAL to fix the field name obfuscation issue

# Keep attributes needed for Gson
-keepattributes Signature
-keepattributes *Annotation*

# CRITICAL RULE: Keep fields annotated with @SerializedName from being obfuscated
# This allows R8 to obfuscate field names but use @SerializedName values for JSON
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep the @SerializedName annotation itself
-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============= NO MORE DEBUG LOGGING RULES NEEDED =============
# BuildConfig.DEBUG approach handles this automatically

# ============= KEEP IMPORTANT CLASSES =============
# Keep your data classes
-keep class com.bodepudi.vexnotebooktracker.network.** { *; }
-keep class com.bodepudi.vexnotebooktracker.data.** { *; }
-keep class com.bodepudi.vexnotebooktracker.models.** { *; }

# Keep Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# CRITICAL: Keep generic signatures for TypeToken (fixes the Dashboard parsing error)
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Additional safety rules for DashboardUrlRecord array parsing
-keep class com.bodepudi.vexnotebooktracker.network.DashboardUrlRecord { *; }
-keep class com.bodepudi.vexnotebooktracker.network.DashboardUrlRecord[] { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class **_HiltModules$*
-keep class **_HiltModules { *; }
-keep class **_MembersInjector
-keep class **_Factory
-keep class * extends dagger.hilt.android.AndroidEntryPoint
-keep @dagger.hilt.android.AndroidEntryPoint class * {
    <init>(...);
}

# Keep security classes
-keep class androidx.security.crypto.** { *; }
-keep class io.jsonwebtoken.** { *; }
-keep class com.auth0.android.jwt.** { *; }
-keep class com.bodepudi.vexnotebooktracker.security.** { *; }

# Keep API service interface
-keep interface com.bodepudi.vexnotebooktracker.network.VexTeamsApiService { *; }

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Kotlin Coroutines
-keep class kotlin.coroutines.Continuation

# Keep Android Architecture Components
-keep class androidx.lifecycle.** { *; }
-keep class androidx.databinding.** { *; }

# Keep Firebase (if using)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep click handlers
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Keep error and warning logs in production (for crash debugging)
-keep class android.util.Log {
    public static int e(...);
    public static int w(...);
}
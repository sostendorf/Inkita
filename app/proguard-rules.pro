# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Rules below are only consulted when the release build is run with
# -PminifyRelease=true. Without them R8 renames the Kavita API models and
# Moshi -- which matches JSON keys to Kotlin properties by reflection -- stops
# being able to parse any server response.
# ---------------------------------------------------------------------------

# Generic signatures and annotations Moshi and Retrofit read at runtime.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin reflection metadata: Moshi's Kotlin adapter needs it to find
# constructors, property names and default values.
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# The API models themselves, plus their synthetic constructors and the
# $Companion / DefaultImpls members Kotlin generates alongside them.
-keep class net.dom53.inkita.data.api.dto.** { *; }
-keep class net.dom53.inkita.domain.model.** { *; }
-keepclassmembers class net.dom53.inkita.data.api.dto.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class net.dom53.inkita.domain.model.** {
    <init>(...);
    <fields>;
}

# Anything Kotlin marks as serialized state, and enum valueOf/values which
# Moshi and Room both call reflectively.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class **JsonAdapter { *; }
-keepnames @com.squareup.moshi.JsonClass class *

# Retrofit: service interfaces are proxied, so their generic return types and
# annotations must survive.
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class net.dom53.inkita.data.api.** { *; }

# OkHttp / Okio platform-specific classes referenced but not always present.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# Room entities and generated DAO implementations.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# WorkManager workers are instantiated by name.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# The EPUB reader talks to the page through a JavaScript interface.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Compile-only annotations pulled in transitively by Tink (via
# androidx.security-crypto). They are not on the runtime classpath and nothing
# reads them at runtime, so R8 only needs to be told to stop warning.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**

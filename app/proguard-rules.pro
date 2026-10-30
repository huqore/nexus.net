# Proguard / R8 configuration for NEXUS.net

# Keep Gomobile / Libv2ray JNI native bindings and reflection
-keep class go.** { *; }
-keep class libv2ray.** { *; }
-dontwarn libv2ray.**
-dontwarn go.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep app data models for serialization/reflection
-keep class net.nexus.app.data.** { *; }
-keep class net.nexus.app.auth.** { *; }
-keep class net.nexus.app.update.** { *; }
-keep class net.nexus.app.core.** { *; }

# Keep AndroidX & Compose rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

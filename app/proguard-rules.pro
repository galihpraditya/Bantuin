# Proguard rules for Bantuin
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization & DTOs
-keep,includedescriptorclasses class id.bantuin.app.data.remote.dto.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Supabase & Ktor / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

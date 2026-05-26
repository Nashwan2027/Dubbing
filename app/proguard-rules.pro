# الحفاظ على نماذج البيانات
-keep class dev.nash.dubbing.data.model.** { *; }

# الحفاظ على خدمات التسجيل
-keep class dev.nash.dubbing.RecordingService { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# إزالة سجلات التصحيح
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Proguard rules for LeanType Handwriting Plugin

# Keep the entry point class, its constructor, and all public methods,
# as it is loaded dynamically by class name reflection.
-keep class helium314.keyboard.handwriting.plugin.HandwritingRecognizerImpl {
    public <init>();
    public <methods>;
}

-keep class helium314.keyboard.handwriting.plugin.NativeDigitalInkRecognitionException {
    *;
}

# Keep the interface methods to match the host app
-keep interface helium314.keyboard.latin.handwriting.HandwritingRecognizer {
    <methods>;
}
-keep interface helium314.keyboard.latin.handwriting.ModelDownloadListener {
    <methods>;
}

-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepnames class com.google.mlkit.** extends androidx.work.ListenableWorker

# Keep ML Kit components, JNI classes and Firebase/GMS dependencies
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-dontwarn com.google.**
-dontwarn androidx.**

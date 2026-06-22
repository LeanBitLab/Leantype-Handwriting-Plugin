# Proguard rules for LeanType Handwriting Plugin

# Keep the entry point class, its constructor, and all public methods,
# as it is loaded dynamically by class name reflection.
-keep class helium314.keyboard.handwriting.plugin.HandwritingRecognizerImpl {
    public <init>();
    public <methods>;
}

# Keep the interface methods to match the host app
-keep interface helium314.keyboard.latin.handwriting.HandwritingRecognizer {
    <methods>;
}
-keep interface helium314.keyboard.latin.handwriting.ModelDownloadListener {
    <methods>;
}

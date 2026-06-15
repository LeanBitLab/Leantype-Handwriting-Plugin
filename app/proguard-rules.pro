# Proguard rules for LeanType Handwriting Plugin

# Keep the entry point class and its public constructor from being obfuscated or removed,
# as it is loaded dynamically by class name reflection.
-keep class helium314.keyboard.handwriting.plugin.HandwritingRecognizerImpl {
    public <init>();
}

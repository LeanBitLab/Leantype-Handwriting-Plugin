# LeanType Handwriting Plugin

This is a dynamic plugin APK for **LeanType** keyboard that enables handwriting input support using **Google ML Kit Digital Ink Recognition**.

## How it works

LeanType keyboard is a free and open-source (FOSS) project licensed under GPLv3. To comply with FOSS guidelines and keep the core keyboard codebase free of proprietary dependencies, this plugin isolates the Google ML Kit SDK into a separate APK.

At runtime, LeanType loads this plugin dynamically via `DexClassLoader` if imported by the user in settings.

## Building the APK

To build the APK, run the following Gradle task:

```bash
./gradlew assembleRelease
```

The compiled APK will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if you configure signing).

## Installation

1. Copy the built APK to your Android device.
2. In LeanType Settings, go to **Libraries** > **Load handwriting plugin**.
3. Select the APK using the file picker.

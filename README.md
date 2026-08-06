# LeanType Handwriting Plugin

This is a dynamic plugin APK for **LeanType** keyboard that enables handwriting input support using **Google ML Kit Digital Ink Recognition**.

> [!IMPORTANT]
> - **Do NOT Install**: Do **not** install this APK as a standalone application on your device. It must only be loaded through LeanType settings.
> - **Compatibility**: This plugin works only with **LeanType StandardFull**.
> - **Offline Usage**: Handwriting recognition runs completely **offline** on your device. Internet connectivity is only required once when initially downloading language recognition models.

## How it works

LeanType keyboard is a free and open-source (FOSS) project licensed under GPLv3. To comply with FOSS guidelines and keep the core keyboard codebase free of proprietary dependencies, this plugin isolates the Google ML Kit SDK into a separate APK.

At runtime, LeanType loads this plugin dynamically via `DexClassLoader` when imported by the user in settings.

## Building the APK

To build the APK, run the following Gradle task:

```bash
./gradlew assembleRelease
```

The compiled APK will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if you configure signing).

## How to Use

1. Copy or download the `.apk` file to your Android device storage (**do not install it**).
2. Open LeanType keyboard settings.
3. Navigate to **Libraries** > **Load handwriting plugin**.
4. Select the `.apk` file using the file picker.
5. Download your required language model(s) when prompted (requires internet access for initial download; recognition afterwards is offline).


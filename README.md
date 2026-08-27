# LeanType Handwriting Plugin

A dynamic plugin for the [LeanType Keyboard](https://github.com/LeanBitLab/LeanType) enabling real-time on-device handwriting recognition powered by **Google ML Kit Digital Ink Recognition**.

---

## ✨ Features

- **✍️ 100% On-Device Recognition**: Fast, private character and word recognition directly on your device.
- **🌍 300+ Languages Supported**: Recognize Latin, Cyrillic, CJK, Indic, Arabic, and hundreds of regional scripts.
- **📦 In-App & Offline Model Manager**: Download language models directly in online builds, or use the browser download popup and multi-file/batch `.zip` importer on offline builds.
- **🔄 Universal Compatibility**: Supported across **all 4 LeanType flavors** (`Standard`, `Standard Full`, `Offline`, and `Offline Lite`).
- **🔌 Dynamic Isolated Loading**: Loaded dynamically via `DexClassLoader` with isolated native libraries and zero runtime footprint when inactive.

---

> [!IMPORTANT]
> - **Do NOT Install Directly**: Do **not** install this APK as a standalone application into Android OS. It must only be loaded internally through LeanType settings.
> - **Zero-Network Recognition**: Recognition runs 100% **offline** on your device. Internet connectivity is only used during initial model downloads on online builds.

---

## 📋 System Requirements

- **Operating System**: Android 6.0 (API 23) or higher
- **Supported CPU Architectures**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`
- **Host Keyboard**: [LeanType](https://github.com/LeanBitLab/LeanType) v4.1.0+ (All flavors)

---

## 🛠️ How it Works

To maintain a lean, pure FOSS core keyboard while giving users access to advanced handwriting capabilities, this plugin isolates the ML Kit SDK and native libraries (`libdigitalink_native.so`) into a separate dynamic package.

At runtime, LeanType loads this plugin dynamically via `PluginClassLoader` when handwriting input is invoked.

---

## 📥 Installation & Setup

### Option 1: In-App Downloader (Online Flavors)
1. Open LeanType **Settings → Handwriting** (or **Settings → Plugins**).
2. Tap **Download Plugin** to automatically fetch and activate the latest release APK.
3. Tap **Offline Handwriting Models** to download your desired language recognition packs.

### Option 2: Manual Loading (Offline & Offline Lite Flavors)
1. Download `handwriting_plugin.apk` from the [Latest Releases](https://github.com/LeanBitLab/Leantype-Handwriting-Plugin/releases/latest).
2. In LeanType, navigate to **Settings → Handwriting** (or **Settings → Plugins**).
3. Tap **Load handwriting plugin** and select the downloaded `.apk` file.
4. Tap **Offline Handwriting Models** to download models via browser or import downloaded `.zip` model archives.

---

## 🏗️ Building From Source

To compile the release APK from source:

```bash
./gradlew assembleRelease
```

The compiled APK will be generated at:
`app/build/outputs/apk/release/handwriting_plugin.apk`

---

## 📄 License

Licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).


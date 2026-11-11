# JARVIS Face Scanner
**Developer:** Ankit Gothwal  
**Target Device:** Infinix Hot 10 (MediaTek Helio G70)  
**Package:** `com.ankit.jarvisfacescanner`  
**License:** MIT

## Overview
JARVIS Face Scanner is an offline, standalone native Android biometric detection and recognition application. It processes frames entirely on-device using a bundled Google ML Kit Face Detector and MobileFaceNet TensorFlow Lite inference engine, comparing 192-dimensional embeddings via cosine similarity.

## Features
- **100% Offline Biometrics:** No cloud API, no server, no internet required for face recognition.
- **Dual Camera Input:** Switch between Infinix Hot 10 phone camera (CameraX) and TP-Link Tapo C200 IP camera (RTSP stream over local Wi-Fi).
- **Keystore Encryption:** Tapo camera credentials encrypted using Android Keystore (`EncryptedSharedPreferences`).
- **Windows Sample Import:** Directly import existing `known_faces/` folder from Windows as a ZIP file without exposing photos publicly.
- **JARVIS HUD Interface:** Sci-Fi dark navy/cyan reticle overlay with real-time latency and confidence telemetry.

## GitHub Actions Cloud Build
This repository includes `.github/workflows/build.yml`. Every push automatically builds `app-debug.apk` and uploads it to GitHub Actions Artifacts—no local Android Studio or emulator required.

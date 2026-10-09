# O-Beat — Fast, Privacy-First Music Player for Android

<img width="1024" height="1024" alt="image" src="https://github.com/user-attachments/assets/16cdbdb3-b3ac-4523-aa0d-3a97527ab3c9" />


O-Beat blends your **local music library** with your **YouTube Music** world.  
It’s lightweight, beautiful, and respects your privacy: **no analytics, no trackers**.

<p align="left">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img alt="License: GPL-3.0" src="https://img.shields.io/badge/License-GPL--3.0-blue.svg"></a>
  <img alt="Min SDK" src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-brightgreen.svg">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-✅-7957d5.svg">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-✅-2ea043.svg">
</p>

---

## Table of Contents

Maintenance guides: [native design](docs/DESIGN.md),
[YouTube Music and OuterTune comparison](docs/YOUTUBE_MUSIC.md),
[signed GitHub releases and update notifications](docs/RELEASING.md).
Release evidence: [verification results](docs/VERIFICATION.md) and
[unused-file cleanup](docs/CLEANUP.md).

- [Highlights](#highlights)
- [Screenshots](#screenshots)
- [Download & Install](#download--install)
- [Compatibility (CPU ABI)](#compatibility-cpu-abi)
- [Build From Source](#build-from-source)
  - [Flavors](#flavors)
  - [Signing (Release)](#signing-release)
- [Using O-Beat](#using-o-beat)
- [Troubleshooting](#troubleshooting)
- [Privacy](#privacy)
- [Roadmap](#roadmap)
- [Acknowledgments](#acknowledgments)
- [License](#license)
- [Sponsor / Support](#sponsor--support)

---

## Highlights

- ⚡ **Fast & lightweight** — modern Kotlin + Jetpack Compose UI.
- 🔒 **Privacy-first** — no analytics, no trackers; your data stays on your device.
- 🎧 **Local + Cloud** — play MP3/FLAC/OGG/… and access your YouTube Music library.
- 📥 **Smart downloads** — auto-download liked tracks, external storage support, M3U import/export.
- 🧠 **Power tools** — queues, playlist sync (RO/RW), rich filters & sorting.
- 📝 **Lyrics** — local/online sources, karaoke word-by-word mode, multi-line display.
- 🎨 **Beautiful player** — color-adaptive theme, gradient/blur backdrops, tablet-optimized layout.
- 🚗 **Android Auto** — control playback safely on the road.
- 🧩 **Optional FFmpeg/TagLib** — robust metadata extraction for local files.

---

## Screenshots

> _Add a few images from `app/src/main/play/listings` or your device._

docs/
├─ screenshots/
│ ├─ now_playing.png
│ ├─ library.png
│ └─ settings.png

markdown
Copy code

---

## Download & Install

- **Recommended:** grab the latest **APK** from **GitHub Releases** and install on Android **7.0+ (API 24+)**.
- On **Xiaomi/MIUI**, enable:
  - **Settings → Additional settings → Developer options → Install via USB** (if installing with ADB/Android Studio),
  - **Settings → Privacy → Special access → Install unknown apps** for the app you use to install (e.g., Files/SAI).

**ADB install (single APK):**
```bash
adb install -r O-Beat-*.apk
ADB install-multiple (split APKs):
```
```bash
Copy code
adb install-multiple -r O-Beat-*.apk O-Beat-*.dm
```
Compatibility (CPU ABI)
Your device CPU must match the native libraries in the APK:

arm64-v8a → most modern devices (64-bit)

armeabi-v7a → older 32-bit devices (e.g., Redmi 9C)

If you see “Package doesn’t contain native code compatible with this device’s CPU”, install an APK that includes your ABI.
For legacy/32-bit phones, build/install a v7a-only APK (see below).

Build From Source
Requires: Android Studio (latest), JDK 21, Android SDK/NDK, and Git.

Before the first Gradle build, restore the ignored `ffMetadataEx` module and
its pinned FFmpeg libraries as described in [the native build setup](docs/RELEASING.md#github-actions).
Cloning this repository alone does not supply those files. Use SDK 36,
Build Tools 36.0.0, NDK 29.0.13113456 and CMake 3.31.6.

Quick Start (Windows PowerShell)
powershell
Copy code
# Clone
git clone https://github.com/OmarElkhali/O-Beat.git
cd O-Beat

# Debug build (easier for testing)
.\gradlew :app:assembleFullDebug

# Universal release (arm64 + armeabi-v7a)
.\gradlew :app:assembleFullRelease -x lint

# 32-bit only (for older devices like Redmi 9C)
.\gradlew :app:assembleFullRelease -PabiFilters=armeabi-v7a -x lint
Flavors
core — default, smaller

full — includes extra modules (FFmpeg extensions, etc.)

Pick tasks like assembleCoreRelease or assembleFullRelease.

Signing (Release)
For an update to an existing O-Beat installation, keep its original keystore
and application ID. A newly generated key cannot update an APK signed with
the historical key. See [the release guide](docs/RELEASING.md).

Create a keystore (use the same password for store & key to avoid “Cannot recover key”):

powershell
Copy code
# Create folder
mkdir keystore -Force

# Generate keystore (JKS or PKCS12)
keytool -genkeypair -v `
  -keystore app\keystore\ot-release.jks `
  -storetype JKS `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias ot
# When prompted for "key password", press ENTER to reuse the keystore password
keystore.properties at repo root:

ini
Copy code
storeFile=keystore/ot-release.jks
storePassword=YOUR_PASSWORD
keyAlias=ot
keyPassword=YOUR_PASSWORD
Now build:

powershell
Copy code
.\gradlew :app:assembleFullRelease -x lint
Using O-Beat
Local library: choose folders to scan (MP3/OGG/FLAC/…).

YouTube Music: sign in (advanced token supported) to sync library/playlists.

Downloads: set internal/external folder; migrate or rescan from Settings.

Lyrics: prefer local files or online providers; enable karaoke mode if supported.

Player UI: pick background style (theme/gradient/blur), tablet UI, slim navbar, etc.

Sync modes:

RO: view remote only,

RW: modify remote playlists,

Conflict resolution (add-only or overwrite).

Troubleshooting
“App not installed — incompatible native code”
Install an APK that contains your device’s ABI. For older/32-bit devices, build with:

powershell
Copy code
.\gradlew :app:assembleFullRelease -PabiFilters=armeabi-v7a -x lint
INSTALL_FAILED_USER_RESTRICTED (MIUI / unknown sources)
Allow install permissions:

Settings → Privacy → Special access → Install unknown apps (for the installer you use).

If installing via Android Studio/ADB: Developer options → Install via USB.

Retry the install.

“Cannot recover key” during signing
The keystore password and key password differ.
Regenerate the keystore or change the key password to match:

powershell
Copy code
keytool -keypasswd -alias ot -keystore app\keystore\ot-release.jks
Privacy
No analytics. No trackers.

Your local library never leaves your device.

YouTube Music access uses your own account/token.

Not affiliated with Google/YouTube.

Roadmap
More lyric providers and formats

Smarter smart-downloads rules

Widgets and Wear OS companion

More granular backup/restore

Acknowledgments
Based on great open-source components (Jetpack, Media3, TagLib, optional FFmpeg integrations, etc.).

Huge thanks to the broader Android OSS community.

License
GPL-3.0 — see LICENSE.

Sponsor / Support
If you enjoy O-Beat and want to support development:

➡︎ https://paypal.me/omarelkhali

Thank you! 🙌

markdown
Copy code

**Tips**
- Put this content into `README.md` at the repo root.
- Add screenshots to `docs/screenshots/` and reference them in the README.
- If you later need a French version, create `README.fr.md` and link both at the top.
::contentReference[oaicite:0]{index=0}
<img width="1024" height="1536" alt="image" src="https://github.com/user-attachments/assets/3918ffa4-cb43-4ba9-ae97-6acfc98ff176" />


# O-Beat v0.9.3.1 - APK Downloads

## 📥 Quelle version télécharger?

### Option 1: Universal (Recommandé)
**`O-Beat-v0.9.3.1-universal.apk`** (7.09 MB)
- ✅ Fonctionne sur **TOUS** les appareils Android
- Contient toutes les architectures
- **Taille**: Plus volumineux mais compatible avec tout

### Option 2: APK Spécifique (Plus léger)
Choisissez selon votre appareil:

| Architecture | Fichier | Taille | Appareils |
|--------------|---------|--------|-----------|
| **arm64-v8a** | `O-Beat-v0.9.3.1-arm64-v8a.apk` | 7 MB | 📱 La plupart des smartphones modernes (2018+) |
| **armeabi-v7a** | `O-Beat-v0.9.3.1-armeabi-v7a.apk` | 5.97 MB | 📱 Appareils Android 32-bit plus anciens |
| **x86_64** | `O-Beat-v0.9.3.1-x86_64.apk` | 5.97 MB | 💻 Emulateurs Android 64-bit |
| **x86** | `O-Beat-v0.9.3.1-x86.apk` | 5.97 MB | 💻 Emulateurs Android 32-bit |

## 🔍 Comment vérifier votre architecture?

### Méthode 1: Via une app
1. Installez **CPU-Z** ou **AIDA64** depuis le Play Store
2. Regardez la section "CPU" → Architecture

### Méthode 2: Via ADB
```bash
adb shell getprop ro.product.cpu.abi
```

## 📲 Installation

### Via votre téléphone:
1. Téléchargez l'APK
2. Ouvrez le fichier
3. Autorisez l'installation depuis des sources inconnues si demandé
4. Installez

### Via ADB:
```bash
adb install -r O-Beat-v0.9.3.1-universal.apk
```

## ⚠️ Compatibilité
- **Android minimum**: 7.0 (API 24)
- **Android recommandé**: 10.0+ (API 29+)

## 🔒 Sécurité
- **Pas de trackers**
- **Pas d'analytics**
- **Open Source**: [GitHub](https://github.com/OmarElkhali/O-Beat)
- **Licence**: GPL-3.0

## 📝 Changelog v0.9.3.1

### ✅ Corrections YouTube API
- Mise à jour de TOUS les clients YouTube API (Mars 2025)
  - WEB: 2.20250312.04.00
  - WEB_REMIX: 1.20250310.01.00
  - ANDROID_VR: 1.61.48 (Oculus Quest 3)
  - IOS: 20.10.4 (iPhone16,2)
- Correction de l'Authorization header (3 hash SAPISID)
- Structure PlayerBody synchronisée avec OuterTune official
- **Playback YouTube Music fonctionne maintenant SANS compte!** 🎉

### ✅ Améliorations générales
- PipePipeExtractor mis à jour (version 4240401)
- Meilleure gestion des erreurs de playback
- Support complet PoToken (pour clients web)

---

**Besoin d'aide?** Ouvrez une [issue sur GitHub](https://github.com/OmarElkhali/O-Beat/issues)

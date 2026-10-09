# Publier une mise à jour O-Beat

## Ce que voit la personne qui écoute

En production, O-Beat vérifie les releases publiques de `OmarElkhali/O-Beat`
à l'ouverture, au plus une fois toutes les 24 heures, puis une fois par jour
en arrière-plan lorsque le réseau est disponible. Les vérifications automatiques
sont désactivées dans la variante debug.
Android décide du moment d'exécution du travail périodique : ce n'est pas une
notification push instantanée. Un dialogue à l'ouverture propose également
de télécharger la nouvelle version, même si les notifications sont refusées.
Les mises à jour automatiques se règlent dans
Paramètres > À propos, où une vérification manuelle reste possible.
Sur Android 13 et versions ultérieures, autoriser les notifications pour
recevoir l'alerte système ; le dialogue dans l'application reste disponible.

La notification et le bouton de téléchargement ouvrent directement l'APK
GitHub compatible avec la variante installée (`core` ou `full`) et
l'architecture du téléphone, avec un APK universel en repli. Le navigateur
télécharge le fichier puis Android demande de confirmer son installation.
L'application ne s'installe pas sans intervention de l'utilisateur.

## Préparer la version

1. Augmenter `versionCode` et `versionName` dans `app/build.gradle.kts`.
   La refonte actuelle utilise `69` et `0.9.4`.
2. Conserver `applicationId = "com.obeat.ocompany"` et la clé de signature
   historique. Un APK `.debug` est une application séparée et ne remplace
   jamais la version de production.
3. Exécuter les tests et construire les APK signés :

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
   .\gradlew.bat :app:testCoreDebugUnitTest :innertube:test :app:assembleFullRelease
   ```

4. Vérifier l'identité avec `aapt dump badging` et la signature avec
   `apksigner verify --print-certs`, puis essayer `adb install -r` sur
   l'ancienne version sans la désinstaller.

## GitHub Actions

Le workflow `.github/workflows/build_ot_release.yml` se lance manuellement
depuis Actions. Le tag préparé est `v<versionName>`, par exemple `v0.9.4`.
Il teste, construit les deux variantes signées et prépare une release brouillon.
Le push d'un tag ne relance pas cette construction : un APK signé et vérifié
localement peut ainsi être publié sans être remplacé par une construction CI.
Il nécessite les secrets GitHub `KEYSTORE` et `KEYSTORE_PROPERTIES` ; le
fichier désigné par `storeFile` dans ces propriétés doit correspondre à
`test.jks` relativement au module `app` (habituellement `../test.jks`).
Ne jamais committer ou exposer les mots de passe ou la clé privée.
Ces secrets ne sont pas configurés par cette intervention. La publication
0.9.4 utilise les APK signés et vérifiés localement.

La CI installe explicitement Android SDK 36, Build Tools 36.0.0,
NDK 29.0.13113456 et CMake 3.31.6, puis récupère les sources natives avant
la première configuration Gradle. `ffMetadataEx` est ignoré dans ce dépôt
et son fichier `.git` local référence des métadonnées absentes ; la CI
ne dépend donc pas de ce dossier ni du checkout récursif des sous-modules.
Elle récupère le [commit officiel compatible
`5374cd8b5eaf603c09ea8199aaf11311a3e60254`](https://github.com/OuterTune/ffMetadataEx/tree/5374cd8b5eaf603c09ea8199aaf11311a3e60254).
Son module 0.4.0 conserve les classes et signatures JNI utilisées par O-Beat,
avec les mêmes versions SDK/JDK/NDK/CMake : aucun remplacement de fichier
Gradle n'est nécessaire.

La configuration du module local a également été restaurée : le chemin
CMake `src/main/cpp/CMakeLists.txt` est explicite, les déclarations CMake
dupliquées ont été supprimées et aucune restriction à `arm64-v8a` ne bloque
les quatre ABI. Ce module étant ignoré par Git, conserver cette configuration
ou récupérer celle du commit fixé avant une reconstruction locale. Il faut
aussi les bibliothèques et les headers FFmpeg correspondants pour chaque ABI
dans `ffMetadataEx/ffmpeg-android-maker/output/lib` et `output/include`.

Les bibliothèques FFmpeg sont fixées au [commit
`720abaf0f5707b5e9a4a78a2167ccc70ce8a0807`](https://github.com/mikooomich/ffmpeg-android-maker-prebuilt/tree/720abaf0f5707b5e9a4a78a2167ccc70ce8a0807)
de la branche `audio`, avec FFmpeg 7 (`libavcodec` 61), plutôt que suivre
une branche susceptible de passer à une autre version majeure.
Les commandes CI utilisent `bash gradlew` pour fonctionner même lorsque
le fichier `gradlew` n'a pas de permission d'exécution après le checkout.
Ce workflow doit encore être exécuté et vérifié sur GitHub Actions.

Les fichiers produits gardent le nom suivant :

```text
O-Beat-0.9.4-full-universal-release-69.apk
O-Beat-0.9.4-core-arm64-v8a-release-69.apk
```

Après inspection des APK et des notes, publier le brouillon sur
[GitHub Releases](https://github.com/OmarElkhali/O-Beat/releases).
Un brouillon, une prerelease ou un simple APK dans les artifacts Actions ne
déclenche pas d'alerte dans l'application. Ne pas renommer les fichiers APK :
le nom permet de vérifier la variante et le code de version.

Le nouveau code doit d'abord être installé : les anciennes versions qui
consultent encore OuterTune ne peuvent pas détecter les releases O-Beat.

Références : [API des releases GitHub](https://docs.github.com/en/rest/releases/releases),
[publication d'une release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository).

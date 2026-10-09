# Vérification O-Beat 0.9.4

Contrôles effectués les 8 et 9 octobre 2026. Les modifications déjà présentes
dans le checkout ont été conservées. Cette page décrit les contrôles locaux
précédant la publication ; aucune clé privée n'a été envoyée à GitHub pendant
cette intervention. Le nettoyage est décrit dans [CLEANUP.md](CLEANUP.md).

## APK livré

- Variantes `full` et `core`, universelles, version `0.9.4`, code `69`.
- Application `com.obeat.ocompany`, identique à la version 68.
- Signature vérifiée par `apksigner`, identique à l'APK 68 :
  `ac58c9adeb1c7c1ec1b1a58eed1dc9759f62cbd19e3914cb570765cb69634ab4`.
- SHA-256 de l'APK `full` après nettoyage :
  `71d28313e52badd95a1a6ed7b2b4cb3e496b49e8efb7f74b8267f867679c0852`.
- SHA-256 de l'APK `core` après nettoyage :
  `a04e944e19564545717268a1511a2c8b2268023155c949ef24c2c9d05f9b4a2f`.
- Tailles : `full` 22 018 383 octets ; `core` 7 157 171 octets.
- Bibliothèques natives présentes pour `arm64-v8a`, `armeabi-v7a`, `x86`
  et `x86_64` ; contrôle `zipalign -c -P 16 4` réussi.
- Installation `adb install -r` réussie sur l'ancienne version 68 dans
  l'émulateur isolé, sans désinstallation. L'accueil a conservé l'état
  d'onboarding. Les reconstructions suivantes du code 69 ont également été
  installées en place.

## Compilation et contrôles statiques

JDK 21, SDK 36, NDK 29.0.13113456, CMake 3.31.6. Le module natif local
ignoré par Git a été restauré avec les sources et bibliothèques fixées dans
[RELEASING.md](RELEASING.md).

```powershell
.\gradlew.bat :app:assembleFullDebug :app:testFullDebugUnitTest :innertube:test --no-daemon --max-workers=1
.\gradlew.bat :app:assembleFullRelease :innertube:test :app:lintFullRelease --no-daemon --max-workers=1
.\gradlew.bat :app:assembleFullRelease :app:lintFullRelease --no-daemon --max-workers=1
```

Ces trois exécutions ont réussi, respectivement en 9 min 40 s, 15 min et
15 min 12 s. La dernière inclut la correction de conservation du `MenuState`.
Les tâches inchangées ont utilisé les résultats Gradle déjà calculés.

Après nettoyage, la validation des deux variantes a également réussi en
35 min 37 s, avec 1 020 tâches dont 745 exécutées et 275 réutilisées :

```powershell
.\gradlew.bat :app:assembleFullRelease :app:assembleCoreRelease :app:testFullDebugUnitTest :app:testCoreDebugUnitTest :innertube:test :app:lintFullRelease :app:lintCoreRelease --no-daemon --max-workers=1 --console=plain
```

Les rapports JUnit comptent 6 tests distincts réussis : 3 pour les releases
GitHub, 2 pour les résultats annulables et 1 pour le parsing de recherche.
Les 3 tests GitHub ont été exécutés dans les variantes core/debug et
full/debug, soit 9 exécutions réussies au total. Les 13 tests réseau
`YouTubeTest` sont ignorés par leur annotation ; ils ne sont pas des tests
live réussis. Les six tests des variantes app ont été réexécutés le 9 octobre ;
les trois résultats InnerTube inchangés ont été réutilisés par Gradle.
Après nettoyage, le lint rapporte pour chacune des variantes 0 erreur,
125 avertissements et 2 indications, contre 200 avertissements auparavant.
Le contrôle de whitespace de tous les changements passe, y compris les DAOs
dont les espaces finaux ont été nettoyés.

## Lecture et téléchargement sur Android

L'AVD isolé `OBeat_QA_20261008` utilise Android API 35, sans compte YouTube.
La piste exacte est « Instant Crush (feat. Julian Casablancas) »,
identifiant `khnokW3Mw24`, durée du flux 5:37.

Le transport final utilise HTTP/1.1 pour les flux, les en-têtes du client
validé et un resolver sous les caches. Les longueurs et plages restent
celles demandées par Media3. Avec ce correctif, dix relevés automatiques
montrent une progression de 0:23 à 3:08, tous en lecture et sans erreur ;
la même écoute a ensuite été observée jusqu'à 4:47 sans 403. Une recherche
de position vers l'arrière suivie d'une reprise a progressé de 4:22 à 4:58,
puis a été mise en pause à 5:00 sans erreur.

La reconstruction finale conserve ce code de transport. Le menu Options
du titre, qui ne s'ouvrait pas lors du contrôle précédent, s'ouvre désormais.
Le téléchargement de la même piste est passé de « Téléchargement » à
« Supprimer le téléchargement », état terminé. La lecture hors ligne est
contrôlée séparément après fermeture de l'application, activation du mode
avion et désactivation du Wi-Fi : démarrage à 0:03, déplacement dans le morceau,
puis progression de 4:06 à 4:41 en trois relevés, tous en lecture et sans
erreur. Le réseau a ensuite été réactivé sur l'AVD isolé.
Ces contrôles de lecture ont précédé le nettoyage des fichiers et ressources
inutilisés. Le code de transport est identique dans les APK nettoyés ; leurs
signatures, identités et architectures ont été vérifiées séparément.

## Interface et mises à jour

Accueil, recherche et suggestions, bibliothèque de titres, lecteur,
réglages et À propos ont été ouverts sur appareil. Des contrôles en français,
clair/sombre et police agrandie ont été effectués. Les textes français
réparés s'affichent correctement. La pochette de la piste testée reste floue :
la netteté de toutes les images n'est pas validée.

Avant publication, la vérification manuelle dans À propos indiquait correctement
qu'aucune release publique n'était disponible. Le filtrage de version, de variante,
d'architecture et des URL est testé. Le parcours positif notification →
téléchargement d'une nouvelle release publique reste à vérifier après
publication ; aucun workflow GitHub Actions n'a été exécuté à distance.

## Limites du contrôle

Ces résultats couvrent les parcours ci-dessus, pas toutes les pistes, comptes,
régions, versions Android, fonctions Android Auto ou gestes TalkBack.
La fin complète de la piste et les téléchargements SAF externes ne sont pas
validés. Un premier lancement de l'AVD sans connexion a nécessité un
redémarrage de l'application après reconnexion ; la récupération sans
redémarrage de ce premier lancement n'est pas validée.

Les journaux, relevés et captures locaux sont sous `tmp/redesign/`.

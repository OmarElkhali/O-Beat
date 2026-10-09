# Nettoyage avant publication 0.9.4

Le 9 octobre 2026, les références dans le code, les XML et la configuration
Gradle ont été vérifiées avant suppression.

- Trois sauvegardes inutilisées : deux fichiers Gradle `.bak` et un ancien
  `AndroidManifest.xml.bak`.
- Cinq APK 0.9.3.1 retirés des sources ; les binaires actuels sont distribués
  par GitHub Releases.
- Deux fichiers Kotlin sans référence : `EmptyQueue.kt` et
  `ComposeDebugUtils.kt`. R8 les éliminait déjà de l'APK.
- Quatre icônes sans référence : `buymeacoffee`, `library_music`, `radio`
  et `replay`.
- Soixante-dix clés de chaînes inutilisées, soit 2 469 définitions dans
  69 fichiers XML de langue. Les entrées restantes ont été comparées avant
  et après modification ; leurs textes et attributs sont inchangés.
- L'ancien `.gitmodules` retiré : l'index ne contient aucun gitlink.
  `media` et `taglib` sont des snapshots suivis dans les sources ;
  `ffMetadataEx` est récupéré avec le commit fixé dans les workflows.
- Deux fichiers de signature retirés du suivi Git, conservés localement
  et ignorés. Leur présence dans l'historique antérieur n'est pas supprimée.

Les schémas de migration Room, licences, fixtures de tests, profils de
clients YouTube, polices, ressources de langue et modules natifs nécessaires
sont conservés. Les exemples Media3 restent déclarés par les fichiers Gradle
du module fourni ; supprimer leurs dossiers casserait cette configuration.
Les alias d'icônes de notification Media3 restent également nécessaires,
même lorsqu'ils sont signalés comme inutilisés par le lint de l'application.

Le contrôle automatique d'exécution a refusé la suppression du dossier
temporaire `tmp/redesign/qa-avd` avec le motif « blocked by policy ».
Ce dossier ignoré n'est pas envoyé à GitHub.

La compilation des deux variantes, les tests, le lint et la vérification
des APK signés sont consignés dans [VERIFICATION.md](VERIFICATION.md).

# Maintenir YouTube Music dans O-Beat

Le module `innertube` porte l'intégration YouTube Music. L'application utilise ses méthodes `YouTube.*` et garde la résolution des flux audio dans `YTPlayerUtils`. Il n'est pas nécessaire de modifier les écrans pour changer une version de client ou un endpoint.

## Où modifier

| Besoin | Fichier |
| --- | --- |
| Version, nom, identifiant et User-Agent des clients, URL de l'API | `innertube/src/main/java/com/zionhuang/innertube/models/YouTubeClient.kt` |
| Champs de contexte : appareil, langue, région, session | `innertube/src/main/java/com/zionhuang/innertube/models/Context.kt` |
| Requêtes HTTP, cookies, authentification et corps envoyés | `innertube/src/main/java/com/zionhuang/innertube/InnerTube.kt` et `models/body/` |
| Recherche, accueil, bibliothèque, playlists et parsing | `innertube/src/main/java/com/zionhuang/innertube/YouTube.kt`, `models/response/` et `pages/` |
| Déchiffrement des signatures et paramètre de limitation du débit | `innertube/src/main/java/com/zionhuang/innertube/NewPipe.kt` |
| Client principal, ordre des secours, qualité et validation du flux | `app/src/main/java/com/dd3boh/outertune/utils/YTPlayerUtils.kt` |
| PoTokens des clients web | `app/src/main/java/com/dd3boh/outertune/utils/potoken/` |
| Consommation du flux, cache, lecture et téléchargements | `app/src/main/java/com/dd3boh/outertune/playback/MusicService.kt` et `DownloadUtil.kt` |
| Version de NewPipeExtractor | `gradle/libs.versions.toml` (`newpipe-extractor`) |

Les profils restent déclarés à un seul endroit dans `YouTubeClient.kt`. Leur ordre de lecture reste dans `MAIN_CLIENT` et `STREAM_FALLBACK_CLIENTS`, au début de `YTPlayerUtils.kt`. Le client d'un flux validé est conservé dans `PlaybackData.streamClient` : réutiliser ses en-têtes via `streamRequestHeaders` pour la lecture et les téléchargements.

Dans `MusicService`, la résolution des URL et des en-têtes se trouve sous les deux caches, juste avant `DefaultDataSource`. Chaque ouverture réelle du réseau, y compris un trou dans le cache, passe par `resolveStreamDataSpec`. Le resolver extérieur s'occupe seulement des fichiers locaux, des téléchargements SAF et de la récupération des métadonnées. Il conserve la position et la longueur demandées : une limite artificielle dans un resolver ferait passer la taille d'un bloc pour celle du morceau entier auprès de l'extracteur Media3. Les téléchargements historiques entièrement présents dans le cache peuvent être lus sans résolution réseau du flux.

Le diagnostic HTTP n'affiche que les réponses Googlevideo non réussies, avec le code, la plage demandée, la catégorie de User-Agent et la présence d'Origin/Referer. Ne pas ajouter les URL signées, les cookies ou les PoTokens aux logs.

Les transports HTTP de lecture, de validation du flux et de téléchargement dans `MusicService`, `YTPlayerUtils` et `DownloadUtil` utilisent HTTP/1.1, le protocole du probe anonyme réussi sur ce poste. Les plages sont laissées à Media3, sans en-tête `Range` supplémentaire ni modification de longueur. Ne pas mettre `Range` dans `streamRequestHeaders`, car Media3 ajoute lui-même ses plages pour les recherches de position.

Probe anonyme du 8 octobre 2026, sans cookies, avec un nouveau visitorData public et le profil VisionOS : le flux audio itag 251 déclarait 6 126 038 octets. Sur la même URL, `bytes=0-0` renvoyait HTTP 206 et annonçait 1 octet ; `bytes=0-` renvoyait HTTP 206 et annonçait les 6 126 038 octets déclarés ; `bytes=524288-` renvoyait HTTP 206 et annonçait les 5 601 750 octets restants. Une requête sans plage fonctionnait sur ce poste en HTTP/1.1, alors que le diagnostic Android avait montré HTTP 403 sans plage et avec `bytes=0-`. Le probe valide les en-têtes de longueur complète et la recherche de position. Il ne prouve pas que le protocole explique seul cette différence ; le contrôle Android ci-dessous complète ces observations.

Contrôle vérifié de l'APK de production version 69, sans compte, sur un émulateur API 35 isolé : « Instant Crush » (`khnokW3Mw24`) a fourni dix relevés entre `0:23` et `3:08`, tous en lecture et sans erreur. L'observation manuelle de la lecture continue a ensuite atteint `4:47`. Après une recherche de position vers l'arrière, la lecture a repris de `4:22` à `4:58`, puis a été mise en pause à `5:00`, sans HTTP 403 observé. HTTP/1.1 et les en-têtes du client VisionOS ont donc été validés ensemble dans ce parcours Android ; ce résultat ne démontre pas une cause unique pour tous les HTTP 403. Dans la reconstruction finale, le téléchargement a terminé puis la même piste a été relue après fermeture de l'application, avec le mode avion activé et le Wi-Fi désactivé : démarrage à 0:03, recherche de position puis progression de 4:06 à 4:41 sans erreur. La capture suivante montre une pause à 5:18. La fin complète du morceau et les téléchargements SAF externes ne sont pas validés. Voir VERIFICATION.md pour les limites du contrôle.

Le parseur de recherche accepte aussi les résultats `itemSectionRenderer` et les cartes sans en-tête observés en octobre 2026. Une durée telle que `5:38` ne doit pas devenir un nom d'artiste ; lorsque YouTube omet l'artiste, conserver la liste vide. Le test `SearchSummaryTest` utilise une réponse réelle réduite pour vérifier ces cas.

`useSignatureTimestamp` et `useWebPoTokens` indiquent les besoins de chaque client. Le timestamp JavaScript et les PoTokens sont demandés seulement lorsque le client essayé en a besoin. Un client avec `loginRequired` est ignoré hors connexion au compte ; `loginSupported` contrôle l'envoi des cookies et du contexte de compte.

Si la requête du client principal échoue, les clients de secours sont quand même essayés. Les métadonnées du client principal sont conservées lorsqu'il répond ; sinon, celles du client qui fournit le flux validé sont utilisées. Si toutes les requêtes échouent, l'erreur réseau d'origine reste disponible pour le message d'erreur de l'application.

Les méthodes suspendues de `YouTube` utilisent `runCatchingCancellable` : une erreur HTTP ou de parsing reste un `Result.failure`, mais une annulation de coroutine interrompt la requête. Conserver cette règle pour les nouveaux endpoints afin qu'une recherche abandonnée ou un changement de piste ne poursuive pas les clients de secours.

## Comparer avec OuterTune sans écraser O-Beat

Le dépôt officiel est [OuterTune/OuterTune](https://github.com/OuterTune/OuterTune). Vérification du 8 octobre 2026 : son README indique que le développement actif est arrêté. Sa branche par défaut `lite` ne contient plus `innertube`. La branche `dev` contient encore l'intégration musicale ; le commit comparé est `12f61da05f82fdb870c97dcc89c847ce29c75428` (31 janvier 2026). C'est une référence de code, pas une garantie de compatibilité actuelle avec YouTube.

Depuis la racine du projet, sous PowerShell :

```powershell
.\scripts\compare-outertune.ps1
```

Le script récupère la branche `dev` dans le checkout adjacent `OuterTune-upstream` (ou le clone s'il manque), puis écrit les snapshots, les différences et le commit exact dans `out/outertune-compare/<commit>/`, un dossier ignoré par Git. Chaque commit possède son dossier pour conserver une comparaison reproductible. Il ne modifie aucun fichier source d'O-Beat et ne change pas la branche de travail du checkout upstream.

Pour un autre checkout officiel ou une autre branche :

```powershell
.\scripts\compare-outertune.ps1 -UpstreamPath 'D:\Sources\OuterTune' -Branch 'release/0.10.x'
```

Lire les `.diff`, identifier le changement utile et intégrer uniquement les lignes compatibles. La branche `dev` utilise encore Android VR comme client principal et iOS comme secours. O-Beat possède déjà sa propre stratégie VisionOS, TV et web ainsi que ses en-têtes et son cache : les remplacer intégralement réintroduirait les anciennes limites. Les snapshots des clients upstream servent de comparaison, pas de mise à jour automatique des versions.

Si une évolution du serveur nécessite une recherche plus récente, consulter également les projets cités par le README officiel : [Metrolist](https://github.com/MetrolistGroup/Metrolist) et [ArchiveTune](https://github.com/koiverse/ArchiveTune), ainsi que [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) pour le déchiffrement. Vérifier les commits, les licences et la compatibilité avant toute reprise de code.

## Vérifier une modification

```powershell
.\gradlew.bat :innertube:test --tests 'com.zionhuang.innertube.ResultTest' --tests 'com.zionhuang.innertube.SearchSummaryTest'
.\gradlew.bat :app:assembleFullDebug
```

Le premier test fonctionne sans appeler YouTube. Les tests existants `YouTubeTest` contactent le serveur : les lancer séparément lorsque la connexion est disponible. Ils peuvent échouer si la région, le compte ou les réponses YouTube ont changé.

Sur appareil ou émulateur, vérifier la recherche et ses suggestions, l'accueil, une playlist, la lecture d'une piste au-delà de 60 secondes, le changement rapide de piste et un téléchargement relu hors ligne. Refaire les parcours connecté et déconnecté ; vérifier une piste locale pour conserver le fonctionnement mixte. Une compilation réussie ne prouve pas que le serveur accepte encore chaque client.

Éviter de journaliser les cookies, les PoTokens ou les URL signées des flux. Les logs de lecture indiquent le client essayé et le statut obtenu sans afficher l'URL du flux.

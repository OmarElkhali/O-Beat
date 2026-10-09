# Design natif O-Beat

O-Beat reste une application Android en Kotlin et Jetpack Compose. Le design met la musique, les pochettes et les commandes de lecture au premier plan. Material3 fournit les composants, les états de pression, les champs, les dialogues et les cibles tactiles. Aucune bibliothèque graphique supplémentaire n’est nécessaire.

## Couleurs et surfaces

La palette provient de `OBeatTheme`. Les couleurs dynamiques Android, le choix de couleur de l’utilisateur, la couleur extraite d’une pochette et le mode noir restent compatibles. La couleur de repli de la marque est `#ED5564` ; les couleurs finales sont calculées par Material3. Une seule palette traverse les écrans.

- `surface` et `background` portent le contenu principal.
- `surfaceContainerLow` groupe des réglages ; `surfaceContainerHigh` distingue un élément actif ou une icône de raccourci.
- `onSurface` porte les titres ; `onSurfaceVariant` porte les artistes, durées et descriptions.
- `primary` indique une action ou un morceau en cours ; `primaryContainer` indique la sélection.
- Les demandes d’autorisation utilisent `errorContainer` et `onErrorContainer`, avec un contraste adapté aux thèmes clair et sombre.

`GlassSurface` est une surface légèrement translucide, à contraste élevé, avec une bordure discrète et une ombre de 3 dp. Elle sert aux couches flottantes et aux raccourcis existants. Ce traitement ne prétend pas reproduire le flou d’arrière-plan d’Apple : il fonctionne sans effet GPU lourd sur les versions Android prises en charge. Les listes et les groupes de réglages utilisent des surfaces simples.

## Typographie et formes

`Typography.kt` centralise la police système sans empattement, compatible avec les textes latins et arabes. Les tailles restent en `sp` pour respecter le réglage de taille du texte. Les grands titres utilisent une chasse légèrement resserrée ; le corps de texte conserve un interligne confortable. Les titres de section utilisent SemiBold plutôt qu’un gras uniforme, et les descriptions un poids normal.

L’échelle de formes Material3 est de 6, 12, 18, 24 et 32 dp. Les lignes sélectionnées utilisent la petite forme ; les raccourcis et les cartes récentes la forme moyenne ; les surfaces flottantes et les grandes cartes la grande forme. Les avatars d’artiste restent circulaires. Les pochettes conservent leurs proportions.

Les lignes musicales utilisent une hauteur minimale plutôt qu’une hauteur fixe : elles peuvent grandir quand la police augmente. Les titres restent limités au nombre de lignes prévu ; les commandes gardent leur position et leurs actions.

La grande pochette du lecteur demande une image de 1200 × 1200 via le helper `resize` existant pour les URL YouTube reconnues. La résolution fournie par la source reste une limite : la pochette d'« Instant Crush » est encore floue sur la capture de contrôle. Les petits aperçus gardent leur image légère et les pochettes locales utilisent leur image complète. Les deux fichiers historiques de traduction française ont aussi été réparés pour supprimer les caractères mal encodés.

## Interaction, mouvement et accessibilité

Les appuis conservent le retour visuel natif Material3. Les transitions Compose déjà présentes restent interruptibles et prennent en compte l’échelle d’animation Android. Les interactions de lecture, les menus par appui long, les glissements et la sélection utilisent les mécanismes existants ; aucun décor animé permanent n’a été ajouté.

Les boutons redimensionnables réservent l’espace interactif minimal Material3 tout en gardant la taille visuelle de l’icône. Le bouton de lecture superposé mesure 48 dp. Les boutons de retour, de recherche et de changement de vue disposent de libellés ; les boutons redimensionnables acceptent `contentDescription`. Les contrôles désactivés ne déclenchent plus leur appui long.

Les indications décoratives peuvent conserver un `contentDescription = null` lorsque le titre voisin décrit déjà le contenu. Les traductions des nouveaux messages se trouvent dans `obeat_sections.xml` en anglais et en français ; les autres langues utilisent le texte anglais de repli.

## Portée des composants partagés

| Composant | Parties concernées |
|---|---|
| `OBeatTheme`, `OBeatTypography`, formes Material3 | Ensemble des écrans qui utilisent le thème de l’application |
| `NavigationTitle`, `NavigationTile` | Accueil, sections musicales, raccourcis et regroupements |
| `ListItem`, `GridItem`, `YouTubeCardItem` | Bibliothèque, recherche locale et distante, historique, statistiques, albums, artistes et playlists |
| `PreferenceEntry`, `PreferenceGroupTitle` | Réglages et sous-sections de préférences |
| `ChipsRow`, `ChipsLazyRow` | Filtres de bibliothèque, recherche, historique et périodes statistiques |
| `SearchBar`, `EmptyPlaceholder` | Recherche, écrans vides et bibliothèques sans contenu |
| `GlassSurface`, boutons communs | Raccourcis, pied flottant de sélection et commandes partagées |

La bibliothèque utilise maintenant des avertissements d’autorisation lisibles et annonce l’action du changement grille/liste. Les réglages regroupent leurs entrées dans des cartes sobres sans ombres répétées. La recherche de l’historique utilise un champ compact qui partage correctement la largeur avec son icône. Historique et statistiques disposent d’un message utile lorsque leurs données locales sont vides. Dans les statistiques et l’historique distant, toucher un morceau lance la file au morceau touché.

## Adaptation des skills

`apple-design` apporte la hiérarchie, la lisibilité, les matériaux retenus et le retour immédiat. `design-taste-frontend` et `redesign-existing-projects` servent à l’audit des poids, des espacements, des couleurs et des états vides. Leurs recettes React, CSS, survol et navigateur ne sont pas transposées littéralement dans Android : l’équivalent natif est Material3 et Compose. `ponytail` impose de réutiliser les composants et les préférences existants, de préserver les actions et de limiter les changements.

## Revue sur appareil

1. Ouvrir accueil, bibliothèque, recherche, historique, statistiques et chaque groupe de réglages en clair, sombre puis noir.
2. Vérifier que les pochettes restent nettes, les métadonnées lisibles et les commandes accessibles au-dessus du mini-lecteur et des barres système.
3. Passer en grande taille de police puis en arabe : vérifier les lignes, les titres longs, les champs et la direction des flèches.
4. Utiliser TalkBack pour retour, recherche, changement de vue, lecture/pause et appuis longs ; vérifier aussi les états désactivés.
5. Tester les filtres, la sélection multiple, les actions du pied flottant, le rafraîchissement et l’ouverture des réglages.
6. Sur une bibliothèque vide, vérifier les messages ; après quelques écoutes, toucher le deuxième morceau des statistiques et confirmer que c’est lui qui démarre.
7. Couper les animations Android et vérifier que navigation, recherche et lecture restent utilisables.

Les résultats de compilation et les captures prises sur appareil doivent être consignés séparément des conventions de ce document.

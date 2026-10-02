# Filet serveur Google (Apps Script) — guide d'installation

Objectif : que l'alerte e-mail parte **même si votre téléphone est éteint, verrouillé,
sans SIM ou perdu**. C'est Google qui surveille et envoie, indépendamment du téléphone.

## 1. Créer le script
1. Allez sur https://script.google.com → **Nouveau projet**.
2. Supprimez le contenu par défaut, collez tout le contenu de **Code.gs**.
3. Remplacez la valeur de `TOKEN` par un **jeton secret long et aléatoire**
   (ex. une suite de 30+ caractères). **Notez-le**, il faudra le saisir dans l'app.
4. Enregistrez (icône disquette).

## 2. Déployer en application web
1. Bouton **Déployer** → **Nouveau déploiement**.
2. Roue dentée → type **Application Web**.
3. Réglages :
   - **Exécuter en tant que** : moi (votre compte)
   - **Qui a accès** : **Tout le monde**
4. **Déployer**. Autorisez les accès demandés (Gmail / propriétés du script).
5. Copiez l'**URL du déploiement** qui finit par `/exec`
   (ex. `https://script.google.com/macros/s/AKf.../exec`). C'est l'**URL serveur** à coller dans l'app.

## 3. Créer le déclencheur horaire
1. Dans l'éditeur, icône **horloge** (Déclencheurs) à gauche.
2. **Ajouter un déclencheur** :
   - Fonction : **check**
   - Source de l'événement : **Horloge**
   - Type : **Minuteur horaire** → **Toutes les 5 minutes**
3. Enregistrer.

## 4. Configurer l'app YRO
Dans **Dead Man's Switch by YRO → Réglages → Filet serveur (Google)** :
- Activez l'interrupteur.
- Collez l'**URL serveur** (le `/exec`).
- Collez le **jeton** (exactement le même que dans le script).
- Vérifiez que l'**e-mail** est bien configuré (destinataires, objet, message) : le serveur
  réutilise ces informations.

## 5. Tester
1. Activez la surveillance dans l'app → un check-in part vers le serveur.
2. Mettez un **temps d'absence court** (ex. 5 min) et un **délai serveur** = absence + marge
   (la marge est gérée par l'app : ~2 h par défaut ; pour un test, réduisez l'absence et
   attendez que la marge s'écoule, ou lancez `check` à la main après avoir forcé `fireAt`).
3. Plus simple pour tester le serveur seul : dans l'éditeur Apps Script, exécutez `check`
   après avoir fait un check-in puis attendu le dépassement de `fireAt`.

## Comment ça marche (résumé)
- **check-in** : à chaque validation, le téléphone envoie `fireAt` (= maintenant +
  temps d'absence + marge), les destinataires, l'objet, le message et la position.
- **disarm** : quand vous désactivez la surveillance, ou quand le téléphone a **déjà
  envoyé** l'alerte lui-même, il « désarme » le serveur pour éviter un doublon.
- **check()** : si plus de check-in n'arrive et que `fireAt` est dépassé → Google envoie l'e-mail.

## Notes
- **Gratuit** (quota Gmail largement suffisant pour ce besoin).
- Le serveur n'envoie que l'**e-mail** (pas de SMS natif). Le SMS reste géré par le téléphone.
- Vos destinataires/message/position sont stockés dans **votre** projet Apps Script,
  protégés par le jeton. Ne partagez jamais l'URL + le jeton ensemble.
- Marge serveur : le serveur attend un peu **plus longtemps** que le téléphone, pour
  laisser le téléphone envoyer en premier s'il est vivant (et éviter les doublons).

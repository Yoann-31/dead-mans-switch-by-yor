# Dead Man's Switch by YOR — interrupteur d'homme mort pour Android

Application Android qui vous relance à intervalle régulier. Vous devez **valider**
votre présence à chaque relance. **Sans validation dans le délai imparti**, l'application
envoie automatiquement un message (texte, lien, et/ou photo) au destinataire que vous avez
configuré, par **e-mail (SMTP)** ou par **SMS**.

---

## YOR => Première utilisation : 

- Ouvrir le dossier dans VS Code.
- Dans le terminal, taper : winget install EclipseAdoptium.Temurin.17.JDK
(installe le JDK)
- Installer Android Studio (pour le SDK), Téléchargez-le sur https://developer.android.com/studio et lancez l'installeur.
- Installer le setup wizard et l'exécuter.
        Dans Android Studio : More Actions → SDK Manager (ou icône ⚙️ → SDK Manager).
        Onglet SDK Platforms : cochez Android 14.0 (API 34).
        Onglet SDK Tools : cochez Android SDK Build-Tools 34 et Android SDK Platform-Tools.
        Cliquez Apply pour installer ce qui manque.
        Notez le chemin du SDK affiché tout en haut du SDK Manager (« Android SDK Location »), en général
          "C:\Users\YoannROUCHY\AppData\Local\Android\Sdk"
- Créer local.properties à la racine du projet dans VSCode (même niveau que settings.gradle), 
  avec ce chemin — antislashs doublés :sdk.dir=C:\\Users\\YoannROUCHY\\AppData\\Local\\Android\\Sdk
- Dans le terminal, taper : .\gradlew.bat assembleDebug

- Dossier de sortie pour l'APK : app/build/outputs/apk/debug/app-debug.apk

  ## YOR => Première utilisation : Compilation par GitHub : 
  - Sur GitHub : New repository → nom dead-mans-switch-by-yor → ne rien cocher → Create repository. Laissez la page ouverte (elle affiche l'URL du dépôt).
  - Dans le terminal VSCode (dans le dossier qui contient gradlew.bat), lancez les commandes, en remplaçant <VOTRE_COMPTE> : 
      git init
      git add .
      git commit -m "Dead Man's Switch by YOR"
      git branch -M main
      git remote add origin https://github.com/VOTRE_COMPTE/dead-mans-switch-by-yor.git
      git push -u origin main
  - L'onglet Actions compilera l'APK tout seul.
  - Après que le build ait réussi, en bas de la apge, l'APK est disponible pour être télépchargée.


## 1. Obtenir le fichier .apk

Vous n'avez **rien à installer** sur votre ordinateur. GitHub compile l'APK pour vous.

1. Créez un compte gratuit sur https://github.com si vous n'en avez pas.
2. Créez un nouveau dépôt (bouton **New repository**), par ex. `dead-mans-switch-by-yor`. Laissez-le vide.
3. Envoyez-y le contenu de ce dossier. Deux façons :
   - **Simple (glisser-déposer)** : sur la page du dépôt vide, cliquez sur
     *uploading an existing file* et déposez tous les fichiers/dossiers de ce projet.
   - **En ligne de commande** :
     ```bash
     git init
     git add .
     git commit -m "Dead Man's Switch by YOR"
     git branch -M main
     git remote add origin https://github.com/VOTRE_COMPTE/dead-mans-switch-by-yor.git
     git push -u origin main
     ```
4. Ouvrez l'onglet **Actions** du dépôt. Le build « Build APK » démarre tout seul
   (sinon, cliquez dessus puis **Run workflow**). Attendez ~3–5 min qu'il passe au vert.
5. Cliquez sur le build terminé → section **Artifacts** → téléchargez **dead-mans-switch-by-yor-apk**.
   Vous obtenez un `.zip` contenant `app-debug.apk`.

> Alternative sans GitHub : ouvrez le projet dans **Android Studio** (gratuit),
> menu *Build → Build Bundle(s)/APK(s) → Build APK(s)*. L'APK sort dans
> `app/build/outputs/apk/debug/`.
>
> Pour compiler en local dans **VSCode**, voir le guide **BUILD_VSCODE.md**.

## 2. Installer l'APK sur le téléphone

1. Copiez `app-debug.apk` sur le téléphone (câble, e-mail, cloud…).
2. Ouvrez-le avec le gestionnaire de fichiers. Android demandera d'autoriser
   « l'installation d'applications inconnues » pour cette source → acceptez.
3. Installez, puis ouvrez **Dead Man's Switch by YOR**.

## 3. Configurer

- **Méthode** : E-mail ou SMS.
- **Destinataire** : l'adresse e-mail ou le numéro de téléphone.
- **Message** : le texte et/ou le lien à envoyer.
- **Photo** (e-mail uniquement) : pièce jointe optionnelle.
- **Intervalle** : temps entre deux relances (en minutes).
- **Délai pour valider** : temps dont vous disposez pour valider après une relance.
- **SMTP** (e-mail) : voir ci-dessous.
- **Enregistrer**, puis **Activer la veille**. Le bouton **Tester l'envoi maintenant**
  permet de vérifier que l'envoi fonctionne.

### Réglages SMTP pour Gmail (recommandé)
L'envoi automatique d'e-mail passe par SMTP ; il faut un **mot de passe d'application**
(pas votre mot de passe habituel) :
1. Activez la **validation en deux étapes** sur votre compte Google.
2. Créez un mot de passe d'application : https://myaccount.google.com/apppasswords
3. Dans l'app : Serveur `smtp.gmail.com`, Port `587`, Identifiant = votre adresse Gmail,
   Mot de passe = le mot de passe d'application (16 caractères).

Autres fournisseurs : renseignez leur serveur SMTP, port `587` (TLS) ou `465` (SSL).

## 4. Autorisations à accorder (important pour la fiabilité)

À la première activation, acceptez :
- **Notifications** (pour les relances).
- **SMS** (si méthode SMS).
- **Alarmes et rappels / alarmes exactes** : l'app vous y envoie si nécessaire.

Pour éviter qu'Android ne mette l'app en veille et rate un envoi :
- Réglages → Applications → Dead Man's Switch by YOR → **Batterie → Sans restriction / Non optimisée**.

## Fonctionnement interne

- `AlarmManager` (alarmes exactes) programme la relance puis l'échéance.
- La relance affiche une notification avec le bouton **« Je suis là »** :
  - validé → l'échéance est annulée et la relance suivante est reprogrammée ;
  - non validé avant l'échéance → `DeadlineReceiver` lance `SendService`
    (service en avant-plan) qui effectue l'envoi, puis **désactive la veille**.
- `BootReceiver` reprogramme tout après un redémarrage du téléphone.

## Limites et avertissements

- C'est un déclenchement **unique** : après un envoi, la veille se désactive ;
  rouvrez l'app pour la réactiver.
- Selon le constructeur (Xiaomi, Huawei, Samsung…), la gestion agressive de la
  batterie peut retarder les alarmes. Retirez l'optimisation de batterie pour
  cette app si vous comptez dessus.
- Le SMS n'envoie **que du texte** (pas de photo). Pour une photo, utilisez l'e-mail.
- Le mot de passe SMTP est stocké localement sur l'appareil (SharedPreferences).
  N'installez cette app que sur un téléphone qui vous appartient.
- APK de **debug** : signé avec la clé de debug, parfait pour un usage personnel.
  Pour une distribution large, il faudrait une signature de release.

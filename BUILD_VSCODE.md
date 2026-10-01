# Compiler l'APK en local avec VSCode

VSCode est un éditeur : il ne compile pas Android tout seul. Ce qui compile,
c'est **Gradle** (déjà inclus dans le projet via `gradlew`) avec deux outils à
installer une seule fois : un **JDK 17** et le **SDK Android**. Ensuite, on lance
la compilation depuis le terminal de VSCode (ou via les tâches fournies).

---

## Étape 1 — Installer le JDK 17

- **Windows / macOS / Linux** : installez « Temurin 17 » depuis
  https://adoptium.net (choisissez la version 17 LTS).
- Vérifiez dans un terminal :
  ```bash
  java -version
  ```
  Vous devez voir `17.x`.

## Étape 2 — Installer le SDK Android

Deux options. La **A** est la plus simple si vous débutez.

### Option A (recommandée) — via Android Studio, mais éditer dans VSCode
1. Installez Android Studio : https://developer.android.com/studio
2. Lancez-le une fois : il télécharge automatiquement le SDK. Dans
   *More Actions → SDK Manager*, cochez et installez :
   - **Android SDK Platform 34**
   - **Android SDK Build-Tools 34**
   - **Android SDK Platform-Tools** (contient `adb`)
3. Notez le chemin du SDK affiché en haut du SDK Manager, par ex. :
   - Windows : `C:\Users\VOUS\AppData\Local\Android\Sdk`
   - macOS : `/Users/VOUS/Library/Android/sdk`
   - Linux : `/home/VOUS/Android/Sdk`

Vous pouvez ensuite tout faire dans VSCode ; Android Studio n'a servi qu'à
obtenir le SDK.

### Option B — Ligne de commande seule (sans Android Studio)
1. Téléchargez les « Command line tools » depuis
   https://developer.android.com/studio#command-line-tools-only
2. Décompressez-les, par ex. dans `~/android-sdk/cmdline-tools/latest/`
   (le dossier `latest` doit contenir `bin/`, `lib/`…).
3. Installez les composants et acceptez les licences :
   ```bash
   cd ~/android-sdk/cmdline-tools/latest/bin
   ./sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ./sdkmanager --licenses      # tapez "y" à chaque question
   ```
   Le SDK est alors dans `~/android-sdk`.

## Étape 3 — Indiquer le SDK au projet

Créez un fichier **`local.properties`** à la racine du projet (à côté de
`settings.gradle`) avec le chemin de votre SDK. Adaptez selon votre OS :

```properties
# Windows (doublez les antislashs)
sdk.dir=C:\\Users\\VOUS\\AppData\\Local\\Android\\Sdk

# macOS
# sdk.dir=/Users/VOUS/Library/Android/sdk

# Linux
# sdk.dir=/home/VOUS/Android/Sdk
```

> `local.properties` est propre à votre machine et n'est pas versionné (il est
> déjà dans `.gitignore`). Autre possibilité : définir la variable
> d'environnement `ANDROID_HOME` avec ce même chemin.

## Étape 4 — Ouvrir et compiler dans VSCode

1. **Fichier → Ouvrir le dossier** → choisissez le dossier `dead-mans-switch-by-yor`.
2. VSCode proposera d'installer les extensions recommandées (Gradle, Kotlin) —
   acceptez.
3. Compilez, au choix :
   - **Menu Terminal → Exécuter la tâche… → « Compiler l'APK (debug) »**
     (ou simplement `Ctrl+Shift+B`).
   - **Ou** dans un terminal VSCode :
     ```bash
     # macOS / Linux
     ./gradlew assembleDebug
     # Windows (PowerShell)
     .\gradlew.bat assembleDebug
     ```
4. Le premier build télécharge les dépendances (quelques minutes). À la fin :
   ```
   BUILD SUCCESSFUL
   ```
   L'APK se trouve ici :
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Étape 5 — Installer sur le téléphone

- **Simple** : copiez `app-debug.apk` sur le téléphone et ouvrez-le
  (autorisez « sources inconnues »).
- **Via USB (adb)** : activez le *débogage USB* sur le téléphone
  (Options pour développeurs), branchez-le, puis lancez la tâche
  **« Installer sur l'appareil branché (adb) »** ou :
  ```bash
  ./gradlew installDebug
  ```

---

## En cas d'erreur

- **`SDK location not found`** → `local.properties` manquant ou mauvais chemin
  (Étape 3).
- **`Failed to find Build Tools` / `platform 34`** → installez-les via le SDK
  Manager (Option A) ou `sdkmanager` (Option B).
- **`Unsupported class file major version` / erreur Java** → mauvaise version de
  JDK ; vérifiez `java -version` (doit être 17).
- **Licences non acceptées** → exécutez `sdkmanager --licenses` et répondez `y`.

Si un message d'erreur persiste, copiez-le-moi et je vous corrige ça.

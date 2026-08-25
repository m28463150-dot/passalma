# Passalma

Passalma est une application Android de réservation de trajets, avec deux parcours :

- client : recherche d'un chauffeur, réservation et suivi du trajet ;
- chauffeur : réception des demandes, gestion de course et historique.

L'application utilise Android natif en Java, Firebase, Google Maps et Stripe.

## État du projet

Le projet compile en version debug et produit un APK Android. Les services externes doivent toutefois être configurés avant une utilisation réelle : Firebase, Google Maps, Stripe et OneSignal.

Le nom de package Android historique est encore `com.simcoder.uber`.

## Technologies

- Java 8 et Android Gradle Plugin 4.2.1
- Gradle 6.7.1
- Android SDK 30, Android minimum 5.0 (API 21)
- Firebase Authentication, Realtime Database et Storage
- Google Maps, Places et GeoFire
- Stripe pour les paiements
- OneSignal pour les notifications

## Structure

```text
android/                 Application Android
Firebase_Functions/      Fonctions Firebase et intégration Stripe
realtime_database_rules.json
images/                  Images de présentation
.devcontainer/           Configuration Codespaces avec Android et KVM
```

## Prérequis

- Android Studio ou VS Code avec Dev Containers ;
- JDK 11 ;
- Android SDK avec les composants suivants :
  - Android SDK Platform 30 ;
  - Android SDK Build-Tools 30.0.3 ;
  - Android Emulator ;
  - une image système Android 30 ;
- un projet Firebase ;
- Node.js et Firebase CLI pour les fonctions backend.

## Configuration Firebase

1. Créer une application Android dans Firebase avec le package `com.simcoder.uber`.
2. Télécharger `google-services.json`.
3. Placer le fichier dans `android/app/google-services.json`.
4. Activer Authentication, Realtime Database et Storage.
5. Importer [realtime_database_rules.json](realtime_database_rules.json) dans les règles Realtime Database.

Ne jamais publier de clé secrète ou de fichier contenant des identifiants privés dans GitHub.

## Configuration des clés

Remplacer les valeurs de démonstration dans [strings.xml](android/app/src/main/res/values/strings.xml) :

- clé publique Google Maps ;
- URL des Firebase Functions ;
- clé publique Stripe ;
- URL Firebase Hosting.

La clé secrète Stripe doit rester dans la configuration Firebase Functions. Elle ne doit pas être placée dans l'application Android.

## Compiler l'application

Depuis la racine du dépôt :

```bash
cd android
bash ./gradlew --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx768m \
  :app:assembleDebug
```

L'APK est généré ici :

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Émulateur Codespaces

La configuration [.devcontainer/devcontainer.json](.devcontainer/devcontainer.json) expose `/dev/kvm` et le [Dockerfile](.devcontainer/Dockerfile) installe les bibliothèques X11 nécessaires.

Après **Dev Containers: Rebuild Container**, créer ou utiliser l'AVD `passalma_api30`, puis lancer :

```bash
emulator -avd passalma_api30 \
  -no-window -no-audio -no-boot-anim -gpu off
```

Vérifier la connexion :

```bash
adb devices
```

Installer l'APK :

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## Firebase Functions

Configurer les secrets Stripe dans le projet Firebase, depuis `Firebase_Functions/`, puis déployer les fonctions :

```bash
firebase functions:config:set stripe.sk="CLE_SECRETE" stripe.pk="CLE_PUBLIQUE" stripe.currency="EUR"
firebase deploy --only functions
```

Les valeurs sensibles doivent être saisies localement et ne doivent pas être ajoutées aux fichiers suivis par Git.

## Limites connues

- Les services Firebase et les clés API ne sont pas fournis dans le dépôt.
- Le SDK PayPal utilisé est ancien et devra être migré avant une mise en production.
- Les fonctions backend nécessitent une configuration Stripe valide.
- L'émulateur headless peut être limité par la mémoire disponible dans Codespaces.

## Licence

Ce projet est distribué sous licence MIT. Voir [LICENSE](LICENSE).

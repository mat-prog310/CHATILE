# CHATILE - Application de Chat IA avec Personnages et Réseau Social

## 📱 Présentation

CHATILE est une application Android complète de chat IA avec personnages personnalisables, mémoire permanente, et réseau social intégré. Inspiré par Chai, mais avec des fonctionnalités différenciantes fortes.

### 🎯 Positionnement
- **Gratuit = Expérience complète** : Mémoire permanente, personnage du jour illimité, 1000 Pièces de départ, gains quotidiens
- **Premium = Confort et Volume** : Zéro pub, personnages illimités, modèles premium à coût réduit
- **Mémoire transparente et éditable** : Séparation entre mémoire initiale (fournie) et mémoire apprise (extraite)

## 🏗️ Architecture

- **Backend** : Node.js + Express + SQLite (better-sqlite3)
- **Frontend** : Android avec Kotlin + Jetpack Compose
- **Authentification** : Google Sign-In via Credential Manager + Firebase Auth (optionnel)
- **Paiement** : Google Play Billing (vérifié côté serveur)
- **Publicité** : AdMob Rewarded (+10 Pièces) et Interstitial (toutes les 50 messages en gratuit)

## 🚀 Démarrage Rapide

### Prérequis

- Node.js 20+
- Java JDK 17+
- Android Studio Giraffe ou plus récent
- Compte Firebase (optionnel, pour l'auth)
- Compte Mistral AI (pour la clé API)
- Compte Google Cloud (pour le Billing et AdMob)

### Configuration du Backend

1. **Cloner le dépôt** (si ce n'est pas déjà fait)
2. **Installer les dépendances** :
   ```bash
   cd backend
   npm install
   ```

3. **Configurer les variables d'environnement** :
   ```bash
   cp .env.example .env
   ```
   
   Éditer `.env` avec vos clés :
   ```
   MISTRAL_API_KEY=votre_clé_mistral
   PORT=3000
   GOOGLE_APPLICATION_CREDENTIALS=./firebase-service-account.json
   FIREBASE_SERVICE_ACCOUNT={"type": "service_account", ...}
   ```

4. **Configurer Firebase** (optionnel) :
   - Créer un projet Firebase
   - Télécharger le fichier `service-account.json`
   - Placer le fichier dans `backend/firebase-service-account.json`

5. **Lancer le backend** :
   ```bash
   npm start
   ```
   
   Le serveur démarrera sur `http://localhost:3000`

### Configuration de l'Application Android

1. **Ouvrir le projet dans Android Studio** :
   - Ouvrir le dossier `CHATILE`
   - Synchroniser les dépendances Gradle

2. **Configurer l'IP du backend** :
   - Dans `app/src/main/java/com/chatile/ApiClient.kt`
   - Modifier `BASE` pour pointer vers votre serveur :
     ```kotlin
     // Pour l'émulateur Android (10.0.2.2 = localhost)
     private const val BASE = "http://10.0.2.2:3000"
     
     // Pour un vrai device, utiliser votre IP locale ou une URL publique
     // private const val BASE = "https://votre-backend.herokuapp.com"
     ```

3. **Configurer Google Play Billing** :
   - Créer des produits dans Google Play Console
   - **Produits consommables** (Pièces) :
     - `pieces_500` : 500 Pièces
     - `pieces_1500` : 1500 Pièces
     - `pieces_5000` : 5000 Pièces
     - `pieces_15000` : 15000 Pièces
   
   - **Abonnements** :
     - `premium_monthly` : Premium (4,99€/mois)
     - `premium_plus_monthly` : Premium+ (9,99€/mois)
   
   - Mettre à jour les IDs dans `MainActivity.kt`

4. **Configurer AdMob** :
   - Créer une application AdMob
   - **IDs de test** (déjà configurés en dev) :
     ```
     APPLICATION_ID = ca-app-pub-3940256099942544~3347511713
     ```
   - **Pour la production**, remplacer dans `AndroidManifest.xml` :
     ```xml
     <meta-data
         android:name="com.google.android.gms.ads.APPLICATION_ID"
         android:value="votre_app_id_admob" />
     ```

5. **Configurer Firebase Auth** (optionnel) :
   - Créer un projet Firebase
   - Activer l'authentification Google
   - Ajouter le SHA-1 fingerprint dans Firebase Console
   - Configurer dans `build.gradle.kts` :
     ```kotlin
     buildConfigField(
         "String",
         "GOOGLE_WEB_CLIENT_ID",
         "\"votre_web_client_id\""
     )
     ```

6. **Configurer Google Sign-In** :
   - Créer les identifiants OAuth dans Google Cloud Console
   - Configurer le `googleWebClientId` dans `local.properties` ou `gradle.properties` :
     ```properties
     googleWebClientId=votre_web_client_id
     ```

7. **Lancer l'application** :
   - Connecter un émulateur ou un device
   - Cliquer sur Run dans Android Studio

## 📁 Structure du Projet

### Backend (`backend/`)
```
backend/
├── server.js          # Point d'entrée du backend
├── package.json       # Dépendances Node.js
├── .env.example       # Exemple de configuration
├── data/              # Base de données SQLite
│   └── pieces.sqlite  # Base de données (créée automatiquement)
└── uploads/           # Fichiers uploadés (avatars, etc.)
    └── characters/
```

### Android (`app/`)
```
app/
├── build.gradle.kts    # Configuration Gradle
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/chatile/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ApiClient.kt
│   │   │   ├── ChatViewModel.kt
│   │   │   ├── DataModels.kt
│   │   │   └── ui/
│   │   │       ├── AppNavHost.kt
│   │   │       ├── HomeScreen.kt
│   │   │       ├── FeedScreen.kt
│   │   │       ├── ChatScreen.kt
│   │   │       ├── CreateCharacterScreen.kt
│   │   │       ├── ProfileScreen.kt
│   │   │       ├── ShopScreen.kt
│   │   │       ├── ScenesScreen.kt
│   │   │       ├── NotificationsScreen.kt
│   │   │       ├── SearchScreen.kt
│   │   │       ├── PostDetailScreen.kt
│   │   │       ├── CreatePostScreen.kt
│   │   │       ├── UserProfileScreen.kt
│   │   │       ├── MemorySheet.kt
│   │   │       ├── JournalSheet.kt
│   │   │       ├── FeedbackScreen.kt
│   │   │       ├── AdminScreen.kt
│   │   │       ├── OnboardingScreen.kt
│   │   │       ├── VisualComponents.kt
│   │   │       └── theme/
│   │   │           ├── Theme.kt
│   │   │           ├── Color.kt
│   │   │           └── Type.kt
│   │   └── res/
│   │       └── xml/
│   │           └── widget_info.xml
│   └── widget/
│       └── ChatileWidget.kt
```

## ⚙️ Fonctionnalités Principales

### 🎭 Chat avec Personnages IA
- 5 personnages système gratuits (Léa, Kai, Nova, Sacha, Mina)
- Création de personnages personnalisés
- Mémoire permanente (15 faits initiaux + 30 appris)
- Mémoire structurée pour Premium+ (illimitée)
- Mémoire visible et éditable (épinglage, oubli)
- Journal des événements par personnage
- Scénarios et objectifs personnalisés
- Mots interdits et limites
- Mode silencieux
- Mode proactif (le personnage peut initier la conversation)

### 💰 Système de Pièces
- **1 Pièce = 1 message** avec le modèle standard (Mistral Small 4)
- **3 Pièces = 1 message** avec le modèle premium (Mistral Large 3)
- **1 Pièce** pour les abonnés (même avec le modèle premium)
- 1000 Pièces de départ pour chaque nouvel utilisateur

#### Gains gratuits quotidiens :
- **Récompense quotidienne** : Base 20 Pièces + 10 par jour de streak (max 80)
- **Coffre mystérieux** : 10-50 Pièces aléatoires, 1 fois/jour
- **Quêtes hebdomadaires** : 4 quêtes (Marathonien, Créateur prolifique, Explorateur, Collectionneur)
- **Récompenses d'actions** : Compléter le profil (100), premier personnage (150), premier message (50), etc.

#### Achats In-App (Google Play Billing) :
- **Packs de Pièces** :
  - 500 Pièces : 2€
  - 1500 Pièces : 5€
  - 5000 Pièces : 15€
  - 15000 Pièces : 40€

- **Abonnements** :
  - **Premium** : 4,99€/mois
    - 3000 Pièces/mois
    - Personnages illimités
    - Mémoire étendue (30 seed + 60 learned)
    - Modèle premium à 1 Pièce
    - Zéro pub
    - 3 thèmes exclusifs
    - Badge Premium
    - Export .json/.pdf
    - Scènes multi-IA : 3 actives, illimité
  
  - **Premium+** : 9,99€/mois
    - Tout Premium +
    - 5000 Pièces/mois
    - Mémoire structurée illimitée
    - Appels vocaux 30 min/jour
    - Personnage évolutif visuel
    - Export .html interactif
    - 6 thèmes supplémentaires
    - Coffre Premium
    - Badge Premium+ doré
    - Analytics avancées

### 🌐 Réseau Social
- **Feed** : Posts des utilisateurs avec filtres (Pour toi, Abonnements, Récent)
- **Types de posts** : Texte, Extrait de conversation, Personnage
- **Interactions** : Likes, Commentaires, Partages
- **Système de Follow** : Abonnements et abonnés
- **Sondages** : Création et vote dans les posts
- **Signalements** : Signalement de contenu inapproprié
- **Notifications** : Push notifications pour les interactions
- **Compte Admin Officiel** : @chatile_official avec posts de mise à jour

### 🎨 Personnalisation
- **Thèmes gratuits** : Violet, Rose, Bleu
- **Thèmes Premium** : Or, Émeraude, Améthyste, Rubis, Obsidienne, Ivoire
- **Badges** : Officiel (étoile), Modérateur (bouclier), Premium (diamant), Premium+ (couronne), Créateur (plume)

### 📊 Statistiques et Analytics
- **Statistiques par personnage** : Nombre de messages, dernier interaction
- **Statistiques globales** : Messages totaux, personnages préférés
- **Rétrospective hebdomadaire** : Résumé des activités
- **Historique des Pièces** : Transparent et détaillé

### 🎬 Scènes Multi-IA
- **Gratuit** : 1 scène active, 10 messages/jour
- **Premium** : 3 scènes actives, illimité
- **Jusqu'à 4 personnages** par scène (Premium+)
- Chaque personnage répond dans la conversation

### 🎭 Co-écriture
- **1 Pièce** par paragraphe généré
- Mode collaboratif avec les personnages

### 📝 Journal Émotionnel
- Enregistrement de l'humeur
- Notes personnelles
- Timeline des émotions

### 🎯 Défis
- Défis quotidiens générés automatiquement
- Récompenses en Pièces à la complétion

### 🔄 Fonctionnalités Sociales
- **Partage de personnages** : Génération de liens de partage
- **Import de personnages** : Depuis les liens partagés
- **Catalogue public** : Découverte de personnages d'autres utilisateurs

### 🔍 Recherche
- Recherche globale (utilisateurs, posts, personnages)
- Filtres par type
- tris par pertinence, date, popularité

## 💡 Fonctionnalités Différenciantes

### Gratuitement (sans abonnement) :
✅ Mémoire permanente (15 seed + 30 learned)
✅ 1000 Pièces de départ
✅ ~155 Pièces/jour possibles via gains
✅ Personnage du jour illimité
✅ 5 messages gratuits/jour par personnage système
✅ 5 personnages gratuits (puis 50 Pièces par personnage)
✅ Export .txt, partage, journal, coffre, galerie
✅ Mode Récit, co-écriture, résumé
✅ Suggestions, pensées du jour
✅ 3 thèmes, widget
✅ Notes, mode Compagnon Silencieux
✅ Scènes multi-IA : 1 active, 10 msg/jour
✅ Journal émotionnel, défis quotidiens
✅ Rétrospective hebdomadaire
✅ Historique Pièces transparent
✅ Recherche, édition/suppression/régénération messages
✅ Événements, épinglage, rebobinage, mots interdits
✅ Import/export, sauvegarde locale
✅ Onboarding guidé (50 Pièces)
✅ **Réseau social complet** (posts, commentaires, likes, follows, notifications)
✅ Feedback direct à l'admin

### Premium (4,99€/mois) :
✅ 3000 Pièces/mois
✅ Personnages illimités
✅ Mémoire 30 seed + 60 learned
✅ Modèle premium 1 Pièce
✅ Zéro pub
✅ 3 thèmes exclusifs
✅ Badge "premium"
✅ Accès anticipé
✅ Export .json/.pdf
✅ Stats avancées
✅ Voix TTS premium
✅ Scénarios multi-étapes
✅ 3 personnages système exclusifs
✅ Scènes multi-IA : 3 actives, illimité, 4 persos
✅ Ambiances sonores (10)
✅ Publication illimitée dans le catalogue public
✅ Posts avec images générées par IA
✅ Badge "Créateur vérifié"
✅ 1 boost de visibilité/semaine
✅ Stats de post détaillées
✅ Priorité dans les résultats de recherche

### Premium+ (9,99€/mois) :
✅ Tout Premium +
✅ 5000 Pièces/mois
✅ Mémoire structurée illimitée
✅ Appels vocaux 30 min/jour
✅ Personnage évolutif visuel
✅ Export .html interactif
✅ Stats avancées + prédictions
✅ 6 thèmes supplémentaires
✅ Coffre Premium
✅ Scénarios communautaires
✅ Badge "premium_plus" doré
✅ 3 boosts de visibilité/semaine
✅ Analytics avancées
✅ Accès programme de monétisation

## ⚠️ Contraintes de Sécurité

1. **Clé Mistral** : UNIQUEMENT dans le backend (`server.js`), JAMAIS dans l'application Android
2. **Guardrails** : Configurés pour modérer uniquement l'entrée (input), pas la sortie
3. **Vérification des achats** : TOUJOURS côté serveur via Google Play API
4. **Toggle NSFW** : Stocké côté serveur, modifiable via `/api/settings/nsfw`
5. **Publicité** : Interstitielle imposée toutes les 50 messages (gratuit uniquement)
6. **Admin** : Compte créés automatiquement au démarrage, accès restreint

## 🔐 Configuration Firebase et Authentification Google Obligatoire

Pour utiliser CHATILE, l'authentification Google via Firebase est obligatoire. Sans compte Google connecté, l'accès au chat, aux Pièces et au profil est restreint par un écran de connexion bloquant.

### Étapes de configuration :

1. **Créer un projet Firebase** sur la [console Firebase](https://console.firebase.google.com).
2. **Ajouter une application Android** avec le package `com.chatile`.
3. **Télécharger le fichier `google-services.json`** et le placer dans le dossier `app/` du projet.
4. **Activer Google Sign-In** dans la console Firebase (Authentication > Sign-in method).
5. **Ajouter l'empreinte SHA-1** de votre certificat de debug dans les paramètres du projet Firebase (générable via `./gradlew signingReport`).
6. **Configurer Firebase Admin** côté backend avec votre fichier de compte de service (`serviceAccountKey.json`).

## 🎯 Prochaines Étapes

### Pour la Production :

1. **✅ HTTPS** : Configurer un reverse proxy (Nginx, Caddy) ou utiliser un service PaaS
2. **✅ IDs AdMob réels** : Remplacer les IDs de test dans `AndroidManifest.xml`
3. **✅ Vérification Billing** : Tester avec des achats réels en production
4. **✅ Politique de confidentialité** : Créer et lier dans l'application
5. **✅ Conformité Google Play** : 
   - Le toggle NSFW est côté serveur (conformité respectée)
   - Pas de contenu NSFW activé par défaut
   - Système de signalement fonctionnel
6. **✅ Configuration Firebase** : 
   - Activer Firebase Auth
   - Configurer Firebase Cloud Messaging (pour les notifications push)
7. **✅ Notifications RTDN** : Implémenter Real-Time Database ou Cloud Functions pour les notifications push
8. **✅ Modération** : Implémenter un système de modération pour le réseau social
9. **✅ Backup** : Mettre en place des sauvegardes régulières de la base SQLite
10. **✅ Monitoring** : Configurer des logs et monitoring (Sentry, etc.)

### Améliorations Futures :

- **Version iOS** : Portage avec SwiftUI
- **Version Web** : Application web avec React
- **API publique** : Pour les développeurs tiers
- **Marketplace** : Achat/vente de personnages
- **Communauté** : Forum intégré
- **Multilingue** : Traduction en plusieurs langues
- **Accessibilité** : Meilleure support WCAG

## 📄 Licence

Propriétaire - Tous droits réservés

## 🙏 Remerciements

- Mistral AI pour les modèles LLM
- Google pour Firebase, AdMob et Play Billing
- Android/Jetpack Compose équipe
- Communautés open-source

---

**CHATILE - Votre compagnon de conversation IA** 💜

*Gratuit. Illimité. Intelligent.*

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Vue d'ensemble

**Lissafi** — caisse enregistreuse Android pour petits commerces informels (Niamey, Niger). App native Kotlin + Jetpack Compose, **local-first** : SQLite est la source de vérité, Supabase (PostgREST) sert de sauvegarde cloud en synchro asynchrone. APK distribué hors Play Store. La stratégie produit complète est dans `plan-lissafi.md`.

## Commandes

- **Compiler** : `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug`. Aucun JDK système n'est installé — le build échoue sans ce JAVA_HOME (JBR d'Android Studio, JDK 25). Premier build ~8 min.
- **Installer sur un appareil** : `./gradlew installDebug`.
- **Tests** : aucun test unitaire ni instrumentation n'existe dans `app/src`. Rien à lancer.
- La synchro Gradle est lente (timeouts longs + retries déjà configurés dans `gradle.properties`, réseau Niamey → Google Maven).

## Architecture

Couches dans `app/src/main/java/com/lissafi/app/` :

- **UI (Jetpack Compose, Material 3)** — `ui/screen/*` (Caisse, Produits, Clients, Rapports, Réglages, Admin, Auth), `ui/viewmodel/*` (un ViewModel par écran, `remember {}` dans `LissafiNavHost`), `ui/navigation/LissafiNavHost.kt` (routes + bottom bar), `ui/components/LissafiComponents.kt` (composants partagés), `ui/theme/`.
- **Repository** — `data/repository/LissafiRepository.kt` : seule porte d'entrée pour les ViewModels. Connaît la DB locale + l'API distante.
- **Données locales** — `data/LissafiDatabase.kt` : `SQLiteOpenHelper` **brut (SQL brut, pas Room** — le plan doc dit Room mais le code réel est du raw SQL). Contient aussi les extensions `Cursor.toX()` et les méthodes de synchro (getUnsyncedSales, reassignSaleId…). `DATABASE_VERSION = 2` (v2 = ajout de la colonne `user_id`).
- **Remote** — `data/remote/SupabaseApi.kt` : client REST PostgREST via Ktor 3. **Chaque méthode lève `SupabaseException` en cas d'échec** — jamais avalé, pour que la synchro puisse retenter. `data/remote/SupabaseManager.kt` : config URL/anon key + session (tokens) en `SharedPreferences`.
- **Auth** — `data/auth/AuthManager.kt` : appelle directement l'API GoTrue (`/auth/v1/signup`, `/auth/v1/token`, `/auth/v1/logout`, `/auth/v1/recover`) — **pas de SDK Supabase**.
- **Synchro** — `data/sync/SyncManager.kt` : push/pull bidirectionnel (products, clients, sales, dettes, settings), protégé par `Mutex`, déclenché au démarrage, à la connexion, au retour réseau, toutes les 15 min. `SyncWorker.kt` : WorkManager périodique.
- **Services** — `service/ReceiptService.kt` (ticket texte + partage WhatsApp + impression Bluetooth ESC/POS), `service/PremiumManager.kt`, `service/FormatUtils.kt`.
- `MainActivity.kt` (Compose, edge-to-edge) et `LissafiApp.kt` (Application : instancie DB/API/auth/sync, démarre l'observer réseau et le SyncWorker).

## Patterns clés à respecter

- **Local-first** : toute écriture passe D'ABORD en SQLite, puis `syncToRemote { ... }` en arrière-plan silencieux (les échecs réseau sont ignorés ici — le `SyncManager` retentera). Ne jamais bloquer l'UI sur le réseau.
- **L'argent est en `Int` FCFA** partout (jamais de double pour les montants). Les quantités vendues sont des `Double`.
- **Isolation par utilisateur** : chaque table a `user_id`. Le `LissafiRepository` porte un `currentUserIdProvider` réglé via `withUserId(userId)` depuis `LissafiNavHost`. Toujours passer le `userId` aux méthodes DB qui l'acceptent.
- **Fusion des ids de vente** : au push, Supabase génère un id `BIGSERIAL` ; `db.reassignSaleId(localId, remoteId)` réaligne les références (`sale_items`, `debt_transactions`) pour éviter les doublons au pull. Garder cette logique si tu touches à la synchro des ventes.
- **Schéma DB = miroir de `supabase-schema.sql`** : si tu modifies les tables dans `LissafiDatabase.onCreate`, mets à jour `supabase-schema.sql` (RLS par `auth.uid() = user_id` incluse) en miroir, et incrémente `DATABASE_VERSION` avec une migration dans `onUpgrade`.
- **Premium** : codes statiques `LISSAFI-PREMIUM-XXXX` dans `PremiumManager` (liste prédéfinie), démo 7 jours, limites gratuites (10 produits / 10 crédits). Statut stocké dans `app_settings` (`is_premium`, `premium_expiry`, `demo_taken`).
- Les entités (`data/entity/*.kt`) sont `@Serializable` (kotlinx.serialization) — les noms de champs doivent coller aux colonnes PostgREST.
- Les dates sont des `Long` (epoch millis), les booléens SQLite sont des 0/1.

## Outils non-Android dans le repo

- `scripts/seed-demo.sql` + `scripts/README-demo.md` : seed Supabase du **compte démo** `demo@lissafi.app` / `demo123456` (boutique fictive complète pour vidéos TikTok). À coller dans le SQL Editor Supabase (projet `fnyuhpfzkvunscuylvqv`). Idempotent, ne touche que les données du compte démo.
- `docs/demo-scenario-video.md` : scénario de tournage des 6 vidéos de démo.
- `marketing-tiktok/` : scripts de vidéos TikTok + guide montage.
- `prospection/` : outil Python séparé (scraper Google Maps + workflow n8n) pour générer des leads commerçants Niamey.
- `plan-lissafi.md` : doc stratégique/commerciale (pricing, roadmap). Utile pour le contexte produit, mais ne décris pas le code réel.
- `Lissafi.ai` : PDF (spec design, 1 page).

## Conventions

- Tout le code commenté et les messages de commit sont en **français**, commits en style conventional (ex. `fix(demo): …`, `feat(demo): …`, `docs: …`).
- Jamais de mention d'IA/Claude dans les commits (voir les conventions globales).

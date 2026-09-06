# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Vue d'ensemble

**Lissafi** — caisse enregistreuse Android pour petits commerces informels (Niamey, Niger). App native Kotlin + Jetpack Compose, **local-first** : SQLite est la source de vérité, Supabase (PostgREST) sert de sauvegarde cloud en synchro asynchrone. APK distribué hors Play Store. Le repo contient aussi un **back-office web** (`backoffice/`, Next.js) et une **landing page** (`landing/`, Vite + React). La stratégie produit complète est dans `plan-lissafi.md`.

## Commandes

- **Compiler** : `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug`. Aucun JDK système n'est installé — le build échoue sans ce JAVA_HOME (JBR d'Android Studio, JDK 25). Premier build ~8 min.
- **Installer sur un appareil** : `./gradlew installDebug`.
- **Tests** : aucun test unitaire ni instrumentation n'existe dans `app/src`. Rien à lancer.
- La synchro Gradle est lente (timeouts longs + retries déjà configurés dans `gradle.properties`, réseau Niamey → Google Maven).

## Architecture

Couches dans `app/src/main/java/com/lissafi/app/` :

- **UI (Jetpack Compose, Material 3)** — `ui/screen/*` (Caisse, Produits, Clients, Rapports, Réglages, Admin, Auth, BarcodeScanner), `ui/viewmodel/*` (un ViewModel par écran, `remember {}` dans `LissafiNavHost`), `ui/navigation/LissafiNavHost.kt` (routes + bottom bar), `ui/components/LissafiComponents.kt` (composants partagés), `ui/theme/`.
- **Repository** — `data/repository/LissafiRepository.kt` : seule porte d'entrée pour les ViewModels. Connaît la DB locale + l'API distante.
- **Données locales** — `data/LissafiDatabase.kt` : `SQLiteOpenHelper` **brut (SQL brut, pas Room** — le plan doc dit Room mais le code réel est du raw SQL). Contient aussi les extensions `Cursor.toX()` et les méthodes de synchro (getUnsyncedSales, finalizeSalePush…). `DATABASE_VERSION = 6` (v2 = colonne `user_id` ; v3 = PK composite `(barcode, user_id)` + soft delete sur `products` ; v4 = suppression du PIN admin ; v5 = colonne `synced` locale sur `debt_transactions` ; v6 = colonne `shop_id` sur les 5 tables métier — offre Grand boutique, boutique partagée).
- **Remote** — `data/remote/SupabaseApi.kt` : client REST PostgREST via Ktor 3. **Chaque méthode lève `SupabaseException` en cas d'échec** — jamais avalé, pour que la synchro puisse retenter. `data/remote/SupabaseManager.kt` : config URL/anon key + session (tokens) en `SharedPreferences`. `data/remote/ProductLookupService.kt` : recherche produit par code-barres via l'API publique **Open Food Facts** (sans clé) — retourne `null` si non trouvé, repli sur la saisie manuelle.
- **Auth** — `data/auth/AuthManager.kt` : appelle directement l'API GoTrue (`/auth/v1/signup`, `/auth/v1/token`, `/auth/v1/logout`, `/auth/v1/recover`) — **pas de SDK Supabase**.
- **Synchro** — `data/sync/SyncManager.kt` : push/pull bidirectionnel (products, clients, sales, dettes, settings), protégé par `Mutex`, déclenché au démarrage, à la connexion, au retour réseau, toutes les 15 min. `SyncWorker.kt` : WorkManager périodique.
- **Services** — `service/ReceiptService.kt` (ticket texte + partage WhatsApp + impression Bluetooth ESC/POS), `service/PremiumManager.kt`, `service/FormatUtils.kt`.
- `MainActivity.kt` (Compose, edge-to-edge, vérifie la signature APK via `service/SignatureVerifier.kt`) et `LissafiApp.kt` (Application : instancie DB/API/auth/sync, démarre l'observer réseau et le SyncWorker).
- **TLS** — certificate pinning du domaine Supabase dans `res/xml/network_security_config.xml` (pin SPKI SHA-256, expiration 2027-08-12). ⚠️ Si Supabase fait tourner son certificat, mettre à jour le `<pin>` (voir le commentaire du fichier) avant que la synchro ne casse.

## Patterns clés à respecter

- **Local-first** : toute écriture passe D'ABORD en SQLite, puis `syncToRemote { ... }` en arrière-plan silencieux (les échecs réseau sont ignorés ici — le `SyncManager` retentera). Ne jamais bloquer l'UI sur le réseau.
- **L'argent est en `Int` FCFA** partout (jamais de double pour les montants). Les quantités vendues sont des `Double`.
- **Isolation par utilisateur** : chaque table a `user_id`. Le `LissafiRepository` porte un `currentUserIdProvider` réglé via `withUserId(userId)` depuis `LissafiNavHost`. Toujours passer le `userId` aux méthodes DB qui l'acceptent.
- **Fusion des ids de vente** : au push, Supabase génère un id `BIGSERIAL` ; `db.finalizeSalePush(localId, remoteId)` réaligne les références (`sale_items`, `debt_transactions`) ET marque `synced=1`, le tout dans UNE SEULE transaction SQLite (atomicité essentielle : un crash entre les deux créerait une vente en double au prochain push). Garder cette atomicité si tu touches à la synchro des ventes.
- **Schéma DB = miroir de `supabase-schema.sql`** : si tu modifies les tables dans `LissafiDatabase.onCreate`, mets à jour `supabase-schema.sql` (RLS par `auth.uid() = user_id` incluse) en miroir, et incrémente `DATABASE_VERSION` avec une migration dans `onUpgrade`.
- **Premium** : activation **côté serveur** — codes à usage unique dans la table `premium_codes`, validés par `redeem_premium_code()` (SECURITY DEFINER) ; l'app n'embarque plus aucun code. Démo 7 jours locale, limites gratuites (10 produits / 10 crédits / 10 ventes par jour). Statut dans `app_settings` (`is_premium`, `premium_expiry`, `demo_taken`) — posé UNIQUEMENT par le serveur (`redeem_premium_code`) ou le back-office (service_role) ; ces clés sont exclues de `pushSettings`. Pas de PIN admin dans l'app (fonctionnalité retirée, vestigiale — jamais branchée à un écran) : la gestion admin passe entièrement par le back-office.
- Les entités (`data/entity/*.kt`) sont `@Serializable` (kotlinx.serialization) — les noms de champs doivent coller aux colonnes PostgREST.
- Les dates sont des `Long` (epoch millis), les booléens SQLite sont des 0/1.

## Back-office web (`backoffice/`)

Interface d'administration indépendante de l'app : activation premium manuelle, stats globales, connexions, logs, support. Next.js 15 (App Router) + TypeScript + Tailwind, déployable sur Vercel. Détails complets dans `backoffice/README.md`.

- **Auth** : l'admin est un utilisateur Supabase Auth inscrit dans la table `admins`. Connexion → cookie httpOnly `lissafi_admin_token` (JWT). Côté serveur, `SUPABASE_SERVICE_ROLE_KEY` (env, jamais exposée au navigateur) donne accès à tout via bypass RLS. Session validée par `getAdminSession()` (`src/lib/session.ts`).
- **Schéma** : créé par `supabase-admin.sql` (racine) — à exécuter APRÈS `supabase-schema.sql`. Ajoute `admins`, `app_logs`, `support_tickets`, `ticket_replies`, `admin_settings`, `admin_actions`, plus des fonctions SECURITY DEFINER (`admin_stats()`, `admin_user_summaries()`, `admin_logs()`, séries…).
- **Serveur actions** : `src/actions/*.ts` (Server Actions) + `src/lib/supabase.ts` (client service_role) + `src/lib/data.ts` (requêtes aux fonctions d'agrégation). Env requises dans `backoffice/.env.example`.
- L'app Android alimente le back-office en fire-and-forget : `logEvent` / `reportSupportTicket` dans `SupabaseApi.kt`.
- **Portail partenaire** : projet Next.js **séparé** dans `portail-partenaire/` (déployé sur un domaine Vercel distinct du back-office, pour ne pas exposer l'admin). Routes publiques `/partenaire/*` + beacon `/api/visits`. Auto-inscription (`partnerSignUp` crée un user Supabase Auth via `service_role` + ligne `partners.auth_uid`), espace `/partenaire/espace` (lien perso `?p=CODE` + QR, stats, demande de retrait). Cookie httpOnly `lissafi_partner_token`, session `getPartnerSession()`. Tables partenaires **admin-only** (RLS `is_admin()`) : le portail lit/écrit via `service_role` filtré par l'id du cookie. CORS de `/api/visits` restreint à `NEXT_PUBLIC_LANDING_URL` (env du portail). Admin (back-office) : `/partenaires/retraits` valide les retraits (`approvePayout` solde les plus anciennes commissions dues). Design : `docs/superpowers/specs/2026-08-13-portail-partenaire-design.md`.

## Outils non-Android dans le repo

- `supabase-schema.sql` : schéma de l'app (tables, RLS par `auth.uid() = user_id`). Miroir de `LissafiDatabase` — voir Patterns clés.
- `supabase-admin.sql` : schéma du back-office (voir section dédiée). Idempotent, à exécuter après `supabase-schema.sql`.
- `portail-partenaire/` : portail partenaire public (Next.js, séparé du back-office). Env : `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `NEXT_PUBLIC_LANDING_URL`.
- `scripts/seed-demo.sql` + `scripts/README-demo.md` : seed Supabase du **compte démo** `demo@lissafi.app` / `demo123456` (boutique fictive complète pour vidéos TikTok). À coller dans le SQL Editor Supabase (projet `fnyuhpfzkvunscuylvqv`). Idempotent, ne touche que les données du compte démo.
- `landing/` : page de vente statique Vite + React (motion, framer-motion). `npm run dev` / `npm run build`. Coordonnées WhatsApp/APK à configurer dans `src/config.js`.
- `docs/demo-scenario-video.md` : scénario de tournage des 6 vidéos de démo. `docs/etude-marche-et-prix.md` : étude de marché.
- `marketing-tiktok/` : scripts de vidéos TikTok + guide montage.
- `prospection/` : outil Python séparé (scraper Google Maps + workflow n8n) pour générer des leads commerçants Niamey.
- `plan-lissafi.md` : doc stratégique/commerciale (pricing, roadmap). Utile pour le contexte produit, mais ne décris pas le code réel.
- `Lissafi.ai` : PDF (spec design, 1 page).

## Conventions

- Tout le code commenté et les messages de commit sont en **français**, commits en style conventional (ex. `fix(demo): …`, `feat(demo): …`, `docs: …`).
- Jamais de mention d'IA/Claude dans les commits (voir les conventions globales).

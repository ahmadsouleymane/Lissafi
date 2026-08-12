# Spec — Notifications push (Lissafi)

**Date** : 2026-08-12
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Ajouter aux commerçants utilisant l'app Android des notifications push :
1. **Envoi manuel** depuis le back-office (titre + message, ciblage avancé).
2. **Récap automatique** chaque matin, du lundi au vendredi à 8h (heure de
   Niamey), résumant les ventes de la veille + un message "bonne journée".

Aucune infrastructure de notifications n'existe actuellement (pas de Firebase,
pas de table de tokens) — c'est un nouveau système de bout en bout : app
Android, schéma Supabase, back-office.

## Contexte

- L'app Android est **local-first** (SQLite source de vérité, Supabase en
  sauvegarde/synchro). Le back-office (Next.js 15, App Router) a déjà accès
  total aux données via le client `service_role` (`supabaseAdmin()`,
  `src/lib/supabase.ts`), protégé par `getAdminSession()`.
- Aucun SDK Firebase n'est présent dans `app/build.gradle.kts` ni dans le
  projet racine. Aucun `google-services.json`.
- Pattern existant à réutiliser : table `admin_settings` (clé/valeur, toggle
  global lisible/écrivable sans redeploy), table `admin_actions` (journal
  d'actions admin), page `/comptes` (recherche + filtre statut premium sur
  `getUserSummaries()`).
- Argent en FCFA (`Int`), dates en `Long`/`BIGINT` epoch millis. Niamey =
  UTC+1 toute l'année (pas de changement d'heure).

## Prérequis externe (hors code)

Un projet **Firebase** (gratuit) doit être créé pour Lissafi, séparément de ce
travail :
- `google-services.json` → intégré à l'app Android (`app/`).
- Clé de compte de service (Admin SDK, JSON) → variable d'environnement
  serveur du back-office (`FIREBASE_SERVICE_ACCOUNT_JSON`), jamais exposée au
  navigateur — même traitement que `SUPABASE_SERVICE_ROLE_KEY`.

Le code est écrit pour consommer ces credentials via variables d'env ; leur
création reste une étape manuelle guidée séparément.

## Schéma Supabase (mirroir `LissafiDatabase` — voir patterns clés CLAUDE.md)

### Nouvelle table `device_tokens` (ajout à `supabase-schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (fcm_token)
);

ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "User manages own device tokens" ON device_tokens;
CREATE POLICY "User manages own device tokens" ON device_tokens
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
```

Un même `user_id` peut avoir plusieurs lignes (plusieurs téléphones). Upsert
sur `fcm_token` : si le token existe déjà (réinstall, autre compte sur le même
appareil), on le réassigne au nouvel utilisateur.

### Nouvelle table `notification_log` (ajout à `supabase-admin.sql`, service_role uniquement)

```sql
CREATE TABLE IF NOT EXISTS public.notification_log (
    id BIGSERIAL PRIMARY KEY,
    kind TEXT NOT NULL,                 -- 'manuel' | 'recap_quotidien'
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    admin_user_id UUID,                 -- NULL pour les envois automatiques
    target_summary TEXT NOT NULL DEFAULT '', -- description lisible du ciblage
    recap_date TEXT,                    -- 'YYYY-MM-DD' (heure Niamey), recap uniquement — anti-doublon
    recipients INT NOT NULL DEFAULT 0,
    success INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_log_recap_date
    ON public.notification_log(recap_date) WHERE kind = 'recap_quotidien';
CREATE INDEX IF NOT EXISTS idx_notification_log_created ON public.notification_log(created_at DESC);
```

L'index unique sur `recap_date` (pour `kind = 'recap_quotidien'`) empêche un
double envoi si le cron Vercel se redéclenche le même jour : la route insère
d'abord la ligne de log (avec `ON CONFLICT DO NOTHING`) et n'envoie que si
l'insertion a réussi.

### `admin_settings` — nouvelle clé

`recap_notifications_enabled` (`'true'` / `'false'`, défaut `'true'`) — permet
de couper le récap automatique sans redéploiement.

## Côté Android (app/)

- **Dépendances** : plugin `com.google.gms.google-services` (root +
  `app/build.gradle.kts`), `com.google.firebase:firebase-messaging-ktx` (BoM
  Firebase). `google-services.json` déposé dans `app/`.
- **`service/LissafiMessagingService.kt`** *(nouveau)*, étend
  `FirebaseMessagingService` :
  - `onNewToken(token)` → appelle `repository.registerDeviceToken(token)`
    (fire-and-forget réseau, cohérent avec le pattern `syncToRemote {}`
    existant — pas de blocage, pas de retry local, le token sera renvoyé au
    prochain `onNewToken`/démarrage si l'appel échoue).
  - `onMessageReceived(message)` → construit une notification locale
    (`NotificationCompat`, canal `"recap"` créé au démarrage de l'app) à
    partir de `message.notification.title/body`, affichée que l'app soit au
    premier plan ou en arrière-plan.
- **`data/remote/SupabaseApi.kt`** *(modifié)* — nouvelle méthode
  `upsertDeviceToken(userId: String, token: String)`, POST PostgREST sur
  `device_tokens` avec `Prefer: resolution=merge-duplicates` et
  `on_conflict=fcm_token` ; lève `SupabaseException` en cas d'échec (pattern
  existant).
- **`data/repository/LissafiRepository.kt`** *(modifié)* — nouvelle méthode
  `registerDeviceToken(token: String)` : appelle l'API distante uniquement
  (pas de stockage local nécessaire, ce n'est pas une donnée métier
  local-first) si un `userId` courant est présent ; sinon ne fait rien (token
  renvoyé au prochain login via `onNewToken` ou lecture explicite du token
  courant après connexion).
- **Enregistrement après connexion** : dans le flux d'auth réussie
  (`LissafiNavHost` / après `withUserId(userId)`), lecture du token FCM
  courant (`FirebaseMessaging.getInstance().token.await()`) et appel à
  `registerDeviceToken`.
- **Permission** : `POST_NOTIFICATIONS` (`AndroidManifest.xml`, Android 13+) +
  demande runtime au premier lancement post-connexion (pattern à ajouter dans
  l'écran d'onboarding/`MainActivity`, une simple demande de permission
  standard Compose `rememberLauncherForActivityResult`).
- **Icône de notification** : `meta-data
  com.google.firebase.messaging.default_notification_icon` dans le manifest.

## Côté back-office (backoffice/)

### Nouvelle section `/notifications`

- `src/app/(admin)/notifications/layout.tsx` *(nouveau)* — header de section.
- `src/app/(admin)/notifications/page.tsx` *(nouveau)* — page principale :
  - **Bloc "Récap automatique"** : statut actuel (activé/désactivé, lu depuis
    `admin_settings.recap_notifications_enabled`), toggle (server action
    `toggleRecapNotifications`), horaire fixe affiché ("Lundi–vendredi, 8h,
    heure de Niamey"), 5 dernières exécutions (`notification_log` filtré
    `kind='recap_quotidien'` : date, destinataires, succès/échecs).
  - **Bloc "Envoi manuel"** : formulaire (titre, message, ciblage) + server
    action `sendManualNotification` + historique des 20 derniers envois
    manuels (`notification_log` filtré `kind='manuel'`).
- Entrée de menu "Notifications" ajoutée à la nav de `AppShell`
  (`src/components/AppShell.tsx`).

### Ciblage (segmentation avancée)

Formulaire avec deux modes exclusifs (radio) :
- **Par statut** : select réutilisant les mêmes catégories que `/comptes`
  (tous / premium actif / gratuit / expiré).
- **Comptes précis** : champ recherche (email/boutique/téléphone, réutilise
  `getUserSummaries()`) + liste à cocher des comptes correspondants.

Résolution des destinataires : liste de `user_id` ciblés → jointure avec
`device_tokens` pour obtenir les tokens FCM. Les utilisateurs sans token
(app jamais ouverte depuis cette mise à jour, ou notifications refusées) sont
simplement absents de l'envoi — pas d'erreur affichée pour eux individuellement,
seulement dans le compteur `recipients` vs `success`.

### Envoi FCM (`src/lib/notifications.ts`, nouveau)

- Initialise `firebase-admin` une seule fois (`initializeApp` avec
  `credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON!))`),
  pattern singleton (comme `supabaseAdmin()`).
- `sendPushToTokens(tokens: string[], title: string, body: string):
  Promise<{ success: number; failed: number }>` — utilise
  `getMessaging().sendEachForMulticast()` par lots de 500 (limite FCM).
  Les tokens en erreur `messaging/registration-token-not-registered` sont
  supprimés de `device_tokens` (nettoyage best-effort, non bloquant).

### Server actions (`src/actions/notifications.ts`, nouveau)

- `sendManualNotification(formData)` : valide titre/message non vides,
  résout les destinataires selon le ciblage, appelle `sendPushToTokens`,
  insère la ligne `notification_log` (`kind='manuel'`, `admin_user_id` =
  session courante, `target_summary` = résumé lisible ex. "12 comptes premium"
  ou "3 comptes sélectionnés"), journalise aussi dans `admin_actions`
  (cohérent avec le reste du back-office).
- `toggleRecapNotifications(enabled: boolean)` : upsert `admin_settings`.

### Cron automatique — `/api/cron/recap-quotidien`

- `vercel.json` *(nouveau ou modifié)* :
  ```json
  { "crons": [{ "path": "/api/cron/recap-quotidien", "schedule": "0 7 * * 1-5" }] }
  ```
  `07:00 UTC` = `08:00` Niamey, tous les jours ouvrés (lundi=1 … vendredi=5).
- `src/app/api/cron/recap-quotidien/route.ts` *(nouveau)*, `GET` :
  1. Vérifie l'en-tête `Authorization: Bearer ${CRON_SECRET}` (variable d'env,
     envoyée automatiquement par Vercel Cron) → `401` sinon.
  2. Vérifie `admin_settings.recap_notifications_enabled` → sort (`200`, no-op)
     si désactivé.
  3. Calcule la date du jour en heure de Niamey (`UTC+1` fixe) → `recap_date`
     (`YYYY-MM-DD`) = hier. Insère une ligne `notification_log` (`kind =
     'recap_quotidien'`, `recap_date`) via `ON CONFLICT (recap_date) WHERE
     kind='recap_quotidien' DO NOTHING` → si l'insertion n'a rien fait (déjà
     traité aujourd'hui), sort immédiatement.
  4. Calcule les bornes epoch millis de "hier" (Niamey) et interroge, groupé
     par `user_id` :
     - `sales` : `SUM(total)`, `COUNT(*)` sur la fenêtre.
     - `debt_transactions` : `SUM(amount)` où `amount > 0` sur la fenêtre
       (nouvelles dettes ; les montants négatifs = remboursements, exclus).
  5. Pour chaque `user_id` ayant au moins un `device_token` : construit le
     message —
     - Avec ventes : `"Hier : {CA} FCFA de ventes en {n} transaction(s)
       {+ nouvelles dettes si > 0}. Bonne journée !"`
     - Sans vente : message générique encourageant + "Bonne journée !"
  6. Envoie en batch (regroupé par lots de tokens), met à jour la ligne
     `notification_log` avec `recipients`/`success`/`failed`.
- Route protégée uniquement par le secret cron — pas de session admin requise
  (appelée par Vercel, pas par un navigateur).

## Fichiers

**Racine**
- `supabase-schema.sql` *(modifié)* — table `device_tokens` + RLS.
- `supabase-admin.sql` *(modifié)* — table `notification_log` + clé
  `admin_settings.recap_notifications_enabled`.

**App Android (`app/`)**
- `build.gradle.kts` (racine + `app/`) *(modifié)* — plugin google-services,
  dépendance Firebase Messaging.
- `app/google-services.json` *(nouveau, fourni séparément)*.
- `src/main/java/com/lissafi/app/service/LissafiMessagingService.kt` *(nouveau)*.
- `data/remote/SupabaseApi.kt` *(modifié)* — `upsertDeviceToken`.
- `data/repository/LissafiRepository.kt` *(modifié)* — `registerDeviceToken`.
- `AndroidManifest.xml` *(modifié)* — service FCM, permission
  `POST_NOTIFICATIONS`, meta-data icône.
- Point d'appel post-connexion dans `ui/navigation/LissafiNavHost.kt` (ou
  équivalent) pour enregistrer le token après `withUserId`.

**Back-office (`backoffice/`)**
- `package.json` *(modifié)* — dépendance `firebase-admin`.
- `.env.example` *(modifié)* — `FIREBASE_SERVICE_ACCOUNT_JSON`, `CRON_SECRET`.
- `src/lib/notifications.ts` *(nouveau)* — init `firebase-admin` +
  `sendPushToTokens`.
- `src/actions/notifications.ts` *(nouveau)* — server actions.
- `src/lib/data.ts` *(modifié)* — requêtes de lecture (`getRecapHistory`,
  `getManualNotificationHistory`, `getRecapEnabled`).
- `src/app/(admin)/notifications/layout.tsx`, `page.tsx` *(nouveaux)*.
- `src/components/AppShell.tsx` *(modifié)* — entrée de nav.
- `src/app/api/cron/recap-quotidien/route.ts` *(nouveau)*.
- `vercel.json` *(nouveau ou modifié)* — déclaration du cron.

## Décisions techniques

- **Vercel Cron plutôt que pg_cron/Edge Function Supabase** : le back-office
  est déjà déployé sur Vercel avec les credentials nécessaires ; éviter un
  second runtime (Deno) qui dupliquerait la logique d'envoi FCM.
- **`firebase-admin` plutôt qu'appels HTTP manuels à l'API FCM v1** : gère
  l'authentification OAuth2 et le multicast, évite du code de signature de
  requête à maintenir.
- **Anti-doublon du récap** : contrainte SQL (index unique conditionnel), pas
  de verrou applicatif — robuste même si Vercel invoque la route deux fois.
- **Nettoyage des tokens invalides** : best-effort, ne bloque pas l'envoi aux
  autres destinataires.
- **Pas de stockage local du token FCM côté SQLite** : ce n'est pas une donnée
  métier local-first, juste un identifiant d'appareil ; renvoyé au serveur à
  chaque `onNewToken` ou connexion.

## Hors périmètre (V1)

- Configuration d'horaire personnalisable pour le récap (fixe : lun-ven 8h).
- Notifications riches (images, actions, deep links).
- Désinscription explicite du token au logout (le token reste associé au
  compte ; sera réassigné automatiquement si un autre utilisateur se connecte
  sur le même appareil, via l'upsert `ON CONFLICT (fcm_token)`).
- A/B testing ou planification différée des envois manuels.

## Vérification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis
  `./gradlew assembleDebug` (build Android avec les nouvelles dépendances Firebase).
- `cd backoffice && npm run build` (typecheck + build Next.js).
- Test manuel : connexion sur un appareil réel (émulateur avec Play Services
  ou téléphone), vérifier l'apparition d'une ligne dans `device_tokens`.
- Test manuel back-office : envoi d'une notification test à un compte
  sélectionné, vérifier la réception sur l'appareil et la ligne dans
  `notification_log`.
- Déclenchement manuel de `/api/cron/recap-quotidien` (avec le bon en-tête
  `Authorization`) pour valider le calcul du récap avant la première
  exécution planifiée réelle.

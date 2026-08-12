# Notifications push Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter au back-office Lissafi l'envoi de notifications push vers l'app Android — envoi manuel avec ciblage avancé, et récap automatique lundi–vendredi 8h (Niamey) des ventes de la veille.

**Architecture:** Nouvelle table `device_tokens` (Supabase, RLS par utilisateur) alimentée par l'app Android via Firebase Cloud Messaging ; le back-office (Next.js, déjà sur Vercel) envoie via `firebase-admin` (clé de service serveur) et journalise chaque envoi dans `notification_log` ; un Vercel Cron déclenche le récap quotidien.

**Tech Stack:** Firebase Cloud Messaging (Android : `firebase-messaging-ktx` ; serveur : `firebase-admin` npm), Supabase/PostgREST, Next.js 15 App Router (server actions), Kotlin/Compose.

## Global Constraints

- Argent en FCFA = `Int` (Kotlin) / `number` entier (TS) — jamais de flottant pour les montants.
- Dates = `Long` epoch millis (Kotlin) / `number` epoch millis (TS), sauf colonnes `TIMESTAMPTZ` (`created_at` des tables back-office, format existant).
- Isolation par utilisateur : toute nouvelle table métier porte `user_id UUID REFERENCES auth.users(id)` + RLS `auth.uid() = user_id` (voir `supabase-schema.sql`).
- Aucun test unitaire ni instrumentation n'existe dans ce repo (`app/src` et `backoffice/`) — la vérification de chaque tâche se fait par build/typecheck (`npm run build`, `./gradlew compileDebugKotlin` / `assembleDebug`), jamais par un lanceur de tests.
- Commentaires de code et messages de commit en **français**, style conventional commit (`feat: …`, `fix: …`), **aucune mention d'IA/Claude**.
- Niamey = UTC+1 toute l'année (pas de changement d'heure) — tout calcul de date "veille"/"aujourd'hui" côté serveur utilise un décalage fixe de 3 600 000 ms, jamais une bibliothèque de fuseaux horaires.
- Back-office : tout accès aux données passe par `supabaseAdmin()` (clé `service_role`, `src/lib/supabase.ts`), jamais exposé au navigateur (`import "server-only"`).

## Prérequis externe (hors plan, à faire par l'utilisateur en parallèle)

Un projet **Firebase** doit être créé pour Lissafi (gratuit, console.firebase.google.com) :

1. Créer le projet Firebase, y ajouter une app Android avec `applicationId = com.lissafi.app`.
2. Télécharger `google-services.json` → à placer dans `app/google-services.json` (racine du module `app/`, à côté de `build.gradle.kts`). **Ce fichier n'est pas commité par ce plan** — sans lui, `./gradlew assembleDebug` échouera dès la Tâche 9 (attendu, voir note dans cette tâche).
3. Générer une clé de compte de service (Firebase Console → Paramètres du projet → Comptes de service → Générer une nouvelle clé privée, JSON) → à coller dans la variable d'environnement `FIREBASE_SERVICE_ACCOUNT_JSON` du back-office (`.env` local et Vercel).

Aucune tâche de ce plan ne dépend de credentials réels pour être **écrite** ou **compilée côté TypeScript** ; seule la vérification finale Android (Tâche 9, build complet) et l'envoi réel d'une notification (Tâche 6 et Tâche 8, test manuel) nécessitent ces credentials.

---

### Task 1: Schéma Supabase — table `device_tokens`

**Files:**
- Modify: `supabase-schema.sql:80-81` (insertion après la table `app_settings`), `supabase-schema.sql:101-102` (RLS enable), `supabase-schema.sql:130-131` (policy)

**Interfaces:**
- Produces : table `device_tokens(id BIGSERIAL PK, user_id UUID, fcm_token TEXT UNIQUE, updated_at BIGINT)` — consommée par le back-office (Tâche 6, 8) et par l'app Android (Tâche 10, via `POST .../rest/v1/device_tokens?on_conflict=fcm_token`).

- [ ] **Step 1: Ajouter la table après `app_settings`**

Dans `supabase-schema.sql`, juste après le bloc de la table `app_settings` (ligne 80, avant le commentaire `-- INDEX`) :

```sql

-- 7. Table des tokens d'appareil (notifications push FCM)
CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (fcm_token)
);
```

- [ ] **Step 2: Activer la RLS**

Dans le bloc `-- ROW LEVEL SECURITY`, juste après `ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;` :

```sql
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
```

- [ ] **Step 3: Ajouter la policy**

Juste après la policy `"User sees own settings"` (fin de fichier) :

```sql

DROP POLICY IF EXISTS "User sees own device_tokens" ON device_tokens;
CREATE POLICY "User sees own device_tokens" ON device_tokens
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
```

- [ ] **Step 4: Vérifier le SQL**

Pas d'exécuteur SQL local dans ce repo — relire le fichier entier pour confirmer qu'il reste idempotent (tous les `CREATE TABLE IF NOT EXISTS`, `DROP POLICY IF EXISTS` sont respectés) :

Run: `grep -n "device_tokens" supabase-schema.sql`
Expected: 4 lignes (CREATE TABLE, ENABLE ROW LEVEL SECURITY, DROP POLICY, CREATE POLICY).

- [ ] **Step 5: Commit**

```bash
git add supabase-schema.sql
git commit -m "feat(db): ajoute la table device_tokens pour les notifications push"
```

---

### Task 2: Schéma back-office — `notification_log`, réglage récap, fonction d'agrégation

**Files:**
- Modify: `supabase-admin.sql` (nouvelle table + insertion de réglage + nouvelle fonction)

**Interfaces:**
- Produces : table `notification_log(id, kind, title, body, admin_user_id, target_summary, recap_date, recipients, success, failed, created_at)`, clé `admin_settings.recap_notifications_enabled`, fonction `admin_recap_yesterday(from_ts bigint, to_ts bigint) RETURNS TABLE(user_id uuid, sales_total bigint, sales_count bigint, new_debts_total bigint)` — consommés par `backoffice/src/lib/data.ts`, `src/actions/notifications.ts` et `src/app/api/cron/recap-quotidien/route.ts` (Tâches 5, 6, 8).

- [ ] **Step 1: Ajouter la table `notification_log`**

Dans `supabase-admin.sql`, juste après le bloc `admin_actions` (après la ligne `CREATE INDEX IF NOT EXISTS idx_admin_actions_target ...`) et avant la section `-- 6. FONCTIONS D'AGRÉGATION` :

```sql
-- ============================================================
-- 5bis. Journal des notifications push (manuelles + récap auto)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.notification_log (
    id BIGSERIAL PRIMARY KEY,
    kind TEXT NOT NULL,                        -- 'manuel' | 'recap_quotidien'
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    admin_user_id UUID,                        -- NULL pour les envois automatiques
    target_summary TEXT NOT NULL DEFAULT '',
    recap_date TEXT,                            -- 'YYYY-MM-DD' (heure Niamey), recap uniquement
    recipients INT NOT NULL DEFAULT 0,
    success INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_log_recap_date
    ON public.notification_log(recap_date) WHERE kind = 'recap_quotidien';
CREATE INDEX IF NOT EXISTS idx_notification_log_created ON public.notification_log(created_at DESC);
```

- [ ] **Step 2: Ajouter le réglage `recap_notifications_enabled`**

Dans le bloc d'insertion des `admin_settings` par défaut, ajouter une ligne :

```sql
INSERT INTO public.admin_settings (key, value) VALUES
    ('premium_price_fcfa', '10000'),
    ('premium_days', '365'),
    ('demo_days', '7'),
    ('recap_notifications_enabled', 'true')
ON CONFLICT (key) DO NOTHING;
```

(remplace le bloc `INSERT INTO public.admin_settings` existant — mêmes 3 premières lignes, une 4e ajoutée.)

- [ ] **Step 3: Ajouter la fonction d'agrégation `admin_recap_yesterday`**

À la fin du fichier (après `admin_logs`, avant la section `-- 7. MISE À JOUR du schéma existant`) :

```sql

-- Agrégats de la veille par utilisateur, pour le récap automatique quotidien.
-- Ne renvoie que les utilisateurs ayant eu au moins une vente dans la fenêtre ;
-- les utilisateurs sans vente sont traités côté application (valeurs à 0).
CREATE OR REPLACE FUNCTION public.admin_recap_yesterday(from_ts bigint, to_ts bigint)
RETURNS TABLE(user_id uuid, sales_total bigint, sales_count bigint, new_debts_total bigint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT
        s.user_id,
        coalesce(sum(s.total), 0)::bigint AS sales_total,
        count(s.id)::bigint AS sales_count,
        coalesce((
            SELECT sum(dt.amount) FROM debt_transactions dt
            WHERE dt.user_id = s.user_id
              AND dt.date BETWEEN from_ts AND to_ts
              AND dt.amount > 0
        ), 0)::bigint AS new_debts_total
    FROM sales s
    WHERE s.date BETWEEN from_ts AND to_ts
    GROUP BY s.user_id;
$func$;
```

- [ ] **Step 4: Vérifier le SQL**

Run: `grep -n "notification_log\|admin_recap_yesterday\|recap_notifications_enabled" supabase-admin.sql`
Expected: occurrences dans la table, l'index, l'insertion de réglage et la fonction — aucune erreur de copier-coller (parenthèses/`$func$` équilibrés à l'œil).

- [ ] **Step 5: Commit**

```bash
git add supabase-admin.sql
git commit -m "feat(db): ajoute notification_log, le reglage recap et la fonction d'agregation admin_recap_yesterday"
```

---

### Task 3: Back-office — dépendance `firebase-admin` et `lib/notifications.ts`

**Files:**
- Modify: `backoffice/package.json`
- Create: `backoffice/src/lib/notifications.ts`
- Modify: `backoffice/.env.example`

**Interfaces:**
- Consumes: `supabaseAdmin()` de `@/lib/supabase`.
- Produces: `sendPushToTokens(tokens: string[], title: string, body: string): Promise<{ success: number; failed: number }>` — consommé par `src/actions/notifications.ts` (Tâche 6) et `src/app/api/cron/recap-quotidien/route.ts` (Tâche 8). Constante partagée `CHANNEL_ID = "lissafi_recap"` (doit correspondre au `CHANNEL_ID` Kotlin de la Tâche 10).

- [ ] **Step 1: Ajouter la dépendance**

Dans `backoffice/package.json`, section `dependencies` (ordre alphabétique existant) :

```json
    "@supabase/supabase-js": "^2.45.0",
    "firebase-admin": "^14.2.0",
    "next": "^15.1.6",
```

Run: `cd backoffice && npm install`
Expected: `firebase-admin` ajouté à `node_modules` et `package-lock.json` mis à jour, aucune erreur.

- [ ] **Step 2: Créer `src/lib/notifications.ts`**

```ts
import "server-only";
import { getApps, initializeApp, cert, type App } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { supabaseAdmin } from "./supabase";

// Doit correspondre au CHANNEL_ID de LissafiMessagingService.kt (canal Android),
// pour que les notifications reçues en premier plan et en arrière-plan utilisent
// le même canal.
const ANDROID_CHANNEL_ID = "lissafi_recap";

let firebaseApp: App | null = null;

function getFirebaseApp(): App {
  if (firebaseApp) return firebaseApp;
  const existing = getApps();
  if (existing.length > 0) {
    firebaseApp = existing[0];
    return firebaseApp;
  }
  const json = process.env.FIREBASE_SERVICE_ACCOUNT_JSON?.trim();
  if (!json) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON manquant — voir .env.example.");
  const credentials = JSON.parse(json);
  firebaseApp = initializeApp({ credential: cert(credentials) });
  return firebaseApp;
}

export type PushResult = { success: number; failed: number };

/**
 * Envoie une notification push (titre + texte) à une liste de tokens FCM,
 * par lots de 500 (limite de l'API FCM). Supprime de `device_tokens` les
 * tokens invalides rencontrés (nettoyage best-effort, non bloquant).
 */
export async function sendPushToTokens(tokens: string[], title: string, body: string): Promise<PushResult> {
  const unique = Array.from(new Set(tokens.filter(Boolean)));
  if (unique.length === 0) return { success: 0, failed: 0 };

  const messaging = getMessaging(getFirebaseApp());
  const staleTokens: string[] = [];
  let success = 0;
  let failed = 0;

  for (let i = 0; i < unique.length; i += 500) {
    const batch = unique.slice(i, i + 500);
    const response = await messaging.sendEachForMulticast({
      tokens: batch,
      notification: { title, body },
      android: { notification: { channelId: ANDROID_CHANNEL_ID } },
    });
    success += response.successCount;
    failed += response.failureCount;
    response.responses.forEach((r, idx) => {
      if (!r.success && r.error?.code === "messaging/registration-token-not-registered") {
        staleTokens.push(batch[idx]);
      }
    });
  }

  if (staleTokens.length > 0) {
    await supabaseAdmin().from("device_tokens").delete().in("fcm_token", staleTokens);
  }

  return { success, failed };
}
```

- [ ] **Step 3: Documenter les variables d'environnement**

Dans `backoffice/.env.example`, ajouter à la fin :

```bash

# ============================================================
# NOTIFICATIONS PUSH (Firebase Cloud Messaging)
# ============================================================

# Clé de compte de service Firebase Admin SDK — JSON complet sur une seule
# ligne. Firebase Console → Paramètres du projet → Comptes de service →
# Générer une nouvelle clé privée.
FIREBASE_SERVICE_ACCOUNT_JSON={"type":"service_account","project_id":"...",...}

# Secret partagé avec Vercel Cron pour protéger /api/cron/recap-quotidien.
# Vercel l'envoie automatiquement en en-tête Authorization quand cette
# variable est définie dans les réglages du projet.
CRON_SECRET=
```

- [ ] **Step 4: Vérifier le typecheck**

Run: `cd backoffice && npx tsc --noEmit`
Expected: aucune erreur (le fichier compile ; `FIREBASE_SERVICE_ACCOUNT_JSON` absent en local ne casse pas le typecheck, seulement l'exécution).

- [ ] **Step 5: Commit**

```bash
git add backoffice/package.json backoffice/package-lock.json backoffice/src/lib/notifications.ts backoffice/.env.example
git commit -m "feat(backoffice): ajoute l'envoi de notifications push via firebase-admin"
```

---

### Task 4: Back-office — extraire `logAdminAction` dans `lib/audit.ts`

**Files:**
- Create: `backoffice/src/lib/audit.ts`
- Modify: `backoffice/src/actions/admin.ts:1-22`

**Interfaces:**
- Produces: `logAdminAction(action: string, targetUserId: string | null, details: Record<string, unknown>): Promise<void>` — remplace la fonction privée `logAction` d'`admin.ts`, réutilisée par `src/actions/notifications.ts` (Tâche 6).

Cette extraction évite de dupliquer la logique de traçabilité (actuellement privée à `admin.ts`) dans le nouveau fichier d'actions de notifications.

- [ ] **Step 1: Créer `src/lib/audit.ts`**

```ts
import "server-only";
import { supabaseAdmin } from "./supabase";
import { requireAdmin } from "./session";

/** Journalise une action admin (traçabilité). Ne bloque jamais l'action appelante. */
export async function logAdminAction(action: string, targetUserId: string | null, details: Record<string, unknown>) {
  const session = await requireAdmin();
  try {
    await supabaseAdmin().from("admin_actions").insert({
      admin_user_id: session.userId,
      action,
      target_user_id: targetUserId,
      details: JSON.stringify(details),
    });
  } catch {
    // Le journal ne doit jamais faire échouer l'action principale.
  }
}
```

- [ ] **Step 2: Faire pointer `admin.ts` vers `lib/audit.ts`**

Dans `backoffice/src/actions/admin.ts`, remplacer les lignes 1 à 22 :

```ts
"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";

const DAY_MS = 86400000;

/** Journalise une action admin (traçabilité). Ne bloque jamais. */
async function logAction(action: string, targetUserId: string | null, details: Record<string, unknown>) {
  const session = await requireAdmin();
  try {
    await supabaseAdmin().from("admin_actions").insert({
      admin_user_id: session.userId,
      action,
      target_user_id: targetUserId,
      details: JSON.stringify(details),
    });
  } catch {
    // Le journal ne doit jamais faire échouer l'action principale.
  }
}
```

par :

```ts
"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction as logAction } from "@/lib/audit";

const DAY_MS = 86400000;
```

(le reste du fichier, qui appelle `logAction(...)`, ne change pas — l'alias `as logAction` évite de toucher les 8 sites d'appel existants.)

- [ ] **Step 3: Vérifier le build**

Run: `cd backoffice && npm run build`
Expected: `✓ Compiled successfully`, aucune erreur de type, toutes les routes listées comme avant.

- [ ] **Step 4: Commit**

```bash
git add backoffice/src/lib/audit.ts backoffice/src/actions/admin.ts
git commit -m "refactor(backoffice): extrait logAdminAction dans lib/audit pour le partager avec les notifications"
```

---

### Task 5: Back-office — types et lectures (`data.ts`, `types.ts`, icône)

**Files:**
- Modify: `backoffice/src/types.ts` (ajout en fin de fichier)
- Modify: `backoffice/src/lib/data.ts` (nouvel import + nouvelles fonctions en fin de fichier)
- Modify: `backoffice/src/components/icons.tsx` (ajout `IconBell`)

**Interfaces:**
- Consumes: `supabaseAdmin()`, `getAdminSettings()` (déjà dans `data.ts`).
- Produces: type `NotificationLog`, fonctions `getManualNotificationHistory(limit?: number): Promise<NotificationLog[]>`, `getRecapHistory(limit?: number): Promise<NotificationLog[]>`, `getRecapEnabled(): Promise<boolean>`, composant `IconBell` — consommés par `src/app/(admin)/notifications/page.tsx` (Tâche 7).

- [ ] **Step 1: Ajouter le type `NotificationLog`**

À la fin de `backoffice/src/types.ts` :

```ts

export type NotificationLog = {
  id: number;
  kind: "manuel" | "recap_quotidien";
  title: string;
  body: string;
  admin_user_id: string | null;
  target_summary: string;
  recap_date: string | null;
  recipients: number;
  success: number;
  failed: number;
  created_at: string;
};
```

- [ ] **Step 2: Ajouter l'import du type dans `data.ts`**

Dans `backoffice/src/lib/data.ts`, dans le bloc d'import `type { ... } from "@/types"`, ajouter `NotificationLog` (ordre alphabétique) :

```ts
import type {
  AccountSalePage,
  AdminAction,
  AppLog,
  AppSettingRow,
  AuditLog,
  Client,
  DebtTransaction,
  ExplorerData,
  NotificationLog,
  Product,
  SalesPoint,
  SaleItem,
  SaleRow,
  SignupPoint,
  Stats,
  Ticket,
  TicketReply,
  UserSummary,
} from "@/types";
```

- [ ] **Step 3: Ajouter les fonctions de lecture**

À la fin de `backoffice/src/lib/data.ts` :

```ts

// ============================================================
// Notifications push
// ============================================================

/** Historique des envois manuels de notifications (les plus récents d'abord). */
export async function getManualNotificationHistory(limit = 20): Promise<NotificationLog[]> {
  const { data, error } = await supabaseAdmin()
    .from("notification_log")
    .select("*")
    .eq("kind", "manuel")
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error) logRpcError("getManualNotificationHistory", error);
  return (data ?? []) as NotificationLog[];
}

/** Historique des exécutions du récap automatique quotidien. */
export async function getRecapHistory(limit = 5): Promise<NotificationLog[]> {
  const { data, error } = await supabaseAdmin()
    .from("notification_log")
    .select("*")
    .eq("kind", "recap_quotidien")
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error) logRpcError("getRecapHistory", error);
  return (data ?? []) as NotificationLog[];
}

/** État du réglage "récap quotidien activé" (activé par défaut si non défini). */
export async function getRecapEnabled(): Promise<boolean> {
  const settings = await getAdminSettings();
  return settings["recap_notifications_enabled"] !== "false";
}
```

- [ ] **Step 4: Ajouter `IconBell`**

À la fin de `backoffice/src/components/icons.tsx` :

```tsx

export const IconBell = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M6 8a6 6 0 0 1 12 0c0 4.5 1.5 6 2 7H4c.5-1 2-2.5 2-7Z" />
    <path d="M10 19a2 2 0 0 0 4 0" />
  </svg>
);
```

- [ ] **Step 5: Vérifier le build**

Run: `cd backoffice && npm run build`
Expected: `✓ Compiled successfully`, aucune erreur de type.

- [ ] **Step 6: Commit**

```bash
git add backoffice/src/types.ts backoffice/src/lib/data.ts backoffice/src/components/icons.tsx
git commit -m "feat(backoffice): ajoute les lectures et le type NotificationLog pour les notifications"
```

---

### Task 6: Back-office — server actions d'envoi manuel et de toggle récap

**Files:**
- Create: `backoffice/src/actions/notifications.ts`

**Interfaces:**
- Consumes: `supabaseAdmin()` (`@/lib/supabase`), `requireAdmin()` (`@/lib/session`), `logAdminAction()` (`@/lib/audit`, Tâche 4), `sendPushToTokens()` (`@/lib/notifications`, Tâche 3), `getUserSummaries()` (`@/lib/data`), type `ActionResult` (`@/actions/admin`).
- Produces: `sendManualNotificationForm(prev: ActionResult | undefined, formData: FormData): Promise<ActionResult>`, `setRecapNotificationsEnabled(enabled: boolean): Promise<ActionResult>`, `setRecapNotificationsEnabledQuick(enabled: boolean): Promise<void>` — consommés par `NotificationForm.tsx` et `notifications/page.tsx` (Tâche 7).

- [ ] **Step 1: Écrire le fichier**

```ts
"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction } from "@/lib/audit";
import { sendPushToTokens } from "@/lib/notifications";
import { getUserSummaries } from "@/lib/data";
import type { ActionResult } from "./admin";
import type { UserSummary } from "@/types";

const STATUS_LABELS: Record<string, string> = {
  all: "tous les comptes",
  premium: "comptes premium actifs",
  free: "comptes gratuits",
  expired: "comptes premium expirés",
};

function resolveStatusTargets(users: UserSummary[], status: string): UserSummary[] {
  const now = Date.now();
  switch (status) {
    case "premium":
      return users.filter((u) => u.premium && u.premium_expiry > now);
    case "expired":
      return users.filter((u) => u.premium && u.premium_expiry <= now);
    case "free":
      return users.filter((u) => !u.premium);
    default:
      return users;
  }
}

/** Variante formulaire : envoi manuel d'une notification push (useActionState). */
export async function sendManualNotificationForm(_prev: ActionResult | undefined, formData: FormData): Promise<ActionResult> {
  const session = await requireAdmin();

  const title = String(formData.get("title") || "").trim();
  const body = String(formData.get("body") || "").trim();
  const mode = String(formData.get("mode") || "status");
  if (!title || !body) return { error: "Titre et message sont requis." };

  let targetUserIds: string[];
  let targetSummary: string;

  if (mode === "accounts") {
    targetUserIds = formData.getAll("userIds").map(String).filter(Boolean);
    if (targetUserIds.length === 0) return { error: "Sélectionnez au moins un compte." };
    targetSummary = `${targetUserIds.length} compte(s) sélectionné(s)`;
  } else {
    const status = String(formData.get("status") || "all");
    const users = await getUserSummaries();
    targetUserIds = resolveStatusTargets(users, status).map((u) => u.user_id);
    targetSummary = `${targetUserIds.length} ${STATUS_LABELS[status] ?? "comptes"}`;
    if (targetUserIds.length === 0) return { error: "Aucun compte ne correspond à ce ciblage." };
  }

  const { data: tokenRows, error: tokenError } = await supabaseAdmin()
    .from("device_tokens")
    .select("fcm_token")
    .in("user_id", targetUserIds);
  if (tokenError) return { error: tokenError.message };

  const tokens = (tokenRows ?? []).map((r) => r.fcm_token as string);
  const { success, failed } = await sendPushToTokens(tokens, title, body);

  await supabaseAdmin().from("notification_log").insert({
    kind: "manuel",
    title,
    body,
    admin_user_id: session.userId,
    target_summary: targetSummary,
    recipients: tokens.length,
    success,
    failed,
  });

  await logAdminAction("notification_send", null, { title, targetSummary, recipients: tokens.length, success, failed });
  revalidatePath("/notifications", "layout");
  return { ok: true };
}

/** Active/désactive le récap automatique quotidien. */
export async function setRecapNotificationsEnabled(enabled: boolean): Promise<ActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("admin_settings")
    .upsert({ key: "recap_notifications_enabled", value: enabled ? "true" : "false" });
  if (error) return { error: error.message };

  await logAdminAction("recap_toggle", null, { enabled });
  revalidatePath("/notifications", "layout");
  return { ok: true };
}

/** Variante "quick" pour un bouton `<form action={...}>` sans retour d'état. */
export async function setRecapNotificationsEnabledQuick(enabled: boolean) {
  "use server";
  const r = await setRecapNotificationsEnabled(enabled);
  if (r && "error" in r) console.error("[notifications] setRecapNotificationsEnabledQuick:", r.error);
}
```

- [ ] **Step 2: Vérifier le build**

Run: `cd backoffice && npm run build`
Expected: `✓ Compiled successfully`, aucune erreur de type (en particulier sur `ActionResult` importé depuis `./admin`).

- [ ] **Step 3: Commit**

```bash
git add backoffice/src/actions/notifications.ts
git commit -m "feat(backoffice): ajoute les actions d'envoi manuel et de bascule du recap quotidien"
```

---

### Task 7: Back-office — page `/notifications` et navigation

**Files:**
- Create: `backoffice/src/components/NotificationForm.tsx`
- Create: `backoffice/src/app/(admin)/notifications/page.tsx`
- Modify: `backoffice/src/components/AppShell.tsx:23-32`

**Interfaces:**
- Consumes: `sendManualNotificationForm`, `setRecapNotificationsEnabledQuick` (`@/actions/notifications`, Tâche 6), `getUserSummaries`, `getRecapEnabled`, `getRecapHistory`, `getManualNotificationHistory` (`@/lib/data`, Tâche 5), `IconBell` (`@/components/icons`, Tâche 5), `type ActionResult` (`@/actions/admin`), `type UserSummary` (`@/types`), primitives `Badge, Button, Card, CardHeader, EmptyState, Field, Input, PageHeader, Select, Spinner, Table, Td, Th, THead, Textarea, Tr` (`@/components/ui`).
- Produces: route `/notifications` visible dans la nav admin.

- [ ] **Step 1: Créer `NotificationForm.tsx`**

```tsx
"use client";

import { useActionState, useMemo, useState } from "react";
import { sendManualNotificationForm } from "@/actions/notifications";
import type { ActionResult } from "@/actions/admin";
import type { UserSummary } from "@/types";
import { Button, Card, CardHeader, Field, Input, Select, Spinner, Textarea } from "./ui";

function Feedback({ state }: { state: ActionResult | undefined }) {
  if (!state) return null;
  if (state.ok) return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Notification envoyée.</p>;
  if (state.error) return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function NotificationForm({ accounts }: { accounts: UserSummary[] }) {
  const [state, action, pending] = useActionState<ActionResult | undefined, FormData>(sendManualNotificationForm, undefined);
  const [mode, setMode] = useState<"status" | "accounts">("status");
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    if (!q) return accounts;
    return accounts.filter((a) => `${a.email ?? ""} ${a.shop_name} ${a.shop_phone}`.toLowerCase().includes(q));
  }, [accounts, query]);

  return (
    <Card>
      <CardHeader title="Envoi manuel" subtitle="Titre + message envoyés en push à l'app Android" />
      <form action={action} className="space-y-4 px-5 pb-5">
        <Field label="Titre">
          <Input type="text" name="title" required maxLength={80} placeholder="Ex: Nouvelle fonctionnalité disponible" />
        </Field>
        <Field label="Message">
          <Textarea name="body" required maxLength={500} placeholder="Le contenu de la notification…" />
        </Field>

        <div>
          <span className="mb-1 block text-xs font-medium text-slate-600">Destinataires</span>
          <div className="flex gap-4 text-sm text-slate-700">
            <label className="flex items-center gap-1.5">
              <input type="radio" name="mode" value="status" checked={mode === "status"} onChange={() => setMode("status")} />
              Par statut
            </label>
            <label className="flex items-center gap-1.5">
              <input type="radio" name="mode" value="accounts" checked={mode === "accounts"} onChange={() => setMode("accounts")} />
              Comptes précis
            </label>
          </div>
        </div>

        {mode === "status" ? (
          <Field label="Statut ciblé">
            <Select name="status" defaultValue="all">
              <option value="all">Tous les comptes</option>
              <option value="premium">Premium actif</option>
              <option value="free">Gratuit</option>
              <option value="expired">Premium expiré</option>
            </Select>
          </Field>
        ) : (
          <div>
            <Input
              type="search"
              placeholder="Rechercher par boutique, email ou téléphone…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="mb-2"
            />
            <div className="max-h-56 overflow-y-auto rounded-lg border border-slate-200">
              {filtered.length === 0 && <p className="px-3 py-4 text-center text-xs text-slate-400">Aucun compte trouvé.</p>}
              {filtered.map((a) => (
                <label key={a.user_id} className="flex items-center gap-2 border-b border-slate-100 px-3 py-2 text-sm last:border-0 hover:bg-slate-50">
                  <input type="checkbox" name="userIds" value={a.user_id} />
                  <span className="truncate">
                    {a.shop_name || "Boutique sans nom"} — {a.email || "—"}
                  </span>
                </label>
              ))}
            </div>
          </div>
        )}

        <Feedback state={state} />
        <Button type="submit" disabled={pending}>
          {pending && <Spinner />} Envoyer la notification
        </Button>
      </form>
    </Card>
  );
}
```

- [ ] **Step 2: Créer la page `/notifications`**

```tsx
import { NotificationForm } from "@/components/NotificationForm";
import { setRecapNotificationsEnabledQuick } from "@/actions/notifications";
import { Badge, Button, Card, CardHeader, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { IconBell } from "@/components/icons";
import { getManualNotificationHistory, getRecapEnabled, getRecapHistory, getUserSummaries } from "@/lib/data";
import { formatDateTimeIso } from "@/lib/format";

export default async function NotificationsPage() {
  const [accounts, recapEnabled, recapHistory, manualHistory] = await Promise.all([
    getUserSummaries(),
    getRecapEnabled(),
    getRecapHistory(),
    getManualNotificationHistory(),
  ]);

  return (
    <div className="space-y-5">
      <PageHeader
        title="Notifications"
        subtitle="Envoi manuel et récap automatique quotidien vers l'app Android"
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconBell size={15} /> {accounts.length} compte(s)
          </span>
        }
      />

      <Card>
        <CardHeader
          title="Récap automatique"
          subtitle="Lundi–vendredi, 8h, heure de Niamey — CA, ventes et nouvelles dettes de la veille"
          action={
            <form action={setRecapNotificationsEnabledQuick.bind(null, !recapEnabled)}>
              <Button type="submit" size="sm" variant={recapEnabled ? "dangerOutline" : "secondary"}>
                {recapEnabled ? "Désactiver" : "Activer"}
              </Button>
            </form>
          }
        />
        <div className="px-5 pb-5">
          <Badge color={recapEnabled ? "green" : "gray"}>{recapEnabled ? "Activé" : "Désactivé"}</Badge>
          <div className="mt-4">
            {recapHistory.length === 0 ? (
              <EmptyState title="Aucune exécution pour l'instant" subtitle="La première aura lieu au prochain jour ouvré, 8h (Niamey)." />
            ) : (
              <Table>
                <THead>
                  <Th>Date</Th>
                  <Th>Destinataires</Th>
                  <Th>Succès</Th>
                  <Th>Échecs</Th>
                </THead>
                <tbody>
                  {recapHistory.map((r) => (
                    <Tr key={r.id}>
                      <Td>{formatDateTimeIso(r.created_at)}</Td>
                      <Td>{r.recipients}</Td>
                      <Td>{r.success}</Td>
                      <Td>{r.failed}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </div>
      </Card>

      <NotificationForm accounts={accounts} />

      <Card>
        <CardHeader title="Historique des envois manuels" />
        <div className="px-5 pb-5">
          {manualHistory.length === 0 ? (
            <EmptyState title="Aucun envoi manuel pour l'instant" />
          ) : (
            <Table>
              <THead>
                <Th>Date</Th>
                <Th>Titre</Th>
                <Th>Ciblage</Th>
                <Th>Destinataires</Th>
                <Th>Succès</Th>
                <Th>Échecs</Th>
              </THead>
              <tbody>
                {manualHistory.map((n) => (
                  <Tr key={n.id}>
                    <Td>{formatDateTimeIso(n.created_at)}</Td>
                    <Td className="max-w-xs truncate">{n.title}</Td>
                    <Td>{n.target_summary}</Td>
                    <Td>{n.recipients}</Td>
                    <Td>{n.success}</Td>
                    <Td>{n.failed}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Ajouter l'entrée de navigation**

Dans `backoffice/src/components/AppShell.tsx`, importer `IconBell` et l'ajouter au tableau `NAV` :

```ts
import {
  IconActivity,
  IconBell,
  IconChat,
  IconCrown,
  IconDatabase,
  IconGrid,
  IconLogIn,
  IconLogout,
  IconMenu,
  IconClose,
  IconSettings,
  IconUsers,
} from "./icons";
```

```ts
const NAV = [
  { href: "/", label: "Tableau de bord", icon: IconGrid, exact: true },
  { href: "/comptes", label: "Comptes", icon: IconUsers },
  { href: "/premium", label: "Premium", icon: IconCrown },
  { href: "/notifications", label: "Notifications", icon: IconBell },
  { href: "/logs", label: "Erreurs & activité", icon: IconActivity },
  { href: "/connexions", label: "Connexions", icon: IconLogIn },
  { href: "/support", label: "Support", icon: IconChat },
  { href: "/reglages", label: "Réglages", icon: IconSettings },
  { href: "/donnees", label: "Données", icon: IconDatabase },
];
```

- [ ] **Step 4: Vérifier le build**

Run: `cd backoffice && npm run build`
Expected: `✓ Compiled successfully`, la route `/notifications` apparaît dans la liste des routes générées (marquée `ƒ` — dynamique).

- [ ] **Step 5: Test manuel (nécessite les credentials Firebase réels)**

Run: `cd backoffice && npm run dev`, se connecter en admin, aller sur `/notifications`, envoyer une notification test à un compte disposant d'un token FCM valide.
Expected: la notification apparaît dans l'historique avec `success: 1` ; si `FIREBASE_SERVICE_ACCOUNT_JSON` n'est pas encore configuré, l'action échoue avec le message d'erreur explicite du `throw` de `getFirebaseApp()` — comportement attendu tant que le prérequis Firebase n'est pas rempli.

- [ ] **Step 6: Commit**

```bash
git add backoffice/src/components/NotificationForm.tsx "backoffice/src/app/(admin)/notifications/page.tsx" backoffice/src/components/AppShell.tsx
git commit -m "feat(backoffice): ajoute la page Notifications (envoi manuel + etat du recap)"
```

---

### Task 8: Back-office — cron du récap quotidien

**Files:**
- Create: `backoffice/src/app/api/cron/recap-quotidien/route.ts`
- Create: `backoffice/vercel.json`
- Modify: `backoffice/README.md`

**Interfaces:**
- Consumes: `supabaseAdmin()` (`@/lib/supabase`), `sendPushToTokens()` (`@/lib/notifications`, Tâche 3), RPC `admin_recap_yesterday` (Tâche 2), table `notification_log` (Tâche 2), table `device_tokens` (Tâche 1).
- Produces: route `GET /api/cron/recap-quotidien`, appelée par Vercel Cron selon `vercel.json`.

- [ ] **Step 1: Écrire la route**

```ts
import { NextRequest, NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase";
import { sendPushToTokens } from "@/lib/notifications";

export const dynamic = "force-dynamic";

// Niamey = UTC+1 toute l'année (pas de changement d'heure).
const NIAMEY_OFFSET_MS = 60 * 60 * 1000;

function niameyTodayDateStr(nowMs: number): string {
  const d = new Date(nowMs + NIAMEY_OFFSET_MS);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function yesterdayWindowMs(nowMs: number): { from: number; to: number } {
  const d = new Date(nowMs + NIAMEY_OFFSET_MS);
  const todayMidnightNiameyUTC = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()) - NIAMEY_OFFSET_MS;
  return { from: todayMidnightNiameyUTC - 86400000, to: todayMidnightNiameyUTC - 1 };
}

function buildMessage(salesTotal: number, salesCount: number, newDebtsTotal: number): { title: string; body: string } {
  if (salesCount === 0) {
    return { title: "Nouvelle journée, bonne chance !", body: "Aucune vente enregistrée hier. Bonne journée !" };
  }
  const fcfa = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(salesTotal);
  let body = `Hier : ${fcfa} FCFA de ventes en ${salesCount} transaction${salesCount > 1 ? "s" : ""}.`;
  if (newDebtsTotal > 0) {
    const debtFcfa = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(newDebtsTotal);
    body += ` Nouvelles dettes : ${debtFcfa} FCFA.`;
  }
  body += " Bonne journée !";
  return { title: "Récap d'hier", body };
}

export async function GET(request: NextRequest) {
  const authHeader = request.headers.get("authorization");
  if (authHeader !== `Bearer ${process.env.CRON_SECRET}`) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }

  const { data: settingRow } = await supabaseAdmin()
    .from("admin_settings")
    .select("value")
    .eq("key", "recap_notifications_enabled")
    .maybeSingle();
  if (settingRow && settingRow.value === "false") {
    return NextResponse.json({ skipped: "disabled" });
  }

  const now = Date.now();
  const recapDate = niameyTodayDateStr(now);

  // Réservation anti-doublon : si l'insertion échoue (contrainte unique sur
  // recap_date), le récap d'aujourd'hui a déjà été traité (ou est en cours).
  const { error: insertError } = await supabaseAdmin().from("notification_log").insert({
    kind: "recap_quotidien",
    recap_date: recapDate,
    title: "Récap quotidien",
    body: "",
    recipients: 0,
    success: 0,
    failed: 0,
  });
  if (insertError) {
    return NextResponse.json({ skipped: "already_sent_or_error", detail: insertError.message });
  }

  const { from, to } = yesterdayWindowMs(now);

  const [{ data: recapRows }, { data: deviceRows }] = await Promise.all([
    supabaseAdmin().rpc("admin_recap_yesterday", { from_ts: from, to_ts: to }),
    supabaseAdmin().from("device_tokens").select("user_id, fcm_token"),
  ]);

  const recapByUser = new Map<string, { sales_total: number; sales_count: number; new_debts_total: number }>();
  for (const row of recapRows ?? []) {
    recapByUser.set(row.user_id, {
      sales_total: row.sales_total,
      sales_count: row.sales_count,
      new_debts_total: row.new_debts_total,
    });
  }

  const tokensByUser = new Map<string, string[]>();
  for (const row of (deviceRows ?? []) as { user_id: string; fcm_token: string }[]) {
    const list = tokensByUser.get(row.user_id) ?? [];
    list.push(row.fcm_token);
    tokensByUser.set(row.user_id, list);
  }

  let recipients = 0;
  let success = 0;
  let failed = 0;

  for (const [userId, tokens] of tokensByUser) {
    const recap = recapByUser.get(userId) ?? { sales_total: 0, sales_count: 0, new_debts_total: 0 };
    const { title, body } = buildMessage(recap.sales_total, recap.sales_count, recap.new_debts_total);
    const result = await sendPushToTokens(tokens, title, body);
    recipients += tokens.length;
    success += result.success;
    failed += result.failed;
  }

  await supabaseAdmin()
    .from("notification_log")
    .update({ recipients, success, failed })
    .eq("kind", "recap_quotidien")
    .eq("recap_date", recapDate);

  return NextResponse.json({ recipients, success, failed });
}
```

- [ ] **Step 2: Déclarer le cron Vercel**

Créer `backoffice/vercel.json` :

```json
{
  "crons": [
    { "path": "/api/cron/recap-quotidien", "schedule": "0 7 * * 1-5" }
  ]
}
```

(`0 7 * * 1-5` = 07:00 UTC = 08:00 Niamey, du lundi au vendredi.)

- [ ] **Step 3: Documenter dans le README**

Dans `backoffice/README.md`, ajouter une ligne au tableau des pages (section 1) :

```
| **Notifications** `/notifications` | Envoi manuel de notifications push (titre + message, ciblage par statut ou par compte) et état du récap automatique quotidien (lundi–vendredi 8h, heure de Niamey). |
```

Dans la section "5. Déploiement sur Vercel", étendre la liste des variables :

```
2. Ajouter les variables d'environnement `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `CRON_SECRET`.
```

Ajouter une nouvelle section 8 en fin de fichier :

```markdown

## 8. Notifications push (Firebase)

1. Créer un projet Firebase (gratuit) sur console.firebase.google.com, y ajouter
   l'app Android `com.lissafi.app`.
2. Générer une clé de compte de service (Paramètres du projet → Comptes de
   service → Générer une nouvelle clé privée) et la coller dans
   `FIREBASE_SERVICE_ACCOUNT_JSON` (JSON complet sur une ligne).
3. Exécuter `supabase-schema.sql` (table `device_tokens`) et
   `supabase-admin.sql` (table `notification_log`, réglage
   `recap_notifications_enabled`, fonction `admin_recap_yesterday`) si ce
   n'est pas déjà fait.
4. Définir `CRON_SECRET` (chaîne aléatoire) dans les variables d'environnement
   Vercel — Vercel Cron l'envoie automatiquement en en-tête `Authorization`.
5. Le récap automatique tourne lundi–vendredi à 8h (heure de Niamey), défini
   dans `vercel.json`. Activable/désactivable sans redéploiement depuis
   `/notifications`.
```

- [ ] **Step 4: Vérifier le build**

Run: `cd backoffice && npm run build`
Expected: `✓ Compiled successfully`, la route `/api/cron/recap-quotidien` apparaît dans la liste des routes.

- [ ] **Step 5: Test manuel du déclenchement (nécessite `CRON_SECRET` et les credentials Firebase réels)**

Run: `curl -H "Authorization: Bearer $CRON_SECRET" http://localhost:3000/api/cron/recap-quotidien`
Expected première exécution du jour : `{"recipients":N,"success":N,"failed":0}`. Deuxième appel le même jour : `{"skipped":"already_sent_or_error", ...}`.

- [ ] **Step 6: Commit**

```bash
git add "backoffice/src/app/api/cron/recap-quotidien/route.ts" backoffice/vercel.json backoffice/README.md
git commit -m "feat(backoffice): ajoute le cron du recap quotidien (lun-ven 8h Niamey)"
```

---

### Task 9: Android — dépendances Firebase Messaging (Gradle + Manifest)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (racine)
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `.gitignore` (racine)

**Interfaces:**
- Produces: dépendances `com.google.firebase:firebase-messaging-ktx` disponibles pour `LissafiMessagingService` (Tâche 10) ; permission `POST_NOTIFICATIONS` déclarée (demandée à l'exécution en Tâche 11) ; service `.service.LissafiMessagingService` déclaré dans le manifest (créé en Tâche 10).

- [ ] **Step 1: Ajouter les versions et libs Firebase au catalogue**

Dans `gradle/libs.versions.toml`, section `[versions]` (ajouter après `vico`) :

```toml
googleServices = "4.4.4"
firebaseBom = "34.9.0"
```

Section `[libraries]` (ajouter après `vico-compose-m3`) :

```toml
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-messaging-ktx = { group = "com.google.firebase", name = "firebase-messaging-ktx" }
```

Section `[plugins]` (ajouter après `kotlin-serialization`) :

```toml
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

- [ ] **Step 2: Appliquer le plugin dans le build racine**

Dans `build.gradle.kts` (racine) :

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
}
```

- [ ] **Step 3: Activer le plugin et ajouter les dépendances dans `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}
```

Dans le bloc `dependencies { ... }`, ajouter après `implementation(libs.kotlinx.serialization.json)` :

```kotlin
    // Firebase Cloud Messaging (notifications push)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
```

- [ ] **Step 4: Manifest — permission et déclarations**

Dans `app/src/main/AndroidManifest.xml`, ajouter la permission après `BLUETOOTH_CONNECT` :

```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Dans `<application>`, après la balise `<activity>` de `MainActivity` (avant `</application>`) :

```xml
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@mipmap/ic_launcher" />

        <service
            android:name=".service.LissafiMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
```

- [ ] **Step 5: Ignorer `google-services.json` et le placer**

Ajouter au `.gitignore` racine, à côté de `keystore.properties` :

```
app/google-services.json
```

Puis copier le fichier obtenu dans le prérequis externe vers `app/google-services.json`. **Si le fichier n'est pas encore disponible, passer à l'étape suivante quand même** — la vérification ci-dessous échouera avec une erreur explicite du plugin `google-services` ("File google-services.json is missing"), ce qui est attendu tant que le projet Firebase n'a pas été créé.

- [ ] **Step 6: Vérifier le build**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected (si `google-services.json` présent) : `BUILD SUCCESSFUL`. Sinon : échec avec un message mentionnant `google-services.json` manquant — noter que ce point est bloqué sur le prérequis externe et continuer le plan (les tâches 10 et 11 ne dépendent pas d'un build réussi pour être écrites).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml .gitignore
git commit -m "feat(android): ajoute les dependances Firebase Messaging (notifications push)"
```

Ne pas ajouter `app/google-services.json` au commit ci-dessus (il ne fait pas partie de la liste `git add`) — ce fichier contient des identifiants de projet Firebase et doit rester local, comme `keystore.properties`.

---

### Task 10: Android — `SupabaseApi.upsertDeviceToken` et `LissafiMessagingService`

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/data/remote/SupabaseApi.kt`
- Create: `app/src/main/java/com/lissafi/app/service/LissafiMessagingService.kt`

**Interfaces:**
- Consumes: `logScope`, `restUrl()`, `anonKey`, `token`, `currentUserId`, `hasValidSession`, `http` (déjà privés/internes à `SupabaseApi`, Tâche existante).
- Produces: `SupabaseApi.upsertDeviceToken(fcmToken: String): Unit` (fire-and-forget) — consommé par `LissafiMessagingService.onNewToken` (ce fichier) et par `LissafiNavHost.kt` (Tâche 11). Constante `LissafiMessagingService.CHANNEL_ID = "lissafi_recap"` — doit correspondre à `ANDROID_CHANNEL_ID` dans `backoffice/src/lib/notifications.ts` (Tâche 3).

- [ ] **Step 1: Ajouter le payload et la méthode dans `SupabaseApi.kt`**

Ajouter la classe de payload à côté de `SupportTicketPayload` (avant la déclaration de `class SupabaseApi`) :

```kotlin
@Serializable
private data class DeviceTokenPayload(
    val user_id: String,
    val fcm_token: String,
    val updated_at: Long
)
```

Ajouter la méthode juste après `reportSupportTicket(...)` (même style fire-and-forget que `logEvent`/`reportSupportTicket`) :

```kotlin
    /**
     * Enregistre/rafraîchit le token FCM de cet appareil pour l'utilisateur connecté.
     * Fire-and-forget : un échec réseau n'empêche pas l'usage de l'app — le token
     * sera renvoyé au prochain onNewToken() ou au prochain démarrage connecté.
     */
    fun upsertDeviceToken(fcmToken: String) {
        val authToken = token
        val uid = currentUserId
        if (!hasValidSession || uid.isEmpty() || fcmToken.isBlank()) return

        logScope.launch {
            try {
                val response = http.post(restUrl("device_tokens")) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $authToken")
                    header("Prefer", "resolution=merge-duplicates")
                    parameter("on_conflict", "fcm_token")
                    contentType(ContentType.Application.Json)
                    setBody(DeviceTokenPayload(
                        user_id = uid,
                        fcm_token = fcmToken,
                        updated_at = System.currentTimeMillis()
                    ))
                }
                if (response.status.value !in 200..299) {
                    Log.w("LissafiLog", "upsertDeviceToken HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                Log.w("LissafiLog", "upsertDeviceToken ignoré : ${e.message}")
            }
        }
    }
```

- [ ] **Step 2: Créer `LissafiMessagingService.kt`**

```kotlin
package com.lissafi.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lissafi.app.LissafiApp
import com.lissafi.app.MainActivity
import com.lissafi.app.R

/**
 * Reçoit les notifications push (récap quotidien, envois manuels du back-office)
 * et renvoie le token FCM à Supabase à chaque renouvellement.
 */
class LissafiMessagingService : FirebaseMessagingService() {

    companion object {
        // Doit correspondre à ANDROID_CHANNEL_ID dans backoffice/src/lib/notifications.ts
        const val CHANNEL_ID = "lissafi_recap"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        (applicationContext as LissafiApp).supabaseApi.upsertDeviceToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Récap et annonces", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 3: Vérifier la compilation Kotlin**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected : `BUILD SUCCESSFUL` si `google-services.json` est présent (Tâche 9) ; sinon même échec attendu que Tâche 9 (fichier manquant), le code Kotlin lui-même doit être syntaxiquement correct.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/data/remote/SupabaseApi.kt app/src/main/java/com/lissafi/app/service/LissafiMessagingService.kt
git commit -m "feat(android): recoit les notifications push et enregistre le token FCM"
```

---

### Task 11: Android — enregistrement du token et permission à la connexion

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`

**Interfaces:**
- Consumes: `api.upsertDeviceToken(fcmToken: String)` (Tâche 10), `FirebaseMessaging.getInstance().token` (SDK Firebase, Tâche 9).

- [ ] **Step 1: Ajouter les imports**

Dans `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`, ajouter aux imports existants :

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
```

- [ ] **Step 2: Ajouter le lanceur de permission**

Juste après la ligne `val navController = rememberNavController()` (avant `navBackStackEntry`), ajouter :

```kotlin
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* refus = pas de notifications, non bloquant */ }
```

- [ ] **Step 3: Demander la permission et enregistrer le token à la connexion**

Remplacer le bloc `LaunchedEffect(isLoggedIn) { ... }` existant :

```kotlin
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()
            navController.navigate(Routes.CAISSE) {
                // launchSingleTop : au démarrage déjà connecté, startDestination
                // est déjà CAISSE → évite d'empiler un doublon.
                launchSingleTop = true
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (OnboardingManager.isCompleted(context)) {
            // Déjà passé par l'onboarding (ou déconnecté) → inscription
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
        // Sinon : on reste sur ONBOARDING, ne rien faire (évite d'écraser l'onboarding)
    }
```

par :

```kotlin
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            // Enregistre le token FCM de cet appareil pour le compte connecté
            // (fire-and-forget : onNewToken() le renverra si ça échoue ici).
            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                api.upsertDeviceToken(fcmToken)
            }

            navController.navigate(Routes.CAISSE) {
                // launchSingleTop : au démarrage déjà connecté, startDestination
                // est déjà CAISSE → évite d'empiler un doublon.
                launchSingleTop = true
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (OnboardingManager.isCompleted(context)) {
            // Déjà passé par l'onboarding (ou déconnecté) → inscription
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
        // Sinon : on reste sur ONBOARDING, ne rien faire (évite d'écraser l'onboarding)
    }
```

- [ ] **Step 4: Vérifier le build complet**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
Expected : `BUILD SUCCESSFUL` — nécessite `app/google-services.json` réel (prérequis externe). Si absent, ce point reste bloqué jusqu'à ce que l'utilisateur fournisse le fichier ; tout le reste du code est en place.

- [ ] **Step 5: Test manuel (nécessite un appareil/émulateur avec Google Play Services et le build ci-dessus réussi)**

Installer l'APK (`./gradlew installDebug`), se connecter, accepter la permission de notification, vérifier côté Supabase (table `device_tokens`) qu'une ligne existe pour l'utilisateur connecté. Envoyer une notification test depuis `/notifications` (back-office) et vérifier sa réception sur l'appareil.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt
git commit -m "feat(android): enregistre le token FCM et demande la permission de notification a la connexion"
```

---

## Vérification finale (toutes tâches terminées)

- `cd backoffice && npm run build` → `✓ Compiled successfully`, routes `/notifications` et `/api/cron/recap-quotidien` présentes.
- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug` → `BUILD SUCCESSFUL` (nécessite `app/google-services.json` réel).
- SQL : `supabase-schema.sql` et `supabase-admin.sql` exécutés dans le SQL Editor Supabase (idempotents, sans erreur).
- Test de bout en bout : connexion sur un appareil réel → ligne dans `device_tokens` → envoi manuel depuis `/notifications` → réception sur l'appareil → ligne dans `notification_log` avec `success: 1`.
- Déclenchement manuel de `/api/cron/recap-quotidien` (avec `CRON_SECRET`) → réception du récap sur un compte ayant des ventes de la veille, et sur un compte sans vente (message encourageant).

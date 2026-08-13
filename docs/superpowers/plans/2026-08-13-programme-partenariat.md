# Programme de partenariat Lissafi — Plan d'implémentation

> **Pour les agents d'exécution :** SUB-SKILL REQUIS : utilise `superpowers:subagent-driven-development` (recommandé) ou `superpowers:executing-plans` pour implémenter ce plan tâche par tâche. Les étapes utilisent la syntaxe checkbox (`- [ ]`) pour le suivi.

**Objectif :** Ajouter au back-office Lissafi une section « Partenaires » (revendeurs, ambassadeurs, stratégiques, parrainage) qui enregistre chaque client payant amené par un partenaire et trace la commission fixe en espèces due puis payée.

**Architecture :** Le programme vit entièrement dans le back-office existant (Next.js + Supabase). Deux tables (`partners`, `partner_sales`) + une fonction d'agrégation `admin_partner_summaries()` ajoutées à `supabase-admin.sql`, exposées via des pages `(admin)` (`/partenaires`, `/partenaires/[id]`), des Server Actions (`src/actions/partners.ts`) et la couche data (`src/lib/data.ts`). Aucune modification de l'app Android ni de la landing. L'attribution d'une vente est manuelle : l'admin la saisit quand il active un client payant.

**Tech Stack :** Next.js 15 (App Router), TypeScript, Tailwind, `@supabase/supabase-js` (client service_role côté serveur), Server Actions, RLS Supabase.

**Spec :** `docs/superpowers/specs/2026-08-13-programme-partenariat-design.md`

## Global Constraints

- **Aucune infra de test automatisé** dans `backoffice/` (pas de script `test`). La vérification de chaque tâche = `npm run build` (typecheck + build Next) + test manuel navigateur décrit dans la tâche.
- **SQL** : après le commit, l'utilisateur DOIT relancer `supabase-admin.sql` dans le SQL Editor Supabase (idempotent) — sinon `/partenaires` s'affichera vide en runtime. À noter dans le message final, pas à faire par l'agent d'exécution.
- **Copy et commentaires en français**, code commenté en français.
- **Commits** en conventional style, en français, sans mention d'IA/Claude.
- **L'argent est en `Int` FCFA**, formaté avec `formatFCFA` (`src/lib/format.ts`).
- **Dates** : `partner_sales.created_at` en `BIGINT` epoch millis (comme `app_logs`), `paid_at` en `TIMESTAMPTZ` (ISO string).
- **Barème fixe** (constant de code, `src/lib/partners.ts`) : Plus 10 000 F, Business 15 000 F, Pack 5 000 F.
- Ne pas modifier l'app Android, la landing, ni le schéma app (`supabase-schema.sql`).

---

### Task 1: Schéma SQL du programme

**Files:**
- Modify: `supabase-admin.sql` (append à la fin du fichier)

**Interfaces:**
- Consumes: la fonction `public.is_admin()` (définie en tête du fichier).
- Produces: tables `public.partners` et `public.partner_sales` (RLS admin), fonction `public.admin_partner_summaries()` (SECURITY DEFINER, exécutable par `service_role` uniquement).

- [ ] **Step 1: Ajouter le bloc SQL**

Ajoute à la **fin** de `supabase-admin.sql` (après la ligne `CREATE INDEX IF NOT EXISTS idx_app_settings_key ON public.app_settings(key);`) :

```sql
-- ============================================================
-- 8. PROGRAMME DE PARTENARIAT (commission cash par client payant)
--    Tables administrées uniquement depuis le back-office
--    (service_role). Aucun accès depuis l'app Android.
-- ============================================================

CREATE TABLE IF NOT EXISTS public.partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('agent', 'ambassador', 'strategic', 'referral')),
    phone TEXT NOT NULL DEFAULT '',
    code TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.partners ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partners FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partners" ON public.partners;
CREATE POLICY "admins manage partners" ON public.partners
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.partner_sales (
    id BIGSERIAL PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES public.partners(id) ON DELETE CASCADE,
    client_name TEXT NOT NULL DEFAULT '',
    client_phone TEXT NOT NULL DEFAULT '',
    plan TEXT NOT NULL CHECK (plan IN ('plus', 'business', 'pack')),
    amount_paid_fcfa INT NOT NULL DEFAULT 0,
    commission_fcfa INT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'owed' CHECK (status IN ('owed', 'paid')),
    paid_at TIMESTAMPTZ,
    note TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL
);

ALTER TABLE public.partner_sales ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partner_sales FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partner sales" ON public.partner_sales;
CREATE POLICY "admins manage partner sales" ON public.partner_sales
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());

CREATE INDEX IF NOT EXISTS idx_partner_sales_partner ON public.partner_sales(partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_sales_status ON public.partner_sales(status);

-- Agrégat par partenaire pour la liste /partenaires
CREATE OR REPLACE FUNCTION public.admin_partner_summaries()
RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT coalesce(jsonb_agg(row_to_json(x) ORDER BY x.name), '[]'::jsonb)
    FROM (
        SELECT
            p.id,
            p.name,
            p.type,
            p.phone,
            p.code,
            p.status,
            p.created_at,
            coalesce(s.sale_count, 0) AS sale_count,
            coalesce(s.commission_due, 0) AS commission_due,
            coalesce(s.commission_paid, 0) AS commission_paid,
            coalesce(s.commission_due, 0) - coalesce(s.commission_paid, 0) AS commission_remaining
        FROM partners p
        LEFT JOIN (
            SELECT partner_id,
                   count(*) AS sale_count,
                   sum(commission_fcfa) FILTER (WHERE status = 'owed') AS commission_due,
                   sum(commission_fcfa) FILTER (WHERE status = 'paid') AS commission_paid
            FROM partner_sales
            GROUP BY partner_id
        ) s ON s.partner_id = p.id
    ) x;
$func$;

REVOKE EXECUTE ON FUNCTION public.admin_partner_summaries() FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.admin_partner_summaries() TO service_role;
```

- [ ] **Step 2: Vérifier la cohérence du fichier**

Run: `grep -n "admin_partner_summaries\|CREATE TABLE IF NOT EXISTS public.partners\|CREATE TABLE IF NOT EXISTS public.partner_sales" supabase-admin.sql`
Expected : 4 occurrences (la table `partners`, la table `partner_sales`, la fonction, et rien d'autre). Le fichier reste idempotent (`IF NOT EXISTS`, `CREATE OR REPLACE`, `DROP POLICY IF EXISTS`).

- [ ] **Step 3: Commit**

```bash
git add supabase-admin.sql
git commit -m "feat(partenariat): schéma back-office du programme de partenariat (partners, partner_sales, stats)"
```

---

### Task 2: Types + barème

**Files:**
- Modify: `backoffice/src/types.ts`
- Create: `backoffice/src/lib/partners.ts`

**Interfaces:**
- Consumes: rien (nouveaux types).
- Produces: `PartnerType`, `Plan`, `PartnerSummary`, `PartnerSale` (types.ts) ; `COMMISSIONS`, `PARTNER_TYPE_LABELS`, `PLAN_LABELS` (partners.ts). La Task 3, 4, 5 et 6 les importent.

- [ ] **Step 1: Ajouter les types**

Append à la fin de `backoffice/src/types.ts` :

```ts
// ============================================================
// Programme de partenariat
// ============================================================

export type PartnerType = "agent" | "ambassador" | "strategic" | "referral";
export type Plan = "plus" | "business" | "pack";

export type PartnerSummary = {
  id: string;
  name: string;
  type: PartnerType;
  phone: string;
  code: string;
  status: "active" | "inactive";
  created_at: string;
  sale_count: number;
  commission_due: number;
  commission_paid: number;
  commission_remaining: number;
};

export type PartnerSale = {
  id: number;
  partner_id: string;
  client_name: string;
  client_phone: string;
  plan: Plan;
  amount_paid_fcfa: number;
  commission_fcfa: number;
  status: "owed" | "paid";
  paid_at: string | null;
  note: string;
  created_at: number;
};
```

- [ ] **Step 2: Créer le barème partagé**

Créer `backoffice/src/lib/partners.ts` :

```ts
import type { Plan, PartnerType } from "@/types";

// Barème fixe et permanent — commission une fois par vente, abonnement récurrent.
export const COMMISSIONS: Record<Plan, number> = {
  plus: 10_000,
  business: 15_000,
  pack: 5_000,
};

export const PARTNER_TYPE_LABELS: Record<PartnerType, string> = {
  agent: "Revendeur / agent",
  ambassador: "Ambassadeur / influenceur",
  strategic: "Partenariat stratégique",
  referral: "Parrainage utilisateur",
};

export const PLAN_LABELS: Record<Plan, string> = {
  plus: "Lissafi Plus",
  business: "Lissafi Business",
  pack: "Pack Boutique",
};
```

- [ ] **Step 3: Vérifier le typecheck**

Run: `cd backoffice && npm run build`
Expected : le build passe (Next compile, aucun type inconnu).

- [ ] **Step 4: Commit**

```bash
git add backoffice/src/types.ts backoffice/src/lib/partners.ts
git commit -m "feat(partenariat): types et barème fixe des commissions (Plus 10k, Business 15k, Pack 5k)"
```

---

### Task 3: Couche données

**Files:**
- Modify: `backoffice/src/lib/data.ts` (append des fonctions à la fin)

**Interfaces:**
- Consumes: `supabaseAdmin()` (déjà importé), `logRpcError` (déjà défini), types `PartnerSummary`, `PartnerSale` (Task 2).
- Produces: `getPartners(): Promise<PartnerSummary[]>`, `getPartner(id): Promise<PartnerSummary | null>`, `getPartnerSales(partnerId): Promise<PartnerSale[]>`. Consommées par les pages (Task 5, 6).

- [ ] **Step 1: Ajouter les fonctions de lecture**

Append à la fin de `backoffice/src/lib/data.ts` :

```ts
// ============================================================
// Programme de partenariat
// ============================================================

/** Liste des partenaires avec indicateurs agrégés (ventes, commissions). */
export async function getPartners(): Promise<PartnerSummary[]> {
  const { data, error } = await supabaseAdmin().rpc("admin_partner_summaries");
  if (error) logRpcError("getPartners", error);
  return Array.isArray(data) ? (data as PartnerSummary[]) : [];
}

/** Un partenaire par id (avec agrégats), null si introuvable. */
export async function getPartner(id: string): Promise<PartnerSummary | null> {
  const partners = await getPartners();
  return partners.find((p) => p.id === id) ?? null;
}

/** Historique des ventes attribuées à un partenaire (plus récentes d'abord). */
export async function getPartnerSales(partnerId: string): Promise<PartnerSale[]> {
  const { data, error } = await supabaseAdmin()
    .from("partner_sales")
    .select("*")
    .eq("partner_id", partnerId)
    .order("created_at", { ascending: false })
    .limit(200);
  if (error) {
    console.error("[admin] getPartnerSales:", error);
    return [];
  }
  return (data ?? []) as PartnerSale[];
}
```

Complète l'import des types en tête de `data.ts` — ajoute `PartnerSale` et `PartnerSummary` à la liste importée depuis `"@/types"`.

- [ ] **Step 2: Vérifier le typecheck**

Run: `cd backoffice && npm run build`
Expected : build OK. (Si un import type manque, le typecheck le signale — l'ajoute en tête de fichier.)

- [ ] **Step 3: Commit**

```bash
git add backoffice/src/lib/data.ts
git commit -m "feat(partenariat): lectures data des partenaires et ventes attribuées"
```

---

### Task 4: Actions serveur

**Files:**
- Create: `backoffice/src/actions/partners.ts`

**Interfaces:**
- Consumes: `requireAdmin()` (`@/lib/session`), `supabaseAdmin()` (`@/lib/supabase`), `logAdminAction` (`@/lib/audit`), `COMMISSIONS` + type `Plan` (`@/lib/partners`, Task 2).
- Produces: `PartnerActionResult`, `addPartner(prev, formData)`, `addPartnerSale(prev, formData)`, `markCommissionPaid(saleId)`, `markAllCommissionsPaid(partnerId)`, `togglePartnerStatus(partnerId)`, et les variantes `Quick` (`markCommissionPaidQuick`, `markAllCommissionsPaidQuick`, `togglePartnerStatusQuick`). Consommées par les formulaires (Task 5, 6).

- [ ] **Step 1: Créer les actions**

Créer `backoffice/src/actions/partners.ts` :

```ts
"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction as logAction } from "@/lib/audit";
import { COMMISSIONS } from "@/lib/partners";
import type { Plan } from "@/types";

export type PartnerActionResult = { ok?: boolean; error?: string };

const PARTNER_TYPES = new Set(["agent", "ambassador", "strategic", "referral"]);
const PLANS: Plan[] = ["plus", "business", "pack"];

function isPlan(v: string): v is Plan {
  return (PLANS as string[]).includes(v);
}

/** Génère un code partenaire lisible et unique (ex. PTN-K2M7Q), sans ambiguïté 0/O/1/I. */
function makeCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 5; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return `PTN-${s}`;
}

// ============================================================
// PARTENAIRES
// ============================================================

/** Crée un partenaire. Le code est généré si non fourni. */
export async function addPartner(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  await requireAdmin();
  const name = String(formData.get("name") || "").trim();
  const type = String(formData.get("type") || "").trim();
  const phone = String(formData.get("phone") || "").trim();
  const notes = String(formData.get("notes") || "").trim();
  const code = (String(formData.get("code") || "").trim().toUpperCase()) || makeCode();

  if (!name) return { error: "Nom du partenaire obligatoire." };
  if (!PARTNER_TYPES.has(type)) return { error: "Type de partenaire invalide." };

  const { error } = await supabaseAdmin().from("partners").insert({
    name,
    type,
    phone,
    notes,
    code,
    status: "active",
  });
  if (error) {
    console.error("[admin] addPartner:", error);
    if (error.code === "23505") return { error: `Code déjà utilisé : ${code}. Choisis-en un autre.` };
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_add", null, { name, type, phone, code });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Change l'état actif/inactif d'un partenaire. */
export async function togglePartnerStatus(partnerId: string): Promise<PartnerActionResult> {
  await requireAdmin();
  const { data: partner } = await supabaseAdmin()
    .from("partners")
    .select("status")
    .eq("id", partnerId)
    .maybeSingle();
  if (!partner) return { error: "Partenaire introuvable." };
  const next = partner.status === "active" ? "inactive" : "active";

  const { error } = await supabaseAdmin().from("partners").update({ status: next }).eq("id", partnerId);
  if (error) {
    console.error("[admin] togglePartnerStatus:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_toggle_status", null, { partnerId, next });
  revalidatePath("/", "layout");
  return { ok: true };
}

// ============================================================
// VENTES ATTRIBUÉES
// ============================================================

/** Attribue une vente payante à un partenaire : crée la commission due selon le barème. */
export async function addPartnerSale(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  await requireAdmin();
  const partnerId = String(formData.get("partnerId") || "").trim();
  const plan = String(formData.get("plan") || "").trim();
  const clientName = String(formData.get("clientName") || "").trim();
  const clientPhone = String(formData.get("clientPhone") || "").trim();
  const note = String(formData.get("note") || "").trim();
  const amountRaw = Number(formData.get("amountPaid") || 0);

  if (!partnerId) return { error: "Partenaire manquant." };
  if (!isPlan(plan)) return { error: "Plan invalide." };
  const amountPaidFcfa = Math.max(0, Math.floor(Number.isFinite(amountRaw) ? amountRaw : 0));
  const commissionFcfa = COMMISSIONS[plan];

  const { error } = await supabaseAdmin().from("partner_sales").insert({
    partner_id: partnerId,
    client_name: clientName,
    client_phone: clientPhone,
    plan,
    amount_paid_fcfa: amountPaidFcfa,
    commission_fcfa: commissionFcfa,
    status: "owed",
    note,
    created_at: Date.now(),
  });
  if (error) {
    console.error("[admin] addPartnerSale:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_sale_add", null, { partnerId, plan, commissionFcfa, clientName });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Passe une vente attribuée de 'due' à 'payée'. */
export async function markCommissionPaid(saleId: number): Promise<PartnerActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("partner_sales")
    .update({ status: "paid", paid_at: new Date().toISOString() })
    .eq("id", saleId)
    .eq("status", "owed");
  if (error) {
    console.error("[admin] markCommissionPaid:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_commission_paid", null, { saleId });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Passe toutes les ventes dues d'un partenaire en 'payées' (confort, un clic). */
export async function markAllCommissionsPaid(partnerId: string): Promise<PartnerActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("partner_sales")
    .update({ status: "paid", paid_at: new Date().toISOString() })
    .eq("partner_id", partnerId)
    .eq("status", "owed");
  if (error) {
    console.error("[admin] markAllCommissionsPaid:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_commission_paid_all", null, { partnerId });
  revalidatePath("/", "layout");
  return { ok: true };
}

// ============================================================
// Variantes "Quick" — pour les boutons dans les formulaires
// ============================================================

export async function markCommissionPaidQuick(saleId: number) {
  "use server";
  const r = await markCommissionPaid(saleId);
  if (r && "error" in r) console.error("[admin] markCommissionPaidQuick:", r.error);
}

export async function markAllCommissionsPaidQuick(partnerId: string) {
  "use server";
  const r = await markAllCommissionsPaid(partnerId);
  if (r && "error" in r) console.error("[admin] markAllCommissionsPaidQuick:", r.error);
}

export async function togglePartnerStatusQuick(partnerId: string) {
  "use server";
  const r = await togglePartnerStatus(partnerId);
  if (r && "error" in r) console.error("[admin] togglePartnerStatusQuick:", r.error);
}
```

- [ ] **Step 2: Vérifier le typecheck**

Run: `cd backoffice && npm run build`
Expected : build OK.

- [ ] **Step 3: Commit**

```bash
git add backoffice/src/actions/partners.ts
git commit -m "feat(partenariat): actions serveur partenaires (ajout, vente attribuée, commissions payées)"
```

---

### Task 5: Page liste `/partenaires` (+ icône, navigation, formulaire d'ajout)

**Files:**
- Create: `backoffice/src/app/(admin)/partenaires/page.tsx`
- Create: `backoffice/src/components/AddPartnerForm.tsx`
- Modify: `backoffice/src/components/icons.tsx` (ajout `IconHandshake`)
- Modify: `backoffice/src/components/AppShell.tsx` (item nav « Partenaires »)

**Interfaces:**
- Consumes: `getPartners()` (Task 3), `addPartner` (Task 4), `markAllCommissionsPaidQuick`, `togglePartnerStatusQuick` (Task 4), `COMMISSIONS` non utilisé ici, `PARTNER_TYPE_LABELS` (Task 2), composants `ui.tsx`, `ConfirmForm`, `IconHandshake`.
- Produces: la route `/partenaires` (liste + stats + ajout + actions). Consommée par Task 6 (lien vers le détail).

- [ ] **Step 1: Ajouter l'icône `IconHandshake`**

Ajoute à la fin de `backoffice/src/components/icons.tsx` :

```tsx
export const IconHandshake = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m11 17 2 2a1 1 0 1 0 3-3" />
    <path d="m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4" />
    <path d="m21 3 1 11h-2" />
    <path d="M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3" />
    <path d="M3 4h8" />
  </svg>
);
```

- [ ] **Step 2: Ajouter l'entrée de navigation**

Dans `backoffice/src/components/AppShell.tsx`, importe `IconHandshake` (ajoute-le à l'import depuis `./icons`) et ajoute l'item dans le tableau `NAV`, après `Premium` :

```tsx
{ href: "/partenaires", label: "Partenaires", icon: IconHandshake },
```

- [ ] **Step 3: Créer le formulaire d'ajout**

Créer `backoffice/src/components/AddPartnerForm.tsx` :

```tsx
"use client";

import { useActionState } from "react";
import { addPartner } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { PARTNER_TYPE_LABELS } from "@/lib/partners";
import { Button, Card, CardHeader, Field, Input, Select, Textarea } from "./ui";

function Feedback({ state }: { state: PartnerActionResult | undefined }) {
  if (!state) return null;
  if (state.ok)
    return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Partenaire ajouté.</p>;
  if (state.error)
    return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function AddPartnerForm() {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(addPartner, undefined);

  return (
    <Card>
      <CardHeader
        title="Ajouter un partenaire"
        subtitle="Code unique à communiquer — commission en espèces à chaque client payant amené."
      />
      <form action={action} className="grid gap-4 px-5 pb-5 sm:grid-cols-2">
        <Field label="Nom">
          <Input type="text" name="name" required placeholder="Ex: Amadou Boubacar" />
        </Field>
        <Field label="Type">
          <Select name="type" defaultValue="agent">
            {Object.entries(PARTNER_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </Select>
        </Field>
        <Field label="Téléphone">
          <Input type="tel" name="phone" placeholder="Ex: 96 12 34 56" />
        </Field>
        <Field label="Code (optionnel)" hint="Vide = généré automatiquement (ex: PTN-K2M7Q).">
          <Input type="text" name="code" placeholder="Ex: PTN-AMADOU" />
        </Field>
        <Field label="Notes">
          <Textarea name="notes" placeholder="Boutique, quartier, accord…" />
        </Field>
        <div className="flex flex-col items-start justify-end gap-2 sm:col-span-2">
          <Feedback state={state} />
          <Button type="submit" disabled={pending}>Ajouter le partenaire</Button>
        </div>
      </form>
    </Card>
  );
}
```

- [ ] **Step 4: Créer la page liste**

Créer `backoffice/src/app/(admin)/partenaires/page.tsx` :

```tsx
import Link from "next/link";
import { markAllCommissionsPaidQuick, togglePartnerStatusQuick } from "@/actions/partners";
import { AddPartnerForm } from "@/components/AddPartnerForm";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, EmptyState, PageHeader, StatCard, Table, Td, Th, THead, Tr } from "@/components/ui";
import { IconHandshake } from "@/components/icons";
import { getPartners } from "@/lib/data";
import { PARTNER_TYPE_LABELS } from "@/lib/partners";
import { formatFCFA } from "@/lib/format";
import type { PartnerSummary } from "@/types";

export default async function PartnersPage() {
  const partners = await getPartners();
  const activeCount = partners.filter((p) => p.status === "active").length;
  const totalClients = partners.reduce((s, p) => s + p.sale_count, 0);
  const totalDue = partners.reduce((s, p) => s + p.commission_due, 0);
  const totalPaid = partners.reduce((s, p) => s + p.commission_paid, 0);

  return (
    <div className="space-y-5">
      <PageHeader
        title="Partenaires"
        subtitle="Réseau de revendeurs, ambassadeurs et parrainage — commission en espèces par client payant."
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconHandshake size={15} /> {activeCount} actif(s)
          </span>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Partenaires actifs" value={activeCount} icon={<IconHandshake size={18} />} accent="green" />
        <StatCard label="Clients amenés" value={totalClients} accent="blue" />
        <StatCard label="Commissions dues" value={formatFCFA(totalDue)} accent="orange" />
        <StatCard label="Commissions payées" value={formatFCFA(totalPaid)} accent="gray" />
      </div>

      <AddPartnerForm />

      {partners.length === 0 ? (
        <EmptyState title="Aucun partenaire" subtitle="Ajoute ton premier partenaire ci-dessus." />
      ) : (
        <Table>
          <THead>
            <Th>Partenaire</Th>
            <Th>Type</Th>
            <Th className="text-right">Clients</Th>
            <Th className="text-right">Commission due</Th>
            <Th className="text-right">Commission payée</Th>
            <Th>Statut</Th>
            <Th>Actions</Th>
          </THead>
          <tbody>
            {partners.map((p) => (
              <PartnerRow key={p.id} partner={p} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function PartnerRow({ partner }: { partner: PartnerSummary }) {
  const active = partner.status === "active";
  return (
    <Tr>
      <Td>
        <Link href={`/partenaires/${partner.id}`} className="font-medium text-brand-600 hover:underline">
          {partner.name}
        </Link>
        <p className="text-xs text-slate-400">
          <code>{partner.code}</code>
          {partner.phone ? ` · ${partner.phone}` : ""}
        </p>
      </Td>
      <Td>
        <Badge color="blue">{PARTNER_TYPE_LABELS[partner.type]}</Badge>
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{partner.sale_count}</Td>
      <Td className="text-right tabular-nums text-orange-600">{formatFCFA(partner.commission_due)}</Td>
      <Td className="text-right tabular-nums text-slate-600">{formatFCFA(partner.commission_paid)}</Td>
      <Td>
        <Badge color={active ? "green" : "gray"}>{active ? "Actif" : "Inactif"}</Badge>
      </Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <Link href={`/partenaires/${partner.id}`}>
            <Button size="sm" variant="secondary">+ Vente</Button>
          </Link>
          {partner.commission_due > 0 && (
            <form action={markAllCommissionsPaidQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm">Tout payer</Button>
            </form>
          )}
          {active ? (
            <ConfirmForm action={togglePartnerStatusQuick.bind(null, partner.id)} confirmText={`Désactiver ${partner.name} ?`}>
              <Button type="submit" size="sm" variant="dangerOutline">Désactiver</Button>
            </ConfirmForm>
          ) : (
            <form action={togglePartnerStatusQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm" variant="secondary">Réactiver</Button>
            </form>
          )}
        </div>
      </Td>
    </Tr>
  );
}
```

- [ ] **Step 5: Vérifier le typecheck**

Run: `cd backoffice && npm run build`
Expected : build OK.

- [ ] **Step 6: Test manuel (navigateur)**

Lance `cd backoffice && npm run dev`, connecte-toi en admin, ouvre `/partenaires`.
Expected : page avec 4 cartes stats à 0, formulaire d'ajout, « Aucun partenaire ». Ajoute un partenaire (type « Revendeur / agent », code vide) → la liste affiche « 1 actif », le code généré `PTN-XXXXX`, et le bouton « + Vente ». *(Si la liste reste vide alors que la table est créée : l'utilisateur n'a pas encore relancé `supabase-admin.sql` dans Supabase — le RPC n'existe pas encore.)*

- [ ] **Step 7: Commit**

```bash
git add backoffice/src/components/icons.tsx backoffice/src/components/AppShell.tsx backoffice/src/components/AddPartnerForm.tsx "backoffice/src/app/(admin)/partenaires/page.tsx"
git commit -m "feat(partenariat): page liste des partenaires (stats, ajout, actions)"
```

---

### Task 6: Page détail `/partenaires/[id]`

**Files:**
- Create: `backoffice/src/app/(admin)/partenaires/[id]/page.tsx`
- Create: `backoffice/src/components/AddPartnerSaleForm.tsx`

**Interfaces:**
- Consumes: `getPartner(id)`, `getPartnerSales(partnerId)` (Task 3), `addPartnerSale` (Task 4), `markCommissionPaidQuick`, `markAllCommissionsPaidQuick`, `togglePartnerStatusQuick` (Task 4), `PARTNER_TYPE_LABELS`, `PLAN_LABELS`, `COMMISSIONS` (Task 2), composants `ui.tsx`, `ConfirmForm`.
- Produces: la route `/partenaires/[id]` (fiche, attribution de vente, historique, marquage payé).

- [ ] **Step 1: Créer le formulaire d'attribution**

Créer `backoffice/src/components/AddPartnerSaleForm.tsx` :

```tsx
"use client";

import { useActionState } from "react";
import { addPartnerSale } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { COMMISSIONS, PLAN_LABELS } from "@/lib/partners";
import type { Plan } from "@/types";
import { Button, Card, CardHeader, Field, Input, Select, Textarea } from "./ui";

function Feedback({ state }: { state: PartnerActionResult | undefined }) {
  if (!state) return null;
  if (state.ok)
    return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Vente attribuée — commission enregistrée.</p>;
  if (state.error)
    return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function AddPartnerSaleForm({ partnerId }: { partnerId: string }) {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(addPartnerSale, undefined);

  return (
    <Card>
      <CardHeader
        title="Attribuer une vente"
        subtitle="Le client a payé et a été activé — enregistre la commission due au partenaire."
      />
      <form action={action} className="grid gap-4 px-5 pb-5 sm:grid-cols-2">
        <input type="hidden" name="partnerId" value={partnerId} />
        <Field label="Plan vendu">
          <Select name="plan" defaultValue="plus">
            {(Object.keys(PLAN_LABELS) as Plan[]).map((p) => (
              <option key={p} value={p}>
                {PLAN_LABELS[p]} — commission {COMMISSIONS[p].toLocaleString("fr-FR")} F
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Montant payé (FCFA)">
          <Input type="number" name="amountPaid" min={0} step={100} placeholder="Ex: 30000" />
        </Field>
        <Field label="Client — nom">
          <Input type="text" name="clientName" placeholder="Ex: Boutique Awa" />
        </Field>
        <Field label="Client — téléphone">
          <Input type="tel" name="clientPhone" placeholder="Ex: 90 11 22 33" />
        </Field>
        <Field label="Note">
          <Textarea name="note" placeholder="Ex: activé avec le code PTN-AMADOU" />
        </Field>
        <div className="flex flex-col items-start justify-end gap-2 sm:col-span-2">
          <Feedback state={state} />
          <Button type="submit" disabled={pending}>Enregistrer la vente</Button>
        </div>
      </form>
    </Card>
  );
}
```

- [ ] **Step 2: Créer la page détail**

Créer `backoffice/src/app/(admin)/partenaires/[id]/page.tsx` :

```tsx
import Link from "next/link";
import { notFound } from "next/navigation";
import { markAllCommissionsPaidQuick, markCommissionPaidQuick, togglePartnerStatusQuick } from "@/actions/partners";
import { AddPartnerSaleForm } from "@/components/AddPartnerSaleForm";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, Card, CardHeader, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getPartner, getPartnerSales } from "@/lib/data";
import { PARTNER_TYPE_LABELS, PLAN_LABELS } from "@/lib/partners";
import { formatDate, formatFCFA } from "@/lib/format";
import type { PartnerSale } from "@/types";

export default async function PartnerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [partner, sales] = await Promise.all([getPartner(id), getPartnerSales(id)]);
  if (!partner) notFound();

  const active = partner.status === "active";

  return (
    <div className="space-y-5">
      <PageHeader
        title={partner.name}
        subtitle={`${PARTNER_TYPE_LABELS[partner.type]} · code ${partner.code}`}
        action={
          <Link href="/partenaires">
            <Button size="sm" variant="secondary">← Tous les partenaires</Button>
          </Link>
        }
      />

      <Card className="p-4">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div>
            <p className="text-xs text-slate-500">Téléphone</p>
            <p className="font-medium text-slate-900">{partner.phone || "—"}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Statut</p>
            <Badge color={active ? "green" : "gray"}>{active ? "Actif" : "Inactif"}</Badge>
          </div>
          <div>
            <p className="text-xs text-slate-500">Commission due</p>
            <p className="font-bold text-orange-600">{formatFCFA(partner.commission_due)}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Commission payée</p>
            <p className="font-bold text-slate-700">{formatFCFA(partner.commission_paid)}</p>
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-1.5">
          {active && partner.commission_due > 0 && (
            <form action={markAllCommissionsPaidQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm">Tout payer</Button>
            </form>
          )}
          {active ? (
            <ConfirmForm action={togglePartnerStatusQuick.bind(null, partner.id)} confirmText={`Désactiver ${partner.name} ?`}>
              <Button type="submit" size="sm" variant="dangerOutline">Désactiver</Button>
            </ConfirmForm>
          ) : (
            <form action={togglePartnerStatusQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm" variant="secondary">Réactiver</Button>
            </form>
          )}
        </div>
      </Card>

      <AddPartnerSaleForm partnerId={partner.id} />

      <Card>
        <CardHeader title="Ventes attribuées" subtitle="Historique des commissions" />
        <div className="px-5 pb-5">
          {sales.length === 0 ? (
            <EmptyState title="Aucune vente attribuée" />
          ) : (
            <Table>
              <THead>
                <Th>Client</Th>
                <Th>Plan</Th>
                <Th className="text-right">Payé (FCFA)</Th>
                <Th className="text-right">Commission</Th>
                <Th>Statut</Th>
                <Th>Date</Th>
                <Th></Th>
              </THead>
              <tbody>
                {sales.map((s) => (
                  <SaleRow key={s.id} sale={s} />
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>
  );
}

function SaleRow({ sale }: { sale: PartnerSale }) {
  const paid = sale.status === "paid";
  return (
    <Tr>
      <Td>
        <p className="font-medium text-slate-900">{sale.client_name || "—"}</p>
        {sale.client_phone && <p className="text-xs text-slate-400">{sale.client_phone}</p>}
      </Td>
      <Td>
        <Badge color="blue">{PLAN_LABELS[sale.plan]}</Badge>
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{formatFCFA(sale.amount_paid_fcfa)}</Td>
      <Td className="text-right tabular-nums font-semibold text-slate-700">{formatFCFA(sale.commission_fcfa)}</Td>
      <Td>
        <Badge color={paid ? "green" : "orange"}>{paid ? "Payée" : "Due"}</Badge>
      </Td>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(sale.created_at)}</Td>
      <Td>
        {!paid && (
          <form action={markCommissionPaidQuick.bind(null, sale.id)}>
            <Button type="submit" size="sm" variant="secondary">Marquer payée</Button>
          </form>
        )}
      </Td>
    </Tr>
  );
}
```

- [ ] **Step 3: Vérifier le typecheck**

Run: `cd backoffice && npm run build`
Expected : build OK.

- [ ] **Step 4: Test manuel (navigateur)**

`cd backoffice && npm run dev`, connecte-toi en admin, ouvre `/partenaires`, clique sur un partenaire (ou ajoutes-en un).
Expected : fiche partenaire + formulaire « Attribuer une vente » (montre les commissions par plan). Sélectionne « Lissafi Plus », montant 30000, client « Boutique Awa », téléphone → Enregistrer → la ligne apparaît dans « Ventes attribuées » avec commission **10 000 F**, badge **Due**, date affichée. Clique « Marquer payée » → badge **Payée**, la carte « Commission due » baisse de 10 000 F. Reviens sur `/partenaires` → le compteur « Clients amenés » est à 1, « Commissions dues » reflète l'état.

- [ ] **Step 5: Commit**

```bash
git add backoffice/src/components/AddPartnerSaleForm.tsx "backoffice/src/app/(admin)/partenaires/[id]/page.tsx"
git commit -m "feat(partenariat): page détail partenaire avec attribution de ventes et suivi des commissions"
```

---

## Vérification finale (après la dernière tâche)

1. `cd backoffice && npm run build` — doit passer.
2. Parcours navigateur complet : `/partenaires` → ajout partenaire → détail → attribution vente → marquage payé → retour liste : stats cohérentes.
3. `git log --oneline -5` — 6 commits conventional en français.
4. **Action utilisateur requise** : relancer `supabase-admin.sql` dans le SQL Editor Supabase (idempotent) pour créer les tables `partners`/`partner_sales` et la fonction `admin_partner_summaries()` avant utilisation en production.

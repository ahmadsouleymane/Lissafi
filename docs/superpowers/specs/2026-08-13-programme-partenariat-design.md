# Design — Programme de partenariat Lissafi

> Date : 2026-08-13
> Contexte : Lissafi est en pré-lancement (landing pas encore déployée, APK hors Play Store, activation premium manuelle via le back-office). L'objectif an 1 est 50 clients payants. Ce programme vise à accélérer l'acquisition via un réseau de partenaires rémunérés.

## Décisions actées

| Décision | Choix |
|---|---|
| Objectif prioritaire | **Revenus d'abord** — le programme récompense des ventes conclues, pas des vues |
| Segments de partenaires | **4** : revendeurs/agents, ambassadeurs/influenceurs, partenariats stratégiques, parrainage utilisateurs |
| Récompense | **Commission fixe en espèces** par client payant amené |
| Palier de commission | **Par partenaire** (pas de logique de date) : `launch` ou `standard` |
| Attribution | **Manuelle** — enregistrée dans le back-office au moment de l'activation premium |
| Succès à 6 mois | **~15–20 clients payants amenés** par des partenaires (sur les 50 visés), commissions ≤ ~20 % du CA du programme |
| Paiement des commissions | Orange Money / Moov Money, tracé dans le back-office (statut `due` → `payée`) |

## Règles du programme

1. Chaque partenaire reçoit un **code unique** (ex. `PTN-AMADOU` ou son n° de téléphone), à communiquer aux clients.
2. Quand un client amené par un partenaire paie, l'admin **attribue la vente** au partenaire dans le back-office → une ligne `partner_sales` avec commission calculée selon le palier du partenaire.
3. Le **parrainage utilisateur** (un commerçant déjà équipé qui amène un client payant) utilise le même mécanisme : il reçoit un code `type=referral` et touche sa commission en espèces. Aucun système séparé.
4. Les **ambassadeurs/influenceurs** diffusent leur code (bio TikTok, légende, affichettes). Même attribution manuelle.
5. Une vente = une ligne `partner_sales`. Un renouvellement = une nouvelle ligne.

### Barème de commissions

| Plan | Palier Lancement | Palier Standard |
|---|---|---|
| Lissafi Plus (30 000 F/an) | 10 000 F | 5 000 F |
| Lissafi Business (75 000 F/an) | 15 000 F | 10 000 F |
| Pack Boutique (60 000 F) | 5 000 F | 3 000 F |

Les premiers partenaires (fenêtre de recrutement) sont créés en palier **Lancement**, les suivants en **Standard**. Un clic pour changer le palier d'un partenaire.

## Modèle de données

Ajout à `supabase-admin.sql` (idempotent). RLS identique à `admin_actions` (`FOR ALL USING (public.is_admin())`). `REVOKE ALL` sur anon/authenticated.

### Table `partners`

```sql
CREATE TABLE IF NOT EXISTS public.partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('agent', 'ambassador', 'strategic', 'referral')),
    phone TEXT NOT NULL DEFAULT '',
    code TEXT NOT NULL UNIQUE,
    commission_tier TEXT NOT NULL DEFAULT 'standard' CHECK (commission_tier IN ('launch', 'standard')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.partners ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partners FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partners" ON public.partners;
CREATE POLICY "admins manage partners" ON public.partners
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());
```

### Table `partner_sales`

```sql
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.partner_sales ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partner_sales FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partner sales" ON public.partner_sales;
CREATE POLICY "admins manage partner sales" ON public.partner_sales
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());

CREATE INDEX IF NOT EXISTS idx_partner_sales_partner ON public.partner_sales(partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_sales_status ON public.partner_sales(status);
```

### Fonction d'agrégation `admin_partner_summaries()`

SECURITY DEFINER, style `admin_user_summaries`, restreinte à `service_role` (REVOKE EXECUTE de PUBLIC/anon/authenticated, GRANT à service_role).

```sql
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
            p.commission_tier,
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

## Écrans (pages `(admin)`)

### `/partenaires`

- Cartes stats : partenaires actifs, clients amenés, commissions dues, commissions payées.
- Formulaire « + Nouveau partenaire » : nom, type (agent/ambassadeur/stratégique/parrainage), téléphone, code (auto-suggéré si vide), palier (lancement/standard).
- Table des partenaires (composants existants `Table`/`Td`/`Th`…) : code, type, palier, clients amenés, commission due, commission payée, statut. Actions par ligne :
  - **+ Vente attribuée** (form inline ou page détail) : nom client, téléphone, plan → commission calculée depuis le palier.
  - **Tout payer** : passe toutes les ventes `owed` de ce partenaire à `paid` en un clic (confort).
  - **Voir** → `/partenaires/[id]`.
  - **Désactiver/Activer** (`ConfirmForm` pour désactiver).
- Item « Partenaires » dans `AppShell.NAV` avec une icône dédiée.

### `/partenaires/[id]`

- Fiche partenaire : nom, type, téléphone, code, palier, statut, historique complet des `partner_sales` (client, plan, montant, commission, statut, date).
- Actions : ajouter une vente, **marquer payée (par vente)**, changer le palier, activer/désactiver.

## Code (patterns existants)

- **`src/actions/partners.ts`** (server actions) :
  - `addPartner(formData)` → insère, `requireAdmin()`, `logAdminAction("add_partner", ...)`, `revalidatePath`.
  - `addPartnerSale(partnerId, { clientName, clientPhone, plan })` → calcule `commission_fcfa` depuis `commission_tier` du partenaire + table des barèmes, insère `partner_sales` (statut `owed`), audit, revalidate.
  - `markCommissionPaid(saleId)` (et variante `markPartnerCommissionPaid(partnerId)` pour tout payer d'un coup) → passe à `paid`, `paid_at = now()`, audit, revalidate.
  - `togglePartnerStatus(partnerId)` → audit, revalidate.
  - `setPartnerTier(partnerId, tier)` → audit, revalidate.
  - Variantes `Quick` pour les boutons de `<form>` (pattern existant).
- **`src/lib/data.ts`** :
  - `getPartners(): Promise<PartnerSummary[]>` → `supabaseAdmin().rpc("admin_partner_summaries")`.
  - `getPartner(id): Promise<PartnerSummary | null>`.
  - `getPartnerSales(partnerId): Promise<PartnerSale[]>` → requête directe sur `partner_sales` (service_role).
- **`src/types.ts`** : `PartnerType`, `CommissionTier`, `PartnerSummary`, `PartnerSale`.
- **Barèmes** : constantes partagées dans `src/lib/partners.ts` :
  ```ts
  export const COMMISSIONS: Record<CommissionTier, Record<Plan, number>> = {
    launch:   { plus: 10_000, business: 15_000, pack: 5_000 },
    standard: { plus: 5_000,  business: 10_000, pack: 3_000 },
  };
  ```
- **`src/components/icons.tsx`** : nouvelle icône (ex. `IconHandshake`).
- **`src/components/ui.tsx`** : aucun composant nouveau nécessaire.

## Gestion des erreurs & limites

- Actions serveur → retour `{ error }` (pattern existant) ; variantes `Quick` loguent côté serveur.
- Désactivation d'un partenaire → `ConfirmForm` (destructif : cache le partenaire, garde l'historique).
- Pas de double attribution possible : une vente = une ligne ; le suivi se fait à la main par l'admin.
- **Pas de portail public partenaire** en V1 : l'admin partage les gains via WhatsApp (récap manuel). Phase 2 optionnelle : lien de suivi landing `?p=CODE` + portail partenaire.
- Pas de mécanisme anti-fraude automatisé : l'attribution repose sur la question posée au client à l'activation (« qui t'a envoyé ? »). Documenté dans le copy de la page.

## Vérification

1. `cd backoffice && npm run build` (typecheck + build Next.js).
2. Test manuel navigateur (via `/partenaires`) : créer un partenaire (palier lancement) → attribuer une vente Plus → vérifier commission 10 000 F → marquer payée → vérifier les stats (dues/payées) et `admin_actions`.
3. Rejouer `supabase-admin.sql` dans le SQL Editor Supabase (idempotent) — c'est l'action utilisateur à faire après le commit.

## Hors périmètre (V1)

- Portail public partenaire / tableau de bord partenaire en app.
- Lien de suivi landing `?p=CODE` (phase 2).
- Attribution automatique depuis l'app Android.
- Paiement automatisé des commissions (reste manuel, via Orange Money/Moov).

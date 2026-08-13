# Design — Portail partenaire auto-serveur Lissafi

> Date : 2026-08-13
> Contexte : le programme de partenariat (spec `2026-08-13-programme-partenariat-design.md`) est en prod : l'admin ajoute les partenaires manuellement et attribue les ventes. Ce design ajoute l'**auto-inscription des partenaires**, un **espace partenaire** avec suivi des clients/gains/visites, et un **système de retrait à la demande**. Attribution conservée : lien unique par partenaire → message WhatsApp pré-rempli contenant le code. Paiement physique (Orange Money/Moov), pas de paiement en ligne.

## Décisions actées

| Décision | Choix |
|---|---|
| Objectif | **Automatiser** : les partenaires s'inscrivent seuls, suivent leurs clients et demandent leurs retraits |
| Où vit le portail | **Back-office (Next.js)** en routes publiques — approche A + URL propre (URL Vercel gratuites pour l'instant) |
| Accès partenaire | **Email + mot de passe** via Supabase Auth, session cookie httpOnly côté serveur |
| Inscription | **Auto-inscription, actif immédiatement** (pas d'approbation admin) |
| Tracking d'attribution | Lien unique `?p=CODE` → landing → **message WhatsApp pré-rempli** avec le code ; le code voyage dans le message |
| Visites | **Enregistrement automatique** d'une visite à chaque clic sur un lien partenaire (landing → route API `/api/visits`) |
| Espace partenaire | Clients confirmés, statut commission (Due/Payée), gains (mois + cumulés), visites |
| Retrait | **À la demande par le partenaire**, montant **libre** ≤ disponible, statut `requested` → `paid` |
| Paiement | Physique (Orange Money/Moov) — le partenaire déclenche, l'admin valide |
| Sécurité | **Tout côté serveur** (`service_role`) — RLS admin inchangée, aucune clé exposée au navigateur |

## Flux complet

### Parcours partenaire
1. Landing → « Devenir partenaire » → `/partenaire`.
2. **Inscription** : nom, téléphone, email, mot de passe → compte Supabase Auth créé côté serveur + ligne `partners` liée (`auth_uid`), type `agent` par défaut, statut `active`, code auto `PTN-XXXXX`. Connexion automatique.
3. Son espace affiche : son **lien personnel** `…/?p=PTN-XXXXX` + **QR code** (téléchargeable pour affichette) + bouton copier.
4. Il diffuse son lien (statut WhatsApp, bio TikTok, affichette).

### Parcours client (attribution)
5. Le client clique le lien → la landing lit `?p=`, **enregistre une visite**, et le bouton « Démarrer » ouvre `wa.me/<numero>?text=…je viens de la part de PTN-XXXXX`.
6. Le client envoie → l'admin reçoit le message **avec le code**.
7. Le client paie (mobile money) → l'admin active → l'admin **attribue la vente** au partenaire dans `/partenaires` (un clic ; le partenaire existe déjà). → `partner_sales` créée (commission Due).

### Suivi & retrait
8. Le partenaire voit le client apparaître dans son espace (confirmé, commission Due), ses gains et ses visites.
9. **Demande de retrait** : bouton « Demander un retrait » → montant libre ≤ disponible → `partner_payouts` (status `requested`).
10. L'admin voit la file des demandes → paie (Orange Money/Moov) → « Valider le paiement » → la demande passe `paid` ET les plus anciennes commissions dues du partenaire sont marquées `paid` pour couvrir le montant (zéro double comptage).
11. Le partenaire voit la demande `Payée`.

## Règles produit

- **Disponible** = `somme(commissions dues)` − `somme(retraits demandés non payés)`. Une demande est refusée si le montant dépasse le disponible.
- **Barème inchangé** (constant de code) : Plus 10 000 F, Business 15 000 F, Pack 5 000 F.
- Un partenaire **inactif** (désactivé par l'admin) : ne peut plus être attribué (comportement existant) mais peut **voir** son espace et demander un retrait de ce qui est dû.
- Un client = une ligne `partner_sales` ; un renouvellement = une nouvelle vente.
- Auto-inscription : type `agent` par défaut ; l'admin peut le changer dans `/partenaires`.

## Modèle de données (migration dans `supabase-admin.sql`, idempotente)

```sql
-- partners : lien vers le compte Supabase Auth + email
ALTER TABLE public.partners ADD COLUMN IF NOT EXISTS auth_uid UUID;
ALTER TABLE public.partners ADD COLUMN IF NOT EXISTS email TEXT NOT NULL DEFAULT '';
CREATE UNIQUE INDEX IF NOT EXISTS idx_partners_auth_uid
    ON public.partners(auth_uid) WHERE auth_uid IS NOT NULL;

-- Visites (un clic sur un lien partenaire) — écrites UNIQUEMENT via service_role (route API)
CREATE TABLE IF NOT EXISTS public.partner_visits (
    id BIGSERIAL PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES public.partners(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL
);
ALTER TABLE public.partner_visits ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partner_visits FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partner visits" ON public.partner_visits;
CREATE POLICY "admins manage partner visits" ON public.partner_visits
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());
CREATE INDEX IF NOT EXISTS idx_partner_visits_partner ON public.partner_visits(partner_id);

-- Demandes de retrait
CREATE TABLE IF NOT EXISTS public.partner_payouts (
    id BIGSERIAL PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES public.partners(id) ON DELETE CASCADE,
    amount_fcfa INT NOT NULL CHECK (amount_fcfa > 0),
    status TEXT NOT NULL DEFAULT 'requested' CHECK (status IN ('requested', 'paid')),
    requested_at BIGINT NOT NULL,
    paid_at BIGINT
);
ALTER TABLE public.partner_payouts ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.partner_payouts FROM anon, authenticated;
DROP POLICY IF EXISTS "admins manage partner payouts" ON public.partner_payouts;
CREATE POLICY "admins manage partner payouts" ON public.partner_payouts
    FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());
CREATE INDEX IF NOT EXISTS idx_partner_payouts_partner ON public.partner_payouts(partner_id);
```

**Politique RLS inchangée** : les tables restent admin-only (`is_admin()`). Aucune ouverture aux partenaires — le portail lit via `service_role` côté serveur, filtré par l'identité du cookie.

## Sécurité

- **Sessions** : nouveau cookie httpOnly `lissafi_partner_token` (JWT GoTrue), distinct du cookie admin `lissafi_admin_token`. `getPartnerSession()` valide le token et vérifie qu'un `partners.auth_uid` lui correspond → retourne l'id partenaire.
- **Toutes les lectures/écritures partenaire** passent par le serveur Next.js avec `service_role`, filtrées par l'id partenaire du cookie. **Jamais** de requête Supabase depuis le navigateur partenaire.
- **Visites** : la landing (statique) POST vers `/api/visits` (route publique du back-office) avec `{ code }`. La route résout le partenaire par code et insère via `service_role`. **CORS restreint** à l'origine de la landing (Vercel).
- **Inscription** : côté serveur via `supabaseAdmin().auth.admin.createUser({ email, password, email_confirm: true })` → pas de dépendance au toggle « signups » ni à l'email. Doublon email → erreur claire. Mot de passe ≥ 8 caractères.
- **Retrait** : montant validé serveur (entier > 0, ≤ disponible), calcul du disponible côté serveur.

## Écrans

### Portail partenaire (routes publiques du back-office, groupe `(public)`)

| Route | Contenu |
|---|---|
| `/partenaire` | Page « Devenir partenaire » : le deal (commission par client payant), boutons « Créer mon compte » / « Me connecter » |
| `/partenaire/inscription` | Formulaire : nom, téléphone, email, mot de passe |
| `/partenaire/connexion` | Formulaire : email, mot de passe |
| `/partenaire/espace` | Tableau de bord (protégé par session partenaire) : lien + QR + copier ; stats (visites, clients confirmés, gains du mois, gains cumulés, commission due/payée) ; bouton « Demander un retrait » ; historique des retraits ; liste des clients ; liste des visites récentes |

### Back-office admin (groupe `(admin)`)

| Route | Contenu |
|---|---|
| `/partenaires` | Inchangé — les auto-inscrits y apparaissent (+ email affiché) ; attribution de vente et marquage des commissions existants |
| `/partenaires/retraits` | **Nouveau** : file des demandes de retrait (partenaire, montant, téléphone, date, statut) + « Valider le paiement » |

### Landing (`landing/`, Vite statique)

- Bouton « Devenir partenaire » → lien vers l'URL du portail (config).
- Lecture de `?p=CODE` : badge « Parrainé par … », le CTA WhatsApp pré-rempli avec le code, et **beacon de visite** (POST `/api/visits`).
- CTA WhatsApp par défaut inchangé (sans code) quand pas de `?p=`.

## Code (patterns existants)

- **`src/lib/session.ts`** : + `getPartnerSession()`, `requirePartner()`.
- **`src/actions/partners.ts`** : + `partnerSignUp(formData)`, `partnerSignIn(formData)`, `partnerSignOut()`, `requestPayout(partnerId, amount)`.
- **`src/actions/payouts.ts`** (nouveau) : `approvePayout(payoutId)` — passe la demande `paid` + marque les plus anciennes `partner_sales` dues du partenaire `paid` jusqu'à couvrir le montant (transaction ou loop avec contrôle du solde).
- **`src/lib/data.ts`** : + `getPartnerDashboard(partnerId)`, `getPayoutRequests()`, `getPartnerVisits(partnerId)`.
- **`src/types.ts`** : + `PartnerDashboard`, `PartnerVisit`, `PartnerPayout`.
- **`src/app/api/visits/route.ts`** (nouveau, public) : POST `{ code }` → résout partenaire → insère visite. CORS restreint.
- **`src/lib/partners.ts`** : + helpers `computeAvailable(owed, requestedUnpaid)`, label QR/link.
- **Landing** : `src/lib/tracking.js` (lecture `?p=`, beacon), composant CTA avec pré-remplissage WhatsApp, bouton « Devenir partenaire ».

## Gestion d'erreurs & limites

- Inscription : email déjà utilisé → message clair ; mot de passe < 8 → refus ; email invalide → refus.
- Retrait : montant ≤ 0, > disponible, ou partenaire inexistant → refus avec message.
- `/api/visits` : code inconnu → 404 silencieux ; origine non autorisée → 403 ; pas de rate-limit en V1 (accepté, volume faible).
- Session partenaire expirée → redirection `/partenaire/connexion`.

## Vérification

1. `cd backoffice && npm run build` + `cd landing && npm run build` — verts.
2. Test navigateur complet (dev, avec un compte partenaire) :
   - Inscription → dashboard → lien + QR visibles.
   - Attribution par l'admin d'une vente au partenaire → elle apparaît côté partenaire (commission Due).
   - Demande de retrait (montant libre ≤ disponible) → visible dans `/partenaires/retraits` → validation → statut Payée + commissions marquées payées.
   - Lien `?p=CODE` sur la landing → message WhatsApp pré-rempli + une visite enregistrée.
3. **Action utilisateur** : relancer `supabase-admin.sql` dans le SQL Editor Supabase (idempotent) pour les nouvelles colonnes/tables.

## Hors périmètre (V1)

- Domaine custom (URL Vercel gratuites pour l'instant) — ajout d'un domaine plus tard sans changement de code (env).
- Approbation manuelle des inscriptions (actif immédiat).
- Notifications push/WhatsApp automatiques aux partenaires (changement de statut).
- Rate-limiting des visites et anti-spam de clics.
- Paiement en ligne.

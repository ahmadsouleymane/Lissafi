# Lissafi Admin — Back-office

Interface d'administration web pour Lissafi : activation manuelle des comptes premium, statistiques globales, suivi des connexions et des erreurs, et gestion du support.

**Stack** : Next.js 15 (App Router) · TypeScript · Tailwind CSS · Supabase. Déployable sur Vercel.

---

## 1. Ce que fait le back-office

| Page | Contenu |
|---|---|
| **Tableau de bord** `/` | Comptes, premium actifs, ventes, reçus, chiffre, erreurs 24 h, tickets ouverts. Graphiques ventes + inscriptions (30 j). Activité récente. |
| **Comptes** `/comptes` | Liste de tous les comptes (boutique, email, téléphone, statut premium, produits, ventes, chiffre, dates). Recherche + filtres. |
| **Détail compte** `/comptes/[id]` | Tout sur un compte : stats, paramètres, ventes récentes, logs, historique admin. **Actions** : activer premium (1 an / 30 j / durée libre), prolonger, désactiver, réinitialiser le mot de passe, lien WhatsApp. |
| **Premium** `/premium` | Vue dédiée : actifs, expirations ≤ 30 j, expirés. Activation rapide par ligne. |
| **Erreurs & activité** `/logs` | Journal `app_logs` (démarrages, synchros, reçus, erreurs) avec filtres niveau/type/période. |
| **Connexions** `/connexions` | Journal d'authentification Supabase (connexions, inscriptions, échecs, IP) + synthèse par compte. |
| **Support** `/support` | Tickets envoyés depuis l'app : statuts, priorités, réponses, suppression. |
| **Réglages** `/reglages` | Prix premium, durées par défaut, état de la configuration, aide pour ajouter un admin. |
| **Notifications** `/notifications` | Envoi manuel de notifications push (titre + message, ciblage par statut ou par compte) et état du récap automatique quotidien (lundi–vendredi 8h, heure de Niamey). |

---

## 2. Prérequis Supabase

1. Exécuter `supabase-schema.sql` (schéma de l'app — déjà en place si l'app fonctionne).
2. Exécuter **`supabase-admin.sql`** (à la racine du repo) dans le SQL Editor. Il crée :
   - les tables `admins`, `app_logs`, `support_tickets`, `ticket_replies`, `admin_settings`, `admin_actions`
   - les fonctions d'agrégation `admin_stats()`, `admin_user_summaries()`, `admin_audit_logs()`, `admin_logs()`, séries… (protégées, elles ne lisent le schéma `auth` que via le rôle service)

## 3. Donner l'accès admin

Créer un compte dans **Supabase → Authentication → Users → Add user** (email + mot de passe, auto-confirm ON), récupérer son `user_id`, puis :

```sql
INSERT INTO public.admins (user_id, email)
VALUES ('<UUID_DU_COMPTE>', 'ton@email.com');
```

## 4. Configuration locale

```bash
cp .env.example .env
# renseigner SUPABASE_ANON_KEY et SUPABASE_SERVICE_ROLE_KEY
# (Supabase → Settings → API). La service_role est SECRÈTE — jamais côté client.
npm install
npm run dev
```

Connexion sur `http://localhost:3000/login` avec le compte admin.

## 5. Déploiement sur Vercel

1. Importer le dépôt dans Vercel (Root Directory : `backoffice/`).
2. Ajouter les variables d'environnement `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `CRON_SECRET`.
3. Déployer. La connexion se fait sur `/login`.

## 6. Sécurité

- La **service_role key** n'est utilisée que côté serveur (server actions + server components) — jamais envoyée au navigateur.
- L'accès est contrôlé deux fois : le compte doit exister dans Supabase Auth **et** être dans la table `admins`.
- Chaque action admin est journalisée dans `admin_actions` (traçabilité).

## 7. Alimenter les données depuis l'app Android

Le suivi (erreurs, reçus, signalements) est remonté par l'app via deux nouveaux endpoints (`app_logs`, `support_tickets`). Voir `app/src/main/java/com/lissafi/app/data/remote/SupabaseApi.kt` → `logEvent()` et `reportSupportTicket()`. La RLS permet à chaque utilisateur d'écrire ses propres lignes, jamais celles des autres.

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

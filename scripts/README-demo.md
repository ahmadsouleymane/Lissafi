# 🎬 Démo Lissafi — Compte & données fictives

Ce dossier contient le script qui remplit **un compte démo** avec une boutique
fictive complète, pour tourner les vidéos TikTok et les démonstrations.

---

## 1. Ce que fait le script

`seed-demo.sql` :

1. **Crée le compte démo** (déjà confirmé → connexion immédiate) :
   - **Email** : `demo@lissafi.app`
   - **Mot de passe** : `demo123456`
2. **Crée une boutique** : *Boutique Albaraka* (Niamey) avec :
   - **23 produits** (riz, huile, sucre, lait, thé… en francs CFA réels)
   - **8 clients** (noms nigériens, téléphones +227), dont 5 avec dettes
   - **32 ventes** sur les ~26 derniers jours (comptant + crédit)
   - **Transactions de dette** cohérentes (`total_debt` = somme des transactions)
   - **Premium activé** : `is_premium=true`, expirant dans 365 jours,
     code d'activation `LISSAFI-PREMIUM-0001`, `demo_taken=true`
   - Paramètres boutique : `shop_name`, `shop_phone`, `admin_pin=1234`

> ⚠️ Le script ne touche **que** les données du compte `demo@lissafi.app`.
> Les données de tes autres comptes sont intactes (chaque ligne est filtrée par `user_id`).

---

## 2. Comment l'exécuter (2 minutes)

1. Ouvre ton dashboard Supabase : **https://supabase.com/dashboard**
   (projet `fnyuhpfzkvunscuylvqv`)
2. Menu gauche → **SQL Editor** → bouton vert **New query**
3. Colle **tout** le contenu de `scripts/seed-demo.sql`
4. Clique **Run** (ou ⌘ + Enter)
5. Tu dois voir dans le résultat :
   ```
   Compte démo : demo@lissafi.app / demo123456 (uid ...)
   Seed Lissafi terminé : boutique remplie pour demo@lissafi.app
   ```

---

## 3. Connexion dans l'app

1. Installe/ouvre l'APK Lissafi sur l'appareil de démo
2. Écran de connexion → **Se connecter**
3. Email : `demo@lissafi.app` — Mot de passe : `demo123456`
4. **Laisse l'app synchroniser ~5-10 s** (pull des données depuis Supabase).
   Tout apparaît : produits, clients, ventes, dettes, et « Premium » dans les réglages.

> L'app est *local-first* : les données sont téléchargées au premier sign-in,
> puis tout fonctionne **hors-ligne** (parfait pour la vidéo 4 « sans réseau »).

---

## 4. Ce que contient la démo

| Élément | Détail |
|---|---|
| **Boutique** | Boutique Albaraka — +227 91 12 34 56 — PIN admin `1234` |
| **Produits** | 23 articles, 5 catégories (Épicerie, Boissons, Hygiène, Scolaire, Divers) |
| **Stocks faibles** (pour la vidéo stock) | Jus de mangue (9/10), Pain (0/10), Sachet d'eau (3/10) |
| **Clients** | 8 clients, 5 débiteurs |
| **Dettes** | Amadou 12 000 F · Mariama 8 500 F · Fati 15 000 F · Zeinabou 6 500 F · Rakia 20 000 F |
| **Ventes** | 32 ventes sur 26 jours, dont 12 à crédit — CA réaliste |
| **Premium** | Actif 365 jours (statut « Premium » visible dans Réglages) |

---

## 5. Notes importantes

### Réinitialiser la démo
Relancer le script **réinitialise** toutes les données du compte démo à l'état
ci-dessus (suppression puis réinsertion). Utile avant chaque tournage.

> ⚠️ **Pour une réinitialisation propre** : ré-exécute le seed **puis**
> efface les données de l'app (Paramètres Android → Lissafi → Stockage →
> **Effacer les données**) avant de te reconnecter. Sinon l'app repousserait
> vers Supabase les anciennes données qu'elle garde en cache local.

### Fenêtre de 30 jours
L'app ne télécharge que les **30 derniers jours** de ventes au pull.
Le seed ne crée donc pas de ventes plus anciennes — tout tombe dans la fenêtre.

### Si le compte n'est pas créé (versions Supabase différentes)
Si la ligne `auth.users` échoue (rare, dépend de la version de Supabase),
procède autrement :
1. Crée le compte via l'écran **Inscription** de l'app (`demo@lissafi.app` /
   `demo123456`) et confirme l'email si demandé.
2. Relance ensuite le seed : il détectera le compte existant et remplira
   les données sans le recréer.

### Sécurité
`demo@lissafi.app` est un compte public de démo. Il partage la même table
Supabase que les vrais comptes mais **ne voit que ses propres données**
(RLS). Pense à supprimer ce compte (Authentication → Users) après la campagne
de démo si tu ne veux pas le garder.

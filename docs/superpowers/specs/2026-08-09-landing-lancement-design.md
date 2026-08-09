# Spec — Landing page prête au lancement (Lissafi)

**Date** : 2026-08-09
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Mettre la landing page (Vite + React, `landing/`) en état de **mise en ligne sur
Vercel** : vrai lien WhatsApp, téléchargement APK fonctionnel, SEO aligné sur la
grille tarifaire actuelle (25 000 F/an), image de partage en URL absolue,
configuration de déploiement. **Aucune refonte design** (la landing est déjà
moderne et complète).

## Contexte / audit

- `config.js` contient des **placeholders** : `wa.me/227XXXXXXXX`,
  `+227 XX XX XX XX` (`PHONE_DISPLAY`, **inutilisé** dans le code).
- Le bouton « Télécharger l'APK » pointe vers `lissafi.apk` mais **aucun
  fichier `lissafi.apk`** n'existe dans `public/` → lien cassé.
- `index.html` (SEO) annonce « Premium **15 000 F/an** » alors que la grille
  actuelle est **Essentiel 25 000 F/an** (68 F/jour) — incohérence dans la
  description, l'Open Graph, Twitter et les données structurées.
- `og:image` est un **chemin relatif** (`/assets/logo-master.png`) → image
  absente quand le lien est partagé sur WhatsApp/Facebook. Pas d'`og:url` ni
  de `canonical`.
- `.gitignore` contient `*.apk` → un APK dans `public/` ne serait **pas
  déployé** par Vercel (qui build depuis git).
- Le prix 25 000 F/an est cohérent partout dans l'UI (le `15000` du Calculator
  concerne les crédits oubliés, pas le prix — ne pas y toucher).

## Fonctionnalités (validées)

### 1. `src/config.js`

- `WHATSAPP` : conserver le numéro dans une constante unique, avec un
  commentaire clair indiquant de remplacer `227XXXXXXXX` par le vrai numéro
  (sans espaces, format international).
- **Supprimer** `PHONE_DISPLAY` (inutilisé).
- `APK` : conserver `"lissafi.apk"`.

### 2. `index.html` — SEO

- Corriger le prix partout : « Premium 15 000 F/an » → « Essentiel 25 000 F/an »
  dans `description`, `og:description`, `twitter:description`, et l'`Offer`
  structurée (`15000` → `25000`, libellé « Lissafi Essentiel — tout illimité
  pendant 1 an (68 FCFA par jour) »).
- `og:image` → `{SITE_URL}/assets/logo-master.png` (URL absolue).
- Ajouter `og:url` → `{SITE_URL}/` et `<link rel="canonical">` → `{SITE_URL}/`.
- `{SITE_URL}` est injecté au build par un **plugin Vite** (`vite.config.js`) à
  partir de `process.env.VITE_SITE_URL`, avec défaut `""`. Si vide, l'URL
  reste relative (comportement actuel) ; à définir en production (domaine).

### 3. APK téléchargeable

- `public/lissafi.apk` : le fichier APK compilé.
- `scripts/update-apk.mjs` *(nouveau)* : copie l'APK Android compilé vers
  `public/lissafi.apk`. Accepte un chemin en argument ; défaut :
  `../app/build/outputs/apk/release/app-release.apk` (repli debug si absent).
- `package.json` : ajouter le script `"update-apk": "node scripts/update-apk.mjs"`.
- `.gitignore` : ajouter `!public/lissafi.apk` (dés-ignore le fichier spécifique
  pour qu'il soit commité et déployé par Vercel).

### 4. Déploiement Vercel

- `vercel.json` *(nouveau)* :
  ```json
  { "buildCommand": "npm run build", "outputDirectory": "dist", "framework": "vite" }
  ```
- `.env.example` *(nouveau)* : `VITE_SITE_URL=` (commenté : le domaine final,
  ex `https://lissafi.app`).
- `README.md` *(nouveau)* : étapes de lancement — (1) remplir le numéro
  WhatsApp dans `config.js`, (2) `npm run update-apk`, (3) définir
  `VITE_SITE_URL` (local `.env` ou réglage Vercel), (4) `vercel deploy --prod`,
  (5) vérifier le partage.

### 5. `vite.config.js`

- Ajouter le plugin `inject-site-url` (transformIndexHtml) qui remplace
  `{SITE_URL}` par `process.env.VITE_SITE_URL || ""`.

## Fichiers

- Modify : `landing/src/config.js`, `landing/index.html`, `landing/vite.config.js`,
  `landing/package.json`, `.gitignore` (racine).
- Create : `landing/public/lissafi.apk` (binaire), `landing/scripts/update-apk.mjs`,
  `landing/vercel.json`, `landing/.env.example`, `landing/README.md`.

## Décisions techniques

- **Injection de l'URL site** : plugin Vite `transformIndexHtml` (fiable,
  toujours remplace, pas de dépendance à `%VITE_%` qui resterait littéral si la
  variable est absente).
- **APK en git** : dés-ignoré spécifiquement (`!public/lissafi.apk`) — le plus
  simple pour servir le fichier depuis Vercel. Le script `update-apk` est la
  procédure pour mettre à jour à chaque build Android.
- **Aucune modification des composants React** (Hero, Pricing, FAQ…) ni du CSS :
  le design est validé tel quel.

## Hors périmètre

- Refonte design, nouvelles sections, pages CGV/confidentialité.
- Signature release Android de l'APK (à traiter au pilier audit si besoin).
- Le numéro WhatsApp et le domaine réels (fournis par l'utilisateur au moment
  du déploiement — laissés en placeholder clair).

## Vérification

- `cd landing && npm run build` (Vite build OK).
- `npm run dev` + test navigateur : lien WhatsApp, bouton APK (fichier présent),
  page s'affiche.
- Partagé sur WhatsApp/facebook : l'image se charge une fois `VITE_SITE_URL` défini.

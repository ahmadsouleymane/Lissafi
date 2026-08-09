# Landing page — Plan d'implémentation (préparation lancement)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rendre la landing prête à mettre en ligne sur Vercel : lien WhatsApp réel, APK téléchargeable, SEO aligné (25 000 F/an), image de partage en URL absolue, config de déploiement.

**Architecture:** Vite + React statique. `config.js` centralise WhatsApp/APK. Un plugin Vite injecte `{SITE_URL}` (variable `VITE_SITE_URL`) dans `index.html` au build. L'APK est copié dans `public/lissafi.apk` (dés-ignoré pour être déployé par Vercel).

**Tech Stack:** Vite 5, React 18, motion. Déploiement Vercel (framework vite, output `dist`).

## Global Constraints

- **Build/vérification :** `cd landing && npm run build` (Vite build). `npm run dev` pour la vérification manuelle.
- **Langue :** commentaires, README et messages de commit en **français**. Jamais de mention IA/Claude.
- **Aucune modification des composants React ni du CSS** (design validé tel quel) — on ne touche que : `config.js`, `index.html`, `vite.config.js`, `package.json`, `.gitignore`, + fichiers de déploiement/scripts.
- **APK** : on copie l'APK **debug** (installable) par défaut. Le release existant est **non signé** (non installable) — le README le note.
- Ne pas commiter de gros binaire non souhaité : le `.gitignore` racine contient `*.apk` — on ajoute la négation `!landing/public/lissafi.apk` pour ne dés-ignorer QUE ce fichier.

---

### Task 1: SEO + config (`config.js`, `vite.config.js`, `index.html`)

**Files:**
- Modify: `landing/src/config.js`
- Modify: `landing/vite.config.js`
- Modify: `landing/index.html`

**Interfaces:**
- Produces: `WHATSAPP`/`APK` exportés de config.js (déjà consommés par les composants) ; plugin `inject-site-url` ; `index.html` aligné (25 000 F/an, `{SITE_URL}`).

- [ ] **Step 1: Modifier `config.js`**

Remplace tout le contenu de `landing/src/config.js` par :

```js
// ⚠️ AVANT DE DÉPLOYER : remplace `227XXXXXXXX` par ton numéro WhatsApp
// (format international, sans espaces ni +) — ex : 22790123456.
export const WHATSAPP = "https://wa.me/227XXXXXXXX";

// Fichier APK servi depuis le site (public/lissafi.apk).
// À mettre à jour après chaque build Android : `npm run update-apk`.
export const APK = "lissafi.apk";
```

- [ ] **Step 2: Modifier `vite.config.js`**

Remplace tout le contenu de `landing/vite.config.js` par :

```js
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [
    react(),
    {
      name: "inject-site-url",
      transformIndexHtml(html) {
        // `{SITE_URL}` dans index.html → domaine final (VITE_SITE_URL), vide par défaut.
        const site = process.env.VITE_SITE_URL || "";
        return html.replace(/\{SITE_URL\}/g, site);
      },
    },
  ],
  base: "./",
});
```

- [ ] **Step 3: Modifier `index.html`**

Dans `landing/index.html` :

a) **Meta description** (bloc `<meta name="description" .../>`) : remplacer
`Gratuit pour tester, Premium 15 000 F/an.` par
`Gratuit pour tester, Essentiel 25 000 F/an (68 F/jour).`

b) **og:image** : remplacer `content="/assets/logo-master.png"` par
`content="{SITE_URL}/assets/logo-master.png"`

c) **twitter:image** : idem → `content="{SITE_URL}/assets/logo-master.png"`

d) **twitter:description** : remplacer `Gratuit pour tester, Premium 15 000 F/an.`
par `Gratuit pour tester, Essentiel 25 000 F/an.`

e) **Après la balise `og:image`** (et avant `<!-- Twitter -->`), ajouter :

```html
    <meta property="og:url" content="{SITE_URL}/" />
```

f) **Juste après `<link rel="icon" ...>`** (dans le `<head>`, avant l'Open Graph), ajouter :

```html
    <link rel="canonical" href="{SITE_URL}/" />
```

g) **Données structurées** (`script type="application/ld+json"`) : dans l'`Offer`,
remplacer `"price": "15000"` par `"price": "25000"` et
`"description": "Lissafi Premium — tout illimité pendant 1 an (41 FCFA par jour)"`
par
`"description": "Lissafi Essentiel — tout illimité pendant 1 an (68 FCFA par jour)"`.

- [ ] **Step 4: Build**

Run: `cd landing && npm run build`
Expected: BUILD SUCCESSFUL. Vérifier dans `dist/index.html` que `{SITE_URL}` n'apparaît plus (remplacé par vide) et que le prix 25 000 F/an est présent.

- [ ] **Step 5: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add landing/src/config.js landing/vite.config.js landing/index.html
git commit -m "feat(landing): SEO aligné sur 25 000 F/an, image/URL site absolues, slot numéro WhatsApp"
```

---

### Task 2: Déploiement + script APK (`update-apk.mjs`, `package.json`, `.gitignore`, `vercel.json`, `.env.example`, `README.md`)

**Files:**
- Create: `landing/scripts/update-apk.mjs`
- Create: `landing/vercel.json`
- Create: `landing/.env.example`
- Create: `landing/README.md`
- Modify: `landing/package.json`
- Modify: `.gitignore` (racine)

**Interfaces:**
- Consumes: l'APK compilé Android (`../app/build/outputs/apk/debug/app-debug.apk`).
- Produces: `npm run update-apk` (commande), `landing/public/lissafi.apk` (via Task 3), config Vercel, doc de lancement.

- [ ] **Step 1: Créer `scripts/update-apk.mjs`**

Crée `landing/scripts/update-apk.mjs` :

```js
#!/usr/bin/env node
// Copie l'APK Android compilé vers public/lissafi.apk pour le téléchargement.
// Usage : node scripts/update-apk.mjs [chemin/vers/app.apk]
// Défaut : APK debug (installable). Pour la production, génère un APK release
// signé et passe son chemin en argument (le release du build est NON signé).
import { copyFileSync, existsSync, statSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const defaultSrc = resolve(here, "../../app/build/outputs/apk/debug/app-debug.apk");
const src = process.argv[2] ? resolve(process.argv[2]) : defaultSrc;
const dest = resolve(here, "../public/lissafi.apk");

if (!existsSync(src)) {
  console.error(`❌ APK introuvable : ${src}`);
  console.error("   Compile d'abord l'app (./gradlew assembleDebug) ou passe le chemin en argument.");
  process.exit(1);
}

copyFileSync(src, dest);
const mb = Math.round((statSync(dest).size / 1024 / 1024) * 10) / 10;
console.log(`✅ APK copié vers ${dest} (${mb} MB)`);
```

- [ ] **Step 2: Modifier `package.json`**

Dans `landing/package.json`, dans `"scripts"`, ajouter (après `"preview"`) :

```json
    "update-apk": "node scripts/update-apk.mjs"
```

- [ ] **Step 3: Modifier `.gitignore` (racine)**

Dans `.gitignore` (à la racine du repo), **juste après la ligne `*.apk`**, ajouter :

```
!landing/public/lissafi.apk
```

- [ ] **Step 4: Créer `vercel.json`**

Crée `landing/vercel.json` :

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "framework": "vite"
}
```

- [ ] **Step 5: Créer `.env.example`**

Crée `landing/.env.example` :

```
# Domaine final de la landing — sert à rendre absolus og:image, og:url et canonical.
# Exemple : VITE_SITE_URL=https://lissafi.app
VITE_SITE_URL=
```

- [ ] **Step 6: Créer `README.md`**

Crée `landing/README.md` :

```markdown
# Landing Lissafi

Page de vente statique (Vite + React + motion). Déployée sur Vercel.

## Avant de mettre en ligne

1. **Numéro WhatsApp** — dans `src/config.js`, remplace `227XXXXXXXX` par ton
   numéro réel (format international, sans espaces ni +).
2. **APK** — compile l'app Android puis copie l'APK dans `public/` :
   `npm run update-apk`
   (par défaut : `app-debug.apk`, installable. Pour la production, génère un
   APK **release signé** et passe son chemin : `npm run update-apk ../app/build/outputs/apk/release/app-release-signed.apk`).
3. **Domaine** — définis `VITE_SITE_URL` (ex `https://lissafi.app`) dans
   `landing/.env` en local ET dans les réglages du projet Vercel
   (Settings → Environment Variables). Sans lui, l'image de partage
   (`og:image`) reste un chemin relatif et n'apparaîtra pas sur WhatsApp/Facebook.

## Déploiement

- Vercel : importer le dossier `landing` (ou la racine avec le bon root),
  framework détecté `Vite`, output `dist`. Déploiement auto à chaque push.
- Local : `npm run dev` (développement), `npm run build` (build de prod),
  `npm run preview` (aperçu du build).

## Vérification après déploiement

- Ouvre la landing → clique « Télécharger l'APK » : le fichier se télécharge.
- Clique « WhatsApp » : ouvre un chat vers ton numéro.
- Partage le lien sur WhatsApp/Facebook : l'aperçu affiche l'image et la
  description correctes.
```

- [ ] **Step 7: Build**

Run: `cd landing && npm run build`
Expected: BUILD SUCCESSFUL (aucun impact des nouveaux fichiers sur le build).

- [ ] **Step 8: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add landing/scripts/update-apk.mjs landing/vercel.json landing/.env.example landing/README.md landing/package.json .gitignore
git commit -m "feat(landing): script update-apk, config Vercel, env.example et guide de lancement"
```

---

### Task 3: APK en place + build final + vérification

**Files:**
- Create (via script): `landing/public/lissafi.apk` (binaire, commité)

**Interfaces:**
- Consumes: `npm run update-apk` (Task 2).

- [ ] **Step 1: Copier l'APK**

Run: `cd landing && npm run update-apk`
Expected: `✅ APK copié vers .../landing/public/lissafi.apk (35.9 MB)` (le fichier debug installable).

- [ ] **Step 2: S'assurer que le fichier sera suivi par git**

Run: `cd /Users/macbookair/Desktop/Lissafi && git check-ignore -q landing/public/lissafi.apk && echo "IGNORE (problème)" || echo "OK — suivi par git"`
Expected: `OK — suivi par git` (la négation `.gitignore` fonctionne).

- [ ] **Step 3: Build final**

Run: `cd landing && npm run build`
Expected: BUILD SUCCESSFUL. Vérifier dans `dist/` :
- `ls -la dist/lissafi.apk` → le fichier est copié dans la sortie de build (Vite copie `public/` tel quel).
- `grep -o "25 000 F/an" dist/index.html` → présent.
- `grep -c "{SITE_URL}" dist/index.html` → `0` (remplacé).

- [ ] **Step 4: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add landing/public/lissafi.apk
git commit -m "build(landing): ajoute l'APK téléchargeable (public/lissafi.apk)"
```

- [ ] **Step 5: Vérification manuelle (serveur dev)**

Run: `cd landing && npm run dev` puis en navigateur (ou via la sortie de `npm run build` + `npm run preview`) :
1. La page s'affiche (Hero, démo, tarifs, FAQ, CTA).
2. Le lien « Télécharger l'APK » pointe vers `/lissafi.apk` et télécharge le fichier.
3. Le lien « WhatsApp » pointe vers `https://wa.me/227XXXXXXXX` (à remplacer par le vrai numéro avant prod).
4. Le prix affiché est « 25 000 F/an » (Essentiel).

---

## Self-Review (à exécuter après rédaction)

- [ ] Spec couverte : config.js (WhatsApp + suppression PHONE_DISPLAY), index.html (25 000 F/an, og:image/url/canonical absolus), APK (script + public/ + .gitignore), Vercel (vercel.json + .env.example + README), plugin Vite.
- [ ] Aucun placeholder : chaque étape contient le code complet. Le numéro WhatsApp est un placeholder DÉLIBÉRÉ et commenté (fourni par l'utilisateur au déploiement).
- [ ] Types/chemins cohérents : `{SITE_URL}` dans index.html ↔ plugin vite ; `scripts/update-apk.mjs` ↔ package.json ; `!landing/public/lissafi.apk` ↔ APK copié en Task 3.

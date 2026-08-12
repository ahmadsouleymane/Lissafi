# Refonte landing sur le design system de l'app — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer l'identité visuelle « ticket papier » de `landing/` par le design system réel de l'app Android (Inter, vert forêt `#0F6E46`, orange brûlé `#E67E22`, cartes plates, clair/sombre), avec une structure de sections repensée et de vraies captures d'écran.

**Architecture:** Landing statique Vite + React (aucun changement d'outillage). Un socle de tokens CSS (`landing/src/index.css`) porte toute l'identité visuelle et le mode clair/sombre ; chaque composant de section est réécrit sur ce socle. Les visuels d'app sont de vraies captures PNG (émulateur Android + compte démo Supabase existant), pas des recréations CSS.

**Tech Stack:** React 18, Vite 5, `motion` (Framer Motion), `lucide-react` (nouveau), CSS global (pas de framework CSS), Android Gradle + émulateur `Pixel_6a` pour les captures.

## Global Constraints

- Palette exacte de l'app (`app/src/main/java/com/lissafi/app/ui/theme/Color.kt`) : Primary `#0F6E46`, Secondary `#E67E22`, Background clair `#F8F9FA` / sombre `#121212`, Surface clair `#FFFFFF` / sombre `#1E1E1E`, Border clair `#E5E7EB` / sombre `#33383F`, TextSecondary clair `#6B7280` / sombre `#AEB4BC`, Error `#EF4444`, Success `#10B981`.
- Typo : Inter uniquement (poids 300/400/500/600), plus aucune trace de Fraunces/Instrument Sans/Space Mono.
- Formes : coins arrondis 8/12/16/20/24px (`LissafiShapes`), ombres douces `0 4px 12px rgba(0,0,0,0.04)`, pas de bordures épaisses ni de bords crénelés façon ticket.
- Tout le code, commentaires et messages de commit en **français**, commits conventionnels (`feat(landing): …`), **aucune mention d'IA/Claude**.
- Mobile-first : chaque section stylée d'abord pour 375px, puis étendue en desktop via media queries (`min-width`), jamais l'inverse.
- Contenu textuel des sections conservées (Problem, Calculator, Pricing, Install, FAQ, FinalCTA) repris tel quel — seul l'habillage visuel et la structure du markup changent, sauf ajout explicite prévu dans une tâche.
- Aucune modification du code Android au-delà de l'installation/lancement pour capture d'écran (pas de commit côté `app/`).

---

## Vue d'ensemble des fichiers

- **Create** :
  `landing/public/assets/screens/{caisse,produits,clients,rapports,recu}.png`,
  `landing/src/components/TrustBand.jsx`, `landing/src/components/AvantApres.jsx`,
  `landing/src/components/LocalFirst.jsx`,
  `landing/src/components/ThemeToggle.jsx`, `landing/src/lib/theme.js`.
- **Modify** : `landing/src/index.css` (réécriture quasi complète），
  `landing/index.html`, `landing/package.json`, `landing/src/App.jsx`,
  et tous les composants existants sous `landing/src/components/`
  (`Header.jsx`, `Hero.jsx`, `Problem.jsx`, `PhoneDemo.jsx`, `Calculator.jsx`,
  `Pricing.jsx`, `Install.jsx`, `FAQ.jsx`, `FinalCTA.jsx`, `Footer.jsx`, `ui.jsx`).
- **Remove** : rien (pas de fichier obsolète — `index.css` est réécrit en place).

---

### Task 1 : Captures d'écran réelles de l'app

**Files:**
- Create : `landing/public/assets/screens/caisse.png`, `produits.png`,
  `clients.png`, `rapports.png`, `recu.png`

**Interfaces:**
- Produces : 5 fichiers PNG statiques, largeur ≥ 1080px (résolution native de
  l'émulateur), utilisés tels quels comme `src` d'`<img>` par les tâches 3 et 7.

- [ ] **Étape 1 : builder l'app**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/macbookair/Desktop/Lissafi
./gradlew assembleDebug
```
Attendu : `BUILD SUCCESSFUL`, APK à
`app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Étape 2 : démarrer l'émulateur et installer l'APK**

```bash
~/Library/Android/sdk/emulator/emulator -avd Pixel_6a -no-snapshot-load &
~/Library/Android/sdk/platform-tools/adb wait-for-device
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Étape 3 : lancer l'app et se connecter au compte démo**

Lancer l'app (`adb shell monkey -p com.lissafi.app -c android.intent.category.LAUNCHER 1`),
passer l'onboarding, puis se connecter avec `demo@lissafi.app` / `demo123456`
(compte seedé par `scripts/seed-demo.sql` — boutique fictive avec produits,
clients et ventes déjà présents). Piloter l'UI via `adb shell input tap x y`
et `adb shell input text "..."` (coordonnées à repérer via
`adb exec-out screencap -p > /tmp/probe.png` avant chaque tap, en lisant le
PNG). Si le compte démo ne fonctionne pas (mot de passe changé, réseau
indisponible), replier sur la création d'un compte local avec 4-5 produits et
2-3 ventes saisis manuellement pour peupler les écrans.

- [ ] **Étape 4 : capturer chaque écran**

Pour chaque écran cible, naviguer dans l'app puis :
```bash
adb exec-out screencap -p > landing/public/assets/screens/<nom>.png
```
- `caisse.png` : écran Caisse avec panier rempli (2-3 produits ajoutés, total visible).
- `produits.png` : écran Produits avec la liste garnie par le seed.
- `clients.png` : écran Clients avec au moins un crédit en cours visible.
- `rapports.png` : écran Rapports/Activité avec graphique et chiffres non nuls.
- `recu.png` : `ReceiptSheet` (bottom sheet affiché après une vente sur
  l'écran Caisse — bouton « Encaisser » après avoir choisi un montant reçu)
  montrant le reçu et l'option de partage WhatsApp.

- [ ] **Étape 5 : vérifier et committer**

Ouvrir chaque PNG (`Read` tool) pour confirmer lisibilité (pas de barre de
notification qui gêne, pas d'écran de chargement vide). Recadrer si besoin
avec `sips` (ex. `sips -c <hauteur> <largeur> fichier.png`) pour retirer une
zone de statut système disgracieuse.

```bash
git add landing/public/assets/screens/
git commit -m "feat(landing): ajoute les captures d'écran réelles de l'app"
```

---

### Task 2 : Socle de design tokens + mode clair/sombre

**Files:**
- Modify : `landing/src/index.css` (section `:root` et globales, lignes 1-110
  actuelles), `landing/index.html` (police + `theme-color`)
- Create : `landing/src/lib/theme.js`, `landing/src/components/ThemeToggle.jsx`
- Modify : `landing/package.json` (ajout `lucide-react`)

**Interfaces:**
- Produces : variables CSS `--primary`, `--secondary`, `--bg`, `--surface`,
  `--surface-alt`, `--border`, `--text`, `--text-secondary`, `--text-tertiary`,
  `--success`, `--error`, `--radius-xs/sm/md/lg/xl`, `--shadow-card`,
  `--font-body` — consommées par toutes les tâches suivantes. Fonction
  exportée `initTheme()` et `toggleTheme()` dans `landing/src/lib/theme.js`,
  consommées par `ThemeToggle.jsx` et `App.jsx`.

- [ ] **Étape 1 : remplacer les tokens dans `index.css`**

Remplacer le bloc `:root { ... }` (actuellement lignes 6-30) par :

```css
:root {
  --primary: #0f6e46;
  --primary-container: #e8f5e9;
  --secondary: #e67e22;
  --secondary-container: #fff3e8;
  --bg: #f8f9fa;
  --surface: #ffffff;
  --surface-alt: #f4f5f6;
  --border: #e5e7eb;
  --text: #121212;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  --success: #10b981;
  --success-bg: #d1fae5;
  --error: #ef4444;
  --error-bg: #fee2e2;

  --font-body: "Inter", system-ui, sans-serif;

  --radius-xs: 8px;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-xl: 24px;
  --shadow-card: 0 4px 12px rgba(0, 0, 0, 0.04);
  --shadow-card-hover: 0 8px 24px rgba(0, 0, 0, 0.08);

  --container: 1160px;
}

html[data-theme="dark"] {
  --primary: #0f6e46;
  --primary-container: #123825;
  --secondary: #e67e22;
  --secondary-container: #3d2610;
  --bg: #121212;
  --surface: #1e1e1e;
  --surface-alt: #2a2a2a;
  --border: #33383f;
  --text: #f5f5f5;
  --text-secondary: #aeb4bc;
  --text-tertiary: #7d8590;
  --success: #10b981;
  --success-bg: #0f3d2a;
  --error: #ef4444;
  --error-bg: #4a1515;
  --shadow-card: 0 4px 12px rgba(0, 0, 0, 0.35);
  --shadow-card-hover: 0 8px 24px rgba(0, 0, 0, 0.5);
}
```

Mettre à jour `body` pour utiliser `background: var(--bg); color: var(--text); font-family: var(--font-body);`
et supprimer toute référence à `--paper`, `--ink`, `--green`, `--orange`,
`--font-display`, `--font-mono`, `--grain`, `.grain` (le grain de papier
n'a plus de sens avec le nouveau design system).

- [ ] **Étape 2 : gérer le thème en JS**

Créer `landing/src/lib/theme.js` :

```js
const STORAGE_KEY = "lissafi-theme";

export function initTheme() {
  const saved = localStorage.getItem(STORAGE_KEY);
  const theme = saved || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  document.documentElement.setAttribute("data-theme", theme);
  return theme;
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme");
  const next = current === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", next);
  localStorage.setItem(STORAGE_KEY, next);
  return next;
}

export function getTheme() {
  return document.documentElement.getAttribute("data-theme") || "light";
}
```

- [ ] **Étape 2bis : appliquer `initTheme()` avant le premier rendu**

Dans `landing/src/main.jsx`, appeler `initTheme()` avant `ReactDOM.createRoot`
(import ajouté en tête de fichier) pour éviter un flash de thème incorrect.

- [ ] **Étape 3 : composant `ThemeToggle`**

Créer `landing/src/components/ThemeToggle.jsx` :

```jsx
import { useState } from "react";
import { Sun, Moon } from "lucide-react";
import { toggleTheme, getTheme } from "../lib/theme";

export default function ThemeToggle() {
  const [theme, setTheme] = useState(getTheme());
  return (
    <button
      className="theme-toggle"
      aria-label={theme === "dark" ? "Passer en mode clair" : "Passer en mode sombre"}
      onClick={() => setTheme(toggleTheme())}
    >
      {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
```

Ajouter dans `index.css` :
```css
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, transform 0.15s;
}
.theme-toggle:hover {
  color: var(--primary);
  transform: translateY(-1px);
}
```

- [ ] **Étape 4 : installer `lucide-react` et remplacer les polices**

```bash
cd landing && npm install lucide-react
```

Dans `landing/index.html`, remplacer le bloc « Polices » (lignes 38-44
actuelles) par :

```html
<!-- Police : Inter (identique à l'app) -->
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link
  href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
  rel="stylesheet"
/>
```

Remplacer `<meta name="theme-color" content="#2E8B57" />` par
`<meta name="theme-color" content="#0F6E46" />`.

- [ ] **Étape 5 : vérifier**

```bash
cd landing && npm run build
```
Attendu : build Vite réussi (les composants ne sont pas encore reskinnés,
des styles cassés dans le rendu visuel sont normaux à ce stade — seule
l'absence d'erreur de build compte ici).

- [ ] **Étape 6 : commit**

```bash
git add landing/src/index.css landing/index.html landing/src/lib/theme.js \
  landing/src/components/ThemeToggle.jsx landing/package.json landing/package-lock.json \
  landing/src/main.jsx
git commit -m "feat(landing): socle de design tokens et bascule clair/sombre alignés sur l'app"
```

---

### Task 3 : Header + Hero (avec vraie capture)

**Files:**
- Modify : `landing/src/components/Header.jsx`, `landing/src/components/Hero.jsx`,
  `landing/src/index.css` (sections HEADER + HERO)

**Interfaces:**
- Consumes : tokens de la Task 2 (`--primary`, `--surface`, `--radius-*`,
  `--shadow-card`), `ThemeToggle` (Task 2), capture `caisse.png` (Task 1).

- [ ] **Étape 1 : réécrire le CSS du header**

Remplacer les règles `.header`, `.header-inner`, `.brand`, `.header-nav`,
`.header-cta` (lignes 191-241 actuelles) par une version sur les nouveaux
tokens :

```css
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: color-mix(in srgb, var(--bg) 90%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
}
.brand { display: flex; align-items: center; gap: 10px; text-decoration: none; }
.brand img { height: 28px; width: auto; }
.header-nav {
  display: flex;
  align-items: center;
  gap: clamp(16px, 2.4vw, 32px);
  font-size: 14px;
  font-weight: 500;
}
.header-nav a { text-decoration: none; color: var(--text-secondary); transition: color 0.15s; }
.header-nav a:hover { color: var(--primary); }
.header-actions { display: flex; align-items: center; gap: 10px; }
@media (max-width: 760px) {
  .header-nav { display: none; }
}
```

- [ ] **Étape 2 : mettre à jour `Header.jsx`**

Ajouter `ThemeToggle` dans les actions du header :

```jsx
import { APK } from "../config";
import ThemeToggle from "./ThemeToggle";

export default function Header() {
  return (
    <header className="header">
      <div className="container header-inner">
        <a className="brand" href="#top" aria-label="Lissafi — retour en haut">
          <img src="/assets/logo-header.svg" alt="Lissafi" />
        </a>
        <nav className="header-nav" aria-label="Navigation">
          <a href="#probleme">Le problème</a>
          <a href="#demo">La caisse</a>
          <a href="#tarifs">Tarifs</a>
          <a href="#faq">FAQ</a>
        </nav>
        <div className="header-actions">
          <ThemeToggle />
          <a className="btn btn-primary header-cta" href={APK} download>
            Télécharger
          </a>
        </div>
      </div>
    </header>
  );
}
```

- [ ] **Étape 3 : réécrire les boutons (`.btn`) et `.section`/`.container`**

Remplacer les règles `.btn`, `.btn-primary`, `.btn-ghost`, `.btn-whatsapp`,
`.kicker`, `.section`, `.container` (lignes 105-178 actuelles) :

```css
.container { width: 100%; max-width: var(--container); margin: 0 auto; padding: 0 clamp(20px, 4vw, 40px); }
.section { padding: clamp(64px, 10vw, 120px) 0; }

.kicker {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  padding: 13px 22px;
  border-radius: var(--radius-md);
  border: 1.5px solid transparent;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}
.btn:active { transform: translateY(1px) scale(0.99); }
.btn-primary {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 8px 20px -10px rgba(15, 110, 70, 0.6);
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 12px 26px -10px rgba(15, 110, 70, 0.7); }
.btn-ghost { background: transparent; border-color: var(--border); color: var(--text); }
.btn-ghost:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-2px); }
.btn-whatsapp {
  background: var(--secondary);
  color: #fff;
  box-shadow: 0 8px 20px -10px rgba(230, 126, 34, 0.6);
}
.btn-whatsapp:hover { transform: translateY(-2px); box-shadow: 0 12px 26px -10px rgba(230, 126, 34, 0.7); }
```

- [ ] **Étape 4 : réécrire le hero (markup + CSS) avec la vraie capture**

Remplacer le reçu CSS animé de `Hero.jsx` par la capture `caisse.png` dans un
mockup téléphone simple (pas besoin du plein cadre téléphone détaillé ici,
réservé à la démo Task 7) :

```jsx
import { motion } from "motion/react";
import { APK } from "../config";
import { Kicker } from "./ui";

export default function Hero() {
  return (
    <section className="hero" id="top">
      <div className="container hero-grid">
        <div>
          <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
            <Kicker>Caisse enregistreuse mobile</Kicker>
          </motion.div>
          <motion.h1 initial={{ opacity: 0, y: 26 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.08 }}>
            Le cahier, <span className="accent">c'est fini.</span>
          </motion.h1>
          <motion.p className="hero-sub" initial={{ opacity: 0, y: 22 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.18 }}>
            Toute ta boutique dans ton téléphone : la caisse, la monnaie, les crédits clients,
            le stock. Ça marche même sans réseau.
          </motion.p>
          <motion.div className="hero-cta" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.28 }}>
            <a className="btn btn-primary" href={APK} download>⬇ Télécharger l'APK</a>
            <a className="btn btn-ghost" href="#demo">Voir la démo</a>
          </motion.div>
          <motion.div className="hero-meta" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.8, delay: 0.45 }}>
            <span><b>Android uniquement</b></span>
            <span><b>Gratuit</b></span>
          </motion.div>
        </div>

        <motion.div className="hero-shot-wrap" initial={{ opacity: 0, y: 34 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.85, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}>
          <img className="hero-shot" src="/assets/screens/caisse.png" alt="Écran de caisse de l'application Lissafi" />
        </motion.div>
      </div>
    </section>
  );
}
```

Remplacer les règles `.hero`, `.hero-bg-barcode`, `.hero-grid`, `.hero h1`,
`.hero-sub`, `.hero-invite`, `.hero-cta`, `.hero-meta` et tout le bloc
`.receipt*`/`.scan-beam` (lignes 246-463 actuelles) par :

```css
.hero { padding: clamp(48px, 8vw, 100px) 0 40px; }
.hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;
  align-items: center;
}
.hero h1 {
  font-size: clamp(2.2rem, 8vw, 3.6rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.08;
}
.hero h1 .accent { color: var(--primary); }
.hero-sub { margin-top: 18px; font-size: clamp(16px, 2vw, 18px); color: var(--text-secondary); max-width: 34em; }
.hero-cta { margin-top: 26px; display: flex; flex-wrap: wrap; gap: 12px; }
.hero-meta { margin-top: 24px; font-size: 13px; color: var(--text-secondary); display: flex; flex-wrap: wrap; gap: 6px 20px; }
.hero-meta b { color: var(--primary); font-weight: 700; }
.hero-shot-wrap { display: flex; justify-content: center; }
.hero-shot {
  width: min(320px, 80vw);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card-hover);
}
@media (min-width: 900px) {
  .hero-grid { grid-template-columns: 1.15fr 0.85fr; gap: clamp(32px, 6vw, 80px); }
  .hero-shot-wrap { justify-content: flex-end; }
}
```

- [ ] **Étape 5 : vérifier visuellement**

```bash
cd landing && npm run dev
```
Ouvrir dans le navigateur (Chrome tool ou Playwright), vérifier à 375px et
1440px : header sticky lisible, toggle thème change bien les couleurs,
capture d'écran affichée sans déformation dans le hero.

- [ ] **Étape 6 : commit**

```bash
git add landing/src/components/Header.jsx landing/src/components/Hero.jsx landing/src/index.css
git commit -m "feat(landing): reskin header et hero sur le design system de l'app"
```

---

### Task 4 : Nouvelle section — Bandeau de confiance (`TrustBand`)

**Files:**
- Create : `landing/src/components/TrustBand.jsx`
- Modify : `landing/src/index.css` (nouvelle section TRUST BAND)

**Interfaces:**
- Consumes : tokens Task 2.
- Produces : composant `TrustBand` exporté par défaut, monté par `App.jsx`
  (Task 14) juste après le Hero.

- [ ] **Étape 1 : créer le composant**

```jsx
import { Reveal } from "./ui";

const POINTS = [
  "Conçu avec des commerçants de Niamey",
  "Fonctionne même sans réseau",
  "Tes données restent sur ton téléphone",
];

export default function TrustBand() {
  return (
    <section className="trust-band">
      <div className="container trust-band-inner">
        {POINTS.map((p) => (
          <Reveal key={p} as="span" className="trust-point">
            {p}
          </Reveal>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Étape 2 : CSS**

```css
.trust-band { border-bottom: 1px solid var(--border); background: var(--surface-alt); }
.trust-band-inner {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px 28px;
  padding: 18px 0;
}
.trust-point {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
}
.trust-point::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  margin-right: 8px;
}
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/TrustBand.jsx landing/src/index.css
git commit -m "feat(landing): ajoute le bandeau de confiance"
```

---

### Task 5 : Nouvelle section — Avant / Après (`AvantApres`)

**Files:**
- Create : `landing/src/components/AvantApres.jsx`
- Modify : `landing/src/index.css` (nouvelle section AVANT/APRÈS)

**Interfaces:**
- Consumes : tokens Task 2, capture `rapports.png` (Task 1).

- [ ] **Étape 1 : créer le composant**

```jsx
import { Kicker, Reveal } from "./ui";

const AVANT = [
  "Calculs de tête, erreurs fréquentes",
  "Crédits notés sur un bout de papier, vite perdu",
  "Aucune idée du chiffre du mois",
  "Le stock, à l'œil et à la mémoire",
];

const APRES = [
  "Le total et la monnaie calculés automatiquement",
  "Chaque crédit client daté et retrouvable",
  "Le chiffre du jour, de la semaine, du mois — en un coup d'œil",
  "Une alerte avant la rupture de stock",
];

export default function AvantApres() {
  return (
    <section className="section" id="avant-apres">
      <div className="container">
        <Reveal>
          <Kicker>Avant / Après</Kicker>
          <h2>Le cahier vs Lissafi.</h2>
        </Reveal>

        <div className="avant-apres-grid">
          <Reveal>
            <div className="aa-col aa-avant">
              <div className="aa-label">Avec le cahier</div>
              <ul>
                {AVANT.map((t) => <li key={t}>{t}</li>)}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.1}>
            <div className="aa-col aa-apres">
              <div className="aa-label">Avec Lissafi</div>
              <ul>
                {APRES.map((t) => <li key={t}>{t}</li>)}
              </ul>
              <img className="aa-shot" src="/assets/screens/rapports.png" alt="Écran de rapports de l'application Lissafi" />
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Étape 2 : CSS**

```css
.avant-apres-grid {
  margin-top: 36px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
.aa-col {
  border-radius: var(--radius-lg);
  padding: 28px 24px;
  border: 1px solid var(--border);
}
.aa-avant { background: var(--surface-alt); }
.aa-apres { background: var(--primary-container); border-color: var(--primary); }
.aa-label { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 16px; }
.aa-avant .aa-label { color: var(--text-secondary); }
.aa-apres .aa-label { color: var(--primary); }
.aa-col ul { list-style: none; padding: 0; margin: 0; display: grid; gap: 12px; }
.aa-col li { font-size: 15px; padding-left: 22px; position: relative; color: var(--text); }
.aa-avant li::before { content: "✕"; position: absolute; left: 0; color: var(--error); font-weight: 700; }
.aa-apres li::before { content: "✓"; position: absolute; left: 0; color: var(--primary); font-weight: 700; }
.aa-shot {
  margin-top: 20px;
  width: 100%;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card);
}
@media (min-width: 820px) {
  .avant-apres-grid { grid-template-columns: 1fr 1fr; }
}
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/AvantApres.jsx landing/src/index.css
git commit -m "feat(landing): ajoute la section avant/apres"
```

---

### Task 6 : Reskin `Problem`

**Files:**
- Modify : `landing/src/components/Problem.jsx`, `landing/src/index.css`
  (section PROBLÈME)

**Interfaces:**
- Consumes : tokens Task 2. Contenu (`PAINS`) inchangé.

- [ ] **Étape 1 : adapter le markup**

Garder la structure de `Problem.jsx` (kicker, lede, stat, liste de cartes)
mais retirer le `<em>` italique stylé « display » (remplacé par
`<span className="accent">`) pour rester cohérent avec Inter :

```jsx
<p className="problem-lede">
  Le cahier ne coûte rien. <span className="accent">Et chaque mois, il te fait perdre de l'argent.</span>
</p>
```

- [ ] **Étape 2 : CSS**

Remplacer les règles `.problem-grid`, `.problem-lede`, `.problem-stat`,
`.problem-card*`, `.problem-num` (lignes 468-541 actuelles) :

```css
.problem-grid { display: grid; grid-template-columns: 1fr; gap: 32px; }
.problem-lede { font-size: clamp(1.4rem, 4vw, 1.9rem); font-weight: 600; line-height: 1.3; }
.problem-lede .accent { color: var(--primary); }
.problem-stat {
  margin-top: 22px;
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  background: var(--primary-container);
  color: var(--primary);
  padding: 10px 16px;
  border-radius: var(--radius-sm);
}
.problem-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  box-shadow: var(--shadow-card);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.problem-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-card-hover); }
.problem-num {
  font-weight: 700;
  font-size: 13px;
  color: var(--secondary);
  border: 1.5px solid var(--secondary);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.problem-card h3 { font-size: 16px; font-weight: 600; margin-bottom: 4px; color: var(--text); }
.problem-card p { font-size: 14.5px; color: var(--text-secondary); }
@media (min-width: 780px) {
  .problem-grid { grid-template-columns: 1fr 1fr; gap: clamp(28px, 5vw, 72px); align-items: start; }
}
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/Problem.jsx landing/src/index.css
git commit -m "feat(landing): reskin section probleme"
```

---

### Task 7 : Démo scroll-driven avec vraies captures (`PhoneDemo`)

**Files:**
- Modify : `landing/src/components/PhoneDemo.jsx` (réécriture quasi complète),
  `landing/src/index.css` (section DÉMO)

**Interfaces:**
- Consumes : `caisse.png`, `produits.png`, `clients.png`, `rapports.png`,
  `recu.png` (Task 1), tokens Task 2.
- Produces : composant `PhoneDemo` inchangé en API externe (aucune prop),
  toujours monté sous `id="demo"`.

- [ ] **Étape 1 : remplacer les mini-écrans CSS par des images réelles**

Supprimer entièrement les fonctions `CaisseScreen`, `CreditsScreen`,
`StockScreen`, `HorsligneScreen`, `TicketScreen`, `SoirScreen`
(lignes 11-269 actuelles) et le `PHONE_DEMO_MEDIA` import. Nouveau fichier :

```jsx
import { useState, useEffect, useRef } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Kicker, Reveal } from "./ui";

const FEATURES = [
  {
    id: "caisse",
    num: "01",
    title: "La caisse dans la poche",
    desc: "Touche un produit : le panier se remplit, le total se calcule, la monnaie se rend. Zéro erreur dans la foule.",
    img: "/assets/screens/caisse.png",
  },
  {
    id: "produits",
    num: "02",
    title: "Le stock à l'unité près",
    desc: "L'alerte sonne avant la rupture. Tu commandes avec des chiffres, pas des souvenirs.",
    img: "/assets/screens/produits.png",
  },
  {
    id: "clients",
    num: "03",
    title: "Les crédits qui se rappellent",
    desc: "Le nom, le montant, la date. Plus jamais de « je paie demain » oublié.",
    img: "/assets/screens/clients.png",
  },
  {
    id: "recu",
    num: "04",
    title: "La preuve par WhatsApp",
    desc: "Le ticket part directement sur le téléphone du client. La confiance, la fin des disputes.",
    img: "/assets/screens/recu.png",
  },
  {
    id: "rapports",
    num: "05",
    title: "Le soir, tout est là",
    desc: "Ton chiffre, tes ventes, tes crédits. Sans compter tes billets un par un.",
    img: "/assets/screens/rapports.png",
  },
];

export default function PhoneDemo() {
  const [active, setActive] = useState(0);
  const refs = useRef([]);

  useEffect(() => {
    const obs = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) setActive(Number(e.target.dataset.i));
        });
      },
      { rootMargin: "-42% 0px -42% 0px" }
    );
    refs.current.forEach((r) => r && obs.observe(r));
    return () => obs.disconnect();
  }, []);

  return (
    <section className="section demo" id="demo">
      <div className="container">
        <Reveal>
          <Kicker>La démo</Kicker>
          <h2>La caisse, en vrai. Scrolle, regarde.</h2>
        </Reveal>

        <div className="demo-grid" style={{ marginTop: 44 }}>
          <div className="phone-sticky">
            <div className="phone">
              <div className="phone-notch" aria-hidden="true" />
              <div className="phone-screen">
                <AnimatePresence mode="wait">
                  <motion.img
                    key={FEATURES[active].id}
                    className="phone-demo-media"
                    src={FEATURES[active].img}
                    alt={`Écran ${FEATURES[active].title} de l'application Lissafi`}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.28 }}
                  />
                </AnimatePresence>
              </div>
            </div>
          </div>

          <div className="feature-blocks">
            {FEATURES.map((f, i) => (
              <div
                key={f.id}
                className={`feature ${active === i ? "active" : ""}`}
                data-i={i}
                ref={(el) => (refs.current[i] = el)}
              >
                <span className="num">{f.num}</span>
                <h3 style={{ opacity: active === i ? 1 : 0.55, transition: "opacity 0.35s ease" }}>
                  {f.title}
                </h3>
                <p>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Étape 2 : CSS de la démo**

Remplacer tout le bloc « DÉMO TÉLÉPHONE » (lignes 543-872 actuelles :
`.demo*`, `.phone*`, `.app-*`) — les classes `.app-*` disparaissent
entièrement (plus de mini-écrans CSS) :

```css
.demo { background: var(--surface-alt); }
.demo .kicker { color: var(--secondary); }
.demo h2 { color: var(--text); }
.demo-grid { display: grid; grid-template-columns: 1fr; gap: 32px; }
.phone-sticky { display: flex; justify-content: center; }
.phone {
  width: 280px;
  height: 572px;
  border-radius: 40px;
  background: #0f0f0c;
  padding: 12px;
  box-shadow: 0 30px 70px -20px rgba(0, 0, 0, 0.45);
  position: relative;
}
.phone-notch {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 110px;
  height: 22px;
  background: #0f0f0c;
  border-radius: 0 0 14px 14px;
  z-index: 3;
}
.phone-screen { width: 100%; height: 100%; border-radius: 30px; overflow: hidden; position: relative; background: var(--surface); }
.phone-demo-media { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }

.feature-blocks { display: flex; flex-direction: column; }
.feature {
  min-height: auto;
  padding: 24px 0;
  border-bottom: 1px dashed var(--border);
}
.feature:last-child { border-bottom: none; }
.feature .num { font-size: 13px; font-weight: 700; letter-spacing: 0.08em; color: var(--secondary); }
.feature h3 { font-size: clamp(1.3rem, 4vw, 1.7rem); margin: 8px 0 10px; color: var(--text); }
.feature p { color: var(--text-secondary); font-size: 15px; max-width: 30em; }

@media (min-width: 860px) {
  .demo-grid { grid-template-columns: 0.9fr 1.1fr; gap: clamp(28px, 5vw, 64px); align-items: start; }
  .phone-sticky { position: sticky; top: 100px; }
  .feature { min-height: 58vh; display: flex; flex-direction: column; justify-content: center; }
}
```

- [ ] **Étape 3 : nettoyer `config.js`**

Supprimer `PHONE_DEMO_MEDIA` et `HERO_DEMO_MEDIA` de `landing/src/config.js`
(remplacés par les chemins directs vers `public/assets/screens/`), garder
`WHATSAPP`, `APK`, `INSTALL_SCREENSHOT`.

- [ ] **Étape 4 : vérifier visuellement**

`npm run dev`, scroller la section démo à 375px et 1440px : le téléphone doit
rester sticky en desktop, les images doivent changer sans déformation au
scroll.

- [ ] **Étape 5 : commit**

```bash
git add landing/src/components/PhoneDemo.jsx landing/src/index.css landing/src/config.js
git commit -m "feat(landing): demo scroll avec vraies captures d'ecran de l'app"
```

---

### Task 8 : Nouvelle section — Local-first / hors-ligne (`LocalFirst`)

**Files:**
- Create : `landing/src/components/LocalFirst.jsx`
- Modify : `landing/src/index.css` (nouvelle section LOCAL-FIRST)

**Interfaces:**
- Consumes : tokens Task 2, icônes `lucide-react` (`CloudOff`, `Cloud`, `ShieldCheck`).

- [ ] **Étape 1 : créer le composant**

```jsx
import { CloudOff, RefreshCw, ShieldCheck } from "lucide-react";
import { Kicker, Reveal } from "./ui";

const POINTS = [
  {
    Icon: CloudOff,
    title: "Ça marche sans réseau",
    desc: "Chaque vente s'enregistre directement sur ton téléphone, même en zone blanche ou en pleine coupure.",
  },
  {
    Icon: RefreshCw,
    title: "La synchro se fait toute seule",
    desc: "Dès que le réseau revient, tout part se sauvegarder en arrière-plan. Tu n'as rien à faire.",
  },
  {
    Icon: ShieldCheck,
    title: "Tes données ne se perdent jamais",
    desc: "Téléphone perdu, cassé, volé : tes ventes, tes clients et ton stock restent sauvegardés en ligne.",
  },
];

export default function LocalFirst() {
  return (
    <section className="section local-first" id="local-first">
      <div className="container">
        <Reveal>
          <Kicker>Fiabilité</Kicker>
          <h2>Le réseau n'est pas fiable à Niamey. Lissafi, si.</h2>
        </Reveal>

        <div className="local-first-grid">
          {POINTS.map((p, i) => (
            <Reveal key={p.title} delay={i * 0.08}>
              <div className="local-first-card">
                <p.Icon size={22} color="var(--primary)" />
                <h3>{p.title}</h3>
                <p>{p.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Étape 2 : CSS**

```css
.local-first-grid { margin-top: 36px; display: grid; grid-template-columns: 1fr; gap: 18px; }
.local-first-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px 22px;
  box-shadow: var(--shadow-card);
}
.local-first-card h3 { margin: 14px 0 8px; font-size: 16px; font-weight: 600; color: var(--text); }
.local-first-card p { font-size: 14.5px; color: var(--text-secondary); }
@media (min-width: 820px) {
  .local-first-grid { grid-template-columns: repeat(3, 1fr); }
}
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/LocalFirst.jsx landing/src/index.css
git commit -m "feat(landing): ajoute la section local-first"
```

---

### Task 9 : Reskin `Calculator`

**Files:**
- Modify : `landing/src/components/Calculator.jsx` (markup identique, classes
  inchangées), `landing/src/index.css` (section CALCULATEUR)

**Interfaces:**
- Consumes : tokens Task 2. Logique JS (slider, calcul) inchangée.

- [ ] **Étape 1 : CSS**

Remplacer les règles `.calc-*` (lignes 877-968 actuelles) :

```css
.calc-wrap { display: grid; grid-template-columns: 1fr; gap: 32px; align-items: center; }
.calc-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px 26px;
  box-shadow: var(--shadow-card);
}
.calc-panel label { display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 14px; }
.calc-slider { width: 100%; appearance: none; height: 6px; border-radius: 999px; background: var(--border); outline: none; }
.calc-slider::-webkit-slider-thumb {
  appearance: none; width: 24px; height: 24px; border-radius: 50%;
  background: var(--primary); border: 4px solid var(--surface);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2); cursor: grab;
}
.calc-slider::-moz-range-thumb {
  width: 20px; height: 20px; border-radius: 50%; background: var(--primary);
  border: 4px solid var(--surface); box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2); cursor: grab;
}
.calc-scales { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-tertiary); margin-top: 8px; }
.calc-numbers { margin-top: 22px; display: grid; gap: 6px; }
.calc-line { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; color: var(--text-secondary); }
.calc-line b { font-size: clamp(1.3rem, 3vw, 1.8rem); color: var(--text); font-weight: 700; }
.calc-line.saving b { color: var(--primary); }
.calc-verdict {
  margin-top: 18px; padding: 14px 16px; border-radius: var(--radius-sm);
  background: var(--primary-container); color: var(--primary); font-size: 14.5px;
}
@media (min-width: 820px) {
  .calc-wrap { grid-template-columns: 1fr 1fr; gap: clamp(28px, 5vw, 72px); }
}
```

- [ ] **Étape 2 : commit**

```bash
git add landing/src/index.css
git commit -m "feat(landing): reskin calculateur d'economies"
```

---

### Task 10 : Reskin `Pricing`

**Files:**
- Modify : `landing/src/components/Pricing.jsx` (markup identique), `landing/src/index.css`
  (section TARIFICATION)

**Interfaces:**
- Consumes : tokens Task 2. Contenu tarifaire inchangé (hors périmètre du spec).

- [ ] **Étape 1 : CSS**

Remplacer les règles `.pricing-grid`, `.price-card*`, `.price-badge*`,
`.price-plan`, `.price-amount`, `.price-sub`, `.price-features*`
(lignes 973-1095 actuelles) :

```css
.pricing-grid { display: grid; grid-template-columns: 1fr; gap: 20px; margin-top: 40px; }
.price-card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 30px 26px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: var(--shadow-card);
}
.price-card.premium { border: 2px solid var(--primary); background: var(--primary-container); }
.price-card.top { background: var(--text); color: var(--bg); border: 2px solid var(--text); }
.price-card.top .price-plan, .price-card.top .price-sub { color: var(--secondary); }
.price-card.top .price-features li { color: rgba(255, 255, 255, 0.88); }
.price-card.top .price-features li::before { color: var(--secondary); }
.price-badge {
  position: absolute; top: -12px; right: 22px;
  background: var(--primary); color: #fff;
  font-size: 11px; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase;
  padding: 6px 12px; border-radius: 999px;
}
.price-badge.top { background: var(--secondary); color: #fff; }
.price-plan { font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-secondary); }
.price-amount { font-size: clamp(1.8rem, 5vw, 2.3rem); font-weight: 700; letter-spacing: -0.01em; white-space: nowrap; }
.price-amount small { font-size: 0.42em; font-weight: 500; color: var(--text-secondary); }
.price-sub { font-size: 13px; color: var(--primary); margin-top: -8px; }
.price-features { list-style: none; padding: 0; margin: 0; display: grid; gap: 10px; flex: 1; }
.price-features li { display: flex; gap: 10px; font-size: 14.5px; align-items: flex-start; }
.price-features li::before { content: "✓"; color: var(--primary); font-weight: 700; flex-shrink: 0; }
.price-features li.off { color: var(--text-tertiary); }
.price-features li.off::before { content: "—"; color: var(--border); }
@media (min-width: 640px) and (max-width: 980px) {
  .pricing-grid { grid-template-columns: 1fr 1fr; }
  .price-card.top { grid-column: 1 / -1; }
}
@media (min-width: 980px) {
  .pricing-grid { grid-template-columns: repeat(3, 1fr); align-items: stretch; }
}
```

- [ ] **Étape 2 : commit**

```bash
git add landing/src/index.css
git commit -m "feat(landing): reskin section tarifs"
```

---

### Task 11 : Reskin `Install`

**Files:**
- Modify : `landing/src/components/Install.jsx` (styles inline → classes),
  `landing/src/index.css` (section INSTALLATION)

**Interfaces:**
- Consumes : tokens Task 2. Contenu (`STEPS`) inchangé.

- [ ] **Étape 1 : remplacer les styles inline par des classes**

Dans `Install.jsx`, remplacer le `<p style={{ marginTop: 28, ... }}>` et les
`<a style={{ color: "var(--green-deep)", ... }}>` par des classes CSS
(`.install-help`, `.link-primary`) — mêmes couleurs, sourcées sur les
nouveaux tokens :

```jsx
<p className="install-help">
  Un téléphone qui bloque ? Écris-moi sur{" "}
  <a href={WHATSAPP} target="_blank" rel="noreferrer" className="link-primary">WhatsApp</a>{" "}
  et je t'accompagne jusqu'au bout. Le fichier APK :{" "}
  <a href={APK} download className="link-primary">lissafi.apk</a>.
</p>
```

- [ ] **Étape 2 : CSS**

Remplacer les règles `.install-*` (lignes 1100-1161 actuelles) et ajouter
`.install-help` / `.link-primary` :

```css
.install-layout { display: grid; grid-template-columns: 1fr; gap: clamp(28px, 4vw, 48px); }
.install-grid { display: grid; grid-template-columns: 1fr; gap: 16px; margin-top: 40px; }
.install-layout.has-shot .install-grid { grid-template-columns: 1fr; }
.install-shot { margin-top: 40px; border-radius: var(--radius-lg); overflow: hidden; border: 1px solid var(--border); box-shadow: var(--shadow-card); }
.install-shot img { width: 100%; display: block; }
.install-step { position: relative; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 28px 24px; box-shadow: var(--shadow-card); }
.install-step .step-num { font-size: 44px; font-weight: 700; line-height: 1; color: var(--secondary); }
.install-step h3 { font-size: 17px; font-weight: 600; margin: 12px 0 8px; color: var(--text); }
.install-step p { font-size: 14px; color: var(--text-secondary); }
.install-help { margin-top: 28px; color: var(--text-secondary); font-size: 15px; }
.link-primary { color: var(--primary); font-weight: 600; text-decoration: none; }
.link-primary:hover { text-decoration: underline; }
@media (min-width: 800px) {
  .install-grid { grid-template-columns: repeat(3, 1fr); }
  .install-layout.has-shot { grid-template-columns: 1.3fr 0.7fr; }
  .install-layout.has-shot .install-grid { grid-template-columns: 1fr; }
}
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/Install.jsx landing/src/index.css
git commit -m "feat(landing): reskin section installation"
```

---

### Task 12 : Reskin `FAQ`

**Files:**
- Modify : `landing/src/components/FAQ.jsx` (icône `+` → lucide `Plus`),
  `landing/src/index.css` (section FAQ)

**Interfaces:**
- Consumes : tokens Task 2, `lucide-react` (`Plus`). Contenu (`ITEMS`) inchangé.

- [ ] **Étape 1 : remplacer l'icône `+`**

```jsx
import { Plus } from "lucide-react";
// ...
<button className="faq-q" onClick={onToggle} aria-expanded={open}>
  {q}
  <Plus className="ico" size={18} />
</button>
```

- [ ] **Étape 2 : CSS**

Remplacer les règles `.faq-*` (lignes 1166-1207 actuelles) :

```css
.faq-list { margin-top: 32px; border-top: 1px solid var(--border); }
.faq-item { border-bottom: 1px solid var(--border); }
.faq-q {
  width: 100%; text-align: left; background: none; border: none; cursor: pointer;
  padding: 20px 0; display: flex; justify-content: space-between; align-items: center;
  gap: 20px; font-size: clamp(1rem, 2vw, 1.15rem); font-weight: 600; color: var(--text);
}
.faq-q .ico { color: var(--primary); transition: transform 0.25s ease; flex-shrink: 0; }
.faq-item.open .ico { transform: rotate(45deg); }
.faq-a p { padding-bottom: 20px; font-size: 15px; color: var(--text-secondary); max-width: 52em; }
```

- [ ] **Étape 3 : commit**

```bash
git add landing/src/components/FAQ.jsx landing/src/index.css
git commit -m "feat(landing): reskin section faq"
```

---

### Task 13 : Reskin `FinalCTA` + `Footer`

**Files:**
- Modify : `landing/src/components/FinalCTA.jsx` (retrait du reçu CSS),
  `landing/src/components/Footer.jsx` (styles inline → classes),
  `landing/src/index.css` (sections CTA FINAL + FOOTER)

**Interfaces:**
- Consumes : tokens Task 2. Contenu textuel inchangé.

- [ ] **Étape 1 : `FinalCTA.jsx` sans reçu CSS**

Retirer le bloc `.receipt` du CTA final (dépendait de l'esthétique ticket
papier) et le remplacer par un simple visuel de mise en avant du CTA :

```jsx
import { motion } from "motion/react";
import { WHATSAPP, APK } from "../config";

export default function FinalCTA() {
  return (
    <section className="section final" id="commencer">
      <div className="container">
        <motion.h2
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.6 }}
        >
          Ta boutique mérite mieux qu'un cahier.
        </motion.h2>
        <motion.p
          className="final-sub"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          Gratuit pour tester. Essentiel à 25 000 F/an pour tout débloquer.
          L'installation prend 2 minutes.
        </motion.p>
        <motion.div
          className="final-cta"
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <a className="btn btn-primary" href={APK} download>⬇ Télécharger l'APK</a>
          <a className="btn btn-whatsapp" href={WHATSAPP} target="_blank" rel="noreferrer">
            Je veux une démo sur WhatsApp
          </a>
        </motion.div>
      </div>
    </section>
  );
}
```

- [ ] **Étape 2 : `Footer.jsx` sans style inline**

```jsx
import { WHATSAPP } from "../config";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-brand">
          <img src="/assets/logo-master.png" alt="Logo Lissafi" />
          <span>Lissafi</span>
        </div>
        <div className="footer-meta">Le cahier, c'est fini · Android</div>
        <div className="footer-meta">
          <a href={WHATSAPP} target="_blank" rel="noreferrer" className="link-primary">WhatsApp</a>
          {" "}· © 2026 Lissafi
        </div>
      </div>
    </footer>
  );
}
```

- [ ] **Étape 3 : CSS**

Remplacer les règles `.final*` et `.footer*` (lignes 1212-1270 actuelles) :

```css
.final { text-align: center; }
.final h2 { font-size: clamp(1.9rem, 6vw, 3rem); max-width: 20em; margin: 0 auto; }
.final-sub { margin: 16px auto 0; color: var(--text-secondary); font-size: 17px; max-width: 34em; }
.final-cta { margin-top: 28px; display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; }

.footer { border-top: 1px solid var(--border); padding: 36px 0 48px; margin-top: 40px; }
.footer-inner { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 16px; }
.footer-brand { display: flex; align-items: center; gap: 10px; }
.footer-brand img { height: 22px; }
.footer-brand span { font-weight: 700; font-size: 17px; color: var(--text); }
.footer-meta { font-size: 13px; color: var(--text-secondary); }
```

- [ ] **Étape 4 : commit**

```bash
git add landing/src/components/FinalCTA.jsx landing/src/components/Footer.jsx landing/src/index.css
git commit -m "feat(landing): reskin cta final et footer"
```

---

### Task 14 : Intégration finale, ordre des sections et vérification globale

**Files:**
- Modify : `landing/src/App.jsx`

**Interfaces:**
- Consumes : tous les composants des tâches 3-13.

- [ ] **Étape 1 : nouvel ordre des sections**

```jsx
import Header from "./components/Header";
import Hero from "./components/Hero";
import TrustBand from "./components/TrustBand";
import AvantApres from "./components/AvantApres";
import Problem from "./components/Problem";
import PhoneDemo from "./components/PhoneDemo";
import LocalFirst from "./components/LocalFirst";
import Calculator from "./components/Calculator";
import Pricing from "./components/Pricing";
import Install from "./components/Install";
import FAQ from "./components/FAQ";
import FinalCTA from "./components/FinalCTA";
import Footer from "./components/Footer";

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Hero />
        <TrustBand />
        <AvantApres />
        <Problem />
        <PhoneDemo />
        <LocalFirst />
        <Calculator />
        <Pricing />
        <Install />
        <FAQ />
        <FinalCTA />
      </main>
      <Footer />
    </>
  );
}
```

(Retrait du `<div className="grain" />` — plus de grain de papier dans le
nouveau design system.)

- [ ] **Étape 2 : build final**

```bash
cd landing && npm run build
```
Attendu : `BUILD SUCCESSFUL`, aucun avertissement sur des imports manquants
(`PHONE_DEMO_MEDIA`, `HERO_DEMO_MEDIA` ne doivent plus être référencés nulle
part — `grep -rn "PHONE_DEMO_MEDIA\|HERO_DEMO_MEDIA\|--paper\|--green-deep\|--font-mono\|--font-display" landing/src` doit être vide).

- [ ] **Étape 3 : vérification visuelle complète**

`npm run dev`, revue de bout en bout à 375px (mobile) et 1440px (desktop) :
- Toggle clair/sombre fonctionne sur toutes les sections (pas de couleur
  codée en dur qui ne réagit pas au changement de thème).
- Toutes les images de capture s'affichent sans être étirées/coupées
  bizarrement.
- Les CTA (`Télécharger l'APK`, `WhatsApp`) restent fonctionnels.
- Pas de résidu visuel de l'ancien design (police serif, beige, bords
  crénelés).

- [ ] **Étape 4 : commit final**

```bash
git add landing/src/App.jsx
git commit -m "feat(landing): nouvel ordre des sections de la landing"
```

---

## Self-Review

- **Couverture du spec** : système visuel (Task 2-3), 3 nouvelles sections
  (Task 4, 5, 8), vraies captures (Task 1, consommées en 3 et 5 et 7),
  reskin des sections conservées (Task 6, 9-13), nouvel ordre (Task 14),
  mobile-first partout (chaque bloc CSS part d'un layout à une colonne puis
  élargit via `min-width`). Tout couvert.
- **Cohérence des tokens** : toutes les tâches consomment les mêmes noms de
  variables définis en Task 2 (`--primary`, `--secondary`, `--surface`,
  `--border`, `--text`, `--text-secondary`, `--radius-*`, `--shadow-card*`)
  — aucune tâche n'introduit de nouvelle variable en doublon.
- **Cohérence des chemins d'images** : `caisse.png`, `produits.png`,
  `clients.png`, `rapports.png`, `recu.png` produits en Task 1 sont
  référencés à l'identique (même noms) en Task 3, 5 et 7.

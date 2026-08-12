# Spec — Refonte complète de la landing sur le design system de l'app

**Date** : 2026-08-12
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Remplacer intégralement l'identité visuelle actuelle de la landing (`landing/`,
concept « ticket de caisse papier » — Fraunces/Space Mono, beige, vert
`#2e8b57`) par le **design system réel de l'app Android** (Inter, vert forêt
`#0F6E46`, orange brûlé `#E67E22`, fond `#F8F9FA`, cartes plates à coins
arrondis, icônes Lucide). La structure des sections est repensée (pas un
simple reskin) et de nouveaux angles de contenu sont ajoutés. Priorité
**mobile-first**, sans négliger le rendu desktop.

## Contexte / audit

- Palette/typo actuelles de la landing (`landing/src/index.css`) : variables
  `--paper`, `--ink`, `--green: #2e8b57`, polices Fraunces/Instrument
  Sans/Space Mono — **aucun rapport** avec le thème réel de l'app.
- Design system réel de l'app (`app/.../ui/theme/Color.kt`, `Theme.kt`,
  `ui/components/LissafiComponents.kt`) :
  - Couleurs : `Primary #0F6E46`, `Secondary #E67E22`, `Background #F8F9FA`,
    `Surface #FFFFFF`, `Border #E5E7EB`, `TextSecondary #6B7280`,
    `Error #EF4444`, `Success #10B981`. Variantes sombres définies (app
    supporte clair/sombre via `ThemeManager`).
  - Typo : Inter (Light/Regular/Medium/SemiBold), tailles Material 3 standard.
  - Formes : coins arrondis 8/12/16/20/24dp selon le contexte
    (`LissafiShapes`), cartes plates avec ombre très douce (4dp, 4 % opacité
    noire), pas de bordure épaisse.
  - Icônes : Lucide (`com.composables.icons.lucide`).
- La démo actuelle (`PhoneDemo.jsx`) recrée les écrans en CSS pur (mini-écrans
  factices). L'utilisateur veut désormais de **vraies captures d'écran** de
  l'app compilée.
- Grille tarifaire affichée dans `Pricing.jsx` (50 produits/clients/crédits
  gratuits, 25 000 F/an, 100 000 F/an) diffère des limites décrites dans
  `CLAUDE.md` (10 produits / 10 crédits gratuits, codes premium statiques,
  démo 7 jours). **Hors périmètre** : cette refonte ne corrige pas les
  chiffres métier, seulement l'habillage visuel et la structure ; les textes
  de contenu sont repris tels quels sauf mention contraire ci-dessous.
- Émulateur Android disponible en local : AVD `Pixel_6a` +
  `~/Library/Android/sdk/platform-tools/adb`. Build nécessite
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
  (voir `CLAUDE.md`).

## Décisions validées (questions posées à l'utilisateur)

1. **Structure** : repensée, pas un simple reskin.
2. **Visuels d'app** : vraies captures d'écran de l'app compilée (pas de
   recréation CSS des écrans).
3. **Nouveaux angles de contenu** : local-first/hors-ligne, avant/après
   (cahier papier vs Lissafi), preuve sociale, + exploration libre
   d'angles supplémentaires côté design (sécurité des données, reçu
   WhatsApp partageable, impression Bluetooth — intégrés dans les sections
   existantes plutôt qu'en sections dédiées, pour ne pas diluer le parcours).

## Système visuel cible

- **Typo** : Inter (Google Fonts ou fichiers statiques `landing/public/fonts/`
  si dispo, sinon `@fontsource/inter` ou lien Google Fonts) — remplace
  Fraunces/Space Mono/Instrument Sans partout, y compris les libellés mono
  actuels (`kicker`, prix, tickets).
- **Couleurs** : reprise exacte des valeurs hex de `Color.kt` en variables CSS
  (`--primary: #0F6E46`, `--secondary: #E67E22`, `--bg: #F8F9FA`,
  `--surface: #FFFFFF`, `--border: #E5E7EB`, `--text-secondary: #6B7280`,
  etc.), avec le jeu de variantes sombres pour le toggle thème (cf.
  ci-dessous).
- **Formes** : cartes à coins arrondis 16–24px, ombres douces
  (`0 4px 12px rgba(0,0,0,0.04)` type), boutons à coins arrondis (12–16px,
  pas de pilule à outrance sauf badges), cohérent avec `LissafiShapes`.
- **Icônes** : `lucide-react` (mêmes pictos que l'app : Store, Package, Users,
  ChartColumnBig, Settings2, Scan, Banknote, CreditCard, Cloud, etc.).
- **Toggle clair/sombre** : un bouton dans le header bascule un attribut
  `data-theme="dark"` sur `<html>`, qui recalcule les variables CSS vers les
  valeurs sombres de `Color.kt` (`Background #121212`, `Surface #1E1E1E`,
  etc.). Démontre que la landing partage vraiment le design system de l'app.
  Préférence persistée en `localStorage`, défaut = préférence système
  (`prefers-color-scheme`).

## Structure des sections (nouvelle)

1. **Header** — logo, nav (ancrages), toggle thème, CTA (Télécharger /
   WhatsApp).
2. **Hero** — accroche + CTA + mockup téléphone avec **vraie capture** de
   l'écran Caisse (panier rempli, montant, bouton encaisser).
3. **Bandeau de confiance** — ligne courte de preuve sociale (citations ou
   chiffres représentatifs de commerçants Niamey ; contenu à valider avec
   l'utilisateur si de vrais témoignages existent, sinon formulation neutre
   du type « conçu avec des commerçants de Niamey »).
4. **Avant / Après** — comparatif 2 colonnes : cahier papier (erreurs de
   calcul, pages perdues, pas d'historique) vs Lissafi (capture d'écran
   Rapports ou Activité). Nouvelle section.
5. **Problème → Solution** — condensé du contenu actuel de `Problem.jsx`
   (3-4 cartes), reskinné.
6. **Démo produit scroll-driven** — remplace les mini-écrans CSS de
   `PhoneDemo.jsx` par de vraies captures (Caisse, Produits, Rapports, reçu
   WhatsApp) dans le mockup téléphone sticky, features listées au scroll.
7. **Local-first / fonctionne sans réseau** — nouvelle section dédiée :
   SQLite en local, la caisse fonctionne même sans connexion, synchro
   silencieuse en arrière-plan, sauvegarde cloud automatique (jamais de
   perte de données même en cas de panne du téléphone).
8. **Calculateur d'économies** — contenu gardé (`Calculator.jsx`), reskin visuel.
9. **Tarifs** — contenu gardé (`Pricing.jsx`), reskin visuel (cartes façon
   `LissafiCard`).
10. **Installation** — contenu gardé (`Install.jsx`), reskin, capture d'écran
    réelle du processus si simple à obtenir (sinon étapes textuelles comme
    aujourd'hui).
11. **FAQ** — contenu gardé (`FAQ.jsx`), reskin (accordéon plat, coins
    arrondis).
12. **CTA final + Footer** — gardé, reskin.

Chaque section existante est **réécrite visuellement** (nouveau CSS/markup
sur le design system app) ; le contenu textuel est repris sauf ajout
explicite ci-dessus.

## Captures d'écran réelles — plan technique

1. `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
   puis `./gradlew assembleDebug` (~8 min premier build).
2. Démarrer l'émulateur `Pixel_6a`
   (`~/Library/Android/sdk/emulator/emulator -avd Pixel_6a`).
3. `adb install app/build/outputs/apk/debug/app-debug.apk`.
4. Créer un compte/connexion locale minimale, saisir quelques produits et
   ventes de démo réalistes (noms/prix FCFA plausibles pour un petit
   commerce) pour que les captures ne soient pas vides.
5. Naviguer vers chaque écran cible via `adb shell input tap/text` ou
   pilotage manuel, capturer avec `adb exec-out screencap -p > fichier.png`.
6. Écrans à capturer (minimum) : Caisse (panier rempli), Produits (liste),
   Rapports/Activité (graphique avec données), reçu de vente (partage
   WhatsApp ou aperçu impression).
7. Recadrer/optimiser les PNG (retirer la barre de statut si besoin, export
   web-friendly) et les placer dans `landing/public/assets/screens/`.
8. Si une capture s'avère impossible à obtenir proprement dans un temps
   raisonnable (ex. état réseau/sync qui bloque), repli sur une recréation
   CSS fidèle pour cet écran précis uniquement, avec note dans le code.

## Fichiers

- **Modify** : tous les composants de `landing/src/components/*.jsx`,
  `landing/src/index.css` (refonte quasi complète), `landing/index.html`
  (police, meta si besoin), `landing/src/App.jsx` (nouvel ordre de sections),
  `landing/package.json` (ajout `lucide-react`, éventuellement
  `@fontsource/inter`).
- **Create** : nouveaux composants pour les sections ajoutées (`AvantApres.jsx`,
  `LocalFirst.jsx`, `TrustBand.jsx` ou noms équivalents), captures dans
  `landing/public/assets/screens/`.
- **Remove/simplify** : logique CSS spécifique au concept « ticket papier »
  qui n'a plus lieu d'être (grain de papier, bords crénelés du reçu, police
  mono partout) — sauf si conservée volontairement comme clin d'œil ponctuel
  dans le hero (à trancher pendant l'implémentation, par défaut : supprimée
  au profit du style app).

## Hors périmètre

- Correction des chiffres de tarification/limites gratuites (signalé, non
  traité ici).
- Vrais témoignages clients (contenu à fournir plus tard par l'utilisateur si
  disponible ; formulation neutre en attendant).
- Refonte du back-office (`backoffice/`) — non concerné.
- Déploiement Vercel / config domaine (déjà traité dans le spec du
  2026-08-09, non revisité ici sauf régression).

## Vérification

- `cd landing && npm run build` (Vite build OK, pas d'erreur).
- `npm run dev` + revue visuelle mobile (375px) et desktop (1440px) de
  chaque section.
- Toggle clair/sombre fonctionnel sur toutes les sections.
- Captures d'écran réelles intégrées et lisibles (pas de recadrage cassé,
  pas de barre de statut moche).
- Liens CTA (WhatsApp, APK) toujours fonctionnels après refonte.

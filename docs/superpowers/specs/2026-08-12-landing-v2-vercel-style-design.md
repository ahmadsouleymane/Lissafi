# Spec — Landing v2 : refonte complète style SaaS Vercel

**Date** : 2026-08-12
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Remplacer entièrement la landing v1 (refonte du même jour, jugée insuffisante :
mise en page plate, pas assez mobile-first, copywriting à revoir) par une
nouvelle version repensée de zéro : esprit SaaS moderne façon Vercel/Linear
(sombre-clair contrasté, grille visible, cartes bento nettes), tout en gardant
la charte de marque de l'app (vert forêt, orange brûlé, Inter). Mobile-first
strict, desktop hyper-soigné. Suppression totale du mode sombre (bascule
clair/sombre) et de tout contact WhatsApp.

## Contexte

- La landing v1 (`landing/src/`) vient d'être entièrement reskinnée sur le
  design system de l'app dans la même session (voir
  `2026-08-12-landing-redesign-app-design.md`), avec un système de tokens
  clair/sombre, des captures d'écran réelles (`public/assets/screens/*.png`,
  toujours valables et réutilisées), et 12 sections. L'utilisateur juge le
  résultat insuffisant : disposition plate, pas assez optimisée mobile,
  copywriting à revoir.
- Décision : on repart de zéro sur la structure et le CSS, mais on **garde**
  les captures d'écran déjà produites (`caisse.png`, `produits.png`,
  `clients.png`, `rapports.png`, `recu.png`) et les valeurs de couleur brand
  (`#0F6E46` vert, `#E67E22` orange) et la police Inter — inutile de
  recompiler l'app ou de refaire les captures.
- Trois contraintes explicites de l'utilisateur, validées en session :
  1. Un seul mode (clair) — suppression complète de `ThemeToggle`,
     `lib/theme.js`, et de tous les tokens `html[data-theme="dark"]`.
  2. Header transparent sur la hero section, qui devient un bandeau opaque
     clair dès qu'on quitte la hero (scroll).
  3. Logo blanc sur la hero (nécessite un hero à fond sombre — résolu par la
     direction visuelle ci-dessous, cohérente avec l'esthétique Vercel dont le
     hero réel est noir).
- Suppression de toute mention de pays (déjà faite dans la v1, à conserver).
- Suppression de tout CTA/lien WhatsApp (nouveau) : un seul CTA partout,
  « Télécharger l'APK ».

## Direction visuelle validée

- **Palette** : vert forêt `#0F6E46` (primary), orange brûlé `#E67E22`
  (secondary), fond principal quasi-blanc `#FAFAFA`, texte quasi-noir
  `#0A0A0A`, bordures fines `#E5E5E5` (grilles visibles façon Vercel docs).
  Plus de variantes sombres pour le reste du site (mode clair unique).
- **Hero** : bande sombre dédiée (noir teinté vert très profond, ex.
  `#05100B`/`#0A0A0A`), indépendante du reste du site qui est clair — c'est un
  choix esthétique délibéré (chapitre sombre d'ouverture), pas un mode sombre
  global. Grille de points subtile + halo dégradé vert/orange en fond, comme
  dans la v1 (réutilisable). Logo blanc, texte blanc/gris clair.
- **Header** : `position: fixed` (pas `sticky` avec fond permanent), fond
  transparent tant que le hero est visible (logo blanc, nav en blanc/gris
  clair), puis fond blanc opaque + ombre légère + logo vert dès que le scroll
  dépasse la hauteur du hero (détecté via `IntersectionObserver` sur un
  marqueur en bas du hero, pas de calcul de scroll manuel).
- **Typographie** : Inter conservée (contrainte de charte), mais traitement
  plus tranché : poids 800/900 pour les titres, tracking serré
  (`letter-spacing: -0.03em` à `-0.045em` selon taille), échelle plus grande
  en hero (`clamp(2.6rem, 9vw, 5rem)`).
- **Composants** : cartes à coins nets (`8–12px`, pas `20–24px`), bordures
  fines `1px solid #E5E7EB` plutôt que grosses ombres, grille de colonnes
  visible sur desktop (traits verticaux `1px` de part et d'autre du contenu
  central, comme les pages marketing Vercel), séparateurs horizontaux `1px`
  entre sections plutôt que changements de fond systématiques. Boutons à
  coins nets/légèrement arrondis (`8–10px`, pas pilule).
- **Kickers** : libellés courts en majuscules, `letter-spacing` large, petite
  taille — pas de style « mono » factice (on n'ajoute pas de police mono,
  contrainte Inter-only maintenue), juste une typographie de label nette.
- **Un seul CTA** : « Télécharger l'APK » partout (hero, tarifs, CTA final,
  header). Aucune trace de WhatsApp dans le code (composants, `config.js`,
  copie).

## Structure des sections (condensée : 9 au lieu de 12)

1. **Header** — transparent/blanc selon scroll, logo (variante blanche +
   variante verte selon état), nav minimale (ancrages), CTA téléchargement.
2. **Hero** (sombre) — accroche forte, sous-titre, CTA téléchargement, capture
   d'écran de l'app flottante avec glow (réutilise `caisse.png`), ligne de
   confiance discrète en bas de hero (« Gratuit à tester · Fonctionne hors
   ligne · Android »).
3. **Bento fonctionnalités** — grille asymétrique dense (remplace les 3
   anciennes sections Problème / Avant-après / Local-first de la v1) :
   quelques cartes de tailles différentes combinant argument texte + capture
   d'écran réelle (ex. grande carte hors-ligne avec `rapports.png`, carte
   crédits clients avec `clients.png`, carte stock avec `produits.png`).
4. **Démo scroll** — téléphone sticky + vraies captures au scroll (logique
   `PhoneDemo.jsx` de la v1 conservée), réhabillée en clair avec bordures
   nettes plutôt que fond sombre dupliqué (le hero est déjà le chapitre
   sombre, pas besoin d'un deuxième).
5. **Calculateur d'économies** — contenu gardé, cartes nettes.
6. **Tarifs** — contenu gardé (3 paliers), CTA unique téléchargement (plus de
   lien WhatsApp sur les cartes Essentiel/Premium — texte adapté en
   conséquence, ex. « Comment activer Essentiel/Premium » revu en FAQ).
7. **Installation** — 3 étapes gardées, le paragraphe d'aide qui pointait vers
   WhatsApp est retiré ou reformulé sans contact direct (renvoi vers la FAQ).
8. **FAQ** — contenu gardé et adapté (la question sur le paiement
   Essentiel/Premium ne mentionne plus WhatsApp comme canal de contact —
   reformulation nécessaire, à trancher pendant l'implémentation en gardant
   un message honnête sans sur-promettre un canal qui n'existe plus).
9. **CTA final + footer** — CTA unique téléchargement, footer minimal (logo,
   copyright), plus de lien WhatsApp.

## Fichiers

- **Modify** : tous les composants `landing/src/components/*.jsx` (réécriture
  quasi complète du markup et du contenu), `landing/src/index.css` (réécriture
  complète), `landing/src/App.jsx` (nouvelle liste de sections),
  `landing/src/config.js` (suppression de `WHATSAPP`), `landing/index.html`
  (meta `theme-color` ajusté au nouveau hero sombre).
- **Remove** : `landing/src/lib/theme.js`, `landing/src/components/ThemeToggle.jsx`
  (mode sombre supprimé).
- **Create** : un composant Header avec logique de scroll (`Header.jsx` réécrit
  avec `IntersectionObserver`), un composant pour le bloc bento fonctionnalités
  (ex. `FeatureBento.jsx`, remplace `Problem.jsx` + `AvantApres.jsx` +
  `LocalFirst.jsx` — ces trois fichiers sont supprimés).
- **Keep as-is** : `landing/public/assets/screens/*.png` (captures déjà
  produites), `landing/public/assets/logo-header.svg` (déjà blanc-compatible
  car vert sur transparent — à vérifier s'il faut une variante blanche pure
  pour le hero sombre, sinon le vert `#0F6E46` reste lisible sur fond très
  sombre et peut suffire sans nouvelle variante).

## Hors périmètre

- Refaire les captures d'écran de l'app (déjà faites, réutilisées).
- Corriger les montants/limites de tarification (déjà fait en v1, non
  revisité).
- Formulaire de contact ou email de remplacement pour WhatsApp (décision
  utilisateur : aucun canal de contact, tout mène au téléchargement).
- Back-office, app Android, autres parties du repo.

## Vérification

- `cd landing && npm run build` sans erreur.
- Revue visuelle mobile (375px) et desktop (1440px) : hero sombre avec logo
  blanc lisible, header qui bascule transparent → opaque au scroll, aucune
  trace de mode sombre résiduel (pas de bouton toggle, pas de flash de thème),
  aucune mention WhatsApp nulle part dans le rendu ni le code source
  (`grep -ri whatsapp landing/src` doit être vide).

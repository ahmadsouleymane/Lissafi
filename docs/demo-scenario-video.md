# 🎥 Scénario de tournage — Démo Lissafi avec le compte fictif

> Utilise le compte **`demo@lissafi.app` / `demo123456`** (voir
> `scripts/README-demo.md`). Les données ci-dessous proviennent du seed —
> elles sont déjà dans l'app après le premier sign-in.
>
> Format global : **screen recording** + voix off + textes à l'écran.
> Toujours montrer les **vrais chiffres** à l'écran (jamais de valeurs inventées
> en overlay). L'angle émotionnel de chaque vidéo est décrit dans `marketing-tiktok/`.

---

## 🎬 Rappel de la série (une vidéo = un bénéfice)

| # | Sujet | Bénéfice montré |
|---|---|---|
| 1 | Caisse dans ton téléphone | Scan, total, monnaie — le WOW |
| 2 | Crédits clients | Fini les « je paie demain » oubliés |
| 3 | Stock | Tu sais ce qui te reste, à l'unité près |
| 4 | Sans réseau | Ça marche quand même au marché |
| 5 | Ticket WhatsApp | Le client garde sa preuve d'achat |
| 6 | Le prix | Gratuit pour tester, Premium 15 000 F/an |

---

## Vidéo 1 — La caisse dans ton téléphone (scan → total → monnaie)

**Données à utiliser** (panier type, à scanner en vrai) :

| Article | Prix |
|---|---|
| Riz parfumé 25kg | 10 500 F |
| Huile végétale 1L | 1 000 F |
| Sucre en poudre 1kg | 700 F |
| **Total** | **12 200 F** |

**Déroulé caméra :**
1. Écran caisse → **scanner** le riz → *bip* → ligne ajoutée.
2. Scanner l'huile, le sucre. Le **total se calcule tout seul** (12 200 F).
3. Client donne **15 000 F** → l'app affiche **monnaie à rendre : 2 800 F**.
4. Coupé : on valide, le ticket apparaît.
5. Bonus punch : montrer la liste des produits récents déjà présents (boutique remplie).

> ⚠️ Si tu veux exactement **2 400 F** de monnaie, ajoute un « Concentré de
> tomate » (400 F) → total **12 600 F** (c'est la dernière vente du seed, celle d'aujourd'hui).

---

## Vidéo 2 — Les crédits clients (fini les oublis)

**Données à utiliser :**
- Client avec grosse dette : **Rakia Ali — 20 000 F**
- Autres débiteurs : Amadou Salifou **12 000 F**, Fati Seyni **15 000 F**
- Client propre : **Ibrahim Moussa — 0 F**

**Déroulé caméra :**
1. Ouvrir l'onglet **Clients** → la liste des débiteurs s'affiche avec les montants.
2. Ouvrir **Rakia Ali** → son historique de dette défile :
   - « Vente N°9 — 13 900 F » (riz + huile, le gros panier)
   - « Vente N°14 — 5 100 F »
   - « Vente N°17 — 2 500 F »
   - « Remboursement — 1 500 F »
   - **Total restant : 20 000 F**
3. Montrer qu'on peut **noter un nouveau crédit** et **barrer un paiement**
   (ajouter un remboursement de 2 000 F pendant le tournage → le total descend à 18 000 F).
4. Punch : « Le cahier, il est où ? Il est là, dans le téléphone. »

---

## Vidéo 3 — Le stock (tu sais ce qui te reste)

**Données à utiliser (stocks faibles du seed) :**

| Article | Stock | Seuil |
|---|---|---|
| Jus de mangue 1L | 9 | 10 → **alerte bas** |
| Pain (baguette) | 0 | 10 → **épuisé** |
| Sachet d'eau (pack) | 3 | 10 → **alerte bas** |

**Déroulé caméra :**
1. Ouvrir **Produits** → défiler jusqu'aux articles en alerte (badge de stock bas).
2. Ouvrir **Pain (baguette)** → stock **0** → on le voit en « rupture ».
3. Modifier le stock après une livraison (ex. passer le pain à 20) → le badge disparaît.
4. Punch : « Tu sais à l'unité près ce qui te reste. Avant, qui comptait le stock ? »

---

## Vidéo 4 — Sans réseau (mode avion)

**Données à utiliser :** la boutique déjà chargée (tout est déjà dans le téléphone).

**Déroulé caméra :**
1. Afficher la caisse avec la boutique remplie.
2. **Activer le mode avion devant la caméra** (barre de notifications visible).
3. Faire une vente complète **en mode avion** : scanner, total, monnaie, valider.
4. Le ticket sort, la vente est enregistrée **localement**.
5. Coupé : on remet le réseau → « ça s'est synchronisé tout seul ».
6. Punch : « Le marché, la foule, pas de réseau… Lissafi s'en fout. »

---

## Vidéo 5 — Ticket WhatsApp

**Données à utiliser :** la dernière vente du jour (la 32e du seed), ou une vente
faite en direct pendant le tournage.

**Déroulé caméra :**
1. Faire une vente (ex. Riz 10 500 F + Sucre 700 F + Huile 1 000 F = **12 200 F**).
2. Valider → **Ticket** affiché à l'écran (total, monnaie, articles).
3. Taper **Envoyer par WhatsApp** → le ticket part au client.
4. Punch : « Le client repart avec la preuve sur SON téléphone. »

---

## Vidéo 6 — Le prix (Gratuit puis Premium)

**Données à utiliser :**
- L'app est **déjà en Premium** (statut visible dans **Réglages** → « Premium actif,
  expire le … »).
- Comparaison visuelle : la boutique compte **23 produits** et **8 clients** —
  au-delà de la limite gratuite (10/10).

**Déroulé caméra :**
1. Réglages → montrer la ligne **Premium** active (boutique « Boutique Albaraka »,
   téléphone, expiration dans 1 an).
2. Montrer qu'on a **23 produits** → impossible en version gratuite (10 max).
3. Carte finale : « 0 F pour tester · 15 000 F/an · c'est 41 F par jour ».
4. Punch : « Toute une boutique dans ton téléphone, pour le prix d'un sachet
   d'eau par jour. »

---

## ✅ Checklist avant chaque tournage

- [ ] Relancer `scripts/seed-demo.sql` (réinitialise la démo à l'identique)
- [ ] Effacer les données de l'app (Android → Paramètres → Lissafi → Stockage → Effacer)
- [ ] Se connecter avec `demo@lissafi.app` / `demo123456`
- [ ] Laisser synchroniser 5-10 s (badge de synchro OK)
- [ ] Passer en mode avion pour la vidéo 4 seulement
- [ ] Vérifier le nom de la boutique (Réglages → « Boutique Albaraka »)

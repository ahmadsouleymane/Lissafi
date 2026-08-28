# PRD — Rétention & Monétisation Lissafi

> Chantier lancé le 2026-08-28 pour réduire la friction et fidéliser avant le démarchage terrain.
> Ce document sert à piloter les sessions de dev restantes. **Chaque bloc a un périmètre de fichiers
> disjoint** : deux sessions sur deux blocs différents ne créent pas de conflit git.

---

## 0. Décisions produit figées (à respecter dans tous les blocs)

**Modèle** : 100 % payant, plus de freemium. Essai **14 jours full-option** (compteur dès le 1er login),
puis **hard-paywall** bloquant. Conversion assistée (push J-3/J-1 + relance WhatsApp).

**3 offres** (déjà câblées dans l'app, cf. `PremiumManager`) :

| Offre | Code `Plan` | Mensuel | Trimestriel | Annuel |
|---|---|---|---|---|
| Petite boutique | `PLUS` | 3 000 | 7 500 | 24 000 |
| Commerce / Supermarché | `BUSINESS` | 6 000 | 15 000 | 50 000 |
| Pack Boutique (une fois) | — | **60 000** = imprimante 58 mm + 2 rouleaux + 1 an Petite boutique | | |

- Petite boutique : ~200 produits, ventes **illimitées**, 1 poste. Commerce : illimité, multi-postes (CA global), multi-users, export CSV, support prioritaire.
- **Ne jamais rate-limiter les ventes/jour sur un plan payant.**
- Paiement : **WhatsApp `+227 99 28 14 91`** (const `PremiumManager.WHATSAPP_NUMBER = "22799281491"`) **+** carte/Mobile Money (page `/payer`).
- Prix « early adopter » possible : 20 000 F/an pour les 10 premiers.

**Budget lancement 10 000 F** : terrain (transport + flyers QR + data), **pas de pub**. Domaine : sous-domaine Vercel gratuit tant qu'il n'y a pas de 1re vente.

---

## 1. Déjà livré (branche `feat/paywall-abonnements`)

> **État au 2026-08-29 :** Phases 1-2 + Blocs **B, C, D terminés** et build vert (app `assembleDebug`,
> back-office `next build`, landing `vite build`). **Seul le Bloc A (Google Sign-In) reste** — il attend
> les credentials Google Cloud + Supabase. Décision Bloc D : le paiement en ligne carte/MoMo est **annuel
> uniquement** (prix serveur `planAmount` = 24000/50000) ; mensuel/trimestriel/espèces passent par WhatsApp
> (note + bouton sur `/payer` quand `?period=monthly|quarterly`).

- **Phase 1 — Moteur premium & paywall** : `PremiumManager` (`TRIAL/PLUS/BUSINESS/LOCKED`),
  essai 14 j (`trial_start` en `app_settings`), `PaywallScreen.kt` (3 offres, sélecteur période,
  boutons Carte/MoMo + WhatsApp), hard-gate dans `LissafiNavHost` (`Routes.PAYWALL` + overlay si `isLocked`),
  bannière `SettingsScreen`.
- **Phase 2 — Collecte de données à l'inscription** : champs nom, boutique, **WhatsApp (requis)**, marché ;
  envoyés à Supabase Auth (`raw_user_meta_data` : clés `shop_name`, `owner_name`, `phone`, `market`) ;
  persistés en `app_settings` (`shop_name`, `owner_name`, `shop_phone`, `market`).

### Contexte technique réutilisable
- **Settings locaux** : `repository.setSetting(k,v)` / `getSetting(k)`. Clés posées UNIQUEMENT par le serveur : `is_premium`, `premium_expiry` (exclues de `pushSettings`).
- **Analytics** : `SupabaseApi.logEvent(eventType, level="info", message="", meta="{}")` → table `app_logs` (fire-and-forget). Déjà émis : `app_start`, `sync`, `sync_error`, `session_invalid`.
- **FCM** : `LissafiMessagingService.kt` existe, token enregistré via `api.upsertDeviceToken(fcmToken)` au login (`LissafiNavHost`). Permission POST_NOTIFICATIONS déjà demandée.
- **Lien paiement** : `PremiumManager.buildActivationPaymentLink(plan, period, email)` → `https://lissafi-one.vercel.app/payer?plan=&period=&email=`.
- **Compilation** : `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug`.

---

## 2. BLOC A — Google Sign-In  🔒 *bloqué par credentials*

**Objectif** : bouton « Continuer avec Google » à l'inscription/connexion pour supprimer la friction email+mot de passe.

**Périmètre fichiers** (⚠️ chevauche l'auth de la Phase 2 — **faire après merge** de `feat/paywall-abonnements`) :
`app/build.gradle.kts`, `data/auth/AuthManager.kt`, `ui/viewmodel/AuthViewModel.kt`, `ui/screen/AuthScreen.kt`,
`ui/navigation/LissafiNavHost.kt`, `app/google-services.json` (si absent), `AndroidManifest.xml`.

**Prérequis externes (à fournir par le porteur)** :
1. Google Cloud → créer un **OAuth 2.0 Client ID** type *Web* (pour Supabase) **et** un client *Android* (package `com.lissafi.app` + SHA-1 du keystore debug ET release).
2. Supabase → Authentication → Providers → **activer Google**, coller le Client ID/Secret Web.

**Tâches** :
- [ ] Ajouter dépendance **Credential Manager** (`androidx.credentials` + `googleid`).
- [ ] `AuthManager.signInWithGoogle(idToken)` → `POST /auth/v1/token?grant_type=id_token` (body `{provider:"google", id_token}`), réutiliser `SupabaseManager.saveSession`.
- [ ] Récupérer l'`id_token` via Credential Manager (`GetGoogleIdOption`) dans un helper appelé depuis `AuthScreen`.
- [ ] Bouton « Continuer avec Google » (logo + style outline) au-dessus des formulaires SIGN_IN/SIGN_UP + séparateur « ou ».
- [ ] Après succès Google : si nouveau compte, pré-remplir `owner_name`/`email` depuis le profil Google ; **rediriger vers un mini-écran de complétion** pour le **numéro WhatsApp** (toujours requis, cf. Phase 2) s'il manque.
- [ ] `startTrialIfNeeded` se déclenche déjà au 1er login → rien à faire côté essai.

**Critères d'acceptation** : un compte Google se crée/se connecte sans mot de passe ; le WhatsApp est bien collecté ; build vert.

---

## 3. BLOC B — Notifications de rétention  ✅ *aucun blocage (local d'abord)*

**Objectif** : donner une raison de rouvrir l'app chaque jour (le cœur du problème de rétention).

**Périmètre fichiers** (nouveaux fichiers surtout ; overlap mineur sur `LissafiApp.kt`) :
`service/notification/*` (nouveau), `LissafiApp.kt` (enregistrement des workers), `AndroidManifest.xml`,
éventuellement `data/remote/SupabaseApi.kt` (déjà `upsertDeviceToken`).

**Tâches — V1 locale (WorkManager + notifications locales, sans serveur)** :
- [ ] **Clôture quotidienne** : worker planifié ~20h → notif « Aujourd'hui : X ventes · Y FCFA · Z clients te doivent de l'argent ». Calcul via `repository.countSalesBetween` + total dettes du jour.
- [ ] **Rappels de dettes** : worker quotidien → si une dette a > N jours, notif « {client} te doit {montant} depuis {n} jours ».
- [ ] **Compte à rebours d'essai** : à J-3 et J-1 (`PremiumManager.trialDaysLeft()`), notif « Ton essai finit dans X jours — garde l'accès à tes données » deep-link vers `Routes.PAYWALL`.
- [ ] Canaux de notification (Android O+) + respect de l'opt-out.

**Tâches — V2 serveur (optionnel, plus tard)** :
- [ ] Edge Function Supabase (cron) qui envoie les push FCM côté serveur via les `device_tokens` (permet d'atteindre les inactifs qui n'ouvrent plus l'app). Nécessite la clé serveur FCM.

**Critères d'acceptation** : les 3 notifications se déclenchent (testables en forçant l'horaire) ; le tap ouvre le bon écran ; build vert.

---

## 4. BLOC C — Funnel analytics & back-office  ✅ *projet séparé (backoffice/)*

**Objectif** : voir où ça bloque : visiteurs landing → téléchargements → inscriptions → essais → ventes.

**Périmètre fichiers** :
- Back-office : `backoffice/src/*`, `supabase-admin.sql` (fonctions d'agrégation).
- App (petits ajouts d'events, overlap mineur) : `LissafiApp.kt`, `ui/viewmodel/AuthViewModel.kt`, `ui/screen/CaisseScreen.kt`, `ui/screen/PaywallScreen.kt`.

**Tâches — instrumentation app (`SupabaseApi.logEvent`)** :
- [ ] `signup` (après création de compte) — `AuthViewModel`.
- [ ] `trial_start` (dans `PremiumManager.startTrialIfNeeded`, une seule fois).
- [ ] `first_product` (au 1er produit ajouté) et `first_sale` (à la 1re vente) — flags en settings pour n'émettre qu'une fois.
- [ ] `paywall_view` (ouverture `PaywallScreen`) et `subscribe_click` (clic Carte/MoMo ou WhatsApp, avec `meta` = plan+période).

**Tâches — back-office (funnel)** :
- [ ] Fonction SQL `admin_funnel(from, to)` renvoyant les compteurs par étape (à partir de `app_logs` + `auth.users` + `sales` + `partner_installs`/beacons pour les visites/downloads).
- [ ] Page `/funnel` : entonnoir visuel (visiteurs → downloads → inscriptions → essais démarrés → 1re vente → abonnés) + taux de conversion entre étapes.
- [ ] Vue « activité utilisateur » : dernière ouverture, nb ventes, statut essai/abo, WhatsApp cliquable.

**Sources de données par étape** :
- Visiteurs landing → beacon `/api/visits` (portail) ou analytics landing (Bloc D).
- Téléchargements → compteur sur le lien APK (Bloc D) ou beacon d'install (`recordPartnerInstall` existe).
- Inscriptions → `auth.users` (+ `raw_user_meta_data` : phone/market/shop_name).
- Essais → events `trial_start`. Ventes → table `sales`. Abonnés → `is_premium=true`.

**Critères d'acceptation** : la page funnel affiche des chiffres réels ; chaque event s'émet une seule fois ; back-office build (`npm run build` dans `backoffice/`).

---

## 5. BLOC D — Landing + page de paiement  ✅ *projet séparé (landing/)*

**Objectif** : landing crédible (sous-domaine Vercel) qui vend le nouveau modèle + page `/payer` fonctionnelle.

**Périmètre fichiers** : `landing/src/*`, `landing/src/config.js`, déploiement Vercel.

**Tâches** :
- [ ] Déployer sur un **sous-domaine Vercel** (`lissafi.vercel.app` ou similaire) — pas de TLD gratuit type `.tk`.
- [ ] Section **prix** reflétant les 3 offres et la grille ci-dessus (annuel mis en avant, −33 %). Mettre en avant l'**essai 14 jours**.
- [ ] **QR code** de téléchargement de l'APK + bouton WhatsApp (`+227 99 28 14 91`).
- [ ] Page **`/payer`** (déjà référencée par l'app : `?plan=&period=&email=`) : récapitulatif de la formule choisie + moyens de paiement carte/Mobile Money **selon disponibilités**, avec repli « payer via WhatsApp ». Confirmer le prestataire dispo (iPayMoney/MoMo) avant d'intégrer ; sinon, page = récap + bouton WhatsApp.
- [ ] Beacon de visite (pour le funnel du Bloc C).

**Critères d'acceptation** : landing en ligne sur le sous-domaine ; `/payer?plan=plus&period=yearly` affiche le bon montant ; build (`npm run build` dans `landing/`).

---

## 6. Ordre conseillé & parallélisation

- **En parallèle tout de suite** (périmètres disjoints) : **Bloc B** (notifications), **Bloc C** (funnel/back-office), **Bloc D** (landing).
- **Bloc A** (Google) : **après** merge de `feat/paywall-abonnements` (chevauche les fichiers auth). Prévoir les credentials Google/Supabase d'abord.
- Merger `feat/paywall-abonnements` dans `main` avant de démultiplier les sessions, pour que tous les blocs partent de la même base.

# Lancement — Activer l'inscription dans Supabase Auth

Le constat : l'app renvoie « Erreur d'inscription. Vérifie tes informations. »
quand un nouveau compte est créé. C'est la configuration Auth de Supabase qui
bloque — rien à changer dans le code Android.

## Étapes (dashboard Supabase)

1. Connecte-toi sur https://supabase.com/dashboard et ouvre le projet
   `fnyuhpfzkvunscuylvqv` (Lissafi).
2. Menu de gauche → **Authentication** → **Providers**.
3. Dans la ligne **Email**, clique sur le crayon (éditer).
4. Coche **« Enable Signups »** (autoriser les inscriptions).
5. Décoche **« Confirm email »** si tu veux que le compte soit actif
   immédiatement (sans lien de confirmation dans la boîte mail). Sinon, laisse
   coché et l'utilisateur devra cliquer le lien reçu par email.
6. **Save**.

## Tester

1. Installe l'APK sur un téléphone.
2. Ouvre l'app → crée un compte (email + mot de passe 6+ caractères).
3. Normal : l'app passe à la caisse directement.

## Rappels sécurité (déjà en place, rien à faire)

- RLS active sur toutes les tables (`supabase-schema.sql`) : chaque utilisateur
  ne voit que ses données (`auth.uid() = user_id`).
- La clé `service_role` n'est **jamais** dans l'app Android (seulement dans le
  back-office, côté serveur).
- Le back-office vérifie l'admin via `getAdminSession()`.

## Optionnel : compte démo (vidéos TikTok)

Le bouton « Tester avec la démo » a été retiré de l'app. Si tu veux un compte
pré-rempli pour les vidéos, exécute `scripts/seed-demo.sql` dans le SQL Editor
(compte `demo@lissafi.app` / `demo123456`).

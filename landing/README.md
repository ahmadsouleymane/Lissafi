# Landing Lissafi

Page de vente statique (Vite + React + motion). Déployée sur Vercel.

## Avant de mettre en ligne

1. **Numéro WhatsApp** — dans `src/config.js`, remplace `227XXXXXXXX` par ton
   numéro réel (format international, sans espaces ni +).
2. **APK** — compile l'app Android puis copie l'APK dans `public/` :
   `npm run update-apk`
   (par défaut : `app-debug.apk`, installable. Pour la production, génère un
   APK **release signé** et passe son chemin :
   `npm run update-apk ../app/build/outputs/apk/release/app-release-signed.apk`).
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

# Landing Lissafi

Page de vente statique (Vite + React + motion). Déployée sur Vercel.

## Avant de mettre en ligne

1. **Numéro WhatsApp** — le lien WhatsApp du footer pointe vers
   `src/components/Footer.jsx` (`WHATSAPP_URL = https://wa.me/2250160726314`).
   C'est le même numéro d'activation que dans l'app. Mets-le à jour là s'il
   change.
2. **APK** — compile l'app Android puis copie l'APK dans `public/` :
   `npm run update-apk`
   (par défaut : `app-debug.apk`, installable. Pour la production, génère un
   APK **release signé** et passe son chemin :
   `npm run update-apk ../app/build/outputs/apk/release/app-release-signed.apk`).
3. **Domaine (OBLIGATOIRE avant partage)** — définis `VITE_SITE_URL`
   (ex `https://lissafi.app`) dans `landing/.env` en local ET dans les réglages
   du projet Vercel (Settings → Environment Variables). Sans lui, `og:image`,
   `og:url` et le `canonical` restent relatifs et l'aperçu de partage
   WhatsApp/Facebook est cassé.

## Déploiement

- Vercel : importer le dossier `landing` (ou la racine avec le bon root),
  framework détecté `Vite`, output `dist`. Déploiement auto à chaque push.
- Local : `npm run dev` (développement), `npm run build` (build de prod),
  `npm run preview` (aperçu du build).

## Vérification après déploiement

- Ouvre la landing → clique « Télécharger l'APK » : le fichier se télécharge.
- Clique le lien WhatsApp du footer : ouvre un chat vers ton numéro.
- Partage le lien sur WhatsApp/Facebook : l'aperçu affiche l'image et la
  description correctes.

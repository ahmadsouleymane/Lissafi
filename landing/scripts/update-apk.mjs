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

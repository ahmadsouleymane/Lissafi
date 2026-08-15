#!/usr/bin/env bash
# ============================================================
# Release en 1 commande : build Android + copie de l'APK dans la
# landing + commit + push (Vercel déploie automatiquement).
#
# Usage : npm run release-apk [debug]
#   - sans argument : build APK release signé. Échoue si le keystore
#     de signature est absent — JAMAIS de repli silencieux, car publier
#     un APK signé différemment casse l'installation pour TOUTE la base
#     déjà installée (conflit de signature Android).
#   - "debug"       : force explicitement un APK debug (installable,
#     plus lourd, signature debug — à ne publier que sciemment).
# ============================================================
set -euo pipefail

# Racine du repo (le script vit dans landing/scripts/)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# JDK : JBR d'Android Studio (aucun JDK système installé).
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

# Choix du type de build
BUILD_TYPE="${1:-}"
if [ -z "$BUILD_TYPE" ]; then
  if [ -f keystore.properties ]; then
    BUILD_TYPE="release"
  else
    echo "❌ keystore.properties introuvable — impossible de builder en release." >&2
    echo "   Publier un APK non signé avec la clé de release casserait l'installation" >&2
    echo "   pour tous les utilisateurs déjà installés (conflit de signature)." >&2
    echo "   Lance 'npm run release-apk debug' si tu veux explicitement un build debug." >&2
    exit 1
  fi
fi

case "$BUILD_TYPE" in
  release) TASK="assembleRelease"; APK_SUBDIR="release" ;;
  debug)   TASK="assembleDebug";   APK_SUBDIR="debug" ;;
  *) echo "❌ Type de build inconnu : $BUILD_TYPE (attendu : release | debug)" >&2; exit 2 ;;
esac

# ---- 1. Bump version ---------------------------------------------------
echo "==> [1/5] Bump versionCode…"
VC_FILE="$ROOT/app/build.gradle.kts"
OLD_VC="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\).*/\1/p' "$VC_FILE" | head -1)"
if [ -z "$OLD_VC" ]; then
  echo "❌ versionCode introuvable dans $VC_FILE" >&2
  exit 1
fi
NEW_VC=$((OLD_VC + 1))
VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$VC_FILE" | head -1)"
sed -i.bak "s/versionCode = $OLD_VC/versionCode = $NEW_VC/" "$VC_FILE"
rm -f "$VC_FILE.bak"
echo "   versionCode $OLD_VC → $NEW_VC (versionName ${VERSION_NAME:-1.0})"

# ---- 2. Build Android -------------------------------------------------
echo "==> [2/5] Build Android ($BUILD_TYPE)…"
./gradlew "$TASK"
APK_SRC="$ROOT/app/build/outputs/apk/$APK_SUBDIR/app-${APK_SUBDIR}.apk"
if [ ! -f "$APK_SRC" ]; then
  echo "❌ APK introuvable après build : $APK_SRC" >&2
  exit 1
fi

# ---- 3. Copie dans la landing + manifest de version --------------------
DEST="$ROOT/landing/public/lissafi.apk"
echo "==> [3/5] Copie de l'APK vers la landing…"
cp "$APK_SRC" "$DEST"
MB=$(du -h "$DEST" | cut -f1)
# Empreinte SHA-256 de l'APK publié : vérifiée par UpdateManager avant
# d'installer, pour détecter un fichier corrompu ou altéré côté serveur.
SHA256="$(shasum -a 256 "$DEST" | cut -d' ' -f1)"
cat > "$ROOT/landing/public/latest.json" <<EOF
{
  "versionCode": $NEW_VC,
  "versionName": "${VERSION_NAME:-1.0}",
  "apkUrl": "/lissafi.apk",
  "sha256": "$SHA256"
}
EOF
echo "   APK prêt : $DEST ($MB, sha256 $SHA256)"

# ---- 4. Commit ---------------------------------------------------------
echo "==> [4/5] Commit…"
if git diff --quiet -- landing/public/lissafi.apk app/build.gradle.kts landing/public/latest.json; then
  echo "   Aucun changement (déjà à jour) — commit ignoré."
else
  git add landing/public/lissafi.apk app/build.gradle.kts landing/public/latest.json
  git commit -m "feat(landing): mets a jour l'APK telechargeable (auto-update)"
  echo "   Commit créé."
fi

# ---- 5. Push (déploie Vercel) -----------------------------------------
BRANCH="$(git branch --show-current)"
echo "==> [5/5] Push vers origin/$BRANCH (déploie la landing)…"
git push origin "$BRANCH"

echo
echo "✅ APK publié — la landing sert maintenant la dernière version."

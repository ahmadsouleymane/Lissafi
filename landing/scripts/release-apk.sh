#!/usr/bin/env bash
# ============================================================
# Release en 1 commande : build Android + copie de l'APK dans la
# landing + commit + push (Vercel déploie automatiquement).
#
# Usage : npm run release-apk [debug]
#   - sans argument : build APK release signé (repli sur debug si
#     le keystore de signature est absent).
#   - "debug"       : force un APK debug (installable, plus lourd).
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
    echo "ℹ️  Pas de keystore de signature → build debug (repli)."
    BUILD_TYPE="debug"
  fi
fi

case "$BUILD_TYPE" in
  release) TASK="assembleRelease"; APK_SUBDIR="release" ;;
  debug)   TASK="assembleDebug";   APK_SUBDIR="debug" ;;
  *) echo "❌ Type de build inconnu : $BUILD_TYPE (attendu : release | debug)" >&2; exit 2 ;;
esac

# ---- 1. Build Android -------------------------------------------------
echo "==> [1/4] Build Android ($BUILD_TYPE)…"
./gradlew "$TASK"
APK_SRC="$ROOT/app/build/outputs/apk/$APK_SUBDIR/app-${APK_SUBDIR}.apk"
if [ ! -f "$APK_SRC" ]; then
  echo "❌ APK introuvable après build : $APK_SRC" >&2
  exit 1
fi

# ---- 2. Copie dans la landing ----------------------------------------
DEST="$ROOT/landing/public/lissafi.apk"
echo "==> [2/4] Copie de l'APK vers la landing…"
cp "$APK_SRC" "$DEST"
MB=$(du -h "$DEST" | cut -f1)
echo "   APK prêt : $DEST ($MB)"

# ---- 3. Commit ---------------------------------------------------------
echo "==> [3/4] Commit…"
if git diff --quiet -- landing/public/lissafi.apk; then
  echo "   Aucun changement d'APK (déjà à jour) — commit ignoré."
else
  git add landing/public/lissafi.apk
  git commit -m "feat(landing): mets a jour l'APK telechargeable"
  echo "   Commit créé."
fi

# ---- 4. Push (déploie Vercel) -----------------------------------------
BRANCH="$(git branch --show-current)"
echo "==> [4/4] Push vers origin/$BRANCH (déploie la landing)…"
git push origin "$BRANCH"

echo
echo "✅ APK publié — la landing sert maintenant la dernière version."

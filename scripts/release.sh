#!/usr/bin/env bash
# Compila el APK firmado y prepara los archivos para una release de GitHub.
# NO publica nada: imprime el comando `gh release create` para que lo ejecutes tú.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
VERSION=$(grep -E 'versionName' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
./gradlew :app:assembleRelease -q
mkdir -p dist
APK="dist/equalizer-${VERSION}.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"
shasum -a 256 "$APK" | awk '{print $1}' > "$APK.sha256"
echo "APK: $APK"
echo "sha256: $(cat "$APK.sha256")"
echo
echo "Para publicar (cuando estés listo):"
echo "  gh release create v${VERSION} \"$APK\" \"$APK.sha256\" --title \"v${VERSION}\" --notes \"Equalizer ${VERSION}\""

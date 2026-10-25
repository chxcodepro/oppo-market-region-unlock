#!/usr/bin/env bash
# Build the LSPosed module APK without Gradle.
# Requirements: JDK 17+, Android SDK (build-tools + platforms/android-3x), Python 3, curl.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

PYTHON=""
for cand in python python3; do
    if command -v "$cand" >/dev/null 2>&1 && printf 'print(1)\n' | "$cand" -I - >/dev/null 2>&1; then
        PYTHON="$cand"
        break
    fi
done
[ -n "$PYTHON" ] || { echo "python (3.x) not found" >&2; exit 1; }

# ---------------------------------------------------------------- Android SDK
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
    for d in "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
        if [ -n "$d" ] && [ -d "$d" ]; then SDK="$d"; break; fi
    done
fi
[ -n "$SDK" ] && [ -d "$SDK" ] || { echo "Android SDK not found; set ANDROID_HOME" >&2; exit 1; }

BT_NAME="$(ls "$SDK/build-tools" 2>/dev/null | grep -vE '^android-' | sort -t. -k1,1n -k2,2n -k3,3n | tail -1)"
[ -n "$BT_NAME" ] || BT_NAME="$(ls "$SDK/build-tools" 2>/dev/null | sort | tail -1)"
[ -n "$BT_NAME" ] || { echo "Android build-tools not found" >&2; exit 1; }
BT="$SDK/build-tools/$BT_NAME"

AJ=""
for api in android-36 android-35 android-34 android-33; do
    if [ -f "$SDK/platforms/$api/android.jar" ]; then
        AJ="$SDK/platforms/$api/android.jar"
        break
    fi
done
[ -n "$AJ" ] || { echo "android.jar not found" >&2; exit 1; }
TARGET_SDK="$(basename "$(dirname "$AJ")" | sed 's/^android-//')"

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        D8="$BT/d8.bat"
        AAPT2="$BT/aapt2.exe"
        ZIPALIGN="$BT/zipalign.exe"
        APKSIGNER="$BT/apksigner.bat"
        ;;
    *)
        D8="$BT/d8"
        AAPT2="$BT/aapt2"
        ZIPALIGN="$BT/zipalign"
        APKSIGNER="$BT/apksigner"
        ;;
esac

for c in java javac jar keytool; do
    command -v "$c" >/dev/null 2>&1 || { echo "$c not found" >&2; exit 1; }
done
for f in "$D8" "$AAPT2" "$ZIPALIGN" "$APKSIGNER"; do
    [ -f "$f" ] || { echo "missing build tool: $f" >&2; exit 1; }
done

# ---------------------------------------------------------------- Xposed API
API_JAR="${XPOSED_API_JAR:-$ROOT/libs/api-82.jar}"
if [ ! -f "$API_JAR" ]; then
    mkdir -p "$ROOT/libs"
    command -v curl >/dev/null 2>&1 || { echo "curl not found (needed to download Xposed API)" >&2; exit 1; }
    echo "[*] downloading Xposed API 82"
    curl -fsSL -o "$API_JAR" \
        "https://maven.aliyun.com/repository/jcenter/de/robv/android/xposed/api/82/api-82.jar"
fi

# ---------------------------------------------------------------- signing
KEYSTORE="${KEYSTORE:-$ROOT/build/regionunlock.jks}"
KEY_ALIAS="${KEY_ALIAS:-regionunlock}"
STORE_PASS="${KEYSTORE_PASS:-regionunlock123}"
KEY_PASS="${KEY_PASS:-$STORE_PASS}"

rm -rf "$ROOT/build"
mkdir -p "$ROOT/build/classes" "$ROOT/build/dex" "$ROOT/dist"

echo "[*] SDK        : $SDK"
echo "[*] build-tools: $BT"
echo "[*] android.jar: $AJ (target $TARGET_SDK)"

SOURCES=()
while IFS= read -r f; do
    SOURCES+=("$f")
done < <(find "$ROOT/module/src" -name '*.java' | sort)
[ "${#SOURCES[@]}" -gt 0 ] || { echo "no Java sources found" >&2; exit 1; }

echo "[*] javac"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
    -bootclasspath "$AJ" -cp "$API_JAR" \
    -d "$ROOT/build/classes" "${SOURCES[@]}"

echo "[*] jar + d8"
jar cf "$ROOT/build/classes.jar" -C "$ROOT/build/classes" .
"$D8" --release --min-api 22 --lib "$AJ" --lib "$API_JAR" \
    --output "$ROOT/build/dex" "$ROOT/build/classes.jar"

echo "[*] aapt2 link"
"$AAPT2" link -o "$ROOT/build/base.apk" -I "$AJ" \
    --manifest "$ROOT/module/AndroidManifest.xml" \
    --min-sdk-version 22 --target-sdk-version "$TARGET_SDK"

echo "[*] package"
"$PYTHON" -I - . <<'PY'
import os
import sys
import zipfile

root = sys.argv[1]
src = zipfile.ZipFile(os.path.join(root, "build/base.apk"), "r")
out = zipfile.ZipFile(os.path.join(root, "build/unsigned.apk"), "w", zipfile.ZIP_DEFLATED)
for info in src.infolist():
    data = src.read(info.filename)
    if info.filename == "resources.arsc":
        out.writestr(zipfile.ZipInfo(info.filename), data, compress_type=zipfile.ZIP_STORED)
    else:
        out.writestr(info.filename, data)
out.write(os.path.join(root, "build/dex/classes.dex"), "classes.dex", zipfile.ZIP_DEFLATED)
out.write(os.path.join(root, "module/assets/xposed_init"), "assets/xposed_init", zipfile.ZIP_DEFLATED)
out.close()
src.close()
PY

echo "[*] zipalign"
"$ZIPALIGN" -f 4 "$ROOT/build/unsigned.apk" "$ROOT/build/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    echo "[*] generating keystore: $KEYSTORE"
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 -storetype JKS \
        -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
        -dname "CN=RegionUnlock, OU=Dev, O=RegionUnlock, L=NA, ST=NA, C=CN"
fi

echo "[*] signing"
"$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
    --ks-pass "pass:$STORE_PASS" --key-pass "pass:$KEY_PASS" \
    --min-sdk-version 22 \
    --out "$ROOT/dist/OppoMarketRegionUnlock-1.0.apk" \
    "$ROOT/build/aligned.apk"

echo "[+] $ROOT/dist/OppoMarketRegionUnlock-1.0.apk"
ls -lh "$ROOT/dist/OppoMarketRegionUnlock-1.0.apk"

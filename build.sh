#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${SOL_ANDROID_SDK:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}}"
: "${SDK:?Set SOL_ANDROID_SDK or ANDROID_SDK_ROOT to your Android SDK}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-18/android.jar"
for item in "$BT/aapt" "$BT/d8" "$BT/zipalign" "$BT/apksigner" "$JAR"; do
 test -f "$item" || { echo "Missing Android SDK component: $item" >&2; exit 1; }
done
./tools/build-native.sh
python3 tools/fetch-dependencies.py
KEY_DIR="${SOL_SIGNING_DIR:-${XDG_CONFIG_HOME:-$HOME/.config}/sol-node-android}"
mkdir -p build/gen/one/system42/solnode build/classes build/dex artifacts "$KEY_DIR"
chmod 700 "$KEY_DIR"
python3 - <<'PY'
import json,os
from pathlib import Path
from urllib.parse import urlsplit
base=os.environ.get('SOL_API_BASE','https://sol.system42.one').rstrip('/')
if base:
 p=urlsplit(base)
 if p.scheme!='https' or not p.hostname or p.username or p.password or p.query or p.fragment or p.path:
  raise SystemExit('SOL_API_BASE must be an HTTPS origin without credentials, path, query or fragment')
Path('build/gen/one/system42/solnode/EndpointConfig.java').write_text('package one.system42.solnode; public final class EndpointConfig { public static final String BASE = '+json.dumps(base)+'; }\n')
PY
if [ ! -f "$KEY_DIR/development.jks" ]; then
 keytool -genkeypair -keystore "$KEY_DIR/development.jks" -storepass android -keypass android -alias solnode -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=SOL Node Development' >/dev/null 2>&1
 chmod 600 "$KEY_DIR/development.jks"
fi
"$BT/aapt" package -f -M AndroidManifest.xml --auto-add-overlay -S res -S vendor/base/res -S vendor/basement/res -J build/gen --extra-packages com.google.android.gms.base:com.google.android.gms -A assets -I "$JAR" -F build/resources.apk
find src build/gen -name '*.java' -print > build/sources.txt
javac -source 7 -target 7 -Xlint:-options -bootclasspath "$JAR" -classpath "vendor/*" -d build/classes @build/sources.txt
jar cf build/classes.jar -C build/classes .
"$BT/d8" --min-api 18 --lib "$JAR" --output build/dex build/classes.jar vendor/*.jar
cp build/resources.apk build/unsigned.apk
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$KEY_DIR/development.jks" --ks-key-alias solnode --ks-pass pass:android --key-pass pass:android --min-sdk-version 18 --v1-signing-enabled true --out artifacts/sol-node.apk build/aligned.apk
"$BT/apksigner" verify --verbose --min-sdk-version 18 artifacts/sol-node.apk
sha256sum artifacts/sol-node.apk > artifacts/sol-node.apk.sha256

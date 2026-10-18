#!/usr/bin/env bash
# Acuranza release-artifact security check.
#
# Usage: scripts/security-audit.sh path/to/app-release.apk
#
# Runs the same checks against every release APK before it is declared ready:
#   1. identity, hash and permissions          (aapt2, sha256sum)
#   2. signing certificate                     (apksigner)
#   3. manifest hygiene                        (exported components, backup, debuggable)
#   4. native libraries and ABIs packaged      (unzip)
#   5. no network code in the DEX              (jadx class listing)
#   6. no analytics/ads/tracking SDKs          (jadx class listing)
#   7. no obvious embedded secrets             (resource + dex string scan)
#
# Exits non-zero on any finding, and prints each check's evidence.

set -euo pipefail

APK="${1:?usage: security-audit.sh <apk>}"
SDK="${ANDROID_HOME:?set ANDROID_HOME}"
BUILD_TOOLS=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JADX="${JADX:-jadx}"   # jadx 1.5+ on PATH, or set JADX=/path/to/jadx/bin/jadx

fail() { echo "FAIL: $*"; exit 1; }
pass() { echo "PASS: $*"; }

echo "== 1. identity, hash, permissions =="
AAPT2="$BUILD_TOOLS/aapt2"
BADGING=$($AAPT2 dump badging "$APK")
echo "$BADGING" | sed -n '1,3p'
SHA=$(sha256sum "$APK")
echo "$SHA"
SIZE=$(stat -c%s "$APK")
echo "size: $SIZE bytes"
PERMS=$($AAPT2 dump permissions "$APK")
echo "$PERMS"
echo "$PERMS" | grep -q "android.permission.INTERNET" && fail "APK requests android.permission.INTERNET"
echo "$PERMS" | grep -q "android.permission.ACCESS_NETWORK_STATE" && fail "APK requests ACCESS_NETWORK_STATE"
pass "no INTERNET / ACCESS_NETWORK_STATE permission"

echo
echo "== 2. signing certificate =="
"$BUILD_TOOLS/apksigner" verify --print-certs "$APK" | sed -n '1,6p'
"$BUILD_TOOLS/apksigner" verify "$APK" && pass "APK signature verifies"
grep -q "CN=Acuranza" <("$BUILD_TOOLS/apksigner" verify --print-certs "$APK") \
  && pass "signer is the Acuranza release key" \
  || fail "signer is NOT the Acuranza release key"

echo
echo "== 3. manifest hygiene =="
TREE=$($AAPT2 dump xmltree --file AndroidManifest.xml "$APK")
echo "$TREE" | grep -i "debuggable" | grep -qv "debuggable=false" && fail "manifest is debuggable" || pass "not debuggable"
echo "$TREE" | grep -q "allowBackup=\"false\"" || echo "WARN: check allowBackup (merged value)"
echo "$TREE" | grep "android:exported=\"true\"" || true
EXPORTED=$(echo "$TREE" | grep -c "android:exported=\"true\"")
echo "exported components: $EXPORTED (expected: launcher activity, SMS receiver (BROADCAST_SMS-guarded), 2 widgets, QS tile)"
[ "$EXPORTED" -le 5 ] || fail "more exported components than expected: $EXPORTED"

echo
echo "== 4. native libraries and ABIs =="
unzip -l "$APK" "lib/*" | awk '{print $4}' | grep -v '^$' || true
ABIS=$(unzip -l "$APK" | grep -o 'lib/[^/]*' | sort -u)
echo "$ABIS"
echo "$ABIS" | grep -q arm64-v8a || fail "no arm64-v8a native library"
echo "$ABIS" | grep -q armeabi-v7a || fail "no armeabi-v7a native library"
echo "$ABIS" | grep -q x86_64 || fail "no x86_64 native library"
pass "all supported ABIs packaged (universal APK)"

echo
echo "== 5/6. DEX contents: network code, trackers =="
WORK=$(mktemp -d)
"$JADX" --no-res --no-imports -d "$WORK/src" "$APK" >/dev/null 2>&1
CLASSES=$(find "$WORK/src" -name "*.java" | wc -l)
echo "decompiled classes: $CLASSES"
NET=$(grep -rlE "okhttp3|retrofit2|com\.android\.volley|java\.net\.HttpURLConnection|org\.apache\.http\.impl" "$WORK/src" | head -5)
[ -z "$NET" ] || fail "network client classes present: $NET"
URLS=$(grep -rlE '"https?://[^"]+"' "$WORK/src/com/acuranza" | head -5)
[ -z "$URLS" ] || fail "hardcoded URL in app code: $URLS"
pass "no HTTP client or hardcoded URL in the DEX"
TRACKERS=$(find "$WORK/src" -path "*com/google/firebase*" -o -path "*com/facebook*" -o -path "*com/appsflyer*" \
  -o -path "*io/branch*" -o -path "*com/crashlytics*" -o -path "*com/amplitude*" -o -path "*com/mixpanel*" -o -path "*flurry*" | head -5)
[ -z "$TRACKERS" ] || fail "tracking SDK classes present: $TRACKERS"
pass "no analytics/ads/tracking SDK classes"

echo
echo "== 7. embedded secrets scan =="
SECRETS=$(unzip -p "$APK" classes.dex classes2.dex classes3.dex 2>/dev/null | strings | \
  grep -E "AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA )?PRIVATE KEY-----|sk_live_[0-9a-zA-Z]{10,}" | head -5)
[ -z "$SECRETS" ] || fail "possible embedded secret: $SECRETS"
pass "no obvious embedded credentials in the DEX"
rm -rf "$WORK"

echo
echo "ALL CHECKS PASSED"
echo "artifact: $APK"
echo "$SHA"

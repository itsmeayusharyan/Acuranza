#!/usr/bin/env bash
# Acuranza release pipeline — the one command that ships a release.
#
#   scripts/release.sh
#
# Runs the whole gate in order and stops at the first failure:
#   1. unit tests                          (./gradlew :app:testDebugUnitTest)
#   2. signed release build                (:app:assembleRelease with the configured keystore)
#   3. security audit of the signed APK    (scripts/security-audit.sh — 11 checks, non-zero on findings)
#   4. artifact copy + hash                (/data/workspace/Acuranza-<version>.apk)
#   5. GitHub sync                         (scripts/release-sync.sh: commit, rebase, push, log)
#
# Nothing syncs unless the tests and the audit passed, and the sync itself never force-pushes.
# A release that fails anywhere reports and leaves the remote untouched.

set -euo pipefail

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

export ANDROID_HOME="${ANDROID_HOME:-/data/workspace/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JADX="${JADX:-/data/workspace/jadx/bin/jadx}"

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts)"
[ -n "$VERSION" ] && [ -n "$VERSION_CODE" ] || { echo "RELEASE-FAIL: cannot read version"; exit 1; }
echo "== Release v$VERSION (code $VERSION_CODE) =="

echo "== 1/5 unit tests =="
./gradlew --no-daemon :app:testDebugUnitTest --console=plain

echo "== 2/5 signed release build =="
./gradlew --no-daemon :app:assembleRelease --console=plain
APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "RELEASE-FAIL: no APK at $APK"; exit 1; }

echo "== 3/5 security audit of the signed APK =="
AUDIT_LOG="/data/workspace/release-audit-v$VERSION.log"
bash scripts/security-audit.sh "$APK" > "$AUDIT_LOG" 2>&1 || { tail -5 "$AUDIT_LOG"; echo "RELEASE-FAIL: audit findings — see $AUDIT_LOG"; exit 1; }
grep -q "ALL CHECKS PASSED" "$AUDIT_LOG" || { echo "RELEASE-FAIL: audit incomplete — see $AUDIT_LOG"; exit 1; }

echo "== 4/5 artifact =="
ARTIFACT="/data/workspace/Acuranza-$VERSION.apk"
cp "$APK" "$ARTIFACT"
SHA="$(sha256sum "$ARTIFACT" | awk '{print $1}')"
echo "artifact=$ARTIFACT sha256=$SHA"

echo "== 5/5 GitHub sync =="
bash scripts/release-sync.sh "$ARTIFACT"

echo "RELEASE-OK: v$VERSION (code $VERSION_CODE) tested, signed, audited ($SHA) and synced."

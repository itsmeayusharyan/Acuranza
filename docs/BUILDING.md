# Building and releasing Acuranza

Acuranza builds with the standard Android toolchain: JDK 17+ (21 tested), the Android SDK with
`platforms;android-35` and `build-tools;35.0.0`, and the Gradle wrapper (Gradle 8.9). Point the SDK
out either way Gradle expects:

```bash
echo "sdk.dir=/opt/android-sdk" > local.properties   # or export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
```

## Debug build

```bash
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Release build and signing

The signing configuration reads `keystore/acuranza-release.jks` and two Gradle properties,
`ACURANZA_STORE_PASSWORD` and `ACURANZA_KEY_PASSWORD`, which live in `~/.gradle/gradle.properties`
on the build machine — never in the repository.

One-time keystore setup (keep the file and the passwords safe; a lost key means a new app identity):

```bash
keytool -genkeypair -v \
  -keystore keystore/acuranza-release.jks -alias acuranza \
  -keyalg RSA -keysize 4096 -validity 10000
# then add to ~/.gradle/gradle.properties:
#   ACURANZA_STORE_PASSWORD=...
#   ACURANZA_KEY_PASSWORD=...
```

Build, verify and hash:

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk

$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app-release.apk
sha256sum app-release.apk
```

The release APK is minified (R8) and resource-shrunk. A debug build never signs with the release
key, so a debuggable APK can never install over the real app.

## Universal APK

Acuranza ships a single universal APK for all supported ABIs. The dependency set is pure JVM/Kotlin
(SQLCipher's native library is packaged for every architecture it ships: arm64-v8a, armeabi-v7a and
x86_64), so no `abiFilters` or splits are configured — nothing is excluded. Minimum SDK 26
(Android 8.0), target/compile SDK 35.

## Release checklist

Run against the signed artifact, not the source tree:

1. **Permissions** — `aapt2 dump permissions` shows no INTERNET and no other network permission.
2. **Manifest** — no exported component beyond the launcher activity, the SMS receiver (guarded by
   `BROADCAST_SMS`) and the widget/tile components; `debuggable` is false; `allowBackup` is false.
3. **Signing** — `apksigner verify` names the Acuranza release certificate.
4. **Network code** — the APK contains no HTTP client classes (no OkHttp, no `java.net.URL`
   usage in app code; see the audit report for the class-level scan).
5. **Trackers** — no analytics/ads SDK classes in the DEX.
6. **Data** — no secrets, keystore files or passwords in the APK.
7. **Hash** — record the SHA-256 of the exact artifact alongside the release notes.

CI (`.github/workflows/ci.yml`) runs the unit tests and the INTERNET-permission check on every
push; `release.yml` builds and signs on a tag and runs the same checks before publishing.

# Security policy

## Reporting a vulnerability

Please **don't open a public issue** for security problems. Email **jha.8@alumni.iitj.ac.in** with:

- what you found and how to reproduce it,
- the Acuranza version and your Android version.

You'll get a reply within a few days. Once a fix is released, you'll be credited if you wish.

## Threat model (summary)

Acuranza holds sensitive financial data, entered by the user and imported from bank-statement CSVs. It is designed so that
this data never leaves the phone.

**Protections**

- **No servers or accounts:** Acuranza has no backend, no analytics and no third-party SDKs.
- **Network:** none. The APK requests no network permission, so no code path can reach the
  internet; this is verified against every release artifact.
- **Encryption at rest:** the database is encrypted with SQLCipher (AES-256). Its key is random, generated on
  the device and stored wrapped by a non-exportable AES-GCM key in the Android Keystore.
- **Backups:** user-initiated exports are encrypted with AES-256-GCM and a PBKDF2-SHA256 key (600,000
  iterations) from a password of at least 8 characters. Android cloud backup and device transfer are disabled
  for the database and settings.
- **On the device:** optional biometric/device-credential app lock that re-locks after 60 seconds in the
  background, screenshot and recents-preview blocking, privacy mode, and lock-screen notifications that hide
  amounts and merchants.
- **Messages:** SMS reading exists only behind the opt-in SMS transaction tracking feature
  (Settings → Financial tools). It runs no scan before explicit consent AND the runtime READ_SMS
  permission are both granted. Scans read bank/payment messages, keep only the extracted
  transaction fields (amount, date, merchant, account, reference) plus a dedupe fingerprint and
  the sender id — the message body itself is never written to storage — and never send anything
  anywhere (the APK holds no network permission). Consent can be withdrawn and all imported
  records deleted from the same screen; automatic scanning can be switched off without losing
  imported data.

**Out of scope**

- A device that is already compromised (rooted with malware, or unlocked and in an attacker's hands while
  Acuranza is unlocked).
- Loss of the Android Keystore key (e.g. a factory reset): the encrypted database then can't be recovered.
  Keep an encrypted backup.

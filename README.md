<h1 align="center">Acuranza</h1>
<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-2563EB" alt="GPL-3.0"></a>
  <img src="https://img.shields.io/badge/price-free-111111" alt="Free">
  <img src="https://img.shields.io/badge/Android-8.0%2B-111111" alt="Android 8.0+">
</p>

**Free and open source.** No ads, no account, no servers, no analytics — and no INTERNET permission at
all, so Android itself guarantees that your money data never leaves the phone.

Acuranza tracks income, expenses, credit cards, bills, budgets and goals. You add transactions by
hand, import bank-statement CSVs, or let Acuranza read the bank messages you share with it — and
every insight is computed on the phone.

## What it does

- **Bank-message tracking**: share a bank SMS from your Messages app (or paste it) and Acuranza
  extracts amount, date, merchant and account into a transaction you confirm. The **Full build**
  adds a historical inbox scan (with the SMS permission) and an opt-in notification listener, with
  de-duplication across every source. See *Privacy and security* for the trade-offs.
- **Bank statement import (CSV)**: statements from net banking (HDFC, ICICI, SBI, Axis, Kotak and
  others), de-duplicated and sorted into categories — no message reading of any kind.
- **Review inbox**: confirm or fix imported transactions — individually, or all at once with
  "Confirm all" (flagged large amounts included); recategorising a merchant remembers that
  category for future imports.
- **Themes**: Dark, Light and System modes with six color themes — Default, Dynamic (Material You
  wallpaper colours on Android 12+), Catppuccin, Cloud/Amber, Ocean, Forest and Rose — plus AMOLED
  black, accent colours and reduced-motion support; every palette meets WCAG contrast thresholds.
- **Searchable settings**: every destination is one query away, grouped the way you think about it.
- **Accounts and cards**: balances that reconcile with what you know, card outstanding and
  utilisation, and "mark paid" recording the transfer from your bank account.
- **Card bills**: due dates, amounts and payment status, with due-date reminders.
- **Budgets**: monthly budgets that follow your salary cycle, pace projection, optional rollover of
  unspent money, and an envelope (zero-based) mode.
- **Goals**: with the monthly saving they need, an optional purchase loan (EMI and total interest),
  and a success probability from a Monte Carlo simulation.
- **Safe to spend**: what you can spend each day until payday, after card bills and repeating
  payments due before it.
- **Recurring & subscriptions**: repeating payments found in your spending (subscriptions, EMIs,
  UPI AutoPay / NACH mandates), what's due in the next 30 days, price-change alerts, and scheduled
  entries (rent, SIPs) that add themselves on their dates.
- **Loans & EMIs**: amortisation schedule, what's still owed (counted in net worth), and a
  prepayment calculator (fewer EMIs or a lower EMI).
- **Tags, splits and IOUs**: tag transactions (`#goa-trip`), split one payment across categories,
  and track money lent to or borrowed from people.
- **Receipts**: attach a bill photo or PDF to a transaction, encrypted on the phone.
- **Reports**: monthly and yearly PDF reports, a year-in-review, and a calendar of daily spending
  and upcoming payments.
- **Insights**: offline rules including budget pace, spending spikes, savings rate, subscriptions,
  card utilisation, emergency-fund cover and unusual spends.
- **Export**: CSV (opens in Excel), Beancount and Ledger, so your data is never locked in.
- **Everyday tools**: home-screen widgets (balances, this month, recent transactions, upcoming payments, quick add), a quick-add tile in Quick
  Settings, and an optional daily digest notification.
- **Languages**: English and Hindi (हिन्दी).

## Privacy and security

- **No network, by construction.** The APK does not request the INTERNET permission (or any other
  network permission), so no code in it — ours or a dependency's — can open a socket.
- **Opt-in SMS tracking, on-device.** In Settings → Financial tools you can enable bank-message
  tracking, with a consent screen that explains every access before anything is turned on. The
  **standard build** is permissionless: you share bank messages to Acuranza from your Messages app
  (or paste them), and Acuranza extracts amount, date, merchant and account into transactions you
  confirm. The **Full build** additionally scans your inbox history and can read incoming bank
  notifications: it requests the SMS permission and uses Android's notification-listener access,
  both explicitly granted, both revocable, and it asks again before scanning. OTP/PIN/password
  messages, promotions and ambiguous text are never processed; message bodies are parsed and
  discarded, never stored; the same message can never create two transactions. Withdrawing consent
  stops all processing immediately, deletes every pending candidate and SMS-imported transaction
  (your own entries and every other record are untouched), and points you to system settings to
  revoke the SMS permission — Android does not let an app revoke it itself.
- **Encrypted database:** SQLCipher (AES-256), with the key kept in the Android Keystore.
- **Biometric lock, app PIN with a decoy PIN** (opens an empty profile), privacy mode and
  screenshot blocking.
- **Encrypted backups:** AES-256-GCM with a password-derived key (PBKDF2, 600k iterations), to a
  folder you pick, on a schedule you choose.
- **Theme colours stay honest:** all palettes — including the Material You Dynamic theme and the
  pure-black AMOLED mode — are checked against WCAG contrast thresholds, and financial direction is
  never conveyed by colour alone (rows carry arrows and signs).
- **Local-only data:** everything stays on the device and is excluded from cloud backups
  (`allowBackup="false"`).

Acuranza asks for exactly one permission before the app opens on first launch:
**notifications**. They are how reminders reach you — card-bill due dates, unusual spending and
payments you scheduled — and the app is gated until they are allowed
(with a clear path through system settings; it never crashes or loops when denied).
The standard build's bank-message tracking needs no permission at all — messages arrive only when
you share or paste them. The Full build's inbox scan and notification listener need the SMS
permission and notification access, granted explicitly and used only for bank messages. Neither
build asks for anything else: no contacts, no location, no network.

## License

Acuranza is free software under the [GNU General Public License v3.0 or later](LICENSE), as is the
upstream project it derives from (see the attribution note above). Each file keeps the upstream
copyright notice it carried; the git history records every change made here.

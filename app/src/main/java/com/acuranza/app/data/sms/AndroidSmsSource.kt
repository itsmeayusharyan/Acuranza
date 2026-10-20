package com.acuranza.app.data.sms

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.acuranza.app.repo.ApprovedSmsImport
import com.acuranza.app.repo.SmsImportRepository
import com.acuranza.app.sms.MerchantNormalizer
import com.acuranza.app.sms.SmsConfidence

/** Android side of scanning: the inbox source and the runner that ties everything together. */

/**
 * Reads the device inbox through the SMS content provider. Only the four fields Acuranza needs
 * are projected; the body is used to parse and then dropped — it is never written anywhere.
 */
class AndroidSmsInboxSource(private val resolver: ContentResolver) : SmsInboxSource {
    override suspend fun query(sinceMillis: Long): List<InboxSms> {
        val out = mutableListOf<InboxSms>()
        resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val dateSent = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT)
            while (cursor.moveToNext()) {
                val sender = cursor.getString(address) ?: continue
                val text = cursor.getString(body) ?: continue
                val received = cursor.getLong(date)
                val sent = cursor.getLong(dateSent).takeIf { it > 0 } ?: received
                out += InboxSms(sender = sender, body = text, sentMillis = sent, receivedMillis = received)
            }
        }
        return out
    }
}

/** Whether the SMS permission is currently granted; a revoke flips this and every scan stops. */
fun Context.hasSmsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

/** A candidate as the review screen sees it: the graded parse plus Acuranza's suggestions. */
data class ScanCandidate(
    val graded: GradedCandidate,
    /** The account that already matches, if any; resolved (and created, when needed) at import. */
    val matchedAccountId: Long?,
    val suggestedCategory: String,
)

/**
 * One place that runs a scan end to end: gate checks, inbox read, parse, dedup, import and the
 * settings bookkeeping. The background worker and the UI's buttons all call this, so there is
 * exactly one behaviour to test and audit.
 */
class SmsScanRunner(
    private val context: Context,
    private val repo: SmsImportRepository,
    private val settings: com.acuranza.app.data.prefs.SettingsStore,
) {
    private val source = AndroidSmsInboxSource(context.contentResolver)

    data class Result(
        val decision: SmsScanGate.Decision,
        val imported: Int,
        val skipped: Int,
        val candidates: Int,
        /** The gate's outcome, for the status line ("ok", "no permission", "no consent"…). */
        val status: String,
    )

    /**
     * Scans for new messages. With [dryRun] the candidates are returned and nothing is saved —
     * the first import shows them for review before anything is imported. Background and manual
     * scans import HIGH and MEDIUM confidence directly (MEDIUM lands as `suspicious`, so Review
     * still guards balances); LOW is only ever offered to the user, never imported silently.
     */
    suspend fun run(
        auto: Boolean,
        dryRun: Boolean = false,
        windowMillis: Long = SmsScanOrchestrator.FIRST_IMPORT_WINDOW_MILLIS,
    ): Pair<Result, List<ScanCandidate>> {
        val current = settings.current()
        val decision = if (auto) {
            SmsScanGate.check(current.smsConsent, context.hasSmsPermission(), current.smsAutoScan)
        } else {
            SmsScanGate.checkManual(current.smsConsent, context.hasSmsPermission())
        }
        if (!decision.allowed) {
            if (current.smsLastScanStatus != decision.reason) settings.recordSmsScan(decision.reason)
            return Result(decision, 0, 0, 0, decision.reason) to emptyList()
        }

        val since = if (dryRun || current.smsLastScanAt == 0L) System.currentTimeMillis() - windowMillis else current.smsLastScanAt
        val outcome = SmsScanOrchestrator.scan(source, since, repo.importedFingerprints())

        if (dryRun) {
            val candidates = outcome.candidates.map {
                ScanCandidate(
                    graded = it,
                    matchedAccountId = repo.matchAccount(it.txn),
                    suggestedCategory = repo.suggestCategory(it.txn, it.txn.merchant),
                )
            }
            return Result(decision, 0, outcome.skipped, candidates.size, "ok") to candidates
        }

        val label = when {
            !current.smsFirstImportDone -> "First import"
            auto -> "Automatic scan"
            else -> "Manual refresh"
        }
        val importable = outcome.candidates.filter { !auto || it.confidence != SmsConfidence.LOW }
        var imported = 0
        if (importable.isNotEmpty()) {
            val items = importable.map { graded ->
                val txn = graded.txn
                val merchant = txn.merchant?.let { runCatching { MerchantNormalizer.normalize(it) }.getOrNull() } ?: txn.merchant
                ApprovedSmsImport(
                    parsed = txn,
                    sender = graded.sender,
                    amount = txn.amount,
                    categoryKey = repo.suggestCategory(txn, merchant),
                    merchant = merchant,
                    timestampMillis = txn.timestampMillis,
                    accountId = repo.resolveAccount(txn),
                    confidence = graded.confidence,
                )
            }
            imported = repo.importBatch(label, items, outcome.balances).second
        } else {
            outcome.balances.forEach { repo.applyBalance(it) }
        }

        if (!current.smsFirstImportDone && imported > 0) settings.setSmsFirstImportDone()
        settings.recordSmsScan("ok", outcome.skipped)
        return Result(decision, imported, outcome.skipped, importable.size, "ok") to emptyList()
    }
}

package com.acuranza.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportDao
import com.acuranza.app.data.prefs.Settings
import com.acuranza.app.data.sms.ScanCandidate
import com.acuranza.app.data.sms.hasSmsPermission
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.Money
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.ApprovedSmsImport
import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.work.WorkScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a").withZone(ZoneId.systemDefault())
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

/** Settings → Financial tools → SMS transaction tracking: consent, first import, control, management. */
@Composable
fun SmsTrackingScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var candidates by remember { mutableStateOf<List<ScanCandidate>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val runner = c.smsScanRunner

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    val startReview: () -> Unit = {
        if (runner != null) {
            scope.launch {
                scanning = true
                val (_, found) = runner.run(auto = false, dryRun = true)
                scanning = false
                candidates = found
                if (found.isEmpty()) message = "No bank transactions found in the last 12 months"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        scope.launch {
            c.settings.setSmsConsent(true)
            c.settings.setSmsAutoScan(true)
            WorkScheduler.setSmsScanEnabled(context, true)
            startReview()
        }
    }

    AcuranzaScreen(
        title = "SMS transaction tracking",
        subtitle = "Bank messages, read on this device only",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        when {
            scanning -> Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("Scanning your inbox…", style = AcuranzaText.screenTitle)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reading bank messages from the last 12 months. Nothing is saved until you confirm the import.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AcuranzaTheme.colors.subtleText,
                )
            }
            candidates != null -> SmsReviewContent(
                candidates = candidates.orEmpty(),
                onCancel = { candidates = null },
                onConfirm = { edits ->
                    scope.launch {
                        val items = candidates.orEmpty().mapIndexedNotNull { index, candidate ->
                            val edit = edits[index] ?: return@mapIndexedNotNull null
                            if (edit.excluded) return@mapIndexedNotNull null
                            val parsed = candidate.graded.txn
                            val accountId = candidate.matchedAccountId ?: c.smsImports.resolveAccount(parsed)
                            ApprovedSmsImport(
                                parsed = parsed,
                                sender = candidate.graded.sender,
                                amount = edit.amount ?: parsed.amount,
                                categoryKey = edit.categoryKey ?: candidate.suggestedCategory,
                                merchant = edit.merchant ?: parsed.merchant,
                                timestampMillis = parsed.timestampMillis,
                                accountId = accountId,
                                confidence = candidate.graded.confidence,
                            )
                        }
                        candidates = null
                        if (items.isNotEmpty()) {
                            val (_, created) = c.smsImports.importBatch("First import", items)
                            if (created > 0) c.settings.setSmsFirstImportDone()
                            message = "$created transactions imported — flagged ones wait in Review"
                        } else {
                            message = "Nothing selected to import"
                        }
                        refresh++
                    }
                },
            )
            runner == null -> Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("Unavailable", style = AcuranzaText.screenTitle)
                Spacer(Modifier.height(8.dp))
                Text("SMS tracking is disabled in this profile.", color = AcuranzaTheme.colors.subtleText)
            }
            !s.smsConsent -> SmsConsentContent(
                modifier = Modifier.padding(padding),
                onAgree = { permissionLauncher.launch(android.Manifest.permission.READ_SMS) },
            )
            else -> SmsOverviewContent(
                settings = s,
                hasPermission = context.hasSmsPermission(),
                refreshKey = refresh,
                modifier = Modifier.padding(padding),
                onToggleAuto = { on ->
                    scope.launch {
                        c.settings.setSmsAutoScan(on)
                        WorkScheduler.setSmsScanEnabled(context, on && context.hasSmsPermission())
                    }
                },
                onRefresh = {
                    scope.launch {
                        val (result, _) = runner!!.run(auto = false, dryRun = false)
                        message = when {
                            result.imported > 0 -> "${result.imported} new transactions imported"
                            result.status != "ok" -> result.status
                            else -> "No new bank messages"
                        }
                        refresh++
                    }
                },
                onFirstImport = startReview,
                onDeleteImport = { importId ->
                    scope.launch {
                        c.smsImports.deleteImport(importId)
                        refresh++
                    }
                },
                onUndo = { batchId ->
                    scope.launch {
                        val removed = c.smsImports.undoBatch(batchId)
                        message = "Removed $removed imported transactions"
                        refresh++
                    }
                },
                onDeleteAll = {
                    scope.launch {
                        val removed = c.smsImports.deleteAll()
                        message = "Removed $removed imported transactions"
                        refresh++
                    }
                },
                onWithdraw = {
                    scope.launch {
                        c.settings.revokeSmsConsent()
                        WorkScheduler.setSmsScanEnabled(context, false)
                        message = "SMS access withdrawn. Imported transactions stay until you delete them."
                    }
                },
            )
        }
    }
}

/** What the user agrees to before the permission dialog ever appears. */
@Composable
private fun SmsConsentContent(modifier: Modifier, onAgree: () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        AcuranzaCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Sms, contentDescription = null, tint = AcuranzaTheme.colors.accent)
                    Text("What this does", style = MaterialTheme.typography.titleMedium)
                }
                Bullet("Reads bank and payment SMS on this phone — UPI, card spends, ATM withdrawals, credits, refunds and fees.")
                Bullet("Extracts the amount, date, merchant and account, and turns them into transactions you confirm.")
                Bullet("Stores only those extracted details. The message text itself is never saved, exported or sent anywhere.")
                Bullet("Everything runs offline in Acuranza's private storage. There is no account, cloud or sync.")
                Bullet("You choose automatic background scanning, and can turn it off or delete everything at any time.")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Note for sideloaded installs: Google Play Protect may warn about apps with SMS access. " +
                "That screen is about the permission, not about Acuranza's signature.",
            style = MaterialTheme.typography.bodySmall,
            color = AcuranzaTheme.colors.tertiaryText,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("I agree — scan my SMS", onClick = onAgree, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Acuranza will ask for the SMS permission next. Nothing is scanned before you agree.",
            style = MaterialTheme.typography.bodySmall,
            color = AcuranzaTheme.colors.tertiaryText,
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("·", color = AcuranzaTheme.colors.accent)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AcuranzaTheme.colors.subtleText)
    }
}

/** The user's per-row edits to a scanned candidate. */
data class ReviewEdit(
    /** Amount in paise as edited; null keeps the parsed amount. */
    val amount: Long? = null,
    val merchant: String? = null,
    val categoryKey: String? = null,
    val excluded: Boolean = false,
)

private sealed interface ReviewRow {
    data class Header(val text: String) : ReviewRow
    data class Candidate(val globalIndex: Int, val candidate: ScanCandidate) : ReviewRow
}

/** First-import review: nothing is saved until Confirm import. */
@Composable
private fun SmsReviewContent(
    candidates: List<ScanCandidate>,
    onConfirm: (edits: Map<Int, ReviewEdit>) -> Unit,
    onCancel: () -> Unit,
) {
    val needsReview = candidates.filter { it.graded.confidence != SmsConfidence.HIGH }
    val confident = candidates.filter { it.graded.confidence == SmsConfidence.HIGH }
    val edits = remember(candidates) {
        List(candidates.size) { mutableStateOf(ReviewEdit()) }
    }
    var expanded by remember { mutableStateOf(-1) }

    val rows = remember(candidates) {
        buildList {
            if (needsReview.isNotEmpty()) add(ReviewRow.Header("Needs review — ${needsReview.size}"))
            needsReview.forEachIndexed { i, c -> add(ReviewRow.Candidate(i, c)) }
            if (confident.isNotEmpty()) add(ReviewRow.Header("Confident — ${confident.size}"))
            confident.forEachIndexed { i, c -> add(ReviewRow.Candidate(needsReview.size + i, c)) }
        }
    }
    val includedCount = candidates.indices.count { !edits[it].value.excluded }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("${candidates.size} bank transactions found", style = MaterialTheme.typography.titleLarge)
        Text(
            "Check each one — edit what's wrong, switch off what isn't yours. Nothing is saved until you confirm.",
            style = MaterialTheme.typography.bodyMedium,
            color = AcuranzaTheme.colors.subtleText,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                when (row) {
                    is ReviewRow.Header -> Text(
                        row.text.uppercase(),
                        style = AcuranzaText.sectionLabel,
                        color = AcuranzaTheme.colors.subtleText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    is ReviewRow.Candidate -> {
                        val editState = edits[row.globalIndex]
                        val edit by editState
                        val txn = row.candidate.graded.txn
                        AcuranzaCard(onClick = { expanded = if (expanded == row.globalIndex) -1 else row.globalIndex }) {
                            Column(Modifier.padding(vertical = 14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            edit.merchant ?: txn.merchant ?: row.candidate.graded.sender,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "${txn.bank.displayName}${txn.accountLast4?.let { " $it" } ?: ""} · " +
                                                dateFormat.format(Instant.ofEpochMilli(txn.timestampMillis)) + " · " +
                                                row.candidate.graded.confidence.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AcuranzaTheme.colors.tertiaryText,
                                        )
                                    }
                                    Text(
                                        Money.format(edit.amount ?: txn.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (txn.type == TxnType.INCOME) AcuranzaTheme.colors.income else AcuranzaTheme.colors.expense,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Switch(checked = !edit.excluded, onCheckedChange = { editState.value = editState.value.copy(excluded = !it) })
                                }
                                if (expanded == row.globalIndex) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = edit.merchant ?: txn.merchant ?: "",
                                        onValueChange = { editState.value = editState.value.copy(merchant = it.ifBlank { null }) },
                                        label = { Text("Merchant") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ((edit.amount ?: txn.amount) / 100.0).toString(),
                                        onValueChange = { text ->
                                            editState.value = editState.value.copy(amount = Money.fromRupees(text.toDoubleOrNull() ?: 0.0))
                                        },
                                        label = { Text("Amount (₹)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(
                "Confirm import ($includedCount)",
                onClick = { onConfirm(edits.map { it.value }.mapIndexed { i, e -> i to e }.toMap()) },
                modifier = Modifier.weight(1.6f),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SmsOverviewContent(
    settings: Settings,
    hasPermission: Boolean,
    refreshKey: Int,
    modifier: Modifier,
    onToggleAuto: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onFirstImport: () -> Unit,
    onDeleteImport: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val c = LocalContainer.current
    var batches by remember { mutableStateOf<List<SmsImportBatchEntity>>(emptyList()) }
    var rows by remember { mutableStateOf<List<SmsImportDao.ManagementRow>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        batches = c.smsImports.latestBatches()
        rows = c.smsImports.managementRows(100)
        total = c.smsImports.totalImports()
    }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Column {
                SettingsToggle(
                    title = "Automatic SMS scanning",
                    subtitle = when {
                        !hasPermission -> "Needs the SMS permission — grant it in system settings to resume"
                        else -> "Scans for new bank messages about every 6 hours. Turning it off stops future scans; nothing already imported is deleted."
                    },
                    checked = settings.smsAutoScan && hasPermission,
                    onChecked = onToggleAuto,
                    icon = Icons.Outlined.Sms,
                )
                SettingsRow(
                    title = "Refresh SMS now",
                    subtitle = "Import new bank messages immediately",
                    icon = Icons.Outlined.Autorenew,
                    divider = false,
                ) { onRefresh() }
            }
        }

        item {
            Text(
                if (settings.smsLastScanAt == 0L) "Never scanned"
                else "Last scan ${dateTimeFormat.format(Instant.ofEpochMilli(settings.smsLastScanAt))} — ${settings.smsLastScanStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.tertiaryText,
            )
        }

        if (!settings.smsFirstImportDone) {
            item {
                AcuranzaCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("First import", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Scan your existing messages and pick what to import. You review every transaction before anything is saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcuranzaTheme.colors.subtleText,
                        )
                        PrimaryButton("Scan and review", onClick = onFirstImport, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (total > 0) {
            item { Text("IMPORTED — $total", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText) }
            items(rows) { row ->
                AcuranzaCard {
                    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.merchant ?: row.bank, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            val flagged = if (row.suspicious == true && row.reviewed == false) " · flagged for review" else ""
                            Text(
                                row.bank + flagged,
                                style = MaterialTheme.typography.bodySmall,
                                color = AcuranzaTheme.colors.tertiaryText,
                            )
                        }
                        Text(
                            Money.format(row.amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (row.type == TxnType.INCOME) AcuranzaTheme.colors.income else AcuranzaTheme.colors.expense,
                        )
                        IconButton(onClick = { onDeleteImport(row.importId) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete import", tint = AcuranzaTheme.colors.tertiaryText)
                        }
                    }
                }
            }
            batches.firstOrNull()?.let { last ->
                item {
                    SecondaryButton(
                        "Undo last import batch (${last.txnCount})",
                        onClick = { onUndo(last.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Column {
                if (total > 0) {
                    SettingsRow(
                        title = "Delete all SMS imports",
                        subtitle = "Removes every transaction imported from SMS. Everything else stays.",
                        icon = Icons.Outlined.Delete,
                        tone = RowTone.DESTRUCTIVE,
                    ) { confirmDeleteAll = true }
                    SettingsRow(
                        title = "Withdraw SMS consent",
                        subtitle = "Stops all scanning. Imported transactions stay until deleted.",
                        icon = Icons.Outlined.Block,
                        divider = false,
                        tone = RowTone.DESTRUCTIVE,
                    ) { onWithdraw() }
                } else {
                    SettingsRow(
                        title = "Withdraw SMS consent",
                        subtitle = "Stops all scanning and forgets the agreement.",
                        icon = Icons.Outlined.Block,
                        divider = false,
                        tone = RowTone.DESTRUCTIVE,
                    ) { onWithdraw() }
                }
            }
        }

        item {
            Text(
                "Acuranza reads SMS only on this device, stores only the extracted transaction details, " +
                    "and has no network permission at all.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.tertiaryText,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all SMS imports?") },
            text = { Text("Every transaction imported from SMS will be removed. Transactions you added yourself stay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteAll()
                }) { Text("Delete", color = AcuranzaTheme.colors.alert) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Keep") } },
        )
    }
}

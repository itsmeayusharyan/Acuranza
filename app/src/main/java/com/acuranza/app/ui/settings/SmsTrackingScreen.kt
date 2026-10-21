package com.acuranza.app.ui.settings

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportDao
import com.acuranza.app.data.SmsImportEntity
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.Money
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.SmsImportRepository
import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

/**
 * Settings → Financial tools → SMS transaction tracking, permissionless edition: bank messages
 * arrive by Android's share sheet (SmsShareActivity) or by paste, are parsed on-device, and only
 * the extracted details are stored — after the user confirms them. The app holds no SMS
 * permission (Play Protect blocks sideloaded APKs that request one), so nothing ever reads the
 * inbox.
 */
@Composable
fun SmsTrackingScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<List<SmsImportEntity>>(emptyList()) }
    var reviewing by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf("") }
    var clipboardHit by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }
    LaunchedEffect(refresh, s.smsConsent) {
        pending = if (s.smsConsent && !c.decoy) c.smsImports.pending() else emptyList()
    }

    // One-tap capture: with consent and this screen open, a bank message sitting in the
    // clipboard is offered for parsing. The clipboard is read only here (foreground, user-initiated
    // screen), checked with the same pure pipeline, and never stored unless the user taps Parse.
    LaunchedEffect(refresh, s.smsConsent, reviewing) {
        clipboardHit = null
        if (s.smsConsent && !c.decoy && !reviewing) {
            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as? android.content.ClipboardManager
            val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrBlank() && text != paste) {
                val result = com.acuranza.app.sms.SharedSmsPipeline.process(text, System.currentTimeMillis())
                if (result is com.acuranza.app.sms.SharedSmsPipeline.Result.Candidate) clipboardHit = text
            }
        }
    }

    AcuranzaScreen(
        title = "SMS transaction tracking",
        subtitle = "Bank messages, shared by you, read on this device only",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        when {
            c.decoy -> Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("Unavailable", style = AcuranzaText.screenTitle)
                Spacer(Modifier.height(8.dp))
                Text("SMS tracking is disabled in this profile.", color = AcuranzaTheme.colors.subtleText)
            }
            !s.smsConsent -> SmsConsentContent(
                modifier = Modifier.padding(padding),
                onAgree = { scope.launch { c.settings.setSmsConsent(true) } },
            )
            reviewing -> SmsPendingReviewContent(
                pending = pending,
                onCancel = { reviewing = false },
                onConfirm = { keep, edits ->
                    scope.launch {
                        val created = c.smsImports.confirmPending(keep, edits)
                        reviewing = false
                        message = if (created > 0) {
                            "$created transactions imported — flagged ones wait in Review"
                        } else {
                            "Nothing selected to import"
                        }
                        refresh++
                    }
                },
            )
            else -> SmsOverviewContent(
                pending = pending,
                clipboardHit = clipboardHit,
                onParseClipboard = { hit ->
                    scope.launch {
                        when (val outcome = c.smsImports.addPending(hit)) {
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Added ->
                                message = "Saved for review"
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Duplicate ->
                                message = "Already added"
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Rejected ->
                                message = "Not imported: ${outcome.reason}"
                        }
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as? android.content.ClipboardManager)?.setPrimaryClip(
                            android.content.ClipData.newPlainText("acuranza", ""),
                        )
                        clipboardHit = null
                        refresh++
                    }
                },
                onDismissClipboard = { clipboardHit = null },
                refreshKey = refresh,
                paste = paste,
                onPasteChange = { paste = it },
                onAddPasted = {
                    scope.launch {
                        when (val outcome = c.smsImports.addPending(paste)) {
                            is SmsImportRepository.PendingOutcome.Added -> {
                                paste = ""
                                message = "Saved for review"
                            }
                            is SmsImportRepository.PendingOutcome.Duplicate -> message = "Already added"
                            is SmsImportRepository.PendingOutcome.Rejected -> message = "Not imported: ${outcome.reason}"
                        }
                        refresh++
                    }
                },
                onReview = { reviewing = true },
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
                        message = "Consent withdrawn. Imported transactions stay until you delete them."
                        refresh++
                    }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** What the user agrees to before anything is processed. */
@Composable
private fun SmsConsentContent(modifier: Modifier, onAgree: () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        AcuranzaCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Sms, contentDescription = null, tint = AcuranzaTheme.colors.accent)
                    Text("How it works", style = MaterialTheme.typography.titleMedium)
                }
                Bullet("Share a bank message from your Messages app to Acuranza (long-press → Share → Acuranza), or paste one here.")
                Bullet("Acuranza extracts the amount, date, merchant and account, and turns the message into a transaction you confirm.")
                Bullet("OTP, PIN, password and authentication messages are never processed. Promotions and balance alerts are ignored.")
                Bullet("The message text is parsed and discarded. Only the extracted details are stored — in Acuranza's private, encrypted storage.")
                Bullet("No SMS permission is requested, so Play Protect installs Acuranza like any other app.")
                Bullet("Everything runs offline. There is no account, cloud or sync, and you can delete everything at any time.")
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("I agree — enable", onClick = onAgree, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing is processed — shared, pasted or otherwise — before you agree.",
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

/** The user's per-row corrections before a pending candidate becomes a transaction. */
private data class PendingEditState(
    val amount: Long? = null,
    val merchant: String? = null,
    val excluded: Boolean = false,
)

private sealed interface PendingList {
    data class Header(val text: String) : PendingList
    data class Row(val item: SmsImportEntity) : PendingList
}

/** Confirm-import review of the pending candidates. Nothing is imported until Confirm. */
@Composable
private fun SmsPendingReviewContent(
    pending: List<SmsImportEntity>,
    onCancel: () -> Unit,
    onConfirm: (keep: List<SmsImportEntity>, edits: Map<Long, SmsImportRepository.PendingEdit>) -> Unit,
) {
    val c = LocalContainer.current
    val edits = remember(pending) { pending.associate { it.id to mutableStateOf(PendingEditState()) } }
    var expanded by remember { mutableStateOf(-1L) }
    // Account matching: the hint says where each candidate would land; the user can override it.
    var hints by remember(pending) { mutableStateOf<Map<Long, com.acuranza.app.data.sms.SmsAccountMatcher.Match>>(emptyMap()) }
    var accounts by remember { mutableStateOf<List<com.acuranza.app.data.sms.SmsAccountMatcher.CandidateAccount>>(emptyList()) }
    var chosen by remember(pending) { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var choosing by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pending) {
        accounts = c.smsImports.pickerAccounts()
        hints = pending.associate { it.id to c.smsImports.accountHint(it) }
    }
    val needsReview = pending.filter { it.confidence != SmsConfidence.HIGH }
    val confident = pending.filter { it.confidence == SmsConfidence.HIGH }
    val rows = remember(pending) {
        buildList {
            if (needsReview.isNotEmpty()) add(PendingList.Header("Needs review — ${needsReview.size}"))
            needsReview.forEach { add(PendingList.Row(it)) }
            if (confident.isNotEmpty()) add(PendingList.Header("Confident — ${confident.size}"))
            confident.forEach { add(PendingList.Row(it)) }
        }
    }
    val included = pending.count { !edits[it.id]!!.value.excluded }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("${pending.size} bank transactions ready", style = MaterialTheme.typography.titleLarge)
        Text(
            "Check each one — edit what's wrong, switch off what isn't yours. Nothing is saved until you confirm.",
            style = MaterialTheme.typography.bodyMedium,
            color = AcuranzaTheme.colors.subtleText,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                when (row) {
                    is PendingList.Header -> Text(
                        row.text.uppercase(),
                        style = AcuranzaText.sectionLabel,
                        color = AcuranzaTheme.colors.subtleText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    is PendingList.Row -> {
                        val item = row.item
                        val editState = edits[item.id]!!
                        val edit by editState
                        AcuranzaCard(onClick = { expanded = if (expanded == item.id) -1L else item.id }) {
                            Column(Modifier.padding(vertical = 14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            edit.merchant ?: item.merchant ?: item.bank,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "${item.bank}${item.accountLast4?.let { " $it" } ?: ""} · " +
                                                dateFormat.format(Instant.ofEpochMilli(item.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AcuranzaTheme.colors.tertiaryText,
                                        )
                                        val targetName = chosen[item.id]?.let { id -> accounts.firstOrNull { it.id == id }?.name }
                                            ?: when (val hint = hints[item.id]) {
                                                is com.acuranza.app.data.sms.SmsAccountMatcher.Match.Exact -> hint.account.name
                                                is com.acuranza.app.data.sms.SmsAccountMatcher.Match.ByBank -> hint.account.name
                                                com.acuranza.app.data.sms.SmsAccountMatcher.Match.None -> null
                                                null -> null
                                            }
                                        val needsAccountPick = hints[item.id] is com.acuranza.app.data.sms.SmsAccountMatcher.Match.None
                                        Text(
                                            if (targetName != null) "→ $targetName" else "→ new account: ${item.bank}${item.accountLast4?.let { " $it" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (needsAccountPick) AcuranzaTheme.colors.warning else AcuranzaTheme.colors.accent,
                                            modifier = Modifier.padding(top = 1.dp).clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                role = androidx.compose.ui.semantics.Role.Button,
                                            ) { choosing = item.id },
                                        )
                                    }
                                    Text(
                                        Money.format(edit.amount ?: item.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.type == TxnType.INCOME) AcuranzaTheme.colors.income else AcuranzaTheme.colors.expense,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Switch(checked = !edit.excluded, onCheckedChange = { editState.value = editState.value.copy(excluded = !it) })
                                }
                                if (expanded == item.id) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = edit.merchant ?: item.merchant ?: "",
                                        onValueChange = { editState.value = editState.value.copy(merchant = it.ifBlank { null }) },
                                        label = { Text("Merchant") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ((edit.amount ?: item.amount) / 100.0).toString(),
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
                "Confirm import ($included)",
                onClick = {
                    val keep = pending.filter { !edits[it.id]!!.value.excluded }
                    val editMap = pending.mapNotNull { item ->
                        val e = edits[item.id]!!.value
                        if (e.excluded) null
                        else item.id to SmsImportRepository.PendingEdit(
                            amount = e.amount,
                            merchant = e.merchant,
                            accountId = chosen[item.id],
                        )
                    }.toMap()
                    onConfirm(keep, editMap)
                },
                modifier = Modifier.weight(1.6f),
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    choosing?.let { rowId ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text("Which account?") },
            text = {
                Column {
                    accounts.forEach { account ->
                        TextButton(onClick = {
                            chosen = chosen + (rowId to account.id)
                            choosing = null
                        }) { Text("${account.name} · ${account.type.name.lowercase().replaceFirstChar { it.uppercase() }}") }
                    }
                    TextButton(onClick = {
                        chosen = chosen - rowId
                        choosing = null
                    }) { Text("Create a new account for ${pending.firstOrNull { it.id == rowId }?.bank ?: "this bank"}") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun SmsOverviewContent(
    pending: List<SmsImportEntity>,
    clipboardHit: String?,
    onParseClipboard: (String) -> Unit,
    onDismissClipboard: () -> Unit,
    refreshKey: Int,
    paste: String,
    onPasteChange: (String) -> Unit,
    onAddPasted: () -> Unit,
    onReview: () -> Unit,
    onDeleteImport: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var batches by remember { mutableStateOf<List<SmsImportBatchEntity>>(emptyList()) }
    var rows by remember { mutableStateOf<List<SmsImportDao.ManagementRow>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        batches = c.smsImports.latestBatches().filterNot { it.label == "Pending review" }
        rows = c.smsImports.managementRows(100).filter { it.transactionId != null }
        total = c.smsImports.totalImports() - c.smsImports.pendingCount()
    }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }

        // One-tap: a bank message copied in the Messages app is offered for parsing
        clipboardHit?.let { hit ->
            item {
                AcuranzaCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bank message detected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "You copied a bank message. Parse it now? The clipboard is cleared after.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcuranzaTheme.colors.subtleText,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                "Parse",
                                onClick = { onParseClipboard(hit) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                "Not now",
                                onClick = onDismissClipboard,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // How messages arrive
        item {
            AcuranzaCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add bank messages", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "In your Messages app, long-press a bank message → Share → Acuranza. Or paste one below. " +
                            "Acuranza reads only what you send it — no SMS permission, no inbox access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcuranzaTheme.colors.subtleText,
                    )
                    OutlinedTextField(
                        value = paste,
                        onValueChange = onPasteChange,
                        label = { Text("Paste a bank message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    PrimaryButton("Parse message", onClick = onAddPasted, enabled = paste.isNotBlank(), modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Pending review
        if (pending.isNotEmpty()) {
            item {
                SettingsRow(
                    title = "Pending review — ${pending.size}",
                    subtitle = "Bank messages waiting for your confirmation. Nothing is imported until you confirm.",
                    icon = Icons.Outlined.Sms,
                ) { onReview() }
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
                            Text(row.bank + flagged, style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.tertiaryText)
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

        // Danger zone
        item {
            Column {
                SettingsRow(
                    title = "Delete all SMS data",
                    subtitle = "Removes every pending candidate and every imported SMS transaction. Everything else stays.",
                    icon = Icons.Outlined.Delete,
                    tone = RowTone.DESTRUCTIVE,
                ) { confirmDeleteAll = true }
                SettingsRow(
                    title = "Withdraw consent",
                    subtitle = "Stops all processing. Imported transactions stay until deleted.",
                    icon = Icons.Outlined.Block,
                    divider = false,
                    tone = RowTone.DESTRUCTIVE,
                ) { onWithdraw() }
            }
        }

        item {
            Text(
                "Acuranza holds no SMS permission. Messages arrive only when you share or paste them; " +
                    "the text is parsed and discarded, and the app has no network permission at all.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.tertiaryText,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all SMS data?") },
            text = { Text("Every pending candidate and every transaction imported from SMS will be removed. Transactions you added yourself stay.") },
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

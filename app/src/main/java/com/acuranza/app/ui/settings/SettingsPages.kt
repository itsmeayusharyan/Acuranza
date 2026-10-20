package com.acuranza.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.acuranza.app.data.prefs.ThemeMode
import com.acuranza.app.model.AccentColor
import com.acuranza.app.model.TxnType
import com.acuranza.app.security.BackupCrypto
import com.acuranza.app.ui.accounts.ordinal
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.common.NO_FILE_PICKER
import com.acuranza.app.ui.common.launchOrFalse
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The Settings subpages: the hub in [MoreScreen] opens each of these on its own screen with a back
 * button, so the settings list stays short while every control keeps its existing behaviour.
 */

/** One scrolling page: padding + the standard 20dp gutters, so sections align everywhere. */
@Composable
private fun SettingsPage(
    title: String,
    subtitle: String,
    nav: NavController,
    snackbar: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    AcuranzaScreen(title = title, subtitle = subtitle, onBack = { nav.popBackSafely() }, snackbar = snackbar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ManagementSettingsPage(nav: NavController) {
    SettingsPage("Management", "Accounts, imports and categories", nav) {
        SettingsSection(null) {
            SettingsRow("Accounts & cards", "Balances, limits and reconciliation", Icons.Outlined.AccountBalance) { nav.navigate(Routes.ACCOUNTS) }
            SettingsRow("Review imports", "Confirm imported transactions", Icons.Outlined.Inbox) { nav.navigate(Routes.REVIEW) }
            SettingsRow("Categories", "Add, rename, restyle and delete your categories", Icons.Outlined.Category, divider = false) { nav.navigate(Routes.CATEGORIES) }
        }
    }
}

@Composable
fun FinancialToolsPage(nav: NavController) {
    SettingsPage("Financial tools", "Everything beyond everyday tracking", nav) {
        SettingsSection(null) {
            SettingsRow("SMS transaction tracking", "Import bank messages automatically, on-device only", Icons.Outlined.Sms) { nav.navigate(Routes.SMS_TRACKING) }
            SettingsRow("Recurring payments", "Upcoming payments, AutoPay mandates, price changes", Icons.Outlined.Autorenew) { nav.navigate(Routes.RECURRING) }
            SettingsRow("Loans & EMIs", "What's left, prepayment savings, tax split", Icons.Outlined.RealEstateAgent) { nav.navigate(Routes.LOANS) }
            SettingsRow("Lending", "Money between you and friends or family", Icons.Outlined.Handshake) { nav.navigate(Routes.DEBTS) }
            SettingsRow("Reports", "Monthly and yearly PDF, year in review, calendar", Icons.Outlined.Assessment) { nav.navigate(Routes.REPORTS) }
            SettingsRow("Budgets & goals", "Budgets, savings goals and your salary plan", Icons.Outlined.TrackChanges, divider = false) { nav.navigate(Routes.PLANNING) }
        }
    }
}

@Composable
fun PrivacySecurityPage(nav: NavController) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    SettingsPage("Privacy & security", "Locks, PINs, encryption and recovery", nav, snackbar) {
        PrivacySecuritySection(onMessage = { show(it) })
    }
}

@Composable
fun NotificationsPage(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ensurePermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    SettingsPage("Notifications", "Reminders, and where your budget month starts", nav) {
        SettingsSection(null) {
            SettingsToggle("Bill reminders", "3 days and 1 day before the due date", s.notifyBills, { v ->
                if (v) ensurePermission()
                scope.launch { c.settings.setNotifyBills(v) }
            }, Icons.Outlined.Alarm)
            SettingsToggle("Important insights", "Overspending and unusual spends", s.notifyInsights, { v ->
                if (v) ensurePermission()
                scope.launch { c.settings.setNotifyInsights(v) }
            }, Icons.Outlined.Bolt)
            SettingsPickerRow(
                "Month starts on",
                (1..28).toList(),
                s.monthStartDay,
                { if (it == 1) "1st (calendar month)" else ordinal(it) + " (e.g. salary day)" },
                Icons.Outlined.CalendarMonth,
            ) { d -> scope.launch { c.settings.setMonthStartDay(d) } }
            PaydaySettings(s)
        }
    }
}

@Composable
fun AppearancePage(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()
    SettingsPage("Appearance", "Theme, accent colour and your dashboard", nav) {
        SettingsSection(null) {
            SettingsPickerRow(
                "Theme",
                ThemeMode.entries,
                s.themeMode,
                { when (it) { ThemeMode.SYSTEM -> "System default"; ThemeMode.DARK -> "Dark"; ThemeMode.LIGHT -> "Light" } },
                Icons.Outlined.Palette,
            ) { mode -> scope.launch { c.settings.setThemeMode(mode) } }
            SettingsPickerRow(
                "Accent colour",
                AccentColor.entries,
                s.accentChoice,
                { it.label },
                Icons.Outlined.ColorLens,
            ) { choice -> scope.launch { c.settings.setAccentColor(choice) } }
            SettingsToggle("Hide amounts", "Show ₹ •••• until you tap the eye", s.hideAmounts, { v -> scope.launch { c.settings.setHideAmounts(v) } },
                Icons.Outlined.VisibilityOff)
            SettingsRow("Edit home", "Choose what appears on your dashboard and in what order", Icons.Outlined.DashboardCustomize, divider = false) {
                nav.navigate(Routes.EDIT_HOME)
            }
        }
    }
}

@Composable
fun ToolsPage(nav: NavController) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    SettingsPage("Tools", "Export, digest and widget options", nav, snackbar) {
        com.acuranza.app.ui.tools.ToolsSection(onMessage = { show(it) })
    }
}

/**
 * Backups, imports, sample data and the destructive actions. The backup machinery (activity
 * launchers, password dialogs, busy overlay) moved here unchanged from the old flat Settings.
 */
@Composable
fun DataManagementPage(nav: NavController) {
    val c = LocalContainer.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmRemoveSample by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var sampleLoaded by remember { mutableStateOf(false) }

    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    LaunchedEffect(Unit) { sampleLoaded = runCatching { c.demoData.isLoaded() }.getOrDefault(false) }
    val backupFlow = remember { com.acuranza.app.security.BackupFlow(context.applicationContext as com.acuranza.app.AcuranzaApp) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) backupUri = uri
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    SettingsPage("Data management", "Backups, imports and destructive actions", nav, snackbar) {
        busy?.let { msg ->
            com.acuranza.app.designsystem.components.AcuranzaCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(msg)
                }
            }
        }
        SettingsSection(null) {
            SettingsRow("Back up", "Encrypted file protected by a password", Icons.Outlined.Backup, enabled = busy == null) {
                if (!createBackup.launchOrFalse("acuranza-backup-${LocalDate.now()}.acuranza")) show(NO_FILE_PICKER)
            }
            SettingsRow("Automatic backups", "Daily or weekly, to a folder you choose", Icons.Outlined.Sync) { nav.navigate(Routes.AUTO_BACKUP) }
            SettingsRow("Restore from backup", "Replaces everything on this phone", Icons.Outlined.Restore, enabled = busy == null) {
                if (!openBackup.launchOrFalse(arrayOf("*/*"))) show(NO_FILE_PICKER)
            }
            SettingsRow("Import statements", "CSV from net banking to bring in past transactions", Icons.Outlined.UploadFile) {
                nav.navigate(Routes.STATEMENT_IMPORT)
            }
            if (sampleLoaded) {
                SettingsRow("Remove sample data", "Deletes only the demo accounts, transactions and investments", Icons.Outlined.Science, enabled = busy == null) {
                    confirmRemoveSample = true
                }
            } else {
                SettingsRow("Load sample data", "Explore Acuranza with demo accounts and transactions", Icons.Outlined.Science, enabled = busy == null) {
                    busy = "Loading sample data…"
                    scope.launch {
                        val loaded = runCatching { c.demoData.load() }.getOrDefault(false)
                        if (loaded) c.netWorth.snapshot()
                        sampleLoaded = c.demoData.isLoaded()
                        busy = null
                        show(if (loaded) "Sample data loaded" else "Sample data is already loaded")
                    }
                }
            }
            SettingsRow(
                "Erase all data",
                "Deletes accounts, transactions and bills",
                Icons.Outlined.DeleteForever,
                tone = RowTone.DESTRUCTIVE,
                divider = false,
            ) { confirmErase = true }
        }
    }

    if (confirmErase) {
        ConfirmDialog(
            "Erase all data?",
            "This deletes every account, transaction, bill, budget and goal on this phone. Make a backup first if unsure.",
            "Erase",
            destructive = true,
            onConfirm = {
                confirmErase = false
                scope.launch {
                    c.demoData.clear()
                    runCatching { c.attachmentStore.deleteAll() }
                    sampleLoaded = false
                    show("All data erased")
                }
            },
            onDismiss = { confirmErase = false },
        )
    }
    if (confirmRemoveSample) {
        ConfirmDialog(
            "Remove sample data?",
            "This deletes the demo accounts, transactions and investments. Your own data is untouched.",
            "Remove",
            onConfirm = {
                confirmRemoveSample = false
                busy = "Removing sample data…"
                scope.launch {
                    runCatching { c.demoData.removeSample() }
                    sampleLoaded = c.demoData.isLoaded()
                    busy = null
                    show("Sample data removed")
                }
            },
            onDismiss = { confirmRemoveSample = false },
        )
    }
    backupUri?.let { uri ->
        BackupPasswordDialog("Protect your backup", "Choose a password. You'll need it to restore — it can't be recovered.", confirm = true,
            onDismiss = { backupUri = null }) { pw ->
            backupUri = null
            busy = "Creating backup…"
            scope.launch {
                val r = runCatching { backupFlow.export(uri, pw.toCharArray()) }
                busy = null
                show(r.map { "Backed up ${it.transactions} transactions and ${it.accounts} accounts" }.getOrElse { "Backup failed: ${it.message}" })
            }
        }
    }
    restoreUri?.let { uri ->
        BackupPasswordDialog("Restore backup", "Enter the backup's password. Current data on this phone will be replaced.", confirm = false,
            onDismiss = { restoreUri = null }) { pw ->
            restoreUri = null
            busy = "Restoring…"
            scope.launch {
                val r = runCatching { backupFlow.restore(uri, pw.toCharArray()) }
                busy = null
                sampleLoaded = c.demoData.isLoaded()
                show(
                    r.map { "Restored ${it.transactions} transactions and ${it.accounts} accounts" }.getOrElse {
                        if (it is BackupCrypto.WrongPasswordException) "Wrong password or damaged file" else "Restore failed: ${it.message}"
                    },
                )
            }
        }
    }
}

private const val NEW_BACKUP_MIN_PASSWORD = 8

@Composable
private fun BackupPasswordDialog(title: String, message: String, confirm: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val minLength = if (confirm) NEW_BACKUP_MIN_PASSWORD else 1
    val ok = pw.length >= minLength && (!confirm || pw == pw2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    pw, { pw = it }, label = { Text(if (confirm) "Password ($NEW_BACKUP_MIN_PASSWORD+ characters)" else "Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (confirm) {
                    OutlinedTextField(
                        pw2, { pw2 = it }, label = { Text("Repeat password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = pw2.isNotEmpty() && pw != pw2,
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onDone(pw) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

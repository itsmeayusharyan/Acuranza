package com.acuranza.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.data.prefs.ThemeMode
import com.acuranza.app.designsystem.components.AcuranzaCard
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.CategoryIcon
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.TxnType
import com.acuranza.app.security.BackupCrypto
import com.acuranza.app.security.BiometricGate
import com.acuranza.app.security.UnlockResult
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.common.color
import com.acuranza.app.ui.common.NO_FILE_PICKER
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.common.launchOrFalse
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun Context.versionName(): String = runCatching {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
}.getOrDefault("1.0")

/** The Settings tab: every Acuranza control, grouped into clear categories. */
@Composable
fun MoreScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmRemoveSample by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    var sampleLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { sampleLoaded = runCatching { c.demoData.isLoaded() }.getOrDefault(false) }
    val backupFlow = remember { com.acuranza.app.security.BackupFlow(context.applicationContext as com.acuranza.app.AcuranzaApp) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) backupUri = uri
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp)) {
            item(key = "head") {
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AcuranzaLogo(size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Settings", style = AcuranzaText.screenTitle)
                        Text("Your money, on your phone", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                    }
                }
            }
            item(key = "appearance") {
                SettingsSection("Appearance & privacy") {
                    SettingsRow(
                        "Theme",
                        "Follow the system, or pick light or dark",
                        Icons.Outlined.Palette,
                        footer = {
                            SegmentedTabs(
                                listOf("System", "Light", "Dark"),
                                ThemeMode.entries.indexOf(s.themeMode),
                                onSelect = { scope.launch { c.settings.setThemeMode(ThemeMode.entries[it]) } },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        },
                    )
                    SettingsToggle("Hide amounts", "Show ₹ •••• until you tap the eye", s.hideAmounts, { v -> scope.launch { c.settings.setHideAmounts(v) } },
                        Icons.Outlined.VisibilityOff)
                    val canLock = remember { BiometricGate.isAvailable(context) }
                    SettingsToggle(
                        "App lock",
                        if (canLock) "Fingerprint, face or screen lock when opening Acuranza" else "Set a screen lock on your phone to use this",
                        s.biometricLockEnabled,
                        { v ->
                            val activity = context as? FragmentActivity
                            if (!v) {
                                scope.launch { c.settings.setBiometricLock(false) }
                            } else if (activity != null) {
                                scope.launch {
                                    if (BiometricGate.authenticate(activity, title = "Turn on app lock") == UnlockResult.Success) {
                                        // The user just proved it's them: keep this session unlocked.
                                        com.acuranza.app.ui.AppLock.markUnlocked()
                                        c.settings.setBiometricLock(true)
                                    }
                                }
                            }
                        },
                        Icons.Outlined.Fingerprint,
                        enabled = canLock,
                    )
                    SettingsToggle(
                        "Block screenshots",
                        "Hide Acuranza in the recents preview and block screenshots",
                        s.blockScreenshots,
                        { v -> scope.launch { c.settings.setBlockScreenshots(v) } },
                        Icons.Outlined.ScreenLockPortrait,
                    )
                    val encrypted = com.acuranza.app.data.DatabaseEncryption.status ==
                        com.acuranza.app.data.DatabaseEncryption.Status.ENCRYPTED
                    SettingsRow(
                        "Database encryption",
                        if (encrypted) "AES-256 (SQLCipher), key kept in the phone's secure hardware keystore" else "Acuranza will encrypt it on the next launch",
                        Icons.Outlined.Lock,
                        enabled = false,
                        divider = false,
                        trailing = {
                            Pill(if (encrypted) "On" else "Pending", if (encrypted) AcuranzaTheme.colors.good else AcuranzaTheme.colors.warning)
                        },
                    )
                }
            }
            item(key = "tools") { com.acuranza.app.ui.tools.ToolsSection(onMessage = { show(it) }) }
            item(key = "security") { SecuritySection(onMessage = { show(it) }) }
            item(key = "data") {
                SettingsSection("Data management") {
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
                                if (loaded) {
                                    c.netWorth.snapshot()
                                }
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
            item(key = "about-link") {
                SettingsSection("About") {
                    SettingsRow("About Acuranza", "Version, privacy policy and licence", Icons.Outlined.Info, divider = false) {
                        nav.navigate(Routes.ABOUT)
                    }
                }
            }
            item(key = "about") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Acuranza ${context.versionName()}", style = MaterialTheme.typography.labelLarge, color = AcuranzaTheme.colors.subtleText)
                    Text("Everything stays on this device · no internet access", style = MaterialTheme.typography.labelSmall, color = AcuranzaTheme.colors.subtleText)
                    Text("Free & open source · GPL-3.0 · based on Kosh", style = MaterialTheme.typography.labelSmall, color = AcuranzaTheme.colors.subtleText)
                }
            }
        }
        busy?.let {
            AcuranzaCard(Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp, start = 20.dp, end = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(it)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
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
        PasswordDialog("Protect your backup", "Choose a password. You'll need it to restore — it can't be recovered.", confirm = true,
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
        PasswordDialog("Restore backup", "Enter the backup's password. Current data on this phone will be replaced.", confirm = false,
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

/** A titled group of settings rows on a card. */
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title.uppercase(), style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
        Spacer(Modifier.height(8.dp))
        AcuranzaCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
private fun PasswordDialog(title: String, message: String, confirm: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (confirm) {
                    OutlinedTextField(
                        pw2, { pw2 = it }, label = { Text("Repeat password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = pw2.isNotEmpty() && pw != pw2,
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onDone(pw) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- categories

@Composable
fun CategoriesScreen(nav: NavController) {
    val c = LocalContainer.current
    val cats by c.categories.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    AcuranzaScreen(title = "Categories", subtitle = "Recategorise a transaction to teach Acuranza your merchants") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 40.dp)) {
            listOf(TxnType.EXPENSE to "Spending", TxnType.INCOME to "Income").forEach { (type, title) ->
                item(key = title) {
                    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = AcuranzaTheme.colors.subtleText,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                }
                items(cats.filter { it.type == type }, key = { it.key }) { cat ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(cat.icon, cat.color(), size = 38.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

package com.acuranza.app.ui.settings

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.designsystem.theme.StaggeredEntrance
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.Routes

private fun Context.versionName(): String = runCatching {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
}.getOrDefault("1.0")

/** One destination on the Settings hub, with the group it belongs to. */
private data class SettingsEntry(
    val key: SettingsEntryKey,
    val group: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)

/** Every destination, in the spec's group structure — nothing removed, nothing hidden. */
private val SettingsEntries = listOf(
    SettingsEntry(SettingsEntryKey.ACCOUNTS, "Account", "Management", "Accounts, imports and categories", Icons.Outlined.AccountBalance, Routes.SETTINGS_MANAGEMENT),
    SettingsEntry(SettingsEntryKey.FINANCIAL, "General", "Financial tools", "Recurring, loans, lending, reports, budgets & goals", Icons.AutoMirrored.Outlined.TrendingUp, Routes.SETTINGS_FINANCIAL),
    SettingsEntry(SettingsEntryKey.WIDGETS, "General", "Widgets", "Preview and add home-screen widgets", Icons.Outlined.Widgets, Routes.WIDGETS_PREVIEW),
    SettingsEntry(SettingsEntryKey.APPEARANCE, "Appearance", "Appearance", "Theme, colour theme, accent, AMOLED, hide amounts, edit home", Icons.Outlined.Palette, Routes.SETTINGS_APPEARANCE),
    SettingsEntry(SettingsEntryKey.PRIVACY, "Privacy & Security", "Privacy & security", "App lock, PINs, encryption and recovery", Icons.Outlined.Shield, Routes.SETTINGS_PRIVACY),
    SettingsEntry(SettingsEntryKey.DATA, "Data Management", "Data management", "Backups, imports, exports, sample data, erase", Icons.Outlined.Backup, Routes.SETTINGS_DATA),
    SettingsEntry(SettingsEntryKey.NOTIFICATIONS, "Notifications", "Notifications", "Reminders, digest, widgets and where your budget month starts", Icons.Outlined.Alarm, Routes.SETTINGS_NOTIFICATIONS),
    SettingsEntry(SettingsEntryKey.ABOUT, "About", "About", "Version, privacy policy and licence", Icons.Outlined.Info, Routes.ABOUT),
)

private fun entryOf(key: SettingsEntryKey): SettingsEntry = SettingsEntries.first { it.key == key }

/**
 * The Settings hub (v1.13.14): a prominent header, a search field that filters every destination
 * by name and description, and the destinations grouped into labeled rounded containers. Search
 * shows a flat filtered list with a graceful empty state; with an empty query the groups render
 * in full. All colors come from the theme, so Dark, Light, System and AMOLED flow through.
 */
@Composable
fun MoreScreen(nav: NavController) {
    val hubLayoutPref = LocalSettings.current.settingsLayout
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    // Deep search: the flat registry covers hub destinations AND rows buried inside nested pages.
    val hits = SettingsSearchIndex.searchSettings(q)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
    ) {
        item(key = "head") {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AcuranzaLogo(size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = AcuranzaText.screenTitle)
                    Text("Your money, on your phone", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                }
                androidx.compose.material3.IconButton(onClick = { nav.navigate(Routes.SETTINGS_EDIT) }) {
                    androidx.compose.material3.Icon(Icons.Outlined.Tune, contentDescription = "Customize settings")
                }
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search settings", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AcuranzaTheme.colors.tertiaryText) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = AcuranzaTheme.colors.tertiaryText)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AcuranzaTheme.colors.cardSurface,
                    focusedContainerColor = AcuranzaTheme.colors.cardSurface,
                    unfocusedBorderColor = AcuranzaTheme.colors.hairline,
                    focusedBorderColor = AcuranzaTheme.colors.accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
        }
        item(key = "profile") {
            val s = LocalSettings.current
            ProfileCard(name = s.profileName, avatarPath = s.profileAvatar) { nav.navigate(Routes.PROFILE) }
        }
        if (q.isNotEmpty() && hits.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No settings match “${query.trim()}”", style = MaterialTheme.typography.titleSmall, color = AcuranzaTheme.colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Try a shorter word — everything is also one tap away in the groups below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcuranzaTheme.colors.subtleText,
                    )
                }
            }
        } else if (q.isNotEmpty()) {
            item(key = "results-label") {
                Text(
                    "${hits.size} result" + if (hits.size == 1) "" else "s",
                    style = AcuranzaText.sectionLabel,
                    color = AcuranzaTheme.colors.subtleText,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                )
            }
            items(hits, key = { "${it.group}/${it.page}/${it.title}" }) { hit ->
                AcuranzaCard(Modifier.padding(vertical = 5.dp)) {
                    SettingsRow(
                        hit.title,
                        hit.description,
                        null,
                        value = hit.group,
                    ) { nav.navigate(hit.route) }
                }
            }
        } else {
            // The saved hub layout: order, per-section shortcuts and (display-only) title
            // overrides. Hidden shortcuts are simply absent here — search still finds them,
            // so nothing becomes unreachable, and Customize settings re-shows them any time.
            val layout = SettingsHubLayout.decodeWithHidden(hubLayoutPref)
            layout.sections.forEachIndexed { groupIndex, section ->
                item(key = "group-${section.key}") {
                    StaggeredEntrance(groupIndex) {
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(section.title.uppercase(), style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                            Spacer(Modifier.height(8.dp))
                            AcuranzaCard(contentPadding = PaddingValues(0.dp)) {
                                section.entries.forEachIndexed { index, key ->
                                    val entry = entryOf(key)
                                    SettingsRow(
                                        entry.title,
                                        entry.description,
                                        entry.icon,
                                        divider = index < section.entries.lastIndex,
                                    ) { nav.navigate(entry.route) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "footer") {
            val context = LocalContext.current
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Acuranza ${context.versionName()}", style = MaterialTheme.typography.labelLarge, color = AcuranzaTheme.colors.subtleText)
                Text("Everything stays on this device · no internet access", style = MaterialTheme.typography.labelSmall, color = AcuranzaTheme.colors.subtleText)
                Text("Free & open source · GPL-3.0", style = MaterialTheme.typography.labelSmall, color = AcuranzaTheme.colors.subtleText)
            }
        }
    }
}

/** A titled group of settings rows on a card. */
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title.uppercase(), style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
        Spacer(Modifier.height(8.dp))
        AcuranzaCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

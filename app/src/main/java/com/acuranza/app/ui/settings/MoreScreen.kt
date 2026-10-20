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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.acuranza.app.ui.nav.Routes

private fun Context.versionName(): String = runCatching {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
}.getOrDefault("1.0")

private data class SettingsCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)

private val SettingsCategories = listOf(
    SettingsCategory("Appearance", "Theme, accent colour, hide amounts, edit home", Icons.Outlined.Palette, Routes.SETTINGS_APPEARANCE),
    SettingsCategory("Data management", "Backups, imports, sample data, erase", Icons.Outlined.Backup, Routes.SETTINGS_DATA),
    SettingsCategory("Financial tools", "Recurring, loans, lending, reports, budgets & goals", Icons.AutoMirrored.Outlined.TrendingUp, Routes.SETTINGS_FINANCIAL),
    SettingsCategory("Management", "Accounts, imports and categories", Icons.Outlined.AccountBalance, Routes.SETTINGS_MANAGEMENT),
    SettingsCategory("Notifications", "Reminders, and where your budget month starts", Icons.Outlined.Alarm, Routes.SETTINGS_NOTIFICATIONS),
    SettingsCategory("Privacy & security", "App lock, PINs, encryption and recovery", Icons.Outlined.Shield, Routes.SETTINGS_PRIVACY),
    SettingsCategory("Tools", "Export data, daily digest and widgets", Icons.Outlined.Tune, Routes.SETTINGS_TOOLS),
    SettingsCategory("About", "Version, privacy policy and licence", Icons.Outlined.Info, Routes.ABOUT),
)

/**
 * The Settings hub: one short list of categories, each opening its own subpage with a back
 * button — so Settings stays scannable while every control lives one screen deeper, unchanged.
 */
@Composable
fun MoreScreen(nav: NavController) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
    ) {
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
        item(key = "categories") {
            StaggeredEntrance(0) {
                AcuranzaCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsCategories.forEachIndexed { index, category ->
                        SettingsRow(
                            category.title,
                            category.description,
                            category.icon,
                            divider = index < SettingsCategories.lastIndex,
                        ) { nav.navigate(category.route) }
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

package com.acuranza.app.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acuranza.app.BuildConfig
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection

/** The only external URL Acuranza links to: the source repository it is built from. */
private const val SOURCE_URL = "https://github.com/AIM-IT4/kosh"

/** The About Acuranza screen: branding, version and legal links. Reached from Settings. */
@Composable
fun AboutScreen(nav: androidx.navigation.NavController) {
    val context = LocalContext.current
    var sheet by remember { mutableStateOf<AboutSheet?>(null) }
    AboutContent(
        versionLabel = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onBack = { nav.popBackStack() },
        onPrivacy = { sheet = AboutSheet.PRIVACY },
        onTerms = { sheet = AboutSheet.TERMS },
        onLicence = { sheet = AboutSheet.LICENCE },
        onVisitSource = { openSource(context) },
    )
    when (sheet) {
        AboutSheet.PRIVACY -> InfoSheet(
            title = "Privacy policy",
            onDismiss = { sheet = null },
        ) {
            InfoParagraph("Everything Acuranza knows lives on this phone.")
            InfoParagraph(
                "Acuranza requests no internet permission, so Android itself cannot send your " +
                    "transactions, accounts or reminders anywhere. There are no analytics, no ads, " +
                    "no accounts and no tracking.",
            )
            InfoParagraph(
                "Your database is encrypted on the device, and backups are encrypted with a " +
                    "password only you know.",
            )
            InfoParagraph("Deleting your data or uninstalling the app removes it from the phone completely.")
        }
        AboutSheet.TERMS -> InfoSheet(
            title = "Terms of service",
            onDismiss = { sheet = null },
        ) {
            InfoParagraph("Acuranza is provided as-is, free of charge, for personal use.")
            InfoParagraph(
                "It keeps books and draws charts; nothing in it is financial advice. Decisions you " +
                    "make with its numbers are your own.",
            )
            InfoParagraph(
                "Your data is your responsibility: keep backups, and keep a backup's password safe — " +
                    "it cannot be recovered.",
            )
            InfoParagraph("Use of the app is governed by its licence; see Licence.")
        }
        AboutSheet.LICENCE -> InfoSheet(
            title = "Licence",
            onDismiss = { sheet = null },
        ) {
            InfoParagraph(
                "Acuranza is free and open-source software under the GNU General Public License " +
                    "v3.0. You may use, study, share and modify it; there is no warranty.",
            )
            InfoParagraph(
                "It is a derivative of Kosh by Amit Kumar Jha, published under the same licence.",
            )
        }
        null -> Unit
    }
}

private enum class AboutSheet { PRIVACY, TERMS, LICENCE }

private fun openSource(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
    }.onFailure { e ->
        if (e is ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Stateless body of the About screen, so tests can render it without BuildConfig: a branding
 * block, then the legal and project links in the same row kit Settings uses.
 */
@Composable
fun AboutContent(
    versionLabel: String,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit = {},
    onLicence: () -> Unit,
    onVisitSource: () -> Unit,
) {
    val c = AcuranzaTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header: a slightly lighter band with the title centred between the back arrow and its ghost twin.
        Box(
            Modifier
                .fillMaxWidth()
                .background(c.cardSurface)
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                // A ghost spacer of the same width keeps the title optically centred.
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AcuranzaLogo(size = 76.dp, shape = LogoShape.RoundedSquare)
            Spacer(Modifier.height(14.dp))
            Text("Acuranza", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text("Your money, on your phone", style = MaterialTheme.typography.bodyMedium, color = c.subtleText)
            Spacer(Modifier.height(10.dp))
            Text(
                versionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = c.subtleText,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.fill)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        // The section kit expects its parent to own the horizontal padding (as Settings' list does).
        Column(Modifier.padding(horizontal = 20.dp)) {
            SettingsSection("Legal & project") {
                SettingsRow("Privacy policy", "What Acuranza stores, and what it never sends", Icons.Outlined.Shield) { onPrivacy() }
                SettingsRow("Terms of service", "The plain terms of using the app", Icons.Outlined.Gavel) { onTerms() }
                SettingsRow("Licence", "GPL-3.0 · free and open source", Icons.Outlined.Description) { onLicence() }
                SettingsRow("Source code", "Read, audit and build it yourself", Icons.Outlined.Code, value = "GitHub", divider = false) { onVisitSource() }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Offline by design · everything stays on this device",
                style = MaterialTheme.typography.labelSmall,
                color = c.tertiaryText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = AcuranzaTheme.colors.ink)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoParagraph(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AcuranzaTheme.colors.subtleText,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

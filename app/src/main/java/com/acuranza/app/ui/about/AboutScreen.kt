package com.acuranza.app.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acuranza.app.BuildConfig
import com.acuranza.app.designsystem.components.AcuranzaLogo
import com.acuranza.app.designsystem.theme.AcuranzaTheme

/** The only external URL Acuranza links to: the source repository it is built from. */
private const val SOURCE_URL = "https://github.com/AIM-IT4/kosh"

/** The About Acuranza screen: logo, version, and legal links. Reached from Settings. */
@Composable
fun AboutScreen(nav: androidx.navigation.NavController) {
    val context = LocalContext.current
    var sheet by remember { mutableStateOf<AboutSheet?>(null) }
    AboutContent(
        versionLabel = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onBack = { nav.popBackStack() },
        onPrivacy = { sheet = AboutSheet.PRIVACY },
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

private enum class AboutSheet { PRIVACY, LICENCE }

private fun openSource(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
    }.onFailure { e ->
        if (e is ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Stateless body of the About screen, so tests can render it without BuildConfig. */
@Composable
fun AboutContent(
    versionLabel: String,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onLicence: () -> Unit,
    onVisitSource: () -> Unit,
) {
    val c = AcuranzaTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header: a slightly lighter band with the title centred between the back arrow and its ghost twin.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(c.cardSurface)
                    .heightIn(min = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "About Acuranza",
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
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AcuranzaLogo(size = 88.dp, shape = com.acuranza.app.designsystem.components.LogoShape.RoundedSquare)
                Spacer(Modifier.height(16.dp))
                Text("Acuranza", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(versionLabel, style = MaterialTheme.typography.bodySmall, color = c.subtleText)
            }

            Spacer(Modifier.height(32.dp))
            LinkGroup(
                rows = listOf(
                    "Privacy policy" to onPrivacy,
                    "Licence" to onLicence,
                    "Visit source on GitHub" to onVisitSource,
                ),
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.weight(1f))
        }

        // Branding footer, pinned above the system navigation area.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Acuranza",
                style = MaterialTheme.typography.labelMedium,
                color = c.tertiaryText,
            )
            Text(
                "Your money, on your phone",
                style = MaterialTheme.typography.labelSmall,
                color = c.tertiaryText,
            )
        }
    }
}

/** A rounded card of full-width link rows with chevrons, divided by hairlines. */
@Composable
private fun LinkGroup(rows: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    val c = AcuranzaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.cardSurface)
            .border(1.dp, c.hairline, shape),
    ) {
        rows.forEachIndexed { i, (label, onClick) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = c.tertiaryText,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (i < rows.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(1.dp)
                        .background(c.divider),
                )
            }
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

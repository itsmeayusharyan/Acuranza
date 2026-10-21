package com.acuranza.app.ui.about

import android.content.ActivityNotFoundException
import android.os.Build
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import kotlinx.coroutines.launch

/** Where the source and the releases actually live. */
const val REPO_URL = "https://github.com/itsmeayusharyan/Acuranza"

private fun openUrl(context: android.content.Context, url: String): Boolean = try {
    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

/** Device and build facts shown on the About screen; injectable so tests render deterministically. */
data class AboutDeviceInfo(
    val androidRelease: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val appVersion: String,
) {
    companion object {
        fun fromSystem(versionName: String = BuildConfig.VERSION_NAME) =
            AboutDeviceInfo(
                androidRelease = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                model = Build.MODEL,
                appVersion = versionName,
            )
    }
}

/** The About Acuranza screen: branding, version and release info, device info, legal pages. Reached from Settings. */
@Composable
fun AboutScreen(nav: androidx.navigation.NavController) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AboutContent(
        onBack = { nav.popBackStack() },
        onPrivacy = { nav.navigate(Routes.ABOUT_PRIVACY) },
        onTerms = { nav.navigate(Routes.ABOUT_TERMS) },
        onLicence = { nav.navigate(Routes.ABOUT_GPL) },
        onGithub = {
            if (!openUrl(context, REPO_URL)) {
                scope.launch { snackbar.showSnackbar("No browser found on this device") }
            }
        },
        snackbar = snackbar,
    )
}

/**
 * Stateless body of the About screen, so tests can render it without BuildConfig: branding with
 * version and release information, repository and license actions, device/build facts, and the
 * dedicated legal sections in the same row kit Settings uses.
 */
@Composable
fun AboutContent(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit = {},
    onLicence: () -> Unit,
    onGithub: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    device: AboutDeviceInfo = AboutDeviceInfo.fromSystem(),
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
            AcuranzaLogo(size = 88.dp, shape = LogoShape.RoundedSquare)
            Spacer(Modifier.height(14.dp))
            Text("Acuranza", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text("Your money, on your phone", style = MaterialTheme.typography.bodyMedium, color = c.subtleText)
            Spacer(Modifier.height(24.dp))
        }

        // The section kit expects its parent to own the horizontal padding (as Settings' list does).
        Column(Modifier.padding(horizontal = 20.dp)) {
            SettingsSection("Project") {
                SettingsRow(
                    "GitHub repository",
                    "Source, issues and releases · github.com/itsmeayusharyan/Acuranza",
                    Icons.Outlined.Description,
                    divider = false,
                ) { onGithub() }
            }

            SettingsSection("Device and build") {
                SettingsRow("Android version", "Android ${device.androidRelease} (API ${device.apiLevel})", onClick = null)
                SettingsRow("Device", "${device.manufacturer} ${device.model}", onClick = null)
                SettingsRow("App version", device.appVersion, onClick = null, divider = false)
            }

            SettingsSection("Legal") {
                SettingsRow("Privacy policy", "What Acuranza stores, and what it never sends", Icons.Outlined.Shield) { onPrivacy() }
                SettingsRow("Terms of service", "The plain terms of using the app", Icons.Outlined.Gavel) { onTerms() }
                SettingsRow("Licence", "GPL-3.0 · free and open source", Icons.Outlined.Info, divider = false) { onLicence() }
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
            Spacer(Modifier.height(4.dp))
            Text(
                "A derivative of the Kosh project, used under GPL-3.0",
                style = MaterialTheme.typography.labelSmall,
                color = c.tertiaryText,
            )
        }
    }
}

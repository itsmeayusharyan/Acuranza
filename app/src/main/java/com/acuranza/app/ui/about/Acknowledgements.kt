package com.acuranza.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import kotlinx.coroutines.launch

/** One acknowledged dependency: the real module, its version, its licence and where it lives. */
data class AcknowledgedLibrary(
    val name: String,
    val version: String,
    val licence: String,
    val url: String,
    val group: String,
)

/**
 * The dependencies Acuranza actually ships with, mirrored from gradle/libs.versions.toml at the
 * time of this release — listed with their licences so their authors get the credit the licences
 * ask for. Static by design: acknowledging libraries needs no network.
 */
object AboutAcknowledgements {
    const val APACHE = "Apache-2.0"
    const val BSD = "BSD-3-Clause"
    const val EPL = "EPL-1.0"

    val libraries: List<AcknowledgedLibrary> = listOf(
        AcknowledgedLibrary("Jetpack Compose · Material 3", "BOM 2024.12.01", APACHE, "https://developer.android.com/jetpack/compose", "Interface"),
        AcknowledgedLibrary("androidx.activity (compose)", "1.9.3", APACHE, "https://developer.android.com/jetpack/androidx/releases/activity", "Interface"),
        AcknowledgedLibrary("androidx.lifecycle", "2.8.7", APACHE, "https://developer.android.com/jetpack/androidx/releases/lifecycle", "Interface"),
        AcknowledgedLibrary("androidx.navigation compose", "2.8.5", APACHE, "https://developer.android.com/jetpack/androidx/releases/navigation", "Interface"),
        AcknowledgedLibrary("androidx.core-ktx", "1.15.0", APACHE, "https://developer.android.com/jetpack/androidx/releases/core", "Interface"),
        AcknowledgedLibrary("Room", "2.6.1", APACHE, "https://developer.android.com/jetpack/androidx/releases/room", "Data"),
        AcknowledgedLibrary("SQLCipher (Android)", "4.6.1", BSD, "https://www.zetetic.net/sqlcipher/", "Data"),
        AcknowledgedLibrary("androidx.sqlite", "2.4.0", APACHE, "https://developer.android.com/jetpack/androidx/releases/sqlite", "Data"),
        AcknowledgedLibrary("DataStore", "1.1.1", APACHE, "https://developer.android.com/jetpack/androidx/releases/datastore", "Data"),
        AcknowledgedLibrary("WorkManager", "2.9.0", APACHE, "https://developer.android.com/jetpack/androidx/releases/work", "Data"),
        AcknowledgedLibrary("androidx.exifinterface", "1.3.7", APACHE, "https://developer.android.com/jetpack/androidx/releases/exifinterface", "Data"),
        AcknowledgedLibrary("androidx.biometric", "1.1.0", APACHE, "https://developer.android.com/jetpack/androidx/releases/biometric", "Security"),
        AcknowledgedLibrary("androidx.fragment", "1.8.5", APACHE, "https://developer.android.com/jetpack/androidx/releases/fragment", "Security"),
        AcknowledgedLibrary("kotlinx.coroutines", "1.9.0", APACHE, "https://github.com/Kotlin/kotlinx.coroutines", "Language & testing"),
        AcknowledgedLibrary("JUnit 4", "4.13.2", EPL, "https://junit.org/junit4/", "Language & testing"),
    )

    /** Groups in display order. */
    val groups: List<String> get() = libraries.map { it.group }.distinct()
}

/**
 * Acknowledgements: the libraries Acuranza ships with and their licences, plus the app's own
 * licence and the upstream attribution. Every external link opens the browser with a graceful
 * fallback; nothing here touches the network by itself.
 */
@Composable
fun AcknowledgementsScreen(nav: NavController) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AcknowledgementsContent(
        onBack = { nav.popBackSafely() },
        onLicence = { nav.navigate(com.acuranza.app.ui.nav.Routes.ABOUT_GPL) },
        onUrl = { url ->
            if (!openUrl(context, url)) {
                scope.launch { snackbar.showSnackbar("No browser found on this device") }
            }
        },
        snackbar = snackbar,
    )
}

/** Stateless body so screenshot fixtures render it without navigation or BuildConfig. */
@Composable
fun AcknowledgementsContent(
    onBack: () -> Unit,
    onLicence: () -> Unit,
    onUrl: (String) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    AcuranzaScreen(
        title = "Acknowledgements",
        subtitle = "The libraries Acuranza is built on",
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            AboutAcknowledgements.groups.forEachIndexed { gi, group ->
                SettingsSection(group) {
                    val inGroup = AboutAcknowledgements.libraries.filter { it.group == group }
                    inGroup.forEachIndexed { li, lib ->
                        SettingsRow(
                            lib.name,
                            "v${lib.version} · ${lib.licence}",
                            null,
                            divider = li != inGroup.size - 1,
                        ) { onUrl(lib.url) }
                    }
                }
            }
            SettingsSection(null) {
                SettingsRow(
                    "Acuranza's licence",
                    "GPL-3.0 · free and open source",
                    Icons.Outlined.Gavel,
                    divider = false,
                ) { onLicence() }
            }
            Text(
                "Acuranza derives from the open-source Kosh project; the attribution and the full " +
                    "licence text ship with the source distribution (LICENSE in the repository).",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = com.acuranza.app.designsystem.theme.AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
    }
}

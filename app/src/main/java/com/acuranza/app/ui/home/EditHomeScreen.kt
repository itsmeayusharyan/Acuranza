package com.acuranza.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.IconBubble
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * Settings → Appearance → Edit home: picks which dashboard sections appear and in what order.
 *
 * The editor's local state is the source of truth once seeded from the saved layout: every edit
 * applies immediately and is written through to DataStore. Without this, a second tap before the
 * write round-trips would encode from a stale remembered layout and silently undo the first
 * change — and switches would snap back while the emission was in flight.
 */
@Composable
fun EditHomeScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    // Seeded exactly once, when the persisted settings first arrive (settings == null only during
    // the initial frame); after that the user's in-progress edits always win over flow emissions.
    var layout by remember { mutableStateOf<List<Pair<HomeSection, Boolean>>?>(null) }
    LaunchedEffect(settings) {
        if (layout == null && settings != null) {
            layout = HomeLayout.decode(settings!!.homeLayout)
        }
    }

    fun commit(next: List<Pair<HomeSection, Boolean>>) {
        layout = next
        scope.launch { c.settings.setHomeLayout(HomeLayout.encode(next)) }
    }

    EditHomeContent(
        layout = layout ?: HomeLayout.decode(null),
        saved = settings?.homeLayout != null,
        onChange = { next -> commit(next) },
        onReset = {
            layout = HomeLayout.decode(null)
            scope.launch { c.settings.resetHomeLayout() }
        },
        onBack = { nav.popBackSafely() },
    )
}

/**
 * Stateless editor body, so tests render it without the container. The section list scrolls
 * (12 rows plus header can exceed a phone's height) and the Reset card is pinned below it,
 * so the bottom rows and Reset stay reachable on every screen size.
 */
@Composable
fun EditHomeContent(
    layout: List<Pair<HomeSection, Boolean>>,
    saved: Boolean = false,
    onChange: (List<Pair<HomeSection, Boolean>>) -> Unit = {},
    onReset: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    AcuranzaScreen(
        title = "Edit home",
        subtitle = "Choose what appears on your dashboard and in what order",
        onBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "SECTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
            )
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AcuranzaTheme.colors.cardSurface),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(layout.size, key = { layout[it].first.id }) { index ->
                    val (section, visible) = layout[index]
                    androidx.compose.foundation.layout.Box(Modifier.animateItem()) {
                        Column {
                            if (index > 0) com.acuranza.app.ui.settings.SettingsDivider(startInset = 52.dp)
                            EditHomeRow(
                                section = section,
                                visible = visible,
                                isFirst = index == 0,
                                isLast = index == layout.lastIndex,
                                onToggle = { on ->
                                    onChange(HomeLayout.setVisibility(layout, section, on))
                                },
                                onMove = { up ->
                                    onChange(HomeLayout.move(layout, index, up))
                                },
                            )
                        }
                    }
                }
            }

            // Pinned footer: reset is reachable on any screen height, no scrolling needed.
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AcuranzaTheme.colors.cardSurface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(color = AcuranzaTheme.colors.fill, size = 36.dp, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = AcuranzaTheme.colors.ink, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reset to default", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (saved) "Clears your layout and restores the stock dashboard" else "Already showing the default layout",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcuranzaTheme.colors.subtleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                androidx.compose.material3.TextButton(enabled = saved, onClick = onReset) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun EditHomeRow(
    section: HomeSection,
    visible: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMove: (Boolean) -> Unit,
) {
    val colors = AcuranzaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(color = colors.fill, size = 36.dp, shape = RoundedCornerShape(12.dp)) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = if (visible) colors.ink else colors.tertiaryText,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                section.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (visible) MaterialTheme.colorScheme.onSurface else colors.subtleText,
            )
            Text(
                section.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Move up / down; 48dp square buttons keep the pair an easy touch target.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !isFirst, onClick = { onMove(true) }) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move ${section.label} up")
            }
            IconButton(enabled = !isLast, onClick = { onMove(false) }) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move ${section.label} down")
            }
        }
        Switch(
            checked = visible,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                checkedBorderColor = colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.fill,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}

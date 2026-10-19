package com.acuranza.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.acuranza.app.ui.settings.SettingsDivider
import kotlinx.coroutines.launch

/**
 * Settings → Appearance → Edit home: picks which dashboard sections appear and in what order.
 * Every change is saved the moment it's made; "Reset to default" clears the saved layout.
 */
@Composable
fun EditHomeScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val layout = remember(settings?.homeLayout) { HomeLayout.decode(settings?.homeLayout) }
    EditHomeContent(
        layout = layout,
        saved = settings != null,
        onChange = { raw -> scope.launch { c.settings.setHomeLayout(raw) } },
        onReset = { scope.launch { c.settings.resetHomeLayout() } },
        onBack = { nav.popBackSafely() },
    )
}

/** Stateless editor body, so tests render it without the container; [layout] is section→visible. */
@Composable
fun EditHomeContent(
    layout: List<Pair<HomeSection, Boolean>>,
    saved: Boolean = false,
    onChange: (String) -> Unit = {},
    onReset: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    AcuranzaScreen(
        title = "Edit home",
        subtitle = "Choose what appears on your dashboard and in what order",
        onBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxWidth().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                "SECTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AcuranzaTheme.colors.cardSurface),
            ) {
                layout.forEachIndexed { index, (section, visible) ->
                    if (index > 0) SettingsDivider(startInset = 52.dp)
                    EditHomeRow(
                        section = section,
                        visible = visible,
                        isFirst = index == 0,
                        isLast = index == layout.lastIndex,
                        onToggle = { on ->
                            onChange(HomeLayout.encode(HomeLayout.setVisibility(layout, section, on)))
                        },
                        onMove = { up ->
                            onChange(HomeLayout.encode(HomeLayout.move(layout, index, up)))
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
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
            Spacer(Modifier.height(24.dp))
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
        // Move up / down; the whole pair is one 48dp-tall control block with 48dp square buttons.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
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

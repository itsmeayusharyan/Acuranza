package com.acuranza.app.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Background shape of [AcuranzaLogo]. */
enum class LogoShape { Circle, RoundedSquare, None }

private val LogoTile = Color(0xFF2563EB)
private val LogoInk = Color(0xFF1D4ED8)

/**
 * The Acuranza mark: a geometric letter A drawn as three round-capped strokes — two legs rising to a
 * point and a crossbar — white on a flat brand-blue tile.
 *
 * Drawn on the same 108-unit grid as the launcher icon, so the in-app logo and the home-screen icon
 * are the same drawing. With [LogoShape.None] the mark is drawn alone in [markColor] (deep blue by
 * default) and fills more of the box.
 */
@Composable
fun AcuranzaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: LogoShape = LogoShape.RoundedSquare,
    markColor: Color = if (shape == LogoShape.None) LogoInk else Color.White,
    @Suppress("UNUSED_PARAMETER") accentColor: Color = markColor,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Acuranza logo" },
    ) {
        val u = this.size.minDimension / 108f
        when (shape) {
            LogoShape.Circle -> drawCircle(LogoTile, radius = this.size.minDimension / 2f)
            LogoShape.RoundedSquare -> drawRoundRect(LogoTile, size = this.size, cornerRadius = CornerRadius(26f * u, 26f * u))
            LogoShape.None -> Unit
        }
        // The glyph is scaled up slightly when it stands alone, since it has no tile behind it.
        val k = u * if (shape == LogoShape.None) 1.12f else 1f
        // Grid point to canvas, scaled about the glyph's own middle (54, 53.5).
        fun x(v: Float) = center.x + (v - 54f) * k
        fun y(v: Float) = center.y + (v - 53.5f) * k
        val leg = 9f * k
        val bar = 7.5f * k
        // Left leg, right leg, crossbar — the same three strokes as the launcher icon.
        drawLine(markColor, Offset(x(54f), y(31f)), Offset(x(35.5f), y(76f)), strokeWidth = leg, cap = StrokeCap.Round)
        drawLine(markColor, Offset(x(54f), y(31f)), Offset(x(72.5f), y(76f)), strokeWidth = leg, cap = StrokeCap.Round)
        drawLine(markColor, Offset(x(42.5f), y(60.5f)), Offset(x(65.5f), y(60.5f)), strokeWidth = bar, cap = StrokeCap.Round)
    }
}

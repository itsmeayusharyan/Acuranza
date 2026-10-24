package com.acuranza.app.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Background shape of [AcuranzaLogo]. */
enum class LogoShape { Circle, RoundedSquare, None }

private val LogoTile = Color(0xFF060C16)
private val LogoInk = Color(0xFF0C56D8)

/**
 * The Acuranza mark: a crossbar-less letter A drawn as two wide round-capped legs — the left fading
 * white, the right azure — with a cyan arrow rising through it, on a near-black navy tile.
 *
 * Drawn on the same 108-unit grid as the launcher icon, so the in-app logo and the home-screen icon
 * are the same drawing. With [LogoShape.None] the mark is drawn alone in [markColor] with an
 * [accentColor] arrow (deep blue by default) and fills more of the box.
 */
@Composable
fun AcuranzaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: LogoShape = LogoShape.RoundedSquare,
    markColor: Color = if (shape == LogoShape.None) LogoInk else Color.White,
    accentColor: Color = LogoInk,
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
        val leg = 10f * k
        val shaft = 7f * k
        val apex = Offset(x(54f), y(26f))
        val leftFoot = Offset(x(29f), y(79f))
        val rightFoot = Offset(x(79f), y(79f))
        if (shape == LogoShape.None) {
            drawLine(markColor, apex, leftFoot, strokeWidth = leg, cap = StrokeCap.Round)
            drawLine(markColor, apex, rightFoot, strokeWidth = leg, cap = StrokeCap.Round)
        } else {
            drawLine(
                Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFC7D8F0)), start = apex, end = leftFoot),
                apex, leftFoot, strokeWidth = leg, cap = StrokeCap.Round,
            )
            drawLine(
                Brush.linearGradient(listOf(Color(0xFF3FA9FF), Color(0xFF0C56D8)), start = apex, end = rightFoot),
                apex, rightFoot, strokeWidth = leg, cap = StrokeCap.Round,
            )
        }
        // Rising arrow: a cubic shaft through the A and a triangular head pointing up-right.
        val arrowBrush = if (shape == LogoShape.None) {
            Brush.linearGradient(listOf(accentColor, accentColor), start = Offset(x(34f), y(77f)), end = Offset(x(78f), y(34f)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF38C6FF), Color(0xFF1E8FFF)), start = Offset(x(34f), y(77f)), end = Offset(x(78f), y(34f)))
        }
        val shaftPath = Path().apply {
            moveTo(x(34f), y(77f))
            cubicTo(x(42f), y(72f), x(48f), y(66f), x(54f), y(60f))
            cubicTo(x(60f), y(54f), x(64f), y(50f), x(68.5f), y(45f))
        }
        drawPath(shaftPath, arrowBrush, style = Stroke(shaft, cap = StrokeCap.Round))
        val head = Path().apply {
            moveTo(x(78f), y(34f))
            lineTo(x(73.4f), y(49.2f))
            lineTo(x(62.8f), y(38.6f))
            close()
        }
        drawPath(head, arrowBrush)
    }
}

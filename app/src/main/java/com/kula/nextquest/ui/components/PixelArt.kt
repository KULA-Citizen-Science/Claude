package com.kula.nextquest.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kula.nextquest.ui.theme.Retro

// A one-eyed blob "quest guide": cute + nerdy. '.' transparent.
// O = outline/dark, B = body, W = eye white, P = pupil, M = mouth.
private val MASCOT = listOf(
    "....OOOO....",
    "..OO....OO..",
    ".O..BBBB..O.",
    ".OBBBBBBBBO.",
    "OBBBBBBBBBBO",
    "OBBWWWWWWBBO",
    "OBBWPPPPWBBO",
    "OBBWWWWWWBBO",
    "OBBBBBBBBBBO",
    ".OBBMMMMBBO.",
    ".OOBBBBBBOO.",
    "...OO..OO...",
)

@Composable
fun Mascot(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    // Gentle idle bob so the little guy feels alive.
    val transition = rememberInfiniteTransition(label = "bob")
    val bob by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bob",
    )
    Canvas(modifier = modifier.size(size).offset(y = bob.dp)) {
        val cols = MASCOT.first().length
        val rows = MASCOT.size
        val cell = kotlin.math.min(this.size.width / cols, this.size.height / rows)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val color = when (MASCOT[r][c]) {
                    'O', 'P', 'M' -> Retro.Ink
                    'B' -> Retro.Cyan
                    'W' -> Retro.Cream
                    else -> null
                } ?: continue
                drawRect(
                    color = color,
                    topLeft = Offset(c * cell, r * cell),
                    // +0.6 to overlap neighbours and avoid hairline seams.
                    size = Size(cell + 0.6f, cell + 0.6f),
                )
            }
        }
    }
}

/**
 * A small hand-drawn glyph per archetype [icon] hint (see Archetypes.icon). Bold simple shapes,
 * on-theme without needing image assets. [carve] is the surrounding fill, used to notch shapes
 * like the moon.
 */
@Composable
fun PixelIcon(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = Retro.Cream,
    carve: Color = Retro.PanelFillAlt,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.10f
        when (icon) {
            "form" -> {
                drawRect(color, topLeft = Offset(s * 0.22f, s * 0.12f), size = Size(s * 0.56f, s * 0.76f), style = Stroke(stroke))
                for (i in 0..2) {
                    val y = s * (0.32f + i * 0.18f)
                    drawLine(color, Offset(s * 0.33f, y), Offset(s * 0.67f, y), strokeWidth = stroke * 0.8f)
                }
            }
            "envelope" -> {
                drawRect(color, topLeft = Offset(s * 0.14f, s * 0.26f), size = Size(s * 0.72f, s * 0.48f), style = Stroke(stroke))
                drawLine(color, Offset(s * 0.14f, s * 0.26f), Offset(s * 0.50f, s * 0.54f), strokeWidth = stroke * 0.8f)
                drawLine(color, Offset(s * 0.86f, s * 0.26f), Offset(s * 0.50f, s * 0.54f), strokeWidth = stroke * 0.8f)
            }
            "clock" -> {
                drawCircle(color, radius = s * 0.34f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(stroke))
                drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.26f), strokeWidth = stroke)
                drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.66f, s * 0.56f), strokeWidth = stroke)
            }
            "mountain" -> {
                val p = Path().apply {
                    moveTo(s * 0.12f, s * 0.80f); lineTo(s * 0.44f, s * 0.24f)
                    lineTo(s * 0.62f, s * 0.52f); lineTo(s * 0.74f, s * 0.36f)
                    lineTo(s * 0.90f, s * 0.80f); close()
                }
                drawPath(p, color)
            }
            "broom" -> {
                drawLine(color, Offset(s * 0.68f, s * 0.16f), Offset(s * 0.40f, s * 0.60f), strokeWidth = stroke)
                val bristles = Path().apply {
                    moveTo(s * 0.28f, s * 0.60f); lineTo(s * 0.52f, s * 0.60f)
                    lineTo(s * 0.60f, s * 0.86f); lineTo(s * 0.20f, s * 0.86f); close()
                }
                drawPath(bristles, color)
            }
            "moon" -> {
                drawCircle(color, radius = s * 0.32f, center = Offset(s * 0.46f, s * 0.5f))
                drawCircle(carve, radius = s * 0.28f, center = Offset(s * 0.60f, s * 0.42f))
            }
            "bag" -> {
                val body = Path().apply {
                    moveTo(s * 0.28f, s * 0.42f); lineTo(s * 0.72f, s * 0.42f)
                    lineTo(s * 0.80f, s * 0.84f); lineTo(s * 0.20f, s * 0.84f); close()
                }
                drawPath(body, color)
                drawArc(
                    color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(s * 0.36f, s * 0.20f), size = Size(s * 0.28f, s * 0.28f),
                    style = Stroke(stroke),
                )
            }
            "question" -> {
                drawArc(
                    color = color, startAngle = -210f, sweepAngle = 255f, useCenter = false,
                    topLeft = Offset(s * 0.28f, s * 0.12f), size = Size(s * 0.44f, s * 0.40f),
                    style = Stroke(stroke),
                )
                drawLine(color, Offset(s * 0.5f, s * 0.48f), Offset(s * 0.5f, s * 0.66f), strokeWidth = stroke)
                drawCircle(color, radius = stroke * 0.7f, center = Offset(s * 0.5f, s * 0.84f))
            }
            else -> drawCircle(color, radius = s * 0.30f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(stroke))
        }
    }
}

package com.kula.nextquest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kula.nextquest.domain.EvidenceTier
import com.kula.nextquest.domain.Framework
import com.kula.nextquest.ui.theme.Retro

/**
 * A chunky double-bevelled VGA-style frame drawn behind the content: a black outer frame, a fill,
 * then light (top/left) and dark (bottom/right) edges. Flipping [raised] inverts the edges for a
 * pressed/inset look.
 */
fun Modifier.pixelBevel(
    fill: Color,
    raised: Boolean = true,
    bevel: Dp = 3.dp,
    frame: Dp = 2.dp,
): Modifier = drawBehind {
    val f = frame.toPx()
    val b = bevel.toPx()
    val w = size.width
    val h = size.height
    drawRect(Retro.Ink)
    drawRect(fill, topLeft = Offset(f, f), size = Size(w - 2 * f, h - 2 * f))
    val light = if (raised) Retro.BevelLight else Retro.BevelDark
    val dark = if (raised) Retro.BevelDark else Retro.BevelLight
    drawRect(light, topLeft = Offset(f, f), size = Size(w - 2 * f, b))
    drawRect(light, topLeft = Offset(f, f), size = Size(b, h - 2 * f))
    drawRect(dark, topLeft = Offset(f, h - f - b), size = Size(w - 2 * f, b))
    drawRect(dark, topLeft = Offset(w - f - b, f), size = Size(b, h - 2 * f))
}

/** A subtle CRT scanline wash over whatever it wraps. Kept faint so it reads as charm, not noise. */
fun Modifier.scanlines(): Modifier = drawWithContent {
    drawContent()
    val gap = 3.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color.Black.copy(alpha = 0.10f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += gap
    }
}

/** True/false toggling on a timer, for blinking cursors and 90s sparkle. */
@Composable
fun blinkVisible(periodMs: Int = 900): Boolean {
    val transition = rememberInfiniteTransition(label = "blink")
    val v by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "blink",
    )
    return v < 0.5f
}

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    fill: Color = Retro.PanelFill,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .pixelBevel(fill)
            .padding(6.dp)
            .padding(padding),
        content = content,
    )
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Retro.PanelFillAlt,
    textColor: Color = Retro.Cream,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .offset(y = if (pressed) 2.dp else 0.dp)
            .pixelBevel(fill = fill, raised = !pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(6.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Retro.BodyBold, color = textColor, textAlign = TextAlign.Center)
    }
}

/** The tiny framework tag shown on a reading card: an evidence marker + the framework name. */
@Composable
fun FrameworkChip(framework: Framework, modifier: Modifier = Modifier) {
    val markerColor = when (framework.tier) {
        EvidenceTier.PEER_REVIEWED -> Retro.Lime
        EvidenceTier.CLINICAL_HEURISTIC -> Retro.Gold
    }
    Row(
        modifier = modifier
            .pixelBevel(Retro.PanelFillAlt)
            .padding(5.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(framework.tier.marker, style = Retro.Label, color = markerColor)
        Spacer(Modifier.width(6.dp))
        Text(framework.tag.uppercase(), style = Retro.Label, color = Retro.Cream)
    }
}

/** A pixel checkbox row for the custom-activity toggles. */
@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .pixelBevel(if (checked) Retro.Lime else Retro.PanelFill, raised = !checked),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("X", style = Retro.BodyBold, color = Retro.Ink)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = Retro.Body, color = Retro.Cream)
    }
}

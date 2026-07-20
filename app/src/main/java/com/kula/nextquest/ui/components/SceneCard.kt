package com.kula.nextquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kula.nextquest.domain.art.SceneArt
import com.kula.nextquest.domain.art.Sprite
import com.kula.nextquest.ui.theme.Retro
import kotlin.random.Random

/** Maps a sprite palette character to its colour; null = transparent. */
private fun spriteColor(ch: Char): Color? = when (ch) {
    'K' -> Retro.Ink
    'W' -> Retro.Cream
    'D' -> Retro.Danger
    'G' -> Retro.Gold
    'C' -> Retro.Cyan
    'M' -> Retro.Magenta
    'L' -> Retro.Lime
    'P' -> Retro.PanelFillAlt
    'S' -> Retro.PanelFill
    else -> null
}

/** Draws a [Sprite] scaled to fit its bounds, centred, with chunky square pixels. */
@Composable
fun PixelSprite(sprite: Sprite, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = kotlin.math.min(size.width / sprite.width, size.height / sprite.height)
        val xOff = (size.width - cell * sprite.width) / 2f
        val yOff = (size.height - cell * sprite.height) / 2f
        sprite.rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                spriteColor(ch)?.let {
                    // +0.6 overlap avoids hairline seams between cells.
                    drawRect(it, Offset(xOff + c * cell, yOff + r * cell), Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}

/** Per-archetype sky tint, so each quest's scene reads as its own "room". */
private fun skyFor(icon: String): Color = when (icon) {
    "form" -> Color(0xFF33285A)      // violet office dusk
    "envelope" -> Color(0xFF432B5E)  // plum
    "clock" -> Color(0xFF23305C)     // waiting-room blue
    "mountain" -> Color(0xFF2C3A63)  // steel-blue expedition
    "broom" -> Color(0xFF3D2E4C)     // mauve household
    "moon" -> Color(0xFF1B1638)      // deep night
    "bag" -> Color(0xFF234A4F)       // teal street
    else -> Retro.PanelFill          // custom quest
}

/**
 * The reading's LucasArts-style scene: a quest-tinted starfield "room" with the quest's prop on
 * the wall, the mascot facing the trap's symbolic sprite, and the escape emblem sitting in an
 * inventory slot ("USE"). Star placement is seeded by (quest, trap), so every combination is its
 * own unique picture.
 */
@Composable
fun SceneCard(icon: String, frictionId: String, modifier: Modifier = Modifier) {
    val sky = skyFor(icon)
    val trap = remember(frictionId) { SceneArt.trapFor(frictionId) }
    val emblem = remember(frictionId) { SceneArt.emblemFor(frictionId) }
    val starSeed = remember(icon, frictionId) { (icon + frictionId).hashCode() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .pixelBevel(fill = sky, raised = false)
            .padding(6.dp),
    ) {
        // Starfield + ground, seeded per (quest, trap) combination.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rng = Random(starSeed)
            repeat(22) {
                val x = rng.nextFloat() * size.width
                val y = rng.nextFloat() * size.height * 0.62f
                val big = rng.nextFloat() < 0.2f
                drawRect(
                    color = Retro.Cream.copy(alpha = if (big) 0.8f else 0.35f),
                    topLeft = Offset(x, y),
                    size = Size(if (big) 5f else 3f, if (big) 5f else 3f),
                )
            }
            val groundTop = size.height * 0.78f
            drawRect(Retro.Ink, topLeft = Offset(0f, groundTop), size = Size(size.width, 3f))
            drawRect(
                Retro.BevelDark,
                topLeft = Offset(0f, groundTop + 3f),
                size = Size(size.width, size.height - groundTop - 3f),
            )
        }

        // The quest's prop hanging faint in the background, like set dressing.
        PixelIcon(
            icon = icon,
            size = 64.dp,
            color = Retro.Cream,
            carve = sky,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 14.dp)
                .alpha(0.28f),
        )

        // Our hero, considering the obstacle.
        Mascot(
            size = 62.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 16.dp),
        )

        // The trap itself, centre stage.
        PixelSprite(
            sprite = trap,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 30.dp)
                .size(width = 120.dp, height = 104.dp),
        )

        // Inventory slot with the escape item.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("USE:", style = Retro.Small, color = Retro.Gold)
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(52.dp)
                    .pixelBevel(fill = Retro.BevelDark, raised = false)
                    .padding(8.dp),
            ) {
                PixelSprite(sprite = emblem, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

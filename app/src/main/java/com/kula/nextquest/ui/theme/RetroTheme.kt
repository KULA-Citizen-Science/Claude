package com.kula.nextquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The cozy-VGA / LucasArts palette and type. Everything is drawn by hand in Compose, so this is
 * mostly a small bag of colours + monospace text styles rather than a full Material theme.
 *
 * Font note: we deliberately use the platform monospace family (no bundled binary). Dropping a
 * true OFL pixel font — e.g. Press Start 2P or Silkscreen — into res/font and pointing
 * [questFont] at it is a pure, optional upgrade.
 */
object Retro {
    val Background = Color(0xFF2B2140) // deep aubergine night sky (matches colors.xml)
    val PanelFill = Color(0xFF3B2E63)
    val PanelFillAlt = Color(0xFF4A3A7A)
    val BevelLight = Color(0xFF7A68C0)
    val BevelDark = Color(0xFF160F2C)
    val Ink = Color(0xFF120C24) // near-black outline

    val Cream = Color(0xFFF4E9D0) // primary text
    val CreamDim = Color(0xFFB9AECB)

    val Gold = Color(0xFFFFC247)
    val Cyan = Color(0xFF49D6E0)
    val Magenta = Color(0xFFE85AAD)
    val Lime = Color(0xFF9BE24A)
    val Danger = Color(0xFFFF6B5E) // the "trap" accent

    val questFont = FontFamily.Monospace

    val Title = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 2.sp)
    val Heading = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp)
    val Body = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val BodyBold = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
    val Small = TextStyle(fontFamily = questFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 1.sp)
}

@Composable
fun NextQuestTheme(content: @Composable () -> Unit) {
    // Force a fixed dark scheme regardless of the system setting — the retro palette is constant.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Retro.Gold,
            background = Retro.Background,
            surface = Retro.PanelFill,
            onBackground = Retro.Cream,
            onSurface = Retro.Cream,
        ),
        content = content,
    )
}

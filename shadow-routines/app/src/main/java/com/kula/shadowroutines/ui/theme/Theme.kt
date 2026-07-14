package com.kula.shadowroutines.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// A calm, low-stimulation palette — gentle "tiny dopamine hits" rather than loud gamification.
private val Plum = Color(0xFF4B3F72)
private val PlumLight = Color(0xFF7A6BA8)
private val Apricot = Color(0xFFE8A87C)
private val Sage = Color(0xFF6Fae8f)

private val LightColors = lightColorScheme(
    primary = Plum,
    secondary = Sage,
    tertiary = Apricot,
)

private val DarkColors = darkColorScheme(
    primary = PlumLight,
    secondary = Sage,
    tertiary = Apricot,
)

@Composable
fun ShadowRoutinesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

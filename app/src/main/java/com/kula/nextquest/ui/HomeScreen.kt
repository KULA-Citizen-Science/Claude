package com.kula.nextquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kula.nextquest.ui.components.Mascot
import com.kula.nextquest.ui.components.PixelButton
import com.kula.nextquest.ui.components.blinkVisible
import com.kula.nextquest.ui.theme.Retro

@Composable
fun HomeScreen(onStart: () -> Unit) {
    val blink = blinkVisible()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (blink) "***" else "   ", style = Retro.Heading, color = Retro.Magenta)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", style = Retro.Title, color = Retro.Gold)
                Text("QUEST", style = Retro.Title, color = Retro.Cyan)
            }
            Spacer(Modifier.width(10.dp))
            Text(if (blink) "***" else "   ", style = Retro.Heading, color = Retro.Magenta)
        }

        Spacer(Modifier.height(28.dp))
        Mascot(size = 108.dp)
        Spacer(Modifier.height(28.dp))

        Text(
            "Pick your next thing. I'll flag where it'll trip you up — and one move past it.",
            style = Retro.Body,
            color = Retro.CreamDim,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
        PixelButton(
            text = ">>  WHAT'S MY NEXT QUEST?",
            onClick = onStart,
            fill = Retro.Gold,
            textColor = Retro.Ink,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "A nudge tool, not medical advice.",
            style = Retro.Small,
            color = Retro.CreamDim,
            textAlign = TextAlign.Center,
        )
    }
}

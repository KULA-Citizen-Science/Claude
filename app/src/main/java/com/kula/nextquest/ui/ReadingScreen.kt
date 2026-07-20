package com.kula.nextquest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kula.nextquest.domain.Activity
import com.kula.nextquest.domain.EvidenceTier
import com.kula.nextquest.domain.FrictionEngine
import com.kula.nextquest.ui.components.FrameworkChip
import com.kula.nextquest.ui.components.PixelButton
import com.kula.nextquest.ui.components.PixelPanel
import com.kula.nextquest.ui.components.SceneCard
import com.kula.nextquest.ui.theme.Retro

@Composable
fun ReadingScreen(activity: Activity, icon: String, onNewQuest: () -> Unit) {
    val points = remember(activity) { FrictionEngine.analyze(activity) }
    var index by remember(activity) { mutableIntStateOf(0) }
    val fp = points[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        PixelButton("< NEW QUEST", onClick = onNewQuest)
        Spacer(Modifier.height(18.dp))

        Text(activity.name.uppercase(), style = Retro.Heading, color = Retro.Gold)
        Text(
            "Trap ${index + 1} of ${points.size}",
            style = Retro.Small,
            color = Retro.CreamDim,
        )
        Spacer(Modifier.height(16.dp))

        SceneCard(icon = icon, frictionId = fp.id)
        Spacer(Modifier.height(16.dp))

        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Text("!  THE TRAP", style = Retro.Label, color = Retro.Danger)
            Spacer(Modifier.height(8.dp))
            Text(fp.trap, style = Retro.BodyBold, color = Retro.Cream)

            Spacer(Modifier.height(14.dp))
            FrameworkChip(fp.framework)

            Spacer(Modifier.height(16.dp))
            Text(">  THE ESCAPE", style = Retro.Label, color = Retro.Lime)
            Spacer(Modifier.height(8.dp))
            Text(fp.strategy, style = Retro.Body, color = Retro.Cream)
        }

        if (points.size > 1) {
            Spacer(Modifier.height(16.dp))
            PixelButton(
                text = "NEXT TRAP >",
                onClick = { index = (index + 1) % points.size },
                fill = Retro.Cyan,
                textColor = Retro.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        TierLegend()
    }
}

/** Tiny key explaining the evidence marker shown on the framework chip. */
@Composable
private fun TierLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Swatch(Retro.Lime)
        Spacer(Modifier.width(6.dp))
        Text(EvidenceTier.PEER_REVIEWED.marker + " peer-reviewed", style = Retro.Small, color = Retro.CreamDim)
        Spacer(Modifier.width(16.dp))
        Swatch(Retro.Gold)
        Spacer(Modifier.width(6.dp))
        Text(EvidenceTier.CLINICAL_HEURISTIC.marker + " heuristic", style = Retro.Small, color = Retro.CreamDim)
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(modifier = Modifier.size(10.dp).drawBehind { drawRect(color) })
}

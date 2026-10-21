package com.kula.nextquest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kula.nextquest.domain.Activity
import com.kula.nextquest.domain.Archetypes
import com.kula.nextquest.domain.Deadline
import com.kula.nextquest.domain.Duration
import com.kula.nextquest.domain.Interest
import com.kula.nextquest.domain.Structure
import com.kula.nextquest.ui.components.PixelButton
import com.kula.nextquest.ui.components.PixelIcon
import com.kula.nextquest.ui.components.PixelPanel
import com.kula.nextquest.ui.components.ToggleRow
import com.kula.nextquest.ui.components.pixelBevel
import com.kula.nextquest.ui.theme.Retro

@Composable
fun PickerScreen(onPick: (Activity, String) -> Unit, onBack: () -> Unit) {
    var showCustom by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        PixelButton("< BACK", onClick = onBack)
        Spacer(Modifier.height(18.dp))
        Text("CHOOSE YOUR QUEST", style = Retro.Heading, color = Retro.Gold)
        Spacer(Modifier.height(16.dp))

        Archetypes.all.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { arch ->
                    ArchetypeCard(
                        name = arch.activity.name,
                        icon = arch.icon,
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(arch.activity, arch.icon) },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(6.dp))
        PixelButton(
            text = if (showCustom) "OTHER... (below)" else "OTHER...  (describe it)",
            onClick = { showCustom = !showCustom },
            fill = Retro.Magenta,
            textColor = Retro.Ink,
            modifier = Modifier.fillMaxWidth(),
        )

        if (showCustom) {
            Spacer(Modifier.height(16.dp))
            CustomPanel(onRead = onPick)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ArchetypeCard(
    name: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .offset(y = if (pressed) 2.dp else 0.dp)
            .pixelBevel(fill = Retro.PanelFill, raised = !pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(6.dp)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelIcon(icon = icon, size = 40.dp, color = Retro.Cyan, carve = Retro.PanelFill)
        Spacer(Modifier.height(10.dp))
        Text(name.uppercase(), style = Retro.Small, color = Retro.Cream, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CustomPanel(onRead: (Activity, String) -> Unit) {
    var boring by remember { mutableStateOf(false) }
    var hardDeadline by remember { mutableStateOf(false) }
    var unclear by remember { mutableStateOf(false) }
    var longUnknown by remember { mutableStateOf(false) }
    var manySteps by remember { mutableStateOf(false) }
    var leaveHouse by remember { mutableStateOf(false) }
    var dread by remember { mutableStateOf(false) }

    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Text("DESCRIBE IT", style = Retro.Label, color = Retro.Gold)
        Spacer(Modifier.height(8.dp))
        ToggleRow("It bores me", boring) { boring = it }
        ToggleRow("Fixed time / hard deadline", hardDeadline) { hardDeadline = it }
        ToggleRow("Fuzzy — unclear what 'done' is", unclear) { unclear = it }
        ToggleRow("Long or unknown length", longUnknown) { longUnknown = it }
        ToggleRow("Lots of little steps", manySteps) { manySteps = it }
        ToggleRow("I have to leave the house", leaveHouse) { leaveHouse = it }
        ToggleRow("Thinking about it brings dread", dread) { dread = it }
        Spacer(Modifier.height(14.dp))
        PixelButton(
            text = "READ MY QUEST",
            onClick = {
                onRead(
                    Activity(
                        name = "Your Quest",
                        interest = if (boring) Interest.BORING else Interest.NEUTRAL,
                        deadline = if (hardDeadline) Deadline.HARD else Deadline.NONE,
                        structure = if (unclear) Structure.OPEN else Structure.DEFINED,
                        duration = if (longUnknown) Duration.LONG_OR_UNKNOWN else Duration.MEDIUM,
                        multiStep = manySteps,
                        requiresLeavingHome = leaveHouse,
                        hasDread = dread,
                    ),
                    "question",
                )
            },
            fill = Retro.Gold,
            textColor = Retro.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

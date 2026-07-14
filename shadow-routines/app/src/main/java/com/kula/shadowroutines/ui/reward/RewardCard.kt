package com.kula.shadowroutines.ui.reward

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kula.shadowroutines.ui.RewardUiState

/**
 * The full-screen reward shown on the back face of the flip. Tapping anywhere flips back.
 *
 * Layout: task title at the top, the quote prominently in the middle, and author/source plus a
 * favorite toggle at the bottom.
 */
@Composable
fun RewardCard(
    state: RewardUiState,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Tap anywhere to return; no ripple, so the whole card feels like one surface.
                .clickable(interactionSource = interaction, indication = null, onClick = onDismiss)
                .padding(32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "You just completed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = state.taskTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )

                QuoteBlock(state)

                FooterBlock(state, onToggleFavorite)
            }
        }
    }
}

@Composable
private fun QuoteBlock(state: RewardUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 24.dp),
    ) {
        Text(
            text = "Never forget:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        val quote = state.quote
        if (quote == null) {
            Text(
                text = "Add quotes to your corpus to unlock rewards.",
                style = MaterialTheme.typography.titleLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = quote.text,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FooterBlock(state: RewardUiState, onToggleFavorite: () -> Unit) {
    val quote = state.quote
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val attribution = listOfNotNull(quote?.author, quote?.source).joinToString(" · ")
        if (attribution.isNotBlank()) {
            Text(
                text = attribution,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        if (quote != null) {
            IconButton(onClick = onToggleFavorite) {
                if (quote.isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = "Add to favorites",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Text(
            text = "Tap anywhere to continue",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        )
    }
}

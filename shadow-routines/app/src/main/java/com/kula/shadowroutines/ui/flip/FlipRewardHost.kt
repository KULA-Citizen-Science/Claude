package com.kula.shadowroutines.ui.flip

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.kula.shadowroutines.ui.RewardUiState
import com.kula.shadowroutines.ui.reward.RewardCard

private const val FLIP_DURATION_MS = 600

/**
 * Hosts the whole app on the front face and the reward card on the back, flipping the screen
 * around its horizontal axis when [reward] appears or is dismissed.
 *
 * Both faces are conceptually one surface (not separate nav destinations) so the flip can show
 * one rotating away as the other rotates in. The selected quote is chosen upstream by the
 * QuoteEngine — this composable is pure presentation, which keeps the animation logic simple:
 * the only decision here is which face to show at a given angle.
 *
 * [displayed] retains the last reward while flipping *back* to 0°, so the back face still has
 * content to render during the return animation after [reward] has already gone null.
 */
@Composable
fun FlipRewardHost(
    reward: RewardUiState?,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val showBack = reward != null

    var displayed by remember { mutableStateOf(reward) }
    LaunchedEffect(reward) {
        if (reward != null) displayed = reward
    }

    val rotation by animateFloatAsState(
        targetValue = if (showBack) 180f else 0f,
        animationSpec = tween(durationMillis = FLIP_DURATION_MS, easing = FastOutSlowInEasing),
        finishedListener = { end -> if (end == 0f) displayed = null },
        label = "flipRotation",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 16f * density // reduce perspective distortion mid-flip
            },
    ) {
        if (rotation <= 90f) {
            // Front face: the live app.
            content()
        } else {
            // Back face: counter-rotate 180° so the card isn't upside-down/mirrored.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationX = 180f },
            ) {
                displayed?.let { state ->
                    RewardCard(
                        state = state,
                        onDismiss = onDismiss,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}

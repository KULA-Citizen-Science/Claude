package com.kula.anagram.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.kula.anagram.core.LetterTile
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TileSize = 52.dp
private val TileSpacing = 8.dp

/**
 * The interactive board of letter tiles. Each tile can be dragged; while a drag is in flight the
 * tile floats under the finger, the other letters glide aside to open a gap, and when the finger
 * passes a neighbour's centre the dragged letter slots into that position — so the word being spelled
 * updates live as you rearrange. Releasing drops the letter into its current gap.
 *
 * Layout is a [FlowRow], so long inputs simply wrap onto more lines instead of scrolling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterTilesBoard(
    tiles: List<LetterTile>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val halfTilePx = with(density) { TileSize.toPx() } / 2f
    // Move to a neighbour once the finger is meaningfully closer to it than to the current slot.
    val hysteresisPx = with(density) { TileSize.toPx() } * 0.45f

    // Board-space centre of every tile, keyed by tile id, refreshed as the layout settles.
    val centers = remember { mutableStateMapOf<Int, Offset>() }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var floatCenter by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TileSpacing),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TileSpacing),
        ) {
            tiles.forEach { tile ->
                androidx.compose.runtime.key(tile.id) {
                    val isDragging = tile.id == draggingId
                    LetterCell(
                        char = tile.char,
                        placeholder = isDragging,
                        modifier = Modifier
                            .animatePlacement()
                            .onGloballyPositioned { coords ->
                                centers[tile.id] = coords.positionInParent() +
                                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                            }
                            .pointerInput(tile.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = tile.id
                                        floatCenter = centers[tile.id] ?: Offset.Zero
                                    },
                                    onDragEnd = { draggingId = null },
                                    onDragCancel = { draggingId = null },
                                ) { change, dragAmount ->
                                    change.consume()
                                    floatCenter += dragAmount
                                    maybeReorder(tiles, tile.id, floatCenter, centers, hysteresisPx, onMove)
                                }
                            },
                    )
                }
            }
        }

        // The floating copy of the letter being dragged, pinned under the finger and drawn on top.
        val active = draggingId
        if (active != null) {
            val tile = tiles.firstOrNull { it.id == active }
            if (tile != null) {
                LetterCell(
                    char = tile.char,
                    placeholder = false,
                    lifted = true,
                    modifier = Modifier.offset {
                        IntOffset(
                            (floatCenter.x - halfTilePx).roundToInt(),
                            (floatCenter.y - halfTilePx).roundToInt(),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Moves the dragged tile onto the neighbouring slot its finger has crossed into. Fires [onMove] only
 * when the finger is closer to another tile's centre than to the dragged tile's own slot by more than
 * [hysteresisPx] — the margin stops the letters from flickering back and forth on the boundary.
 */
private fun maybeReorder(
    tiles: List<LetterTile>,
    draggingId: Int,
    finger: Offset,
    centers: Map<Int, Offset>,
    hysteresisPx: Float,
    onMove: (from: Int, to: Int) -> Unit,
) {
    val fromIndex = tiles.indexOfFirst { it.id == draggingId }
    if (fromIndex < 0) return
    val homeCenter = centers[draggingId] ?: return
    val homeDistance = (homeCenter - finger).getDistance()

    var bestIndex = -1
    var bestDistance = Float.MAX_VALUE
    tiles.forEachIndexed { index, tile ->
        if (tile.id == draggingId) return@forEachIndexed
        val c = centers[tile.id] ?: return@forEachIndexed
        val d = (c - finger).getDistance()
        if (d < bestDistance) {
            bestDistance = d
            bestIndex = index
        }
    }
    if (bestIndex >= 0 && bestDistance + hysteresisPx < homeDistance) {
        onMove(fromIndex, bestIndex)
    }
}

@Composable
private fun LetterCell(
    char: Char,
    placeholder: Boolean,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .size(TileSize)
            .then(if (lifted) Modifier.graphicsLayer(scaleX = 1.1f, scaleY = 1.1f) else Modifier)
            .alpha(if (placeholder) 0.25f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = if (placeholder) colors.surfaceVariant else colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        tonalElevation = if (lifted) 8.dp else 1.dp,
        shadowElevation = if (lifted) 8.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!placeholder) {
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Animates a child from its previous placement to its new one whenever the layout reorders, so tiles
 * slide aside smoothly instead of jumping. Adapted from the AOSP Compose animation samples.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, androidx.compose.animation.core.AnimationVector2D>?>(null)
    }
    this
        .onPlaced { coordinates ->
            targetOffset = coordinates.positionInParent().round()
        }
        .offset {
            val anim = animatable
                ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
            anim.value - targetOffset
        }
}

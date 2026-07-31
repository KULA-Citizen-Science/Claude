package com.kula.anagram.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.kula.anagram.R
import com.kula.anagram.core.Cell
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The two boards a cell can live on. */
enum class Zone { WERKBANK, ABLAGE }

private val TileSize = 52.dp
private val TileSpacing = 8.dp
private val ZoneMinHeight = 72.dp

/**
 * The interactive two-board area: a **Werkbank** (workbench) where the anagram is assembled and an
 * **Ablageboard** (tray) where letters can be parked. A cell can be dragged freely — within a board
 * to reorder, or across to the other board — and while a drag is in flight the cell floats under the
 * finger, the others glide aside to open a gap, and the boards update live. Tapping a letter sends it
 * to the other board; tapping a space removes it. "+ Leerzeichen" adds a space to the workbench.
 *
 * All hit-testing works in window coordinates (see [positionInWindow]); that keeps the cell centres,
 * the zone rectangles and the dragged finger in one coordinate space without depending on the order
 * in which the layout nodes happen to be positioned.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkBoard(
    werkbank: List<Cell>,
    ablage: List<Cell.Letter>,
    onDrop: (cellId: Int, zone: Zone, index: Int) -> Unit,
    onTap: (cellId: Int) -> Unit,
    onAddSpace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val halfTilePx = with(density) { TileSize.toPx() } / 2f

    // Window-space geometry, shared across both zones.
    val centers = remember { mutableStateMapOf<Int, Offset>() }
    val zoneRects = remember { mutableStateMapOf<Zone, Rect>() }
    var rootWindowPos by remember { mutableStateOf(Offset.Zero) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var floatCenter by remember { mutableStateOf(Offset.Zero) }

    fun cellsOf(zone: Zone): List<Cell> = if (zone == Zone.WERKBANK) werkbank else ablage

    fun currentPosition(id: Int): Pair<Zone, Int>? {
        werkbank.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.WERKBANK to it }
        ablage.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.ABLAGE to it }
        return null
    }

    // Where would the dragged cell drop, given the finger position? Returns the target board and the
    // insertion index into that board's list (with the dragged cell removed).
    fun targetFor(id: Int, isSpace: Boolean, finger: Offset): Pair<Zone, Int>? {
        val zone = zoneRects.entries
            .firstOrNull { (z, rect) -> (!isSpace || z == Zone.WERKBANK) && rect.contains(finger) }
            ?.key ?: return null
        val others = cellsOf(zone).filter { it.id != id }
        if (others.isEmpty()) return zone to 0
        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        var bestAfter = false
        others.forEachIndexed { index, cell ->
            val c = centers[cell.id] ?: return@forEachIndexed
            val d = (c - finger).getDistanceSquared()
            if (d < bestDistance) {
                bestDistance = d
                bestIndex = index
                bestAfter = finger.x > c.x
            }
        }
        return zone to (bestIndex + if (bestAfter) 1 else 0)
    }

    fun onDragMove(id: Int) {
        val isSpace = werkbank.firstOrNull { it.id == id } is Cell.Space
        val target = targetFor(id, isSpace, floatCenter) ?: return
        if (currentPosition(id) == target) return
        onDrop(id, target.first, target.second)
    }

    Box(modifier = modifier.onGloballyPositioned { rootWindowPos = it.positionInWindow() }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ZoneSection(
                title = stringResource(R.string.zone_werkbank),
                subtitle = stringResource(R.string.zone_werkbank_hint),
                cells = werkbank,
                draggingId = draggingId,
                onZoneBounds = { zoneRects[Zone.WERKBANK] = it },
                onCellCenter = { id, c -> centers[id] = c },
                onTap = onTap,
                onDragStart = { id -> draggingId = id; floatCenter = centers[id] ?: Offset.Zero },
                onDrag = { id, delta -> floatCenter += delta; onDragMove(id) },
                onDragEnd = { draggingId = null },
                trailing = {
                    OutlinedButton(onClick = onAddSpace) {
                        Text(stringResource(R.string.action_add_space))
                    }
                },
            )
            ZoneSection(
                title = stringResource(R.string.zone_ablage),
                subtitle = stringResource(R.string.zone_ablage_hint),
                cells = ablage,
                draggingId = draggingId,
                onZoneBounds = { zoneRects[Zone.ABLAGE] = it },
                onCellCenter = { id, c -> centers[id] = c },
                onTap = onTap,
                onDragStart = { id -> draggingId = id; floatCenter = centers[id] ?: Offset.Zero },
                onDrag = { id, delta -> floatCenter += delta; onDragMove(id) },
                onDragEnd = { draggingId = null },
                trailing = null,
            )
        }

        // The floating copy of the cell being dragged, pinned under the finger and drawn on top.
        // floatCenter is in window space; subtract the board's own window origin to place it locally.
        val active = draggingId
        if (active != null) {
            val cell = werkbank.firstOrNull { it.id == active } ?: ablage.firstOrNull { it.id == active }
            if (cell != null) {
                CellView(
                    cell = cell,
                    placeholder = false,
                    lifted = true,
                    modifier = Modifier.offset {
                        IntOffset(
                            (floatCenter.x - rootWindowPos.x - halfTilePx).roundToInt(),
                            (floatCenter.y - rootWindowPos.y - halfTilePx).roundToInt(),
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneSection(
    title: String,
    subtitle: String,
    cells: List<Cell>,
    draggingId: Int?,
    onZoneBounds: (Rect) -> Unit,
    onCellCenter: (Int, Offset) -> Unit,
    onTap: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Int, Offset) -> Unit,
    onDragEnd: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ZoneMinHeight)
                    .padding(10.dp)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            onZoneBounds(
                                Rect(
                                    coords.positionInWindow(),
                                    Size(coords.size.width.toFloat(), coords.size.height.toFloat()),
                                ),
                            )
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(TileSpacing),
                verticalArrangement = Arrangement.spacedBy(TileSpacing),
            ) {
                cells.forEach { cell ->
                    key(cell.id) {
                        CellView(
                            cell = cell,
                            placeholder = cell.id == draggingId,
                            modifier = Modifier
                                .animatePlacement()
                                .onGloballyPositioned { coords ->
                                    if (coords.isAttached) {
                                        onCellCenter(
                                            cell.id,
                                            coords.positionInWindow() +
                                                Offset(coords.size.width / 2f, coords.size.height / 2f),
                                        )
                                    }
                                }
                                // One combined gesture handler: a quick press without movement is a
                                // tap; crossing the touch slop starts a drag. Kept in a single
                                // pointerInput so the tap detector can't swallow the drag's down event.
                                .pointerInput(cell.id) {
                                    val touchSlop = viewConfiguration.touchSlop
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var dragging = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                if (!dragging) onTap(cell.id)
                                                break
                                            }
                                            if (!dragging) {
                                                if ((change.position - down.position).getDistance() > touchSlop) {
                                                    dragging = true
                                                    onDragStart(cell.id)
                                                    change.consume()
                                                }
                                            } else {
                                                onDrag(cell.id, change.positionChange())
                                                change.consume()
                                            }
                                        }
                                        if (dragging) onDragEnd()
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CellView(
    cell: Cell,
    placeholder: Boolean,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val isSpace = cell is Cell.Space
    Surface(
        modifier = modifier
            .size(TileSize)
            .then(if (lifted) Modifier.graphicsLayer(scaleX = 1.1f, scaleY = 1.1f) else Modifier)
            .alpha(if (placeholder) 0.25f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = when {
            placeholder -> colors.surface
            isSpace -> colors.tertiaryContainer
            else -> colors.primaryContainer
        },
        contentColor = if (isSpace) colors.onTertiaryContainer else colors.onPrimaryContainer,
        tonalElevation = if (lifted) 8.dp else 2.dp,
        shadowElevation = if (lifted) 8.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!placeholder) {
                when (cell) {
                    is Cell.Letter -> Text(
                        text = cell.char.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    is Cell.Space -> Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.onTertiaryContainer),
                    )
                }
            }
        }
    }
}

/**
 * Animates a child from its previous placement to its new one whenever the layout reorders, so cells
 * slide aside smoothly instead of jumping. Adapted from the AOSP Compose animation samples.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { coordinates -> targetOffset = coordinates.positionInParent().round() }
        .offset {
            val anim = animatable
                ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch { anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow)) }
            }
            anim.value - targetOffset
        }
}

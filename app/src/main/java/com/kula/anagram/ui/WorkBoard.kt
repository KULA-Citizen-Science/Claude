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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
 * **Ablageboard** (tray) where letters can be parked. Drag a cell to lift it — it floats under the
 * finger — and release it where it should go: within a board to reorder, or over the other board to
 * move it there. Tapping a letter sends it to the other board; tapping a space removes it.
 * "+ Leerzeichen" adds a space to the workbench.
 *
 * Hit-testing works in window coordinates ([positionInWindow]) so cell centres, zone rectangles and
 * the dragged finger all live in one space regardless of layout ordering. The current board contents
 * are read through [rememberUpdatedState] because a `pointerInput` block, once started, is never
 * restarted for a stable key and would otherwise capture a stale, first-frame copy of the lists.
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

    // Always-current views of the lists, safe to read from the long-lived gesture coroutines.
    val werkbankState = rememberUpdatedState(werkbank)
    val ablageState = rememberUpdatedState(ablage)

    // Window-space geometry, shared across both zones.
    val centers = remember { mutableStateMapOf<Int, Offset>() }
    var rootWindowPos by remember { mutableStateOf(Offset.Zero) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var floatCenter by remember { mutableStateOf(Offset.Zero) }
    // Temporary on-screen diagnostics so drag behaviour can be inspected on a real device.
    var debug by remember { mutableStateOf("bereit") }

    fun cellsOf(zone: Zone): List<Cell> =
        if (zone == Zone.WERKBANK) werkbankState.value else ablageState.value

    fun currentPosition(id: Int): Pair<Zone, Int>? {
        werkbankState.value.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.WERKBANK to it }
        ablageState.value.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.ABLAGE to it }
        return null
    }

    // Where would the dragged cell drop, given the finger position? Returns the target board and the
    // insertion index into that board's list (with the dragged cell removed).
    //
    // This deliberately uses *only* the measured cell centres — verified correct against a screen
    // recording — and never the zone rectangles, which were observed to be stale for the workbench and
    // made every in-board drop resolve to "no target". An empty board is reached by checking whether
    // the finger sits clearly beyond the occupied board's cells, the workbench always being above the
    // tray. As a result a drop resolves to a real slot whenever any other cell exists.
    fun targetFor(id: Int, finger: Offset): Pair<Zone, Int>? {
        val isSpace = werkbankState.value.firstOrNull { it.id == id } is Cell.Space
        val werkCells = werkbankState.value.filter { it.id != id }
        val trayCells = if (isSpace) emptyList() else ablageState.value.filter { it.id != id }
        val threshold = with(density) { TileSize.toPx() }

        fun nearestIn(zone: Zone, cells: List<Cell>): Pair<Zone, Int>? {
            var bestIndex = -1
            var bestDistance = Float.MAX_VALUE
            var bestAfter = false
            cells.forEachIndexed { index, cell ->
                val c = centers[cell.id] ?: return@forEachIndexed
                val d = (c - finger).getDistanceSquared()
                if (d < bestDistance) {
                    bestDistance = d
                    bestIndex = index
                    bestAfter = finger.x > c.x
                }
            }
            return if (bestIndex < 0) null else zone to (bestIndex + if (bestAfter) 1 else 0)
        }

        // Only the tray holds cells: allow reaching the empty workbench by dropping above them.
        if (werkCells.isEmpty()) {
            val topOfTray = trayCells.mapNotNull { centers[it.id]?.y }.minOrNull() ?: return Zone.WERKBANK to 0
            if (finger.y < topOfTray - threshold) return Zone.WERKBANK to 0
            return nearestIn(Zone.ABLAGE, trayCells) ?: (Zone.WERKBANK to 0)
        }

        // Only the workbench holds cells: allow reaching the empty tray by dropping below them.
        if (trayCells.isEmpty()) {
            if (!isSpace) {
                val bottomOfWerkbank = werkCells.mapNotNull { centers[it.id]?.y }.maxOrNull()
                if (bottomOfWerkbank != null && finger.y > bottomOfWerkbank + threshold) {
                    return Zone.ABLAGE to 0
                }
            }
            return nearestIn(Zone.WERKBANK, werkCells)
        }

        // Both boards hold cells: whichever cell is closest wins.
        val a = nearestIn(Zone.WERKBANK, werkCells)
        val b = nearestIn(Zone.ABLAGE, trayCells)
        val da = werkCells.mapNotNull { centers[it.id] }.minOfOrNull { (it - finger).getDistanceSquared() }
        val db = trayCells.mapNotNull { centers[it.id] }.minOfOrNull { (it - finger).getDistanceSquared() }
        return when {
            da == null -> b
            db == null -> a
            da <= db -> a
            else -> b
        }
    }

    // Called once, when the finger lifts: place the dragged cell where it was released.
    fun finishDrag(id: Int) {
        val cur = currentPosition(id)
        val target = targetFor(id, floatCenter)
        debug = "B8 drop id=$id f=${floatCenter.x.toInt()},${floatCenter.y.toInt()} " +
            "cur=$cur tgt=$target kacheln=${centers.size}"
        if (target != null && cur != target) {
            onDrop(id, target.first, target.second)
        }
        draggingId = null
    }

    val startDrag: (Int) -> Unit = { id ->
        draggingId = id
        floatCenter = centers[id] ?: Offset.Zero
        debug = "B8 ziehe id=$id start=${floatCenter.x.toInt()},${floatCenter.y.toInt()}"
    }
    val dragBy: (Offset) -> Unit = { delta -> floatCenter += delta }

    Box(modifier = modifier.onGloballyPositioned { rootWindowPos = it.positionInWindow() }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ZoneSection(
                title = stringResource(R.string.zone_werkbank),
                subtitle = stringResource(R.string.zone_werkbank_hint),
                cells = werkbank,
                draggingId = draggingId,
                onCellCenter = { id, c -> centers[id] = c },
                onTap = onTap,
                onDragStart = startDrag,
                onDrag = dragBy,
                onDragEnd = { id -> finishDrag(id) },
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
                onCellCenter = { id, c -> centers[id] = c },
                onTap = onTap,
                onDragStart = startDrag,
                onDrag = dragBy,
                onDragEnd = { id -> finishDrag(id) },
                trailing = null,
            )
            Text(
                text = "🐞 $debug",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
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
    onCellCenter: (Int, Offset) -> Unit,
    onTap: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Int) -> Unit,
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
                    .padding(10.dp),
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
                                                onDrag(change.positionChange())
                                                change.consume()
                                            }
                                        }
                                        if (dragging) onDragEnd(cell.id)
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
 * slide into place smoothly instead of jumping. Adapted from the AOSP Compose animation samples.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<IntOffset?>(null) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { coordinates ->
            val placed = coordinates.positionInParent().round()
            target = placed
            val anim = animatable
            when {
                anim == null -> animatable = Animatable(placed, IntOffset.VectorConverter)
                anim.targetValue != placed ->
                    scope.launch { anim.animateTo(placed, spring(stiffness = Spring.StiffnessMediumLow)) }
            }
        }
        .offset {
            val anim = animatable
            val t = target
            if (anim == null || t == null) IntOffset.Zero else anim.value - t
        }
}

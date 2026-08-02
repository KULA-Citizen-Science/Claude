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
import androidx.compose.runtime.LaunchedEffect
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
 * move it there. Tapping a letter sends it to the other board. A space lives on the workbench only and
 * moves there exactly like a letter — it can never be dragged off the board, tapping one does nothing,
 * and it is removed with a deliberate long press.
 *
 * Geometry is measured in window coordinates ([positionInWindow]) so cell centres and the dragged
 * finger share one space regardless of layout ordering. The dragged position is recomputed absolutely
 * on every pointer event — cell centre plus the pointer's travel inside its own cell — rather than by
 * summing per-frame deltas, which cannot drift out of step with the layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkBoard(
    werkbank: List<Cell>,
    ablage: List<Cell.Letter>,
    onDrop: (cellId: Int, zone: Zone, index: Int) -> Unit,
    onTap: (cellId: Int) -> Unit,
    onLongPress: (cellId: Int) -> Unit,
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
    // Where inside its own cell the finger first landed, so the tile keeps the grab point under it.
    var grabPoint by remember { mutableStateOf(Offset.Zero) }
    // Temporary on-screen diagnostics so drag behaviour can be inspected on a real device.
    var debug by remember { mutableStateOf("bereit") }

    // Drop measurements for cells that no longer exist, so a stale entry can never be consulted.
    val liveIds = remember(werkbank, ablage) { (werkbank.map { it.id } + ablage.map { it.id }).toSet() }
    LaunchedEffect(liveIds) { centers.keys.retainAll(liveIds) }

    fun currentPosition(id: Int): Pair<Zone, Int>? {
        werkbankState.value.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.WERKBANK to it }
        ablageState.value.indexOfFirst { it.id == id }.let { if (it >= 0) return Zone.ABLAGE to it }
        return null
    }

    // Renders the workbench compactly (letters as themselves, a space as "_") so the diagnostics show
    // whether the model actually changed between one gesture and the next.
    fun renderWerkbank(): String = buildString {
        werkbankState.value.forEach {
            when (it) {
                is Cell.Letter -> append(it.char)
                is Cell.Space -> append('_')
            }
        }
    }

    // Where would the dragged cell drop, given the position of the floating tile? Returns the target
    // board and the insertion index into that board's list (with the dragged cell removed).
    //
    // This uses *only* the measured cell centres — verified correct against a screen recording — and
    // never zone rectangles, which were observed to be stale for the workbench and made every in-board
    // drop resolve to "no target".
    //
    // The board is chosen purely by height: the tray sits on top as the supply of letters, the
    // workbench below it as the assembly area, so everything above the boundary between the two rows
    // of cells is the tray and everything below is the workbench. Only then does the horizontal
    // position pick the slot within that board. Deciding by "nearest cell across both boards" instead
    // made a lone letter on the other board act as a magnet that swallowed drops.
    fun targetFor(id: Int, point: Offset): Pair<Zone, Int>? {
        val isSpace = werkbankState.value.firstOrNull { it.id == id } is Cell.Space
        val werkCells = werkbankState.value.filter { it.id != id }
        val trayCells = ablageState.value.filter { it.id != id }
        val tile = with(density) { TileSize.toPx() }

        val lowestTray = trayCells.mapNotNull { centers[it.id]?.y }.maxOrNull()
        val highestWerkbank = werkCells.mapNotNull { centers[it.id]?.y }.minOrNull()

        // Horizontal line separating the two boards. With only one board occupied, the empty one still
        // needs to be reachable, so the boundary sits a deliberate distance beyond the occupied cells.
        val boundary = when {
            lowestTray != null && highestWerkbank != null -> (lowestTray + highestWerkbank) / 2f
            lowestTray != null -> lowestTray + 1.5f * tile
            highestWerkbank != null -> highestWerkbank - 1.5f * tile
            else -> return null
        }

        // A space belongs to the workbench and simply stays there: an upward drag repositions it among
        // the letters instead of leaving the board, so it moves exactly like a letter and can never be
        // lost by dragging. Removing one is a deliberate long press.
        val zone = when {
            isSpace -> Zone.WERKBANK
            point.y <= boundary -> Zone.ABLAGE
            else -> Zone.WERKBANK
        }
        val cells = if (zone == Zone.WERKBANK) werkCells else trayCells
        if (cells.isEmpty()) return zone to 0

        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        var bestAfter = false
        cells.forEachIndexed { index, cell ->
            val c = centers[cell.id] ?: return@forEachIndexed
            val d = (c - point).getDistanceSquared()
            if (d < bestDistance) {
                bestDistance = d
                bestIndex = index
                bestAfter = point.x > c.x
            }
        }
        return zone to (bestIndex + if (bestAfter) 1 else 0)
    }

    // Called once, when the finger lifts: place the dragged cell where it was released. If the drag was
    // never actually armed (no measured centre for this cell), do nothing rather than act on a position
    // left over from an earlier drag.
    fun finishDrag(id: Int) {
        if (draggingId != id) {
            draggingId = null
            return
        }
        val cur = currentPosition(id)
        val target = targetFor(id, floatCenter)
        val kind = if (werkbankState.value.firstOrNull { it.id == id } is Cell.Space) "SP" else "L"
        debug = "B13 drop id=$id $kind cur=$cur tgt=$target wb=${renderWerkbank()}"
        if (target != null && cur != target) {
            onDrop(id, target.first, target.second)
        }
        draggingId = null
    }

    // Every gesture is logged, so a screenshot shows whether a drag armed at all or the press was
    // classified as something else.
    val tap: (Int) -> Unit = { id ->
        debug = "B13 TIPP id=$id"
        onTap(id)
    }
    val longPress: (Int) -> Unit = { id ->
        debug = "B13 LANG id=$id"
        onLongPress(id)
    }

    // The cell hands in its own measured centre, so arming a drag never depends on the shared table of
    // measurements having an entry for it yet.
    val startDrag: (Int, Offset, Offset?) -> Unit = { id, local, own ->
        val center = own ?: centers[id]
        if (center != null) {
            draggingId = id
            grabPoint = local
            floatCenter = center
            debug = "B13 ziehe id=$id wb=${renderWerkbank()}"
        } else {
            debug = "B13 ziehe id=$id ABBRUCH: keine Position gemessen"
        }
    }

    // Absolute, drift-free: the tile's centre is its home centre plus how far the finger has travelled
    // inside the cell it was grabbed in.
    val dragTo: (Int, Offset, Offset?) -> Unit = { id, local, own ->
        (own ?: centers[id])?.let { floatCenter = it + (local - grabPoint) }
    }

    Box(modifier = modifier.onGloballyPositioned { rootWindowPos = it.positionInWindow() }) {
        // Tray on top as the supply of letters, workbench below it as the assembly area.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ZoneSection(
                title = stringResource(R.string.zone_ablage),
                subtitle = stringResource(R.string.zone_ablage_hint),
                cells = ablage,
                draggingId = draggingId,
                onCellCenter = { id, c -> centers[id] = c },
                onTap = tap,
                onLongPress = longPress,
                onDragStart = startDrag,
                onDragTo = dragTo,
                onDragEnd = { id -> finishDrag(id) },
                trailing = null,
            )
            ZoneSection(
                title = stringResource(R.string.zone_werkbank),
                subtitle = stringResource(R.string.zone_werkbank_hint),
                cells = werkbank,
                draggingId = draggingId,
                onCellCenter = { id, c -> centers[id] = c },
                onTap = tap,
                onLongPress = longPress,
                onDragStart = startDrag,
                onDragTo = dragTo,
                onDragEnd = { id -> finishDrag(id) },
                trailing = {
                    OutlinedButton(onClick = onAddSpace) {
                        Text(stringResource(R.string.action_add_space))
                    }
                },
            )
            if (werkbank.any { it is Cell.Space }) {
                Text(
                    text = stringResource(R.string.space_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    onLongPress: (Int) -> Unit,
    onDragStart: (Int, Offset, Offset?) -> Unit,
    onDragTo: (Int, Offset, Offset?) -> Unit,
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
                        // The cell keeps its own measurement as well, so a gesture on a freshly inserted
                        // cell never has to wait for the shared table to catch up.
                        var ownCenter by remember { mutableStateOf<Offset?>(null) }
                        CellView(
                            cell = cell,
                            placeholder = cell.id == draggingId,
                            modifier = Modifier
                                .animatePlacement()
                                .onGloballyPositioned { coords ->
                                    if (coords.isAttached) {
                                        val c = coords.positionInWindow() +
                                            Offset(coords.size.width / 2f, coords.size.height / 2f)
                                        ownCenter = c
                                        onCellCenter(cell.id, c)
                                    }
                                }
                                // One combined gesture handler: a press that never crosses the touch
                                // slop is a tap, anything beyond it is a drag. Both live in a single
                                // pointerInput so a tap detector cannot swallow the drag's down event.
                                .pointerInput(cell.id) {
                                    val touchSlop = viewConfiguration.touchSlop
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var dragging = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                // Classified on release, so holding can never race a drag.
                                                if (!dragging) {
                                                    val held = change.uptimeMillis - down.uptimeMillis
                                                    if (held >= viewConfiguration.longPressTimeoutMillis) {
                                                        onLongPress(cell.id)
                                                    } else {
                                                        onTap(cell.id)
                                                    }
                                                }
                                                break
                                            }
                                            if (!dragging) {
                                                if ((change.position - down.position).getDistance() > touchSlop) {
                                                    dragging = true
                                                    onDragStart(cell.id, down.position, ownCenter)
                                                    onDragTo(cell.id, change.position, ownCenter)
                                                    change.consume()
                                                }
                                            } else {
                                                onDragTo(cell.id, change.position, ownCenter)
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

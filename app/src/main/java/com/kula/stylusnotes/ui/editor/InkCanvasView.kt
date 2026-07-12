package com.kula.stylusnotes.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.color.resolve
import com.kula.stylusnotes.core.ink.StrokeBounds
import com.kula.stylusnotes.core.ink.StrokeOp
import com.kula.stylusnotes.core.ink.UndoStack
import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.core.model.StrokePoint
import com.kula.stylusnotes.ink.StrokeRenderer
import java.util.UUID
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Freehand ink surface driven by [onTouchEvent] rather than Compose gestures, because only the
 * raw View touch pipeline exposes stylus tool-type, button state, pressure/tilt, and
 * requestUnbufferedDispatch() for a full-resolution stroke.
 */
class InkCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokes = mutableListOf<Stroke>()
    private val undoStack = UndoStack()

    private var activeStylusPointerId = -1
    private var activePoints = mutableListOf<StrokePoint>()
    private var isErasing = false
    private var isPassiveContact = false

    // Infinite-canvas view transform: screen = doc * zoom + pan. Strokes are stored in document
    // coordinates, so panning/zooming never rewrites ink.
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var pendingFitView = false

    // Two-finger pan/pinch-zoom gesture state (screen coordinates).
    private var navPointerId1 = -1
    private var navPointerId2 = -1
    private var navLastX1 = 0f
    private var navLastY1 = 0f
    private var navLastX2 = 0f
    private var navLastY2 = 0f

    private val backgroundPaint = Paint()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val scratchPath = Path()

    private val density = resources.displayMetrics.density

    // Ruled guide lines and the zoom chip are on-screen aids only; NoteExporter never draws
    // them, so exports stay clean white pages for handwriting recognition.
    private val ruleLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f // hairline: stays one pixel on screen at any canvas scale
    }
    private val zoomChipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99222222.toInt()
    }
    private val zoomChipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 12f * density
    }
    private val zoomChipRect = RectF()

    var canvasBackground: CanvasBackground = CanvasBackground.WHITE
        set(value) {
            field = value
            backgroundPaint.color = value.argb
            invalidate()
        }

    var currentInkColor: InkColor = InkColor.Adaptive

    /** Base width (document px) for newly drawn strokes; existing strokes keep their own. */
    var currentStrokeWidthPx: Float = BASE_STROKE_WIDTH_PX

    /**
     * When set (from the toolbar eraser toggle), accepted pointers erase instead of draw. This is
     * the only way to erase with a passive stylus, which has no barrel button or eraser tip.
     */
    var manualEraseMode: Boolean = false

    var onStrokesChanged: ((List<Stroke>) -> Unit)? = null
    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    init {
        backgroundPaint.color = canvasBackground.argb
    }

    fun loadNote(noteStrokes: List<Stroke>, background: CanvasBackground) {
        strokes.clear()
        strokes.addAll(noteStrokes)
        undoStack.clear()
        canvasBackground = background
        notifyUndoRedoState()
        resetView()
        invalidate()
    }

    /**
     * Zoom-to-fit all ink (centered, never above 1:1), or reset to the origin at 1:1 for an
     * empty note. Deferred to first layout if the view isn't measured yet.
     */
    fun resetView() {
        if (width == 0 || height == 0) {
            pendingFitView = true
            return
        }
        if (strokes.isEmpty()) {
            zoom = 1f
            panX = 0f
            panY = 0f
            invalidate()
            return
        }
        val bounds = StrokeBounds.contentBounds(strokes, FIT_PADDING_PX, 1f, 1f)
        zoom = minOf(width / bounds.width, height / bounds.height, 1f).coerceAtLeast(MIN_ZOOM)
        panX = (width - bounds.width * zoom) / 2f - bounds.left * zoom
        panY = (height - bounds.height * zoom) / 2f - bounds.top * zoom
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pendingFitView) {
            pendingFitView = false
            resetView()
        }
    }

    fun getStrokes(): List<Stroke> = strokes.toList()

    fun undo() {
        when (val op = undoStack.undo() ?: return) {
            is StrokeOp.Add -> strokes.removeAll { it.id == op.stroke.id }
            is StrokeOp.Remove -> strokes.add(op.index.coerceIn(0, strokes.size), op.stroke)
        }
        notifyUndoRedoState()
        notifyStrokesChanged()
        invalidate()
    }

    fun redo() {
        when (val op = undoStack.redo() ?: return) {
            is StrokeOp.Add -> strokes.add(op.stroke)
            is StrokeOp.Remove -> strokes.removeAll { it.id == op.stroke.id }
        }
        notifyUndoRedoState()
        notifyStrokesChanged()
        invalidate()
    }

    fun clearAll() {
        if (strokes.isEmpty()) return
        strokes.clear()
        undoStack.clear()
        notifyUndoRedoState()
        notifyStrokesChanged()
        invalidate()
    }

    private fun notifyUndoRedoState() {
        onUndoRedoStateChanged?.invoke(undoStack.canUndo, undoStack.canRedo)
    }

    private fun notifyStrokesChanged() {
        onStrokesChanged?.invoke(strokes.toList())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val toolType = event.getToolType(actionIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val isActivePenTool = toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER
                val isSmallFingerContact = !isActivePenTool &&
                    toolType == MotionEvent.TOOL_TYPE_FINGER &&
                    contactMajorMm(event, actionIndex) <= PASSIVE_ACCEPT_MAX_CONTACT_MM

                if (activeStylusPointerId != -1) {
                    // A stroke is in progress. A second small contact while the stroke itself is
                    // a passive (finger-classified) one means the user planted two fingers to
                    // pan/zoom, not to write — discard the stroke and start navigating. With an
                    // active stylus the pen keeps priority, and everything else — palms
                    // included — is swallowed and ignored (palm rejection).
                    if (isPassiveContact && isSmallFingerContact) {
                        val firstPointerId = activeStylusPointerId
                        cancelActiveStroke()
                        startNavigation(event, firstPointerId, event.getPointerId(actionIndex))
                    }
                    return true
                }
                if (navPointerId1 != -1) return true // already navigating; extra pointers ignored

                if (!isActivePenTool) {
                    if (toolType != MotionEvent.TOOL_TYPE_FINGER) return false
                    // Passive-stylus support (e.g. Moto G Stylus 2024/2025, whose capacitive pen
                    // is reported as TOOL_TYPE_FINGER): tool type can't tell pen from palm, so
                    // fall back to contact size — a pen tip or fingertip is a small contact, a
                    // resting palm a large one. Large contacts are swallowed without drawing.
                    if (!isSmallFingerContact) return true
                }
                activeStylusPointerId = event.getPointerId(actionIndex)
                isPassiveContact = !isActivePenTool
                requestUnbufferedDispatch(event)
                isErasing = manualEraseMode ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER ||
                    (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                if (isErasing) {
                    eraseNear(toDocX(event.getX(actionIndex)), toDocY(event.getY(actionIndex)))
                } else {
                    activePoints = mutableListOf(pointFrom(event, actionIndex))
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (navPointerId1 != -1) {
                    handleNavigationMove(event)
                    return true
                }
                val pointerIndex = event.findPointerIndex(activeStylusPointerId)
                if (pointerIndex == -1) return true
                if (isPassiveContact && contactMajorMm(event, pointerIndex) > PASSIVE_CANCEL_CONTACT_MM) {
                    // The contact flattened out into a palm mid-stroke: it was never a pen tip.
                    // Discard the in-progress stroke instead of committing it.
                    cancelActiveStroke()
                    return true
                }
                if (isErasing) {
                    for (h in 0 until event.historySize) {
                        eraseNear(
                            toDocX(event.getHistoricalX(pointerIndex, h)),
                            toDocY(event.getHistoricalY(pointerIndex, h))
                        )
                    }
                    eraseNear(toDocX(event.getX(pointerIndex)), toDocY(event.getY(pointerIndex)))
                } else {
                    for (h in 0 until event.historySize) {
                        activePoints.add(historicalPointFrom(event, pointerIndex, h))
                    }
                    activePoints.add(pointFrom(event, pointerIndex))
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerId = event.getPointerId(actionIndex)
                if (pointerId == navPointerId1 || pointerId == navPointerId2 ||
                    (navPointerId1 != -1 && event.actionMasked == MotionEvent.ACTION_CANCEL)
                ) {
                    // End the pan/zoom gesture; a finger that stays down is ignored until lifted.
                    navPointerId1 = -1
                    navPointerId2 = -1
                    return true
                }
                if (pointerId != activeStylusPointerId) return true
                if (!isErasing && activePoints.isNotEmpty() && event.actionMasked != MotionEvent.ACTION_CANCEL) {
                    finishStroke()
                }
                activeStylusPointerId = -1
                activePoints = mutableListOf()
                isErasing = false
                isPassiveContact = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Major axis of the touch contact in millimetres, or 0 if the device doesn't report contact
     * size (returning 0 accepts the pointer — with no size data there is nothing to reject on).
     */
    private fun contactMajorMm(event: MotionEvent, pointerIndex: Int): Float {
        val majorPx = event.getTouchMajor(pointerIndex)
        if (majorPx <= 0f) return 0f
        return majorPx / (resources.displayMetrics.xdpi / MM_PER_INCH)
    }

    private fun toDocX(screenX: Float) = (screenX - panX) / zoom

    private fun toDocY(screenY: Float) = (screenY - panY) / zoom

    private fun cancelActiveStroke() {
        activeStylusPointerId = -1
        activePoints = mutableListOf()
        isErasing = false
        isPassiveContact = false
        invalidate()
    }

    private fun startNavigation(event: MotionEvent, pointerId1: Int, pointerId2: Int) {
        val index1 = event.findPointerIndex(pointerId1)
        val index2 = event.findPointerIndex(pointerId2)
        if (index1 == -1 || index2 == -1) return
        navPointerId1 = pointerId1
        navPointerId2 = pointerId2
        navLastX1 = event.getX(index1)
        navLastY1 = event.getY(index1)
        navLastX2 = event.getX(index2)
        navLastY2 = event.getY(index2)
    }

    private fun handleNavigationMove(event: MotionEvent) {
        val index1 = event.findPointerIndex(navPointerId1)
        val index2 = event.findPointerIndex(navPointerId2)
        if (index1 == -1 || index2 == -1) return
        val x1 = event.getX(index1)
        val y1 = event.getY(index1)
        val x2 = event.getX(index2)
        val y2 = event.getY(index2)

        val previousSpan = hypot((navLastX2 - navLastX1).toDouble(), (navLastY2 - navLastY1).toDouble()).toFloat()
        val currentSpan = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        val newZoom = if (previousSpan > 1e-3f) {
            (zoom * currentSpan / previousSpan).coerceIn(MIN_ZOOM, MAX_ZOOM)
        } else {
            zoom
        }
        // Keep the document point under the gesture focal point fixed while zooming, then apply
        // the focal point's own travel as a pan.
        val zoomRatio = newZoom / zoom
        val previousFocalX = (navLastX1 + navLastX2) / 2f
        val previousFocalY = (navLastY1 + navLastY2) / 2f
        val focalX = (x1 + x2) / 2f
        val focalY = (y1 + y2) / 2f
        panX = focalX - zoomRatio * (previousFocalX - panX)
        panY = focalY - zoomRatio * (previousFocalY - panY)
        zoom = newZoom

        navLastX1 = x1
        navLastY1 = y1
        navLastX2 = x2
        navLastY2 = y2
        invalidate()
    }

    private fun pointFrom(event: MotionEvent, pointerIndex: Int) = StrokePoint(
        x = toDocX(event.getX(pointerIndex)),
        y = toDocY(event.getY(pointerIndex)),
        pressure = event.getPressure(pointerIndex),
        tiltRadians = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
        orientationRadians = event.getOrientation(pointerIndex),
        timestampMs = event.eventTime
    )

    private fun historicalPointFrom(event: MotionEvent, pointerIndex: Int, historyIndex: Int) = StrokePoint(
        x = toDocX(event.getHistoricalX(pointerIndex, historyIndex)),
        y = toDocY(event.getHistoricalY(pointerIndex, historyIndex)),
        pressure = event.getHistoricalPressure(pointerIndex, historyIndex),
        tiltRadians = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historyIndex),
        orientationRadians = event.getHistoricalOrientation(pointerIndex, historyIndex),
        timestampMs = event.getHistoricalEventTime(historyIndex)
    )

    private fun finishStroke() {
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            color = currentInkColor,
            baseWidthPx = currentStrokeWidthPx,
            points = activePoints.toList()
        )
        strokes.add(stroke)
        undoStack.push(StrokeOp.Add(stroke))
        notifyUndoRedoState()
        notifyStrokesChanged()
    }

    /** [x]/[y] are document coordinates; the touch radius shrinks as the view zooms in. */
    private fun eraseNear(x: Float, y: Float) {
        val radius = ERASE_TOUCH_RADIUS_PX / zoom
        val index = strokes.indexOfLast { stroke ->
            stroke.points.any { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= radius }
        }
        if (index == -1) return
        val removed = strokes.removeAt(index)
        undoStack.push(StrokeOp.Remove(removed, index))
        notifyUndoRedoState()
        notifyStrokesChanged()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoom, zoom)
        drawRuleLines(canvas)
        for (stroke in strokes) {
            drawStroke(canvas, stroke.color.resolve(canvasBackground), stroke.baseWidthPx, stroke.points)
        }
        if (!isErasing && activePoints.isNotEmpty()) {
            drawStroke(canvas, currentInkColor.resolve(canvasBackground), currentStrokeWidthPx, activePoints)
        }
        canvas.restore()
        drawZoomIndicator(canvas)
    }

    /**
     * Notebook-style ruled lines at a fixed document-space interval: their on-screen spacing
     * stretches and shrinks with the zoom, which is what makes the current scale readable at a
     * glance. Drawn under the ink, only across the visible viewport.
     */
    private fun drawRuleLines(canvas: Canvas) {
        ruleLinePaint.color = if (canvasBackground == CanvasBackground.BLACK) {
            RULE_COLOR_ON_BLACK
        } else {
            RULE_COLOR_ON_WHITE
        }
        val docLeft = toDocX(0f)
        val docRight = toDocX(width.toFloat())
        val docBottom = toDocY(height.toFloat())
        var y = floor(toDocY(0f) / RULE_SPACING_PX) * RULE_SPACING_PX
        while (y <= docBottom) {
            canvas.drawLine(docLeft, y, docRight, y, ruleLinePaint)
            y += RULE_SPACING_PX
        }
    }

    /** Small "137%" chip in the canvas's top-left corner, drawn in screen space. */
    private fun drawZoomIndicator(canvas: Canvas) {
        val label = "${(zoom * 100).roundToInt()}%"
        val padding = 6f * density
        val margin = 12f * density
        val metrics = zoomChipTextPaint.fontMetrics
        zoomChipRect.set(
            margin,
            margin,
            margin + zoomChipTextPaint.measureText(label) + 2 * padding,
            margin + (metrics.descent - metrics.ascent) + 2 * padding
        )
        val cornerRadius = 8f * density
        canvas.drawRoundRect(zoomChipRect, cornerRadius, cornerRadius, zoomChipBackgroundPaint)
        canvas.drawText(label, zoomChipRect.left + padding, zoomChipRect.top + padding - metrics.ascent, zoomChipTextPaint)
    }

    private fun drawStroke(canvas: Canvas, color: Int, baseWidthPx: Float, points: List<StrokePoint>) {
        StrokeRenderer.draw(canvas, strokePaint, scratchPath, color, baseWidthPx, points)
    }

    companion object {
        private const val BASE_STROKE_WIDTH_PX = 6f
        private const val ERASE_TOUCH_RADIUS_PX = 24f
        private const val MM_PER_INCH = 25.4f

        // Contact-size palm rejection for passive styluses: a pen tip is ~2-4 mm and a fingertip
        // ~6-9 mm, while a resting palm is well past 10 mm. Accept below 8 mm, and cancel an
        // in-progress passive stroke if the contact grows past 11 mm (hysteresis so a normal
        // pen stroke isn't dropped by sensor noise).
        private const val PASSIVE_ACCEPT_MAX_CONTACT_MM = 8f
        private const val PASSIVE_CANCEL_CONTACT_MM = 11f

        private const val MIN_ZOOM = 0.2f
        private const val MAX_ZOOM = 8f
        private const val FIT_PADDING_PX = 48f

        // ~6-7 mm between ruled lines at 100% on a ~400 dpi phone: college-ruled territory.
        private const val RULE_SPACING_PX = 100f
        private const val RULE_COLOR_ON_WHITE = 0x1E000000
        private const val RULE_COLOR_ON_BLACK = 0x30FFFFFF
    }
}

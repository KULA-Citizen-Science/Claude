package com.kula.stylusnotes.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.color.resolve
import com.kula.stylusnotes.core.ink.StrokeOp
import com.kula.stylusnotes.core.ink.UndoStack
import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.core.model.StrokePoint
import com.kula.stylusnotes.ink.StrokeRenderer
import java.util.UUID
import kotlin.math.hypot

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

    private val backgroundPaint = Paint()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val scratchPath = Path()

    var canvasBackground: CanvasBackground = CanvasBackground.WHITE
        set(value) {
            field = value
            backgroundPaint.color = value.argb
            invalidate()
        }

    var currentInkColor: InkColor = InkColor.Adaptive

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
        invalidate()
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
                // Palm rejection: once a pen stroke is active, every other pointer
                // (a resting palm reported as TOOL_TYPE_FINGER) is swallowed and ignored.
                if (activeStylusPointerId != -1) return true
                val isActivePenTool = toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER
                if (!isActivePenTool) {
                    if (toolType != MotionEvent.TOOL_TYPE_FINGER) return false
                    // Passive-stylus support (e.g. Moto G Stylus 2024/2025, whose capacitive pen
                    // is reported as TOOL_TYPE_FINGER): tool type can't tell pen from palm, so
                    // fall back to contact size — a pen tip or fingertip is a small contact, a
                    // resting palm a large one. Large contacts are swallowed without drawing.
                    if (contactMajorMm(event, actionIndex) > PASSIVE_ACCEPT_MAX_CONTACT_MM) return true
                }
                activeStylusPointerId = event.getPointerId(actionIndex)
                isPassiveContact = !isActivePenTool
                requestUnbufferedDispatch(event)
                isErasing = manualEraseMode ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER ||
                    (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                if (isErasing) {
                    eraseNear(event.getX(actionIndex), event.getY(actionIndex))
                } else {
                    activePoints = mutableListOf(pointFrom(event, actionIndex))
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activeStylusPointerId)
                if (pointerIndex == -1) return true
                if (isPassiveContact && contactMajorMm(event, pointerIndex) > PASSIVE_CANCEL_CONTACT_MM) {
                    // The contact flattened out into a palm mid-stroke: it was never a pen tip.
                    // Discard the in-progress stroke instead of committing it.
                    activeStylusPointerId = -1
                    activePoints = mutableListOf()
                    isErasing = false
                    isPassiveContact = false
                    invalidate()
                    return true
                }
                if (isErasing) {
                    for (h in 0 until event.historySize) {
                        eraseNear(event.getHistoricalX(pointerIndex, h), event.getHistoricalY(pointerIndex, h))
                    }
                    eraseNear(event.getX(pointerIndex), event.getY(pointerIndex))
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
                if (event.getPointerId(actionIndex) != activeStylusPointerId) return true
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

    private fun pointFrom(event: MotionEvent, pointerIndex: Int) = StrokePoint(
        x = event.getX(pointerIndex),
        y = event.getY(pointerIndex),
        pressure = event.getPressure(pointerIndex),
        tiltRadians = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
        orientationRadians = event.getOrientation(pointerIndex),
        timestampMs = event.eventTime
    )

    private fun historicalPointFrom(event: MotionEvent, pointerIndex: Int, historyIndex: Int) = StrokePoint(
        x = event.getHistoricalX(pointerIndex, historyIndex),
        y = event.getHistoricalY(pointerIndex, historyIndex),
        pressure = event.getHistoricalPressure(pointerIndex, historyIndex),
        tiltRadians = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historyIndex),
        orientationRadians = event.getHistoricalOrientation(pointerIndex, historyIndex),
        timestampMs = event.getHistoricalEventTime(historyIndex)
    )

    private fun finishStroke() {
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            color = currentInkColor,
            baseWidthPx = BASE_STROKE_WIDTH_PX,
            points = activePoints.toList()
        )
        strokes.add(stroke)
        undoStack.push(StrokeOp.Add(stroke))
        notifyUndoRedoState()
        notifyStrokesChanged()
    }

    private fun eraseNear(x: Float, y: Float) {
        val index = strokes.indexOfLast { stroke ->
            stroke.points.any { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= ERASE_TOUCH_RADIUS_PX }
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
        for (stroke in strokes) {
            drawStroke(canvas, stroke.color.resolve(canvasBackground), stroke.baseWidthPx, stroke.points)
        }
        if (!isErasing && activePoints.isNotEmpty()) {
            drawStroke(canvas, currentInkColor.resolve(canvasBackground), BASE_STROKE_WIDTH_PX, activePoints)
        }
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
    }
}

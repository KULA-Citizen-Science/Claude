package com.kula.stylusnotes.ink

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.kula.stylusnotes.core.ink.PathSegment
import com.kula.stylusnotes.core.ink.PressureWidth
import com.kula.stylusnotes.core.ink.StrokeSmoothing
import com.kula.stylusnotes.core.model.StrokePoint

/**
 * Draws pressure-smoothed ink onto a [Canvas]: each smoothed segment gets its own short path
 * so stroke width can follow pressure. Shared by the live [InkCanvasView] and [NoteExporter]'s
 * offscreen bitmap rendering so on-screen and exported notes always look identical.
 */
object StrokeRenderer {
    fun draw(
        canvas: Canvas,
        paint: Paint,
        scratchPath: Path,
        color: Int,
        baseWidthPx: Float,
        points: List<StrokePoint>
    ) {
        if (points.isEmpty()) return
        paint.color = color
        var cursorX = points.first().x
        var cursorY = points.first().y
        for (smoothed in StrokeSmoothing.smoothWithPressure(points)) {
            val segment = smoothed.segment
            if (segment is PathSegment.MoveTo) {
                cursorX = segment.x
                cursorY = segment.y
                continue
            }
            scratchPath.reset()
            scratchPath.moveTo(cursorX, cursorY)
            when (segment) {
                is PathSegment.QuadTo -> {
                    scratchPath.quadTo(segment.controlX, segment.controlY, segment.endX, segment.endY)
                    cursorX = segment.endX
                    cursorY = segment.endY
                }
                is PathSegment.LineTo -> {
                    scratchPath.lineTo(segment.x, segment.y)
                    cursorX = segment.x
                    cursorY = segment.y
                }
                is PathSegment.MoveTo -> Unit
            }
            paint.strokeWidth = PressureWidth.widthForPressure(smoothed.pressure, baseWidthPx)
            canvas.drawPath(scratchPath, paint)
        }
    }
}

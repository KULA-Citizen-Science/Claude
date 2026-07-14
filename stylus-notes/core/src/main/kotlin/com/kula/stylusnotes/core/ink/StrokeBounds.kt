package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.model.Stroke
import kotlin.math.max
import kotlin.math.min

/** Axis-aligned box in document coordinates. */
data class ContentBounds(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * Bounding box of a note's ink in document coordinates, inflated by each stroke's maximum
 * rendered width and [paddingPx] on every side, so nothing gets clipped when the box is used
 * as an export canvas or a zoom-to-fit target on the infinite canvas.
 */
object StrokeBounds {

    /**
     * Returns the padded bounds of all points in [strokes], never smaller than
     * [minWidthPx] x [minHeightPx] (a note with no ink yields exactly that box at the origin;
     * undersized content keeps its left/top and is extended right/down to the minimum).
     */
    fun contentBounds(
        strokes: List<Stroke>,
        paddingPx: Float,
        minWidthPx: Float,
        minHeightPx: Float
    ): ContentBounds {
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        var hasPoints = false

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            hasPoints = true
            val halfWidth = PressureWidth.maxWidthFor(stroke.baseWidthPx) / 2f
            for (point in stroke.points) {
                left = min(left, point.x - halfWidth)
                top = min(top, point.y - halfWidth)
                right = max(right, point.x + halfWidth)
                bottom = max(bottom, point.y + halfWidth)
            }
        }

        if (!hasPoints) return ContentBounds(0f, 0f, minWidthPx, minHeightPx)

        left -= paddingPx
        top -= paddingPx
        right += paddingPx
        bottom += paddingPx
        return ContentBounds(
            left = left,
            top = top,
            width = max(right - left, minWidthPx),
            height = max(bottom - top, minHeightPx)
        )
    }
}

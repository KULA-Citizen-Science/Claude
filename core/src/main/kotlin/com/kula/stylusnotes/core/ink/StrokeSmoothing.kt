package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.model.StrokePoint

sealed class PathSegment {
    data class MoveTo(val x: Float, val y: Float) : PathSegment()
    data class QuadTo(val controlX: Float, val controlY: Float, val endX: Float, val endY: Float) : PathSegment()
    data class LineTo(val x: Float, val y: Float) : PathSegment()
}

/** A drawable segment paired with the pressure driving it, so renderers can vary stroke width. */
data class SmoothedSegment(val segment: PathSegment, val pressure: Float)

/** Renders raw touch samples as a quadratic-bezier-through-midpoints curve to smooth out jitter. */
object StrokeSmoothing {

    fun smooth(points: List<StrokePoint>): List<PathSegment> = smoothWithPressure(points).map { it.segment }

    fun smoothWithPressure(points: List<StrokePoint>): List<SmoothedSegment> {
        if (points.isEmpty()) return emptyList()
        val first = points.first()
        if (points.size == 1) {
            return listOf(
                SmoothedSegment(PathSegment.MoveTo(first.x, first.y), first.pressure),
                SmoothedSegment(PathSegment.LineTo(first.x, first.y), first.pressure)
            )
        }

        val segments = mutableListOf(SmoothedSegment(PathSegment.MoveTo(first.x, first.y), first.pressure))
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            segments += SmoothedSegment(PathSegment.QuadTo(current.x, current.y, midX, midY), current.pressure)
        }
        val last = points.last()
        segments += SmoothedSegment(PathSegment.LineTo(last.x, last.y), last.pressure)
        return segments
    }
}

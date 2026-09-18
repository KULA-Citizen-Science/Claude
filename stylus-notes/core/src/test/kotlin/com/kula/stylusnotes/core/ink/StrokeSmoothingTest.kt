package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeSmoothingTest {

    private fun point(x: Float, y: Float, pressure: Float = 0.5f) = StrokePoint(x, y, pressure, timestampMs = 0)

    @Test
    fun `empty input produces no segments`() {
        assertEquals(emptyList<PathSegment>(), StrokeSmoothing.smooth(emptyList()))
    }

    @Test
    fun `single point produces a move and a line to itself (a dot)`() {
        val segments = StrokeSmoothing.smooth(listOf(point(5f, 5f)))
        assertEquals(2, segments.size)
        assertTrue(segments[0] is PathSegment.MoveTo)
        assertTrue(segments[1] is PathSegment.LineTo)
    }

    @Test
    fun `two points produce a move and a line, no quad needed`() {
        val segments = StrokeSmoothing.smooth(listOf(point(0f, 0f), point(10f, 10f)))
        assertEquals(2, segments.size)
        assertTrue(segments[0] is PathSegment.MoveTo)
        val line = segments[1] as PathSegment.LineTo
        assertEquals(10f, line.x)
        assertEquals(10f, line.y)
    }

    @Test
    fun `interior points become quad-to segments through their midpoints`() {
        val points = listOf(point(0f, 0f), point(10f, 0f), point(20f, 0f), point(30f, 0f))
        val segments = StrokeSmoothing.smooth(points)
        // MoveTo + 2 interior QuadTo (for points[1], points[2]) + final LineTo
        assertEquals(4, segments.size)
        assertTrue(segments[0] is PathSegment.MoveTo)
        val firstQuad = segments[1] as PathSegment.QuadTo
        assertEquals(10f, firstQuad.controlX)
        assertEquals(15f, firstQuad.endX) // midpoint of (10,0)-(20,0) on x
        val secondQuad = segments[2] as PathSegment.QuadTo
        assertEquals(20f, secondQuad.controlX)
        assertEquals(25f, secondQuad.endX) // midpoint of (20,0)-(30,0) on x
        val line = segments[3] as PathSegment.LineTo
        assertEquals(30f, line.x)
    }

    @Test
    fun `smoothWithPressure carries each interior point's pressure onto its segment`() {
        val points = listOf(
            point(0f, 0f, pressure = 0.1f),
            point(10f, 0f, pressure = 0.9f),
            point(20f, 0f, pressure = 0.2f)
        )
        val segments = StrokeSmoothing.smoothWithPressure(points)
        assertEquals(0.1f, segments[0].pressure) // MoveTo carries the first point's pressure
        assertEquals(0.9f, segments[1].pressure) // QuadTo driven by the interior point
        assertEquals(0.2f, segments[2].pressure) // final LineTo carries the last point's pressure
        // smooth() must stay a pure projection of smoothWithPressure()
        assertEquals(segments.map { it.segment }, StrokeSmoothing.smooth(points))
    }

    @Test
    fun `pressure width scales monotonically and stays within bounds`() {
        val low = PressureWidth.widthForPressure(0f, baseWidthPx = 10f)
        val mid = PressureWidth.widthForPressure(0.5f, baseWidthPx = 10f)
        val high = PressureWidth.widthForPressure(1f, baseWidthPx = 10f)
        assertTrue(low < mid)
        assertTrue(mid < high)
        assertTrue(low >= 3.5f) // MIN_WIDTH_FACTOR * base
        assertTrue(high <= 14f) // MAX_WIDTH_FACTOR * base
    }

    @Test
    fun `pressure width clamps out-of-range pressure`() {
        val overPressure = PressureWidth.widthForPressure(2.5f, baseWidthPx = 10f)
        val underPressure = PressureWidth.widthForPressure(-1f, baseWidthPx = 10f)
        assertEquals(PressureWidth.widthForPressure(1f, 10f), overPressure)
        assertEquals(PressureWidth.widthForPressure(0f, 10f), underPressure)
    }
}

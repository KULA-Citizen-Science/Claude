package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.core.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeBoundsTest {

    private fun stroke(id: String, baseWidthPx: Float, vararg xy: Pair<Float, Float>) = Stroke(
        id = id,
        color = InkColor.Adaptive,
        baseWidthPx = baseWidthPx,
        points = xy.map { (x, y) -> StrokePoint(x, y, 0.5f, timestampMs = 0) }
    )

    @Test
    fun `no ink yields the minimum box at the origin`() {
        val bounds = StrokeBounds.contentBounds(emptyList(), paddingPx = 32f, minWidthPx = 100f, minHeightPx = 50f)
        assertEquals(ContentBounds(0f, 0f, 100f, 50f), bounds)
    }

    @Test
    fun `bounds cover the ink inflated by max stroke width and padding`() {
        val bounds = StrokeBounds.contentBounds(
            listOf(stroke("a", baseWidthPx = 10f, 100f to 200f, 300f to 250f)),
            paddingPx = 20f,
            minWidthPx = 0f,
            minHeightPx = 0f
        )
        val halfMaxWidth = PressureWidth.maxWidthFor(10f) / 2f
        assertEquals(100f - halfMaxWidth - 20f, bounds.left, 1e-4f)
        assertEquals(200f - halfMaxWidth - 20f, bounds.top, 1e-4f)
        assertEquals(200f + 2 * (halfMaxWidth + 20f), bounds.width, 1e-4f)
        assertEquals(50f + 2 * (halfMaxWidth + 20f), bounds.height, 1e-4f)
    }

    @Test
    fun `bounds span multiple strokes including negative coordinates`() {
        val bounds = StrokeBounds.contentBounds(
            listOf(
                stroke("a", baseWidthPx = 2f, -500f to -100f),
                stroke("b", baseWidthPx = 2f, 700f to 900f)
            ),
            paddingPx = 0f,
            minWidthPx = 0f,
            minHeightPx = 0f
        )
        val halfMaxWidth = PressureWidth.maxWidthFor(2f) / 2f
        assertEquals(-500f - halfMaxWidth, bounds.left, 1e-4f)
        assertEquals(-100f - halfMaxWidth, bounds.top, 1e-4f)
        assertEquals(1200f + 2 * halfMaxWidth, bounds.width, 1e-4f)
        assertEquals(1000f + 2 * halfMaxWidth, bounds.height, 1e-4f)
    }

    @Test
    fun `undersized content is extended to the minimum box without moving its origin`() {
        val bounds = StrokeBounds.contentBounds(
            listOf(stroke("a", baseWidthPx = 2f, 10f to 10f)),
            paddingPx = 1f,
            minWidthPx = 400f,
            minHeightPx = 300f
        )
        val halfMaxWidth = PressureWidth.maxWidthFor(2f) / 2f
        assertEquals(10f - halfMaxWidth - 1f, bounds.left, 1e-4f)
        assertEquals(10f - halfMaxWidth - 1f, bounds.top, 1e-4f)
        assertEquals(400f, bounds.width, 1e-4f)
        assertEquals(300f, bounds.height, 1e-4f)
    }

    @Test
    fun `strokes with no points are ignored`() {
        val empty = Stroke("e", InkColor.Adaptive, 6f, emptyList())
        val bounds = StrokeBounds.contentBounds(listOf(empty), paddingPx = 8f, minWidthPx = 64f, minHeightPx = 64f)
        assertEquals(ContentBounds(0f, 0f, 64f, 64f), bounds)
    }
}

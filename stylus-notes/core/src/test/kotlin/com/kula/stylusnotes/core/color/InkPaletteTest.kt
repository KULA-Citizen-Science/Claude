package com.kula.stylusnotes.core.color

import com.kula.stylusnotes.core.model.CanvasBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkPaletteTest {

    @Test
    fun `every accent color is legible on both backgrounds`() {
        for (color in InkPalette.accentColors) {
            val contrastOnWhite = Luminance.contrastRatio(color.argb, WHITE_ARGB)
            val contrastOnBlack = Luminance.contrastRatio(color.argb, BLACK_ARGB)
            assertTrue(
                "${color.label} contrast on white was $contrastOnWhite, need >= $MIN_CONTRAST_RATIO",
                contrastOnWhite >= MIN_CONTRAST_RATIO
            )
            assertTrue(
                "${color.label} contrast on black was $contrastOnBlack, need >= $MIN_CONTRAST_RATIO",
                contrastOnBlack >= MIN_CONTRAST_RATIO
            )
            assertTrue(InkPalette.isLegibleOnBothBackgrounds(color.argb))
        }
    }

    @Test
    fun `adaptive resolves to white on black background and black on white background`() {
        assertEquals(WHITE_ARGB, InkColor.Adaptive.resolve(CanvasBackground.BLACK))
        assertEquals(BLACK_ARGB, InkColor.Adaptive.resolve(CanvasBackground.WHITE))
    }

    @Test
    fun `fixed color resolves to itself regardless of background`() {
        val fixed = InkPalette.accentColors.first()
        assertEquals(fixed.argb, fixed.resolve(CanvasBackground.WHITE))
        assertEquals(fixed.argb, fixed.resolve(CanvasBackground.BLACK))
    }

    @Test
    fun `pure black and pure white themselves fail the both-backgrounds bar`() {
        assertTrue(!InkPalette.isLegibleOnBothBackgrounds(BLACK_ARGB))
        assertTrue(!InkPalette.isLegibleOnBothBackgrounds(WHITE_ARGB))
    }
}

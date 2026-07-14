package com.kula.stylusnotes.core.color

import com.kula.stylusnotes.core.model.CanvasBackground

const val WHITE_ARGB = 0xFFFFFFFF.toInt()
const val BLACK_ARGB = 0xFF000000.toInt()

/** WCAG "AA large text" threshold; used here as the bar for accent-color legibility. */
const val MIN_CONTRAST_RATIO = 3.0

sealed class InkColor {
    /** Always renders as the opposite of the current canvas background: maximum contrast. */
    data object Adaptive : InkColor()
    data class Fixed(val argb: Int, val label: String) : InkColor()
}

fun InkColor.resolve(background: CanvasBackground): Int = when (this) {
    InkColor.Adaptive -> if (background == CanvasBackground.BLACK) WHITE_ARGB else BLACK_ARGB
    is InkColor.Fixed -> argb
}

object InkPalette {
    /**
     * Accent colors, each tuned to sit in the relative-luminance band (~0.10-0.30) that
     * keeps contrast >= [MIN_CONTRAST_RATIO] against both pure black and pure white, so
     * a note stays legible (and HTR-readable) whichever canvas background is active.
     */
    val accentColors: List<InkColor.Fixed> = listOf(
        InkColor.Fixed(0xFFC62828.toInt(), "Crimson"),
        InkColor.Fixed(0xFFBF360C.toInt(), "Ember"),
        InkColor.Fixed(0xFF2E7D32.toInt(), "Forest"),
        InkColor.Fixed(0xFF00897B.toInt(), "Teal"),
        InkColor.Fixed(0xFF1A56DB.toInt(), "Royal Blue"),
        InkColor.Fixed(0xFF9C27B0.toInt(), "Violet"),
    )

    val all: List<InkColor> = listOf(InkColor.Adaptive) + accentColors

    /** True if [argb] is legible on both a pure-white and pure-black canvas. */
    fun isLegibleOnBothBackgrounds(argb: Int): Boolean =
        Luminance.contrastRatio(argb, WHITE_ARGB) >= MIN_CONTRAST_RATIO &&
            Luminance.contrastRatio(argb, BLACK_ARGB) >= MIN_CONTRAST_RATIO
}

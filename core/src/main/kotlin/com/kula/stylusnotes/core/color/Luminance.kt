package com.kula.stylusnotes.core.color

import kotlin.math.pow

/** WCAG 2.x relative luminance and contrast ratio for sRGB colors. */
object Luminance {

    fun relativeLuminance(argb: Int): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    fun contrastRatio(argbA: Int, argbB: Int): Double {
        val lighter = maxOf(relativeLuminance(argbA), relativeLuminance(argbB))
        val darker = minOf(relativeLuminance(argbA), relativeLuminance(argbB))
        return (lighter + 0.05) / (darker + 0.05)
    }
}

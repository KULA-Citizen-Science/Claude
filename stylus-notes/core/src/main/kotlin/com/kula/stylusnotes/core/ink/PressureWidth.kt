package com.kula.stylusnotes.core.ink

object PressureWidth {
    private const val MIN_WIDTH_FACTOR = 0.35f
    private const val MAX_WIDTH_FACTOR = 1.4f

    fun widthForPressure(pressure: Float, baseWidthPx: Float): Float {
        val clamped = pressure.coerceIn(0f, 1f)
        val factor = MIN_WIDTH_FACTOR + clamped * (MAX_WIDTH_FACTOR - MIN_WIDTH_FACTOR)
        return baseWidthPx * factor
    }

    /** Widest a stroke of [baseWidthPx] can render at any pressure; used for bounds inflation. */
    fun maxWidthFor(baseWidthPx: Float): Float = baseWidthPx * MAX_WIDTH_FACTOR
}

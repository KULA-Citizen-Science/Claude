package com.kula.stylusnotes.core.model

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tiltRadians: Float = 0f,
    val orientationRadians: Float = 0f,
    val timestampMs: Long
)

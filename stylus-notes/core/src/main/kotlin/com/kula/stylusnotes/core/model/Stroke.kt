package com.kula.stylusnotes.core.model

import com.kula.stylusnotes.core.color.InkColor

data class Stroke(
    val id: String,
    val color: InkColor,
    val baseWidthPx: Float,
    val points: List<StrokePoint>
)

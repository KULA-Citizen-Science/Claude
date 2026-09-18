package com.kula.stylusnotes.data.model

import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.model.Stroke

fun Stroke.toEntity(noteId: String, orderIndex: Int): StrokeEntity = StrokeEntity(
    id = id,
    noteId = noteId,
    orderIndex = orderIndex,
    colorKind = if (color is InkColor.Adaptive) "ADAPTIVE" else "FIXED",
    colorArgb = (color as? InkColor.Fixed)?.argb,
    colorLabel = (color as? InkColor.Fixed)?.label,
    baseWidthPx = baseWidthPx,
    pointsJson = StrokeJson.encodePoints(points)
)

fun StrokeEntity.toStroke(): Stroke = Stroke(
    id = id,
    color = if (colorKind == "ADAPTIVE") {
        InkColor.Adaptive
    } else {
        InkColor.Fixed(colorArgb ?: 0, colorLabel ?: "")
    },
    baseWidthPx = baseWidthPx,
    points = StrokeJson.decodePoints(pointsJson)
)

package com.kula.stylusnotes.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "strokes", indices = [Index("noteId")])
data class StrokeEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val orderIndex: Int,
    /** "ADAPTIVE" or "FIXED" — see [com.kula.stylusnotes.core.color.InkColor] */
    val colorKind: String,
    val colorArgb: Int?,
    val colorLabel: String?,
    val baseWidthPx: Float,
    val pointsJson: String
)

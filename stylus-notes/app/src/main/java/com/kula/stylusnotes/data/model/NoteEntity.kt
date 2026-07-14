package com.kula.stylusnotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** [com.kula.stylusnotes.core.model.CanvasBackground] name */
    val background: String
)

package com.kula.stylusnotes.data.repository

import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.data.db.NoteDao
import com.kula.stylusnotes.data.model.NoteEntity
import com.kula.stylusnotes.data.model.toEntity
import com.kula.stylusnotes.data.model.toStroke
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class NoteSummary(val id: String, val title: String, val updatedAt: Long)

data class NoteWithStrokes(
    val id: String,
    val title: String,
    val background: CanvasBackground,
    val strokes: List<Stroke>
)

class NoteRepository(private val dao: NoteDao) {

    fun observeNotes(): Flow<List<NoteSummary>> =
        dao.observeNotes().map { list -> list.map { NoteSummary(it.id, it.title, it.updatedAt) } }

    suspend fun createNote(title: String = "Untitled note"): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsertNote(
            NoteEntity(
                id = id,
                title = title,
                createdAt = now,
                updatedAt = now,
                background = CanvasBackground.WHITE.name
            )
        )
        return id
    }

    suspend fun loadNote(noteId: String): NoteWithStrokes? {
        val entity = dao.getNote(noteId) ?: return null
        val strokes = dao.getStrokes(noteId).map { it.toStroke() }
        return NoteWithStrokes(
            id = entity.id,
            title = entity.title,
            background = runCatching { CanvasBackground.valueOf(entity.background) }
                .getOrDefault(CanvasBackground.WHITE),
            strokes = strokes
        )
    }

    suspend fun saveStrokes(noteId: String, background: CanvasBackground, strokes: List<Stroke>) {
        val entity = dao.getNote(noteId) ?: return
        dao.upsertNote(entity.copy(background = background.name, updatedAt = System.currentTimeMillis()))
        dao.replaceStrokes(noteId, strokes.mapIndexed { index, stroke -> stroke.toEntity(noteId, index) })
    }

    suspend fun renameNote(noteId: String, title: String) {
        val entity = dao.getNote(noteId) ?: return
        dao.upsertNote(entity.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(noteId: String) {
        dao.deleteStrokesForNote(noteId)
        dao.deleteNote(noteId)
    }
}

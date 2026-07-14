package com.kula.stylusnotes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kula.stylusnotes.data.model.NoteEntity
import com.kula.stylusnotes.data.model.StrokeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNote(noteId: String): NoteEntity?

    @Query("SELECT * FROM strokes WHERE noteId = :noteId ORDER BY orderIndex ASC")
    suspend fun getStrokes(noteId: String): List<StrokeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM strokes WHERE noteId = :noteId")
    suspend fun deleteStrokesForNote(noteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokes(strokes: List<StrokeEntity>)

    @Transaction
    suspend fun replaceStrokes(noteId: String, strokes: List<StrokeEntity>) {
        deleteStrokesForNote(noteId)
        insertStrokes(strokes)
    }
}

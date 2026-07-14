package com.kula.shadowroutines.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kula.shadowroutines.data.db.entity.QuoteEntity
import com.kula.shadowroutines.data.db.entity.QuoteFtsEntity
import kotlinx.coroutines.flow.Flow

/** A single FTS match, with the fields the ranking step needs (see RoomFtsQuoteIndex). */
data class QuoteMatchRow(
    val id: String,
    val text: String,
    val tags: String,
    val seenCount: Int,
)

/** Per-quote state carried across a corpus rescan so novelty/favorites survive a rebuild. */
data class QuoteStateRow(
    val id: String,
    val seenCount: Int,
    val isFavorite: Boolean,
    val lastSeenAt: Long?,
)

/** id + seenCount pair, used to build the deck's novelty weighting. */
data class IdSeenRow(val id: String, val seenCount: Int)

@Dao
interface QuoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotes(quotes: List<QuoteEntity>)

    @Insert
    suspend fun insertFts(rows: List<QuoteFtsEntity>)

    @Query("DELETE FROM quotes")
    suspend fun clearQuotes()

    @Query("DELETE FROM quotes_fts")
    suspend fun clearFts()

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int

    @Query("SELECT id, seenCount, isFavorite, lastSeenAt FROM quotes")
    suspend fun allState(): List<QuoteStateRow>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: String): QuoteEntity?

    @Query("UPDATE quotes SET seenCount = seenCount + 1, lastSeenAt = :now WHERE id = :id")
    suspend fun markSeen(id: String, now: Long)

    @Query("UPDATE quotes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM quotes WHERE seenCount > 0")
    fun uniqueSeenCount(): Flow<Int>

    // --- Blocking reads used by RoomFtsQuoteIndex (always called off the main thread) ---

    @Query("SELECT id FROM quotes")
    fun allIdsBlocking(): List<String>

    @Query("SELECT id, seenCount FROM quotes")
    fun seenCountsBlocking(): List<IdSeenRow>

    /**
     * FTS keyword search. [match] is a column-qualified FTS4 MATCH expression built by
     * RoomFtsQuoteIndex (e.g. `text:"focus" OR tags:"focus"`). Results are unranked here and
     * scored/sorted in Kotlin, so [limit] is a generous safety cap, not the final N.
     */
    @Query(
        "SELECT q.id AS id, q.text AS text, q.tags AS tags, q.seenCount AS seenCount " +
            "FROM quotes q JOIN quotes_fts ON quotes_fts.quoteId = q.id " +
            "WHERE quotes_fts MATCH :match LIMIT :limit",
    )
    fun searchBlocking(match: String, limit: Int): List<QuoteMatchRow>
}

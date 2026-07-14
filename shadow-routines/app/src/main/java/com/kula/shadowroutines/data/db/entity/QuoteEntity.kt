package com.kula.shadowroutines.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * A stored quote. [id] is the stable corpus id, so [seenCount], [lastSeenAt] and [isFavorite]
 * survive a full corpus rescan (the importer carries them over by id). [tags] is a
 * comma-joined, lowercased list.
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String?,
    val source: String?,
    val tags: String,
    val sourceFile: String,
    val isFavorite: Boolean = false,
    val seenCount: Int = 0,
    val lastSeenAt: Long? = null,
)

/**
 * Standalone FTS4 table for keyword search. Kept separate from [QuoteEntity] (rather than an
 * external-content FTS) to keep the mapping explicit and the rebuild simple: on rescan we
 * clear and repopulate it alongside the quotes table. [quoteId] links a match back to the
 * quote row; searches are column-qualified to `text`/`tags` so ids never match accidentally.
 */
@Fts4
@Entity(tableName = "quotes_fts")
data class QuoteFtsEntity(
    val quoteId: String,
    val text: String,
    val tags: String,
    val author: String,
)

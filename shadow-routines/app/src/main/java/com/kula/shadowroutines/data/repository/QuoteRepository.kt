package com.kula.shadowroutines.data.repository

import android.content.Context
import android.net.Uri
import com.kula.shadowroutines.core.quote.MarkdownQuoteParser
import com.kula.shadowroutines.core.quote.ParsedQuote
import com.kula.shadowroutines.core.quote.QuoteEngine
import com.kula.shadowroutines.core.quote.QuoteQuery
import com.kula.shadowroutines.data.corpus.CorpusFile
import com.kula.shadowroutines.data.corpus.CorpusImporter
import com.kula.shadowroutines.data.db.QuoteDao
import com.kula.shadowroutines.data.db.entity.QuoteEntity
import com.kula.shadowroutines.data.db.entity.QuoteFtsEntity
import com.kula.shadowroutines.data.novelty.NoveltyStore
import com.kula.shadowroutines.data.quote.RoomFtsQuoteIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** The quote shown on a reward card. */
data class RewardQuote(
    val id: String,
    val text: String,
    val author: String?,
    val source: String?,
    val isFavorite: Boolean,
)

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val noveltyStore: NoveltyStore,
    private val appContext: Context,
    private val engine: QuoteEngine = QuoteEngine(),
) {

    fun uniqueSeenCount(): Flow<Int> = quoteDao.uniqueSeenCount()

    suspend fun quoteCount(): Int = quoteDao.count()

    /** Seeds the bundled corpus the first time the app runs. */
    suspend fun seedIfEmpty() {
        if (quoteDao.count() == 0) rescanFromAssets()
    }

    suspend fun rescanFromAssets(): Int =
        ingest(CorpusImporter.readAssets(appContext))

    suspend fun importFromDirectory(treeUri: Uri): Int =
        ingest(CorpusImporter.readDirectory(appContext, treeUri))

    /**
     * Parses corpus files and rebuilds the quote + FTS tables. Per-quote novelty/favorite
     * state is preserved by id across the rebuild, matching the persistence guarantee in the
     * plan. Returns the number of quotes now stored.
     */
    private suspend fun ingest(files: List<CorpusFile>): Int = withContext(Dispatchers.IO) {
        val parsed: List<ParsedQuote> = files
            .flatMap { MarkdownQuoteParser.parse(it.name, it.content) }
            .associateBy { it.id } // later duplicate id wins; keeps ids unique
            .values.toList()

        val priorState = quoteDao.allState().associateBy { it.id }

        val entities = parsed.map { p ->
            val prior = priorState[p.id]
            QuoteEntity(
                id = p.id,
                text = p.text,
                author = p.author,
                source = p.source,
                tags = p.tags.joinToString(","),
                sourceFile = p.sourceFile,
                isFavorite = prior?.isFavorite ?: false,
                seenCount = prior?.seenCount ?: 0,
                lastSeenAt = prior?.lastSeenAt,
            )
        }
        val ftsRows = parsed.map { p ->
            QuoteFtsEntity(
                quoteId = p.id,
                text = p.text,
                tags = p.tags.joinToString(" "),
                author = p.author.orEmpty(),
            )
        }

        quoteDao.clearFts()
        quoteDao.clearQuotes()
        quoteDao.upsertQuotes(entities)
        quoteDao.insertFts(ftsRows)
        entities.size
    }

    /**
     * Chooses a reward quote for a completed task, updates and persists the anti-repetition
     * state, and records that the quote was seen. Returns null only when the corpus is empty.
     */
    suspend fun selectReward(query: QuoteQuery): RewardQuote? = withContext(Dispatchers.IO) {
        val index = RoomFtsQuoteIndex(quoteDao)
        val recentlySeen = noveltyStore.loadRecentlySeen()
        val deck = noveltyStore.loadDeck()

        val result = engine.select(query, index, recentlySeen, deck)
        val id = result.quoteId ?: return@withContext null

        noveltyStore.save(result.recentlySeen, result.deck)
        quoteDao.markSeen(id, System.currentTimeMillis())

        quoteDao.getById(id)?.let {
            RewardQuote(it.id, it.text, it.author, it.source, it.isFavorite)
        }
    }

    suspend fun setFavorite(id: String, favorite: Boolean) =
        withContext(Dispatchers.IO) { quoteDao.setFavorite(id, favorite) }
}

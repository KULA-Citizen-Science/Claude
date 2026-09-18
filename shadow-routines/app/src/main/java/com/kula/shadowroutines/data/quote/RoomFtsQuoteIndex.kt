package com.kula.shadowroutines.data.quote

import com.kula.shadowroutines.core.quote.QuoteIndex
import com.kula.shadowroutines.core.quote.QuoteRef
import com.kula.shadowroutines.core.quote.QuoteScoring
import com.kula.shadowroutines.data.db.QuoteDao

/**
 * A [QuoteIndex] backed by the Room FTS4 table. FTS narrows the corpus to rows matching any
 * keyword; the shared [QuoteScoring] then ranks those candidates so results match the
 * in-memory index exactly.
 *
 * All methods block on synchronous Room reads, so this must only be used off the main thread
 * (the repository calls the engine inside `Dispatchers.IO`).
 */
class RoomFtsQuoteIndex(
    private val dao: QuoteDao,
    private val sqlCap: Int = 200,
) : QuoteIndex {

    override fun search(keywords: List<String>, limit: Int): List<QuoteRef> {
        if (keywords.isEmpty()) return emptyList()

        // Column-qualified, quoted terms: safe against FTS operators and never match ids.
        val match = keywords.joinToString(" OR ") { "text:\"$it\" OR tags:\"$it\"" }
        val rows = dao.searchBlocking(match, sqlCap)

        return rows.map { row ->
            val tags = row.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val score = QuoteScoring.score(keywords, QuoteScoring.tokenize(row.text), tags)
            QuoteRef(row.id, score, row.seenCount)
        }.sortedWith(compareByDescending<QuoteRef> { it.score }.thenBy { it.seenCount })
            .take(limit)
    }

    override fun allIds(): List<String> = dao.allIdsBlocking()

    override fun seenCounts(): Map<String, Int> =
        dao.seenCountsBlocking().associate { it.id to it.seenCount }
}

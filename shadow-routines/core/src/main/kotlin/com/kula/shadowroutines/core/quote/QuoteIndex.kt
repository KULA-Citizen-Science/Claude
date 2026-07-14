package com.kula.shadowroutines.core.quote

/**
 * A candidate quote returned from a search, with the information the [QuoteEngine] needs to
 * choose between candidates.
 *
 * @param score relevance (higher = better match). For non-keyword fallbacks this is a flat 1.0.
 * @param seenCount how many times this quote has been shown before, used as a novelty penalty.
 */
data class QuoteRef(
    val id: String,
    val score: Double,
    val seenCount: Int,
)

/**
 * Keyword search over the corpus. The [QuoteEngine] depends only on this interface, so the
 * search backend is swappable:
 * - [InMemoryQuoteIndex] (here in :core) implements the algorithm in pure Kotlin, so it is
 *   fully unit-testable and doubles as a fallback.
 * - The :app module provides a Room FTS-backed implementation for on-device use.
 */
interface QuoteIndex {
    /** Best matches for [keywords], most relevant first, at most [limit] results. */
    fun search(keywords: List<String>, limit: Int): List<QuoteRef>

    /** Every quote id in the corpus (used to build the anti-repetition deck). */
    fun allIds(): List<String>

    /** Per-quote seen counts, for novelty weighting of the deck fallback. */
    fun seenCounts(): Map<String, Int>
}

/** The minimal per-quote record the in-memory index needs. */
data class IndexedQuote(
    val id: String,
    val text: String,
    val tags: List<String>,
    val seenCount: Int = 0,
)

/**
 * Simple, transparent keyword scorer: a tag hit is worth more than a body hit. Scoring is
 * intentionally easy to reason about — freshness and correctness matter more here than
 * sophisticated ranking, and the corpus is expected to be modest in size.
 */
class InMemoryQuoteIndex(records: List<IndexedQuote>) : QuoteIndex {

    private val records = records
    private val tokenized: Map<String, Set<String>> =
        records.associate { it.id to QuoteScoring.tokenize(it.text) }

    override fun search(keywords: List<String>, limit: Int): List<QuoteRef> {
        if (keywords.isEmpty()) return emptyList()
        val keys = keywords.map { it.lowercase() }

        return records.mapNotNull { rec ->
            val score = QuoteScoring.score(keys, tokenized.getValue(rec.id), rec.tags)
            if (score <= 0.0) null else QuoteRef(rec.id, score, rec.seenCount)
        }.sortedWith(compareByDescending<QuoteRef> { it.score }.thenBy { it.seenCount })
            .take(limit)
    }

    override fun allIds(): List<String> = records.map { it.id }

    override fun seenCounts(): Map<String, Int> = records.associate { it.id to it.seenCount }
}

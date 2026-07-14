package com.kula.shadowroutines.core.quote

import kotlin.random.Random

/** How a quote ended up being chosen — surfaced for logging/telemetry and tests. */
enum class SelectionStrategy {
    /** Keyword match, not recently seen. The happy path. */
    KEYWORD,

    /** Keyword matches existed but all were recently seen; the least-recent was chosen. */
    KEYWORD_RELAXED,

    /** No keyword hits; drew a not-recently-seen quote from the shuffled deck. */
    DECK,

    /** Deck fallback, but the whole corpus is recently seen (tiny corpus); relaxed. */
    DECK_RELAXED,

    /** The corpus is empty; nothing to show. */
    EMPTY,
}

/** The task context used to build a quote query. */
data class QuoteQuery(
    val text: String,
    val contextTags: List<String> = emptyList(),
)

/**
 * Result of a selection: the chosen id (null only when the corpus is empty) plus the updated
 * anti-repetition state the caller must persist.
 */
data class SelectionResult(
    val quoteId: String?,
    val strategy: SelectionStrategy,
    val recentlySeen: RecentlySeen,
    val deck: Deck,
    val keywords: List<String>,
)

/**
 * The quote-selection brain. Pure and deterministic given its [random], so the whole novelty
 * policy is unit-testable without Android.
 *
 * Pipeline per reward (see the plan):
 *  1. Build keywords from the task text + context tags.
 *  2. Keyword-search the [QuoteIndex] for a candidate set.
 *  3. Exclude anything in the recently-seen buffer; relax oldest-first if that empties it.
 *  4. Weighted-random pick favouring higher relevance and lower seen-count.
 *  5. Fallbacks: no keyword hits → shuffled-deck draw (full-corpus coverage) → relaxed draw.
 */
class QuoteEngine(
    private val random: Random = Random.Default,
    private val candidateLimit: Int = DEFAULT_CANDIDATE_LIMIT,
) {

    private val shuffle: (List<String>) -> List<String> = { it.shuffled(random) }

    fun select(
        query: QuoteQuery,
        index: QuoteIndex,
        recentlySeen: RecentlySeen,
        deck: Deck,
    ): SelectionResult {
        val keywords = KeywordExtractor.extract(query.text, query.contextTags)
        val ids = index.allIds()

        // Keep the deck coherent with the current corpus.
        var workingDeck = if (deck.matches(ids)) deck else Deck.freshFrom(ids, shuffle)

        if (ids.isEmpty()) {
            return SelectionResult(null, SelectionStrategy.EMPTY, recentlySeen, workingDeck, keywords)
        }

        // --- Keyword path ---
        val candidates = if (keywords.isEmpty()) emptyList() else index.search(keywords, candidateLimit)
        if (candidates.isNotEmpty()) {
            val eligible = candidates.filterNot { recentlySeen.contains(it.id) }
            val (pick, strategy) = if (eligible.isNotEmpty()) {
                weightedPick(eligible) to SelectionStrategy.KEYWORD
            } else {
                relaxedPick(candidates, recentlySeen) to SelectionStrategy.KEYWORD_RELAXED
            }
            return SelectionResult(
                pick.id, strategy, recentlySeen.withSeen(pick.id), workingDeck, keywords,
            )
        }

        // --- Deck fallback (no keyword hits) ---
        val maxAttempts = ids.size * 2
        var attempts = 0
        var chosen: String? = null
        while (attempts < maxAttempts) {
            val (drawn, next) = workingDeck.draw(shuffle)
            workingDeck = next
            attempts++
            if (drawn == null) break
            if (!recentlySeen.contains(drawn)) {
                chosen = drawn
                break
            }
        }
        if (chosen != null) {
            return SelectionResult(
                chosen, SelectionStrategy.DECK, recentlySeen.withSeen(chosen), workingDeck, keywords,
            )
        }

        // Everything is recently seen (corpus smaller than the buffer): relax and draw one.
        val (drawn, next) = workingDeck.draw(shuffle)
        workingDeck = next
        return SelectionResult(
            drawn,
            if (drawn == null) SelectionStrategy.EMPTY else SelectionStrategy.DECK_RELAXED,
            drawn?.let { recentlySeen.withSeen(it) } ?: recentlySeen,
            workingDeck,
            keywords,
        )
    }

    private fun weightedPick(items: List<QuoteRef>): QuoteRef {
        val weights = items.map { maxOf(it.score / (1.0 + it.seenCount), MIN_WEIGHT) }
        val total = weights.sum()
        var roll = random.nextDouble() * total
        for (i in items.indices) {
            roll -= weights[i]
            if (roll < 0) return items[i]
        }
        return items.last()
    }

    /** Among recently-seen candidates, prefer the tier seen longest ago, then weight-pick. */
    private fun relaxedPick(candidates: List<QuoteRef>, recentlySeen: RecentlySeen): QuoteRef {
        val maxRank = candidates.maxOf { recentlySeen.recencyRank(it.id) }
        val tier = candidates.filter { recentlySeen.recencyRank(it.id) == maxRank }
        return weightedPick(tier)
    }

    companion object {
        const val DEFAULT_CANDIDATE_LIMIT = 20
        private const val MIN_WEIGHT = 0.0001
    }
}

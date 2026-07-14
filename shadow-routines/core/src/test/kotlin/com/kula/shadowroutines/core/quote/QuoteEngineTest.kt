package com.kula.shadowroutines.core.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuoteEngineTest {

    private fun index(vararg records: IndexedQuote) = InMemoryQuoteIndex(records.toList())

    private val focusIndex = index(
        IndexedQuote("focus-1", "Concentrate on one thing.", listOf("focus", "work")),
        IndexedQuote("focus-2", "Attention is the rarest generosity.", listOf("focus")),
        IndexedQuote("rest-1", "Rest is productive.", listOf("rest")),
    )

    private fun engine(seed: Long = 42) = QuoteEngine(Random(seed))

    @Test
    fun `keyword path picks a relevant, unseen quote`() {
        val result = engine().select(
            QuoteQuery(text = "focus work"),
            focusIndex,
            RecentlySeen(),
            Deck(focusIndex.allIds()),
        )
        assertEquals(SelectionStrategy.KEYWORD, result.strategy)
        assertTrue(result.quoteId in setOf("focus-1", "focus-2"))
        // The chosen quote is now recorded as recently seen.
        assertTrue(result.recentlySeen.contains(result.quoteId!!))
    }

    @Test
    fun `recently-seen keyword candidates are excluded`() {
        val recent = RecentlySeen().withSeen("focus-1")
        val result = engine().select(
            QuoteQuery(text = "focus"),
            focusIndex,
            recent,
            Deck(focusIndex.allIds()),
        )
        assertEquals(SelectionStrategy.KEYWORD, result.strategy)
        assertEquals("focus-2", result.quoteId)
    }

    @Test
    fun `relaxes oldest-first when all keyword candidates are recent`() {
        // focus-2 seen most recently, focus-1 seen longer ago.
        val recent = RecentlySeen().withSeen("focus-1").withSeen("focus-2")
        val result = engine().select(
            QuoteQuery(text = "focus"),
            focusIndex,
            recent,
            Deck(focusIndex.allIds()),
        )
        assertEquals(SelectionStrategy.KEYWORD_RELAXED, result.strategy)
        assertEquals("focus-1", result.quoteId) // the least-recently-seen candidate
    }

    @Test
    fun `falls back to the deck when there are no keyword hits`() {
        val result = engine().select(
            QuoteQuery(text = "xyzzy nothingmatches"),
            focusIndex,
            RecentlySeen(),
            Deck(focusIndex.allIds()),
        )
        assertEquals(SelectionStrategy.DECK, result.strategy)
        assertTrue(result.quoteId in focusIndex.allIds())
    }

    @Test
    fun `deck fallback avoids recently-seen quotes`() {
        val recent = RecentlySeen().withSeen("focus-1").withSeen("focus-2")
        repeat(20) { seed ->
            val result = QuoteEngine(Random(seed.toLong())).select(
                QuoteQuery(text = ""),
                focusIndex,
                recent,
                Deck(focusIndex.allIds()),
            )
            assertEquals("rest-1", result.quoteId) // the only non-recent id
        }
    }

    @Test
    fun `empty corpus yields EMPTY and null id`() {
        val result = engine().select(
            QuoteQuery(text = "anything"),
            index(),
            RecentlySeen(),
            Deck(emptyList()),
        )
        assertEquals(SelectionStrategy.EMPTY, result.strategy)
        assertNull(result.quoteId)
    }

    @Test
    fun `rebuilds the deck when the corpus changed`() {
        val staleDeck = Deck(listOf("gone-1", "gone-2"))
        val result = engine().select(
            QuoteQuery(text = ""),
            focusIndex,
            RecentlySeen(),
            staleDeck,
        )
        assertTrue(result.quoteId in focusIndex.allIds())
        assertTrue(result.deck.matches(focusIndex.allIds()))
    }

    @Test
    fun `is deterministic for a fixed seed`() {
        val query = QuoteQuery(text = "focus")
        val a = engine(seed = 7).select(query, focusIndex, RecentlySeen(), Deck(focusIndex.allIds()))
        val b = engine(seed = 7).select(query, focusIndex, RecentlySeen(), Deck(focusIndex.allIds()))
        assertEquals(a.quoteId, b.quoteId)
    }

    @Test
    fun `no repetition within the buffer window over many draws`() {
        // 10-quote corpus, capacity-5 buffer, no keywords => pure deck path.
        val corpus = (1..10).map { IndexedQuote("q$it", "text number $it", listOf("t$it")) }
        val idx = InMemoryQuoteIndex(corpus)
        val engine = QuoteEngine(Random(123))

        var recent = RecentlySeen(capacity = 5)
        var deck = Deck.freshFrom(idx.allIds()) { it.shuffled(Random(1)) }
        val picks = mutableListOf<String>()

        repeat(40) {
            val r = engine.select(QuoteQuery(text = ""), idx, recent, deck)
            picks += r.quoteId!!
            recent = r.recentlySeen
            deck = r.deck
        }

        // Each pick must differ from the previous 5 (the exclusion window).
        for (i in picks.indices) {
            val window = picks.subList(maxOf(0, i - 5), i)
            assertTrue("repeat within window at $i: ${picks[i]} in $window", picks[i] !in window)
        }
    }
}

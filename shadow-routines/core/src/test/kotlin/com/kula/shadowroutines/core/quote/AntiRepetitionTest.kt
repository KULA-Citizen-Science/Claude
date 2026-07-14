package com.kula.shadowroutines.core.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentlySeenTest {

    @Test
    fun `withSeen moves id to front and de-duplicates`() {
        val buffer = RecentlySeen(capacity = 5)
            .withSeen("a").withSeen("b").withSeen("a")
        assertEquals(listOf("a", "b"), buffer.ids)
    }

    @Test
    fun `buffer is trimmed to capacity, dropping oldest`() {
        var buffer = RecentlySeen(capacity = 3)
        listOf("a", "b", "c", "d").forEach { buffer = buffer.withSeen(it) }
        assertEquals(listOf("d", "c", "b"), buffer.ids)
        assertFalse(buffer.contains("a"))
    }

    @Test
    fun `recencyRank reflects position and absence`() {
        val buffer = RecentlySeen(capacity = 5).withSeen("old").withSeen("new")
        assertEquals(0, buffer.recencyRank("new"))
        assertEquals(1, buffer.recencyRank("old"))
        assertEquals(Int.MAX_VALUE, buffer.recencyRank("never"))
    }
}

class DeckTest {

    // Deterministic "shuffle" that just reverses, so tests are predictable.
    private val reverseShuffle: (List<String>) -> List<String> = { it.reversed() }

    @Test
    fun `draws every id once before reshuffling`() {
        val ids = listOf("a", "b", "c")
        var deck = Deck.freshFrom(ids, reverseShuffle) // order = [c, b, a]
        val drawn = mutableListOf<String>()
        repeat(3) {
            val (id, next) = deck.draw(reverseShuffle)
            drawn += id!!
            deck = next
        }
        assertEquals(listOf("c", "b", "a"), drawn)
        assertTrue(deck.isExhausted)
    }

    @Test
    fun `reshuffles when exhausted`() {
        val ids = listOf("a", "b")
        var deck = Deck(order = ids, position = 2) // already exhausted
        val (id, next) = deck.draw(reverseShuffle)
        assertEquals("b", id) // reversed order [b, a], first draw = b
        assertEquals(1, next.position)
    }

    @Test
    fun `empty deck draws null`() {
        val (id, next) = Deck(emptyList()).draw(reverseShuffle)
        assertNull(id)
        assertTrue(next.order.isEmpty())
    }

    @Test
    fun `matches detects corpus drift`() {
        val deck = Deck(listOf("a", "b", "c"))
        assertTrue(deck.matches(listOf("c", "b", "a")))
        assertFalse(deck.matches(listOf("a", "b")))
    }
}

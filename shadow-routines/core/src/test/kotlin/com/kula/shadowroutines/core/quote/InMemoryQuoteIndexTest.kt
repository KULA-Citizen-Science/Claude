package com.kula.shadowroutines.core.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryQuoteIndexTest {

    private val records = listOf(
        IndexedQuote("solitude", "A room of her own is needed to write.", listOf("solitude", "work")),
        IndexedQuote("courage", "Do the thing you think you cannot do.", listOf("courage")),
        IndexedQuote("time", "Time you enjoy wasting is not wasted time.", listOf("time"), seenCount = 3),
    )
    private val index = InMemoryQuoteIndex(records)

    @Test
    fun `tag match outranks body-only match`() {
        // "work" is a tag on solitude; also appears nowhere else.
        val results = index.search(listOf("work"), limit = 10)
        assertEquals("solitude", results.first().id)
    }

    @Test
    fun `body token matches are found`() {
        val results = index.search(listOf("write"), limit = 10)
        assertEquals(listOf("solitude"), results.map { it.id })
    }

    @Test
    fun `no keywords yields no results`() {
        assertTrue(index.search(emptyList(), limit = 10).isEmpty())
    }

    @Test
    fun `limit is respected`() {
        val results = index.search(listOf("time", "work", "courage"), limit = 2)
        assertEquals(2, results.size)
    }

    @Test
    fun `allIds and seenCounts expose corpus state`() {
        assertEquals(setOf("solitude", "courage", "time"), index.allIds().toSet())
        assertEquals(3, index.seenCounts()["time"])
    }
}

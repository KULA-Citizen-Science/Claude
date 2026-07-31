package com.kula.anagram.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.random.Random
import org.junit.Test

class AnagramsTest {

    @Test
    fun extractLetters_dropsNonLettersButKeepsUmlauts() {
        assertEquals(
            listOf('A', 'n', 'n', 'a', 'L', 'e', 'n', 'a'),
            Anagrams.extractLetters("Anna-Lena 3!"),
        )
        assertEquals(
            listOf('M', 'ü', 'l', 'l', 'e', 'r'),
            Anagrams.extractLetters("Müller"),
        )
        assertEquals(listOf('S', 't', 'r', 'a', 'ß', 'e'), Anagrams.extractLetters("Straße"))
    }

    @Test
    fun tilesFrom_assignsUniqueSequentialIdsAndPreservesLetters() {
        val tiles = Anagrams.tilesFrom("Anna")
        assertEquals(listOf(0, 1, 2, 3), tiles.map { it.id })
        assertEquals(listOf('A', 'n', 'n', 'a'), tiles.map { it.char })
        assertEquals(tiles.size, tiles.map { it.id }.distinct().size)
    }

    @Test
    fun tilesFrom_honoursStartId() {
        val tiles = Anagrams.tilesFrom("ab", startId = 100)
        assertEquals(listOf(100, 101), tiles.map { it.id })
    }

    @Test
    fun spell_roundTripsTheInputLetters() {
        val tiles = Anagrams.tilesFrom("Garten")
        assertEquals("Garten", Anagrams.spell(tiles))
    }

    @Test
    fun signature_isOrderAndCaseInsensitive() {
        assertEquals(Anagrams.signature("Lena"), Anagrams.signature("Elan"))
        assertEquals(Anagrams.signature("Garten"), Anagrams.signature("tragen"))
        assertEquals("aeln", Anagrams.signature("Lena"))
    }

    @Test
    fun sameLetters_distinguishesRealAnagramsFromNonAnagrams() {
        assertTrue(Anagrams.sameLetters("Garten", "tragen"))
        assertTrue(Anagrams.sameLetters("Anna Lena", "Alan Enna"))
        assertFalse(Anagrams.sameLetters("Garten", "Gitter"))
    }

    @Test
    fun isProperAnagram_requiresSameLettersButDifferentOrder() {
        assertTrue(Anagrams.isProperAnagram("Garten", "tragen"))
        // Same spelling (case/spacing aside) is not a proper anagram of itself.
        assertFalse(Anagrams.isProperAnagram("Garten", "garten"))
        assertFalse(Anagrams.isProperAnagram("Garten", "Gar ten"))
        // Different letters entirely.
        assertFalse(Anagrams.isProperAnagram("Garten", "Haus"))
    }

    @Test
    fun moveItem_movesForwardAndBackwardWithoutMutating() {
        val original = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), Anagrams.moveItem(original, 0, 2))
        assertEquals(listOf("c", "a", "b", "d"), Anagrams.moveItem(original, 2, 0))
        // untouched original
        assertEquals(listOf("a", "b", "c", "d"), original)
    }

    @Test
    fun moveItem_noOpAndClampsOutOfRange() {
        val original = listOf("a", "b", "c")
        assertEquals(original, Anagrams.moveItem(original, 1, 1))
        assertEquals(Anagrams.moveItem(original, 0, 2), Anagrams.moveItem(original, 0, 99))
        assertEquals(emptyList<String>(), Anagrams.moveItem(emptyList<String>(), 0, 1))
    }

    @Test
    fun shuffled_preservesTheMultisetOfLetters() {
        val tiles = Anagrams.tilesFrom("Buchstaben")
        val result = Anagrams.shuffled(tiles, Random(42))
        assertEquals(Anagrams.signature("Buchstaben"), Anagrams.signature(Anagrams.spell(result)))
        assertEquals(tiles.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun shuffled_isDeterministicForAGivenSeed() {
        val tiles = Anagrams.tilesFrom("Buchstaben")
        val a = Anagrams.shuffled(tiles, Random(7))
        val b = Anagrams.shuffled(tiles, Random(7))
        assertEquals(Anagrams.spell(a), Anagrams.spell(b))
    }

    @Test
    fun shuffled_avoidsReturningTheSameOrderWhenPossible() {
        val tiles = Anagrams.tilesFrom("Garten")
        // A seed whose first shuffle would reproduce the input still yields a different order.
        repeat(25) { seed ->
            val result = Anagrams.shuffled(tiles, Random(seed.toLong()), avoid = tiles)
            assertFalse(
                "shuffle with seed $seed returned the original order",
                Anagrams.spell(result) == "Garten",
            )
        }
    }

    @Test
    fun shuffled_returnsInputWhenNoDistinctArrangementExists() {
        val single = Anagrams.tilesFrom("a")
        assertEquals(single, Anagrams.shuffled(single, Random(1)))
        val allSame = Anagrams.tilesFrom("aaa")
        assertEquals("aaa", Anagrams.spell(Anagrams.shuffled(allSame, Random(1))))
    }
}

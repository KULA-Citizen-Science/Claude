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
        assertEquals(listOf('M', 'ü', 'l', 'l', 'e', 'r'), Anagrams.extractLetters("Müller"))
        assertEquals(listOf('S', 't', 'r', 'a', 'ß', 'e'), Anagrams.extractLetters("Straße"))
    }

    @Test
    fun letterCells_assignsUniqueSequentialIdsAndPreservesLetters() {
        val cells = Anagrams.letterCells("Anna")
        assertEquals(listOf(0, 1, 2, 3), cells.map { it.id })
        assertEquals(listOf('A', 'n', 'n', 'a'), cells.map { it.char })
        assertEquals(cells.size, cells.map { it.id }.distinct().size)
    }

    @Test
    fun letterCells_honoursStartId() {
        val cells = Anagrams.letterCells("ab", startId = 100)
        assertEquals(listOf(100, 101), cells.map { it.id })
    }

    @Test
    fun spell_rendersLettersAndSpacesAndTrimsEnds() {
        assertEquals("Garten", Anagrams.spell(Anagrams.letterCells("Garten")))
        val cells: List<Cell> = listOf(
            Cell.Letter(0, 'A'),
            Cell.Space(1),
            Cell.Letter(2, 'B'),
        )
        assertEquals("A B", Anagrams.spell(cells))
        // Leading/trailing spaces are trimmed away.
        assertEquals("AB", Anagrams.spell(listOf(Cell.Space(0), Cell.Letter(1, 'A'), Cell.Letter(2, 'B'), Cell.Space(3))))
    }

    @Test
    fun letters_dropsSpaces() {
        val cells: List<Cell> = listOf(Cell.Letter(0, 'A'), Cell.Space(1), Cell.Letter(2, 'b'))
        assertEquals("Ab", Anagrams.letters(cells))
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
    fun isProperAnagram_ignoresSpacesAndCaseButRequiresDifferentOrder() {
        assertTrue(Anagrams.isProperAnagram("Garten", "tragen"))
        // Splitting into words is still the same letter order -> not a new anagram.
        assertFalse(Anagrams.isProperAnagram("Garten", "Gar ten"))
        assertFalse(Anagrams.isProperAnagram("Garten", "garten"))
        // A real rearrangement across a word break counts.
        assertTrue(Anagrams.isProperAnagram("Anna Lena", "Alan Enna"))
        assertFalse(Anagrams.isProperAnagram("Garten", "Haus"))
    }

    @Test
    fun moveItem_movesForwardAndBackwardWithoutMutating() {
        val original = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), Anagrams.moveItem(original, 0, 2))
        assertEquals(listOf("c", "a", "b", "d"), Anagrams.moveItem(original, 2, 0))
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
    fun shuffled_preservesTheMultisetOfCells() {
        val cells = Anagrams.letterCells("Buchstaben")
        val result = Anagrams.shuffled(cells, Random(42))
        assertEquals(Anagrams.signature("Buchstaben"), Anagrams.signature(Anagrams.spell(result)))
        assertEquals(cells.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun shuffled_isDeterministicForAGivenSeed() {
        val cells = Anagrams.letterCells("Buchstaben")
        assertEquals(
            Anagrams.spell(Anagrams.shuffled(cells, Random(7))),
            Anagrams.spell(Anagrams.shuffled(cells, Random(7))),
        )
    }

    @Test
    fun shuffled_avoidsReturningTheSameOrderWhenPossible() {
        val cells = Anagrams.letterCells("Garten")
        repeat(25) { seed ->
            val result = Anagrams.shuffled(cells, Random(seed.toLong()), avoid = cells)
            assertFalse(
                "shuffle with seed $seed returned the original order",
                Anagrams.spell(result) == "Garten",
            )
        }
    }

    @Test
    fun shuffled_returnsInputWhenNoDistinctArrangementExists() {
        val single = Anagrams.letterCells("a")
        assertEquals(single, Anagrams.shuffled(single, Random(1)))
        val allSame = Anagrams.letterCells("aaa")
        assertEquals("aaa", Anagrams.spell(Anagrams.shuffled(allSame, Random(1))))
    }
}

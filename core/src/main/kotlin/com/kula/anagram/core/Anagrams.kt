package com.kula.anagram.core

import kotlin.random.Random

/**
 * Pure anagram logic, free of any Android dependency so it can be unit-tested on the JVM.
 *
 * The board is modelled as an ordered [List] of [LetterTile]. Rearranging is just reordering that
 * list; forming an anagram means the multiset of letters is preserved while their order changes.
 */
object Anagrams {

    /**
     * The letters of [input], in order, with everything that is not a letter (spaces, digits,
     * punctuation) removed. Umlauts and ß are letters and are kept. Case is preserved.
     *
     * "Anna-Lena 3" -> [A, n, n, a, L, e, n, a]
     */
    fun extractLetters(input: String): List<Char> =
        input.filter { it.isLetter() }.toList()

    /**
     * Builds board tiles from [input]. Ids are assigned left-to-right starting at [startId], so each
     * letter — even repeated ones — gets a distinct, stable identity.
     */
    fun tilesFrom(input: String, startId: Int = 0): List<LetterTile> =
        extractLetters(input).mapIndexed { index, c -> LetterTile(startId + index, c) }

    /** The word currently spelled by [tiles], in their present order. */
    fun spell(tiles: List<LetterTile>): String =
        buildString(tiles.size) { tiles.forEach { append(it.char) } }

    /**
     * A canonical key for anagram comparison: the letters of [text] only, lower-cased and sorted.
     * Two strings are anagrams of each other exactly when their signatures are equal.
     *
     * "Lena" -> "aeln", "Elan" -> "aeln"
     */
    fun signature(text: String): String =
        extractLetters(text).map { it.lowercaseChar() }.sorted().joinToString("")

    /** True when [a] and [b] are built from the same multiset of letters (ignoring case and order). */
    fun sameLetters(a: String, b: String): Boolean =
        signature(a) == signature(b)

    /**
     * True when [b] is a *genuine* rearrangement of [a]: same letters, but a different ordering.
     * Identical spellings (ignoring case and non-letters) are not counted as anagrams of each other.
     */
    fun isProperAnagram(a: String, b: String): Boolean {
        if (!sameLetters(a, b)) return false
        val aLetters = extractLetters(a).map { it.lowercaseChar() }
        val bLetters = extractLetters(b).map { it.lowercaseChar() }
        return aLetters != bLetters
    }

    /**
     * Returns a new list with the tile at [from] moved to index [to], shifting the tiles in between.
     * Indices outside `0 until list.size` are clamped; the original list is never mutated.
     */
    fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
        if (list.isEmpty()) return list
        val lastIndex = list.size - 1
        val src = from.coerceIn(0, lastIndex)
        val dst = to.coerceIn(0, lastIndex)
        if (src == dst) return list
        return list.toMutableList().apply { add(dst, removeAt(src)) }
    }

    /**
     * A shuffled copy of [tiles]. The multiset of letters is preserved (it is still an anagram of the
     * input); only the order changes. Passing a seeded [random] makes the result reproducible, which
     * is what the tests rely on.
     *
     * For inputs of two or more distinct arrangements this retries a bounded number of times to avoid
     * handing back the exact same order the board already had.
     */
    fun shuffled(
        tiles: List<LetterTile>,
        random: Random = Random.Default,
        avoid: List<LetterTile>? = tiles,
    ): List<LetterTile> {
        if (tiles.size < 2) return tiles
        val avoidWord = avoid?.let { spell(it) }
        var result = tiles.shuffled(random)
        var attempts = 0
        // Only keep retrying while a different arrangement is actually reachable.
        while (avoidWord != null && spell(result) == avoidWord && hasDistinctArrangement(tiles) && attempts < 20) {
            result = tiles.shuffled(random)
            attempts++
        }
        return result
    }

    /** True when the tiles' letters are not all identical, i.e. a different ordering is possible. */
    private fun hasDistinctArrangement(tiles: List<LetterTile>): Boolean =
        tiles.map { it.char }.distinct().size > 1
}

package com.kula.anagram.core

import kotlin.random.Random

/**
 * Pure anagram logic, free of any Android dependency so it can be unit-tested on the JVM.
 *
 * A board is an ordered [List] of [Cell]. Rearranging is just reordering that list; forming an
 * anagram means the multiset of letters is preserved while their order (and word breaks) change.
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
     * Builds letter cells from [input]. Ids are assigned left-to-right starting at [startId], so each
     * letter — even repeated ones — gets a distinct, stable identity.
     */
    fun letterCells(input: String, startId: Int = 0): List<Cell.Letter> =
        extractLetters(input).mapIndexed { index, c -> Cell.Letter(startId + index, c) }

    /**
     * The text spelled by [cells] in their present order: letters contribute their character, spaces
     * a single blank. Leading/trailing/duplicate blanks are left as the user arranged them, except
     * that the result is trimmed so a stray space at either end doesn't show.
     */
    fun spell(cells: List<Cell>): String =
        buildString(cells.size) {
            cells.forEach { cell ->
                when (cell) {
                    is Cell.Letter -> append(cell.char)
                    is Cell.Space -> append(' ')
                }
            }
        }.trim()

    /** The letters of [cells] only (spaces dropped), in order — used for order comparisons. */
    fun letters(cells: List<Cell>): String =
        buildString { cells.forEach { if (it is Cell.Letter) append(it.char) } }

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
     * Spaces, punctuation and case are ignored, so only the sequence of letters counts. Identical
     * letter sequences are not anagrams of each other.
     */
    fun isProperAnagram(a: String, b: String): Boolean {
        if (!sameLetters(a, b)) return false
        val aLetters = extractLetters(a).map { it.lowercaseChar() }
        val bLetters = extractLetters(b).map { it.lowercaseChar() }
        return aLetters != bLetters
    }

    /**
     * Returns a new list with the item at [from] moved to index [to], shifting the items in between.
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
     * A shuffled copy of [cells], preserving the multiset of cells (still an anagram of the input);
     * only the order changes. A seeded [random] makes the result reproducible, which the tests rely
     * on. When [avoid] is given, this retries a bounded number of times so the result differs from
     * that arrangement whenever a different one is actually reachable.
     */
    fun shuffled(
        cells: List<Cell>,
        random: Random = Random.Default,
        avoid: List<Cell>? = cells,
    ): List<Cell> {
        if (cells.size < 2) return cells
        val avoidWord = avoid?.let { spell(it) }
        var result = cells.shuffled(random)
        var attempts = 0
        while (avoidWord != null && spell(result) == avoidWord && hasDistinctArrangement(cells) && attempts < 20) {
            result = cells.shuffled(random)
            attempts++
        }
        return result
    }

    /** True when the cells are not all the same kind, i.e. a visibly different order is possible. */
    private fun hasDistinctArrangement(cells: List<Cell>): Boolean {
        val kinds = cells.map {
            when (it) {
                is Cell.Letter -> it.char.lowercaseChar()
                is Cell.Space -> ' '
            }
        }
        return kinds.distinct().size > 1
    }
}

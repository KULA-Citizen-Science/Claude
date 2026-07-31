package com.kula.anagram.core

/**
 * A single, movable letter on the anagram board.
 *
 * [id] is a stable identity that survives reordering and shuffling — the UI keys tiles by it so a
 * letter animates from its old slot to its new one instead of being recreated. Two tiles can carry
 * the same [char] (e.g. the two "n"s in "Anna") but never the same [id].
 */
data class LetterTile(
    val id: Int,
    val char: Char,
)

package com.kula.anagram.core

/**
 * A single cell that can sit on a board: either a [Letter] or a [Space].
 *
 * [id] is a stable identity that survives reordering, shuffling and moving between the workbench and
 * the tray — the UI keys cells by it so a cell animates from its old slot to its new one instead of
 * being recreated. Repeated letters (the two "n"s in "Anna") share a [char] but never an [id]; every
 * inserted space likewise gets its own [id].
 */
sealed interface Cell {

    val id: Int

    data class Letter(override val id: Int, val char: Char) : Cell

    /** A word separator. Only ever lives on the workbench, never on the tray. */
    data class Space(override val id: Int) : Cell
}

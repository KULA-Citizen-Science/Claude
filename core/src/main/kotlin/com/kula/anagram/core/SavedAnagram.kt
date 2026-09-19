package com.kula.anagram.core

/**
 * An anagram the user has kept. [source] is the term they started from, [text] the rearrangement
 * they saved, and [createdAt] an epoch-millis timestamp used only for ordering the saved list.
 *
 * Kept in `:core` (plain data, no Android types) so both the persistence layer and its tests can use it.
 */
data class SavedAnagram(
    val source: String,
    val text: String,
    val createdAt: Long,
)

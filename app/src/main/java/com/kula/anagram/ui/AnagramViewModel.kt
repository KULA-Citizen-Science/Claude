package com.kula.anagram.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kula.anagram.core.Anagrams
import com.kula.anagram.core.LetterTile
import com.kula.anagram.core.SavedAnagram
import com.kula.anagram.data.SavedAnagramStore
import kotlin.random.Random

/**
 * Holds the anagram board state and drives every user action. Kept deliberately thin: all the real
 * letter logic lives in [Anagrams] (`:core`), and this class only wires it to Compose state and to
 * the [SavedAnagramStore].
 */
class AnagramViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SavedAnagramStore(application)

    /** What the user is typing in the input field. */
    var input by mutableStateOf("")
        private set

    /** The term the current board was built from — used for the reset and the "is this an anagram?" check. */
    var source by mutableStateOf("")
        private set

    /** The board: letters in their current order. Reordering this list is what forms anagrams. */
    var tiles by mutableStateOf<List<LetterTile>>(emptyList())
        private set

    /** The user's kept anagrams, newest first. */
    var saved by mutableStateOf<List<SavedAnagram>>(emptyList())
        private set

    init {
        saved = store.load()
    }

    /** The word currently spelled out on the board. */
    val currentWord: String get() = Anagrams.spell(tiles)

    /** True once the board differs from the loaded term, i.e. the letters form a genuine anagram. */
    val isAnagram: Boolean get() = source.isNotBlank() && Anagrams.isProperAnagram(source, currentWord)

    /** True when the current arrangement is worth offering to save (a real anagram not already kept). */
    val canSave: Boolean
        get() = isAnagram && saved.none { it.text.equals(currentWord, ignoreCase = true) }

    fun onInputChange(value: String) {
        input = value
    }

    /** Turns the current [input] into a fresh board of draggable tiles. */
    fun loadLetters() {
        val trimmed = input.trim()
        source = trimmed
        tiles = Anagrams.tilesFrom(trimmed)
    }

    /** Commits a reorder produced by dragging a tile from [from] to [to]. */
    fun moveTile(from: Int, to: Int) {
        tiles = Anagrams.moveItem(tiles, from, to)
    }

    /** Randomly rearranges the letters, preferring an order different from the current one. */
    fun shuffle() {
        tiles = Anagrams.shuffled(tiles, Random.Default, avoid = tiles)
    }

    /** Restores the letters to the order of the originally loaded term. */
    fun reset() {
        tiles = Anagrams.tilesFrom(source)
    }

    /** Empties the board and the input field. */
    fun clearBoard() {
        input = ""
        source = ""
        tiles = emptyList()
    }

    /** Saves the current arrangement to "Meine Anagramme". */
    fun saveCurrent() {
        if (!canSave) return
        val entry = SavedAnagram(source = source, text = currentWord, createdAt = System.currentTimeMillis())
        saved = (listOf(entry) + saved)
        store.save(saved)
    }

    fun deleteSaved(item: SavedAnagram) {
        saved = saved.filterNot { it.createdAt == item.createdAt && it.text == item.text }
        store.save(saved)
    }
}

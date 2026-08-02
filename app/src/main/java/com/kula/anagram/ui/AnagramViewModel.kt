package com.kula.anagram.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kula.anagram.core.Anagrams
import com.kula.anagram.core.Board
import com.kula.anagram.core.BoardState
import com.kula.anagram.core.Boards
import com.kula.anagram.core.Cell
import com.kula.anagram.core.SavedAnagram
import com.kula.anagram.data.SavedAnagramStore

/**
 * Holds the two-board anagram state and drives every user action. Kept deliberately thin: all the
 * real letter logic lives in [Anagrams] (`:core`), and this class only wires it to Compose state and
 * to the [SavedAnagramStore].
 *
 * The letters of the loaded term are split across two boards:
 *  - [werkbank] — the workbench, where the anagram is assembled (letters plus inserted spaces);
 *  - [ablage] — the tray, where letters can be parked while they are not in use.
 * Moving a letter between the boards, reordering within a board and inserting/removing spaces are all
 * that is needed to form an anagram.
 */
class AnagramViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SavedAnagramStore(application)

    /** What the user is typing in the input field. */
    var input by mutableStateOf("")
        private set

    /** The term the current boards were built from — used for reset and the anagram check. */
    var source by mutableStateOf("")
        private set

    /** The workbench: letters and spaces in their current order. */
    var werkbank by mutableStateOf<List<Cell>>(emptyList())
        private set

    /** The tray: letters set aside, never spaces. */
    var ablage by mutableStateOf<List<Cell.Letter>>(emptyList())
        private set

    /** The user's kept anagrams, newest first. */
    var saved by mutableStateOf<List<SavedAnagram>>(emptyList())
        private set

    /** Next free cell id, handed out to inserted spaces so every cell stays uniquely identified. */
    private var nextId = 0

    init {
        saved = store.load()
    }

    /** The word (or words) currently spelled on the workbench. */
    val currentWord: String get() = Anagrams.spell(werkbank)

    /** True once every letter is on the workbench and its order differs from the loaded term. */
    val isAnagram: Boolean
        get() = source.isNotBlank() && ablage.isEmpty() && Anagrams.isProperAnagram(source, currentWord)

    /** True when the current arrangement is worth offering to save (a new, complete anagram). */
    val canSave: Boolean
        get() = isAnagram && saved.none { it.text.equals(currentWord, ignoreCase = true) }

    /** True once a term has been loaded onto the boards. */
    val hasBoard: Boolean get() = werkbank.isNotEmpty() || ablage.isNotEmpty()

    /** Input is kept upper-case throughout: the board shows capitals only. */
    fun onInputChange(value: String) {
        input = value.uppercase()
    }

    /** Turns the current [input] into letter cells, all placed in the tray for the user to draw from. */
    fun loadLetters() {
        val trimmed = input.trim().uppercase()
        source = trimmed
        val cells = Anagrams.letterCells(trimmed)
        nextId = cells.size
        werkbank = emptyList()
        ablage = cells
    }

    /** Appends a space to the end of the workbench. */
    fun addSpace() {
        if (source.isBlank()) return
        werkbank = werkbank + Cell.Space(nextId++)
    }

    /** Moves the cell with [cellId] to [zone] at [index]. Delegates to the unit-tested [Boards.move]. */
    fun drop(cellId: Int, zone: Zone, index: Int) {
        apply(Boards.move(boardState(), cellId, zone.toBoard(), index))
    }

    /** Removes a space from the workbench. Bound to a long press; letters are left untouched. */
    fun longPressCell(cellId: Int) {
        apply(Boards.removeSpace(boardState(), cellId))
    }

    /**
     * Quick action on tap: a workbench letter goes to the end of the tray and a tray letter to the end
     * of the workbench. Tapping a space does nothing — an accidental tap must never destroy a word
     * break the user placed; a space is removed with a long press.
     */
    fun tapCell(cellId: Int) {
        apply(Boards.tap(boardState(), cellId))
    }

    /** Randomly rearranges the workbench, preferring an order different from the current one. */
    fun shuffle() {
        werkbank = Anagrams.shuffled(werkbank, avoid = werkbank)
    }

    /** Returns every letter to the tray in the loaded term's order, clearing the workbench and spaces. */
    fun reset() {
        val cells = Anagrams.letterCells(source)
        nextId = cells.size
        werkbank = emptyList()
        ablage = cells
    }

    /** Empties both boards and the input field. */
    fun clearBoard() {
        input = ""
        source = ""
        werkbank = emptyList()
        ablage = emptyList()
        nextId = 0
    }

    /** Saves the current arrangement to "Meine Anagramme". */
    fun saveCurrent() {
        if (!canSave) return
        val entry = SavedAnagram(source = source, text = currentWord, createdAt = System.currentTimeMillis())
        saved = listOf(entry) + saved
        store.save(saved)
    }

    fun deleteSaved(item: SavedAnagram) {
        saved = saved.filterNot { it.createdAt == item.createdAt && it.text == item.text }
        store.save(saved)
    }

    private fun boardState() = BoardState(werkbank, ablage)

    private fun apply(next: BoardState) {
        werkbank = next.werkbank
        ablage = next.ablage
    }

    private fun Zone.toBoard(): Board = when (this) {
        Zone.WERKBANK -> Board.WERKBANK
        Zone.ABLAGE -> Board.ABLAGE
    }


}

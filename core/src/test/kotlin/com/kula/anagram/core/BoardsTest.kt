package com.kula.anagram.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BoardsTest {

    /** Renders a board compactly for readable assertions: letters as themselves, a space as "_". */
    private fun render(cells: List<Cell>): String = buildString {
        cells.forEach {
            when (it) {
                is Cell.Letter -> append(it.char)
                is Cell.Space -> append('_')
            }
        }
    }

    private fun letters(word: String, startId: Int = 0): List<Cell.Letter> =
        Anagrams.letterCells(word, startId)

    /**
     * The exact arrangement from the screen recording: "IRC_ÜTTE_IE" on the workbench, where the first
     * space has id 20 and sits at index 3, and the drop asked for index 1.
     */
    private fun recordedBoard(): BoardState {
        val cells = mutableListOf<Cell>()
        "IRC".forEachIndexed { i, c -> cells += Cell.Letter(i, c) }
        cells += Cell.Space(20)
        "ÜTTE".forEachIndexed { i, c -> cells += Cell.Letter(10 + i, c) }
        cells += Cell.Space(21)
        "IE".forEachIndexed { i, c -> cells += Cell.Letter(30 + i, c) }
        return BoardState(werkbank = cells)
    }

    @Test
    fun move_spaceBackwardsOnTheWorkbench() {
        val state = recordedBoard()
        assertEquals("IRC_ÜTTE_IE", render(state.werkbank))
        assertEquals(3, state.werkbank.indexOfFirst { it.id == 20 })

        val moved = Boards.move(state, cellId = 20, board = Board.WERKBANK, index = 1)
        assertEquals("I_RCÜTTE_IE", render(moved.werkbank))
    }

    @Test
    fun move_spaceForwardsOnTheWorkbench() {
        val state = BoardState(werkbank = listOf(Cell.Space(20)) + letters("IRCÜ"))
        assertEquals("_IRCÜ", render(state.werkbank))

        val moved = Boards.move(state, cellId = 20, board = Board.WERKBANK, index = 3)
        assertEquals("IRC_Ü", render(moved.werkbank))
    }

    @Test
    fun move_letterWithinTheWorkbench() {
        val state = BoardState(werkbank = letters("ABCD"))
        assertEquals("BCAD", render(Boards.move(state, cellId = 0, board = Board.WERKBANK, index = 2).werkbank))
        assertEquals("CABD", render(Boards.move(state, cellId = 2, board = Board.WERKBANK, index = 0).werkbank))
    }

    @Test
    fun move_letterBetweenBoards() {
        val state = BoardState(werkbank = letters("AB"), ablage = letters("XY", startId = 10))
        val toTray = Boards.move(state, cellId = 0, board = Board.ABLAGE, index = 1)
        assertEquals("B", render(toTray.werkbank))
        assertEquals("XAY", render(toTray.ablage))

        val toBench = Boards.move(state, cellId = 10, board = Board.WERKBANK, index = 0)
        assertEquals("XAB", render(toBench.werkbank))
        assertEquals("Y", render(toBench.ablage))
    }

    @Test
    fun move_spaceIsNeverSentToTheTray() {
        val state = BoardState(werkbank = letters("AB") + Cell.Space(20))
        assertSame(state, Boards.move(state, cellId = 20, board = Board.ABLAGE, index = 0))
    }

    @Test
    fun move_unknownCellIsIgnored() {
        val state = BoardState(werkbank = letters("AB"))
        assertSame(state, Boards.move(state, cellId = 99, board = Board.WERKBANK, index = 0))
    }

    @Test
    fun move_indexBeyondTheEndClampsToTheEnd() {
        val state = BoardState(werkbank = letters("ABC"))
        assertEquals("BCA", render(Boards.move(state, cellId = 0, board = Board.WERKBANK, index = 99).werkbank))
    }

    @Test
    fun tap_sendsLettersAcrossButLeavesSpacesAlone() {
        val state = BoardState(werkbank = letters("AB") + Cell.Space(20), ablage = letters("X", startId = 10))
        val tapped = Boards.tap(state, cellId = 0)
        assertEquals("B_", render(tapped.werkbank))
        assertEquals("XA", render(tapped.ablage))

        val back = Boards.tap(state, cellId = 10)
        assertEquals("AB_X", render(back.werkbank))
        assertEquals("", render(back.ablage))

        assertSame(state, Boards.tap(state, cellId = 20))
    }

    @Test
    fun removeSpace_onlyRemovesSpaces() {
        val state = BoardState(werkbank = letters("AB") + Cell.Space(20))
        assertEquals("AB", render(Boards.removeSpace(state, cellId = 20).werkbank))
        assertSame(state, Boards.removeSpace(state, cellId = 0))
    }
}

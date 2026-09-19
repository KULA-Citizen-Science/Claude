package com.kula.anagram.core

/** The two boards a cell can live on. */
enum class Board { WERKBANK, ABLAGE }

/** The contents of both boards. */
data class BoardState(
    val werkbank: List<Cell> = emptyList(),
    val ablage: List<Cell.Letter> = emptyList(),
)

/**
 * Pure board manipulation, free of Android and of Compose, so every rearrangement rule is unit-tested
 * rather than only observable on a device.
 */
object Boards {

    /**
     * Moves the cell with [cellId] to [board] at [index], wherever it currently sits. [index] counts
     * positions in the target board *with the moved cell taken out*, which is what a drop computes.
     * A space never leaves the workbench; a request to move one to the tray is ignored.
     */
    fun move(state: BoardState, cellId: Int, board: Board, index: Int): BoardState {
        val cell = find(state, cellId) ?: return state
        if (cell is Cell.Space && board == Board.ABLAGE) return state

        val werkbank = state.werkbank.filterNot { it.id == cellId }.toMutableList()
        val ablage = state.ablage.filterNot { it.id == cellId }.toMutableList()
        when (board) {
            Board.WERKBANK -> werkbank.add(index.coerceIn(0, werkbank.size), cell)
            Board.ABLAGE -> ablage.add(index.coerceIn(0, ablage.size), cell as Cell.Letter)
        }
        return BoardState(werkbank, ablage)
    }

    /**
     * The quick tap action: a workbench letter goes to the end of the tray and a tray letter to the end
     * of the workbench. Tapping a space does nothing, so a word break cannot be lost by accident.
     */
    fun tap(state: BoardState, cellId: Int): BoardState {
        val onWerkbank = state.werkbank.firstOrNull { it.id == cellId }
        if (onWerkbank != null) {
            if (onWerkbank !is Cell.Letter) return state
            return BoardState(
                werkbank = state.werkbank.filterNot { it.id == cellId },
                ablage = state.ablage + onWerkbank,
            )
        }
        val onAblage = state.ablage.firstOrNull { it.id == cellId }
            ?: return state
        return BoardState(
            werkbank = state.werkbank + onAblage,
            ablage = state.ablage.filterNot { it.id == cellId },
        )
    }

    /** Removes a space from the workbench; letters are left untouched. */
    fun removeSpace(state: BoardState, cellId: Int): BoardState =
        if (state.werkbank.firstOrNull { it.id == cellId } is Cell.Space) {
            state.copy(werkbank = state.werkbank.filterNot { it.id == cellId })
        } else {
            state
        }

    private fun find(state: BoardState, cellId: Int): Cell? =
        state.werkbank.firstOrNull { it.id == cellId }
            ?: state.ablage.firstOrNull { it.id == cellId }
}

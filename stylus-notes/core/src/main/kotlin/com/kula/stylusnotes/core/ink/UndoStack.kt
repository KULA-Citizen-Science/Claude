package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.model.Stroke

sealed class StrokeOp {
    data class Add(val stroke: Stroke) : StrokeOp()
    data class Remove(val stroke: Stroke, val index: Int) : StrokeOp()
}

/**
 * Logs reversible stroke operations. undo()/redo() only return which op to reverse/replay —
 * the caller owns the actual stroke list and applies the inverse (Add <-> Remove at index).
 */
class UndoStack {
    private val undoOps = ArrayDeque<StrokeOp>()
    private val redoOps = ArrayDeque<StrokeOp>()

    val canUndo: Boolean get() = undoOps.isNotEmpty()
    val canRedo: Boolean get() = redoOps.isNotEmpty()

    fun push(op: StrokeOp) {
        undoOps.addLast(op)
        redoOps.clear()
    }

    fun undo(): StrokeOp? {
        val op = undoOps.removeLastOrNull() ?: return null
        redoOps.addLast(op)
        return op
    }

    fun redo(): StrokeOp? {
        val op = redoOps.removeLastOrNull() ?: return null
        undoOps.addLast(op)
        return op
    }

    fun clear() {
        undoOps.clear()
        redoOps.clear()
    }
}

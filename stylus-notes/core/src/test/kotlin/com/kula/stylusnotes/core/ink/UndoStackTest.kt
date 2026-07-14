package com.kula.stylusnotes.core.ink

import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.core.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    private fun stroke(id: String) = Stroke(
        id = id,
        color = InkColor.Adaptive,
        baseWidthPx = 6f,
        points = listOf(StrokePoint(0f, 0f, 0.5f, timestampMs = 0))
    )

    @Test
    fun `fresh stack has nothing to undo or redo`() {
        val stack = UndoStack()
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
        assertNull(stack.undo())
        assertNull(stack.redo())
    }

    @Test
    fun `push then undo returns the op and moves it to redo`() {
        val stack = UndoStack()
        val op = StrokeOp.Add(stroke("a"))
        stack.push(op)

        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)

        val undone = stack.undo()
        assertEquals(op, undone)
        assertFalse(stack.canUndo)
        assertTrue(stack.canRedo)

        val redone = stack.redo()
        assertEquals(op, redone)
        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)
    }

    @Test
    fun `pushing a new op clears the redo stack`() {
        val stack = UndoStack()
        stack.push(StrokeOp.Add(stroke("a")))
        stack.undo()
        assertTrue(stack.canRedo)

        stack.push(StrokeOp.Add(stroke("b")))
        assertFalse(stack.canRedo)
    }

    @Test
    fun `multiple ops undo in LIFO order`() {
        val stack = UndoStack()
        val opA = StrokeOp.Add(stroke("a"))
        val opB = StrokeOp.Add(stroke("b"))
        stack.push(opA)
        stack.push(opB)

        assertEquals(opB, stack.undo())
        assertEquals(opA, stack.undo())
        assertNull(stack.undo())
    }

    @Test
    fun `clear drops both stacks`() {
        val stack = UndoStack()
        stack.push(StrokeOp.Add(stroke("a")))
        stack.undo()
        stack.clear()
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
    }
}

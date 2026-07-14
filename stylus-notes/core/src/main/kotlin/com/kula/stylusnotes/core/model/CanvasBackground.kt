package com.kula.stylusnotes.core.model

enum class CanvasBackground(val argb: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    BLACK(0xFF000000.toInt());

    fun toggled(): CanvasBackground = if (this == WHITE) BLACK else WHITE
}

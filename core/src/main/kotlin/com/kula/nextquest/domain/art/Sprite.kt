package com.kula.nextquest.domain.art

/**
 * A tiny pixel-art bitmap encoded as rows of palette characters, kept in `:core` as plain data so
 * the art catalog is unit-testable (rectangular grids, known palette, full coverage of the
 * friction taxonomy) without an Android renderer.
 *
 * Palette characters (mapped to actual colours in the app layer):
 * `.` transparent, `K` ink/outline, `W` cream, `D` danger red, `G` gold, `C` cyan, `M` magenta,
 * `L` lime, `P` panel purple, `S` shadow purple.
 */
data class Sprite(val rows: List<String>) {
    val width: Int get() = rows.first().length
    val height: Int get() = rows.size

    companion object {
        const val PALETTE = ".KWDGCMLPS"
    }
}

package com.kula.nextquest.domain.art

import com.kula.nextquest.domain.FrictionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the art catalog: every friction point has scene art, and every sprite is well-formed. */
class SceneArtTest {

    @Test
    fun everyFrictionPointHasTrapAndEmblemArt() {
        val frictionIds = FrictionCatalog.all.map { it.id }.toSet()
        assertEquals(frictionIds, SceneArt.traps.keys)
        assertEquals(frictionIds, SceneArt.emblems.keys)
        // Accessors resolve without throwing.
        for (id in frictionIds) {
            SceneArt.trapFor(id)
            SceneArt.emblemFor(id)
        }
    }

    @Test
    fun allSpritesAreRectangularAndNonEmpty() {
        for ((id, sprite) in SceneArt.traps + SceneArt.emblems) {
            assertTrue("sprite $id has no rows", sprite.rows.isNotEmpty())
            assertTrue("sprite $id has empty rows", sprite.width > 0)
            for ((i, row) in sprite.rows.withIndex()) {
                assertEquals("sprite $id row $i width differs", sprite.width, row.length)
            }
        }
    }

    @Test
    fun allSpritesUseOnlyPaletteCharacters() {
        for ((id, sprite) in SceneArt.traps + SceneArt.emblems) {
            for (row in sprite.rows) {
                for (ch in row) {
                    assertTrue("sprite $id uses unknown palette char '$ch'", ch in Sprite.PALETTE)
                }
            }
        }
    }

    @Test
    fun noSpriteIsBlank() {
        // Every sprite must draw at least something (a fully transparent sprite is a bug).
        for ((id, sprite) in SceneArt.traps + SceneArt.emblems) {
            assertTrue("sprite $id is fully transparent", sprite.rows.any { row -> row.any { it != '.' } })
        }
    }
}

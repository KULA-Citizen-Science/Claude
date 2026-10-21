package com.kula.nextquest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the taxonomy's internal consistency so nothing ships blank or ungrounded. */
class CatalogIntegrityTest {

    @Test
    fun everyFrictionPointHasNonEmptyCopyAndGrounding() {
        for (fp in FrictionCatalog.all) {
            assertTrue("trap blank for ${fp.id}", fp.trap.isNotBlank())
            assertTrue("strategy blank for ${fp.id}", fp.strategy.isNotBlank())
            assertTrue("why blank for ${fp.id}", fp.why.isNotBlank())
        }
    }

    @Test
    fun frictionPointIdsAreUnique() {
        val ids = FrictionCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun surfaceCopyStaysShort() {
        // The whole point is minimal output; keep the two shown lines terse.
        for (fp in FrictionCatalog.all) {
            assertTrue("trap too long for ${fp.id}", fp.trap.length <= 60)
            assertTrue("strategy too long for ${fp.id}", fp.strategy.length <= 80)
        }
    }

    @Test
    fun everyFrameworkHasTagTierAndSource() {
        for (f in Framework.entries) {
            assertTrue("tag blank for $f", f.tag.isNotBlank())
            assertTrue("source blank for $f", f.source.isNotBlank())
            assertTrue("marker blank for ${f.tier}", f.tier.marker.isNotBlank())
        }
    }

    @Test
    fun archetypeIdsAreUniqueAndAllResolve() {
        val ids = Archetypes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Every archetype produces a usable, non-empty reading.
        for (arch in Archetypes.all) {
            assertTrue("empty reading for ${arch.id}", FrictionEngine.analyze(arch.activity).isNotEmpty())
        }
    }
}

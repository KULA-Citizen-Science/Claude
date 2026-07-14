package com.kula.shadowroutines.core.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedTemplatesTest {

    @Test
    fun `all templates have unique ids`() {
        val ids = SeedTemplates.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every template has aspects with unique ordered ids`() {
        for (t in SeedTemplates.all) {
            assertTrue("template ${t.id} has no aspects", t.aspects.isNotEmpty())
            val aspectIds = t.aspects.map { it.id }
            assertEquals("duplicate aspect ids in ${t.id}", aspectIds.size, aspectIds.toSet().size)
            val orders = t.aspects.map { it.order }
            assertEquals("orders not 0..n in ${t.id}", (0 until t.aspects.size).toList(), orders)
        }
    }

    @Test
    fun `byId resolves seeded templates and null otherwise`() {
        assertNotNull(SeedTemplates.byId("meeting"))
        assertEquals("Workshop planning", SeedTemplates.byId("workshop")?.name)
        assertEquals(null, SeedTemplates.byId("does-not-exist"))
    }

    @Test
    fun `templates carry context tags for quote queries`() {
        assertTrue(SeedTemplates.all.all { it.contextTags.isNotEmpty() })
    }
}

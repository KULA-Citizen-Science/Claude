package com.kula.nextquest.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the Kotlin engine to the web port: `web/test/golden.txt` holds the web engine's ranked
 * friction ids for every archetype and all 128 "Other…" toggle combinations, and the Kotlin engine
 * must produce exactly the same ordering. If you change scoring on either side, regenerate the
 * golden (`cd web && npm run golden`) and update the other implementation to match.
 */
class WebParityTest {

    private val golden: Map<String, List<String>> by lazy {
        // Gradle runs :core tests with the module dir (core/) as the working directory.
        val file = listOf(File("../web/test/golden.txt"), File("web/test/golden.txt")).first { it.exists() }
        file.readLines().filter { it.isNotBlank() }.associate { line ->
            val (key, ids) = line.split(": ", limit = 2)
            key to ids.split(",")
        }
    }

    /** Mirrors `customActivity(bits)` in web/js/engine.js and the Android "Other…" panel. */
    private fun custom(bits: String): Activity {
        fun on(i: Int) = bits[i] == '1'
        return Activity(
            name = "Your Quest",
            interest = if (on(0)) Interest.BORING else Interest.NEUTRAL,
            deadline = if (on(1)) Deadline.HARD else Deadline.NONE,
            structure = if (on(2)) Structure.OPEN else Structure.DEFINED,
            duration = if (on(3)) Duration.LONG_OR_UNKNOWN else Duration.MEDIUM,
            multiStep = on(4),
            requiresLeavingHome = on(5),
            hasDread = on(6),
        )
    }

    @Test
    fun goldenCoversEveryArchetypeAndAllCustomCombinations() {
        assertEquals(Archetypes.all.size + 128, golden.size)
    }

    @Test
    fun archetypeRankingsMatchTheWebEngine() {
        for (arch in Archetypes.all) {
            val expected = golden.getValue(arch.id)
            assertEquals(arch.id, expected, FrictionEngine.analyze(arch.activity).map { it.id })
        }
    }

    @Test
    fun customRankingsMatchTheWebEngine() {
        var checked = 0
        for (n in 0 until 128) {
            val bits = n.toString(2).padStart(7, '0')
            val expected = golden.getValue("custom/$bits")
            assertEquals("custom/$bits", expected, FrictionEngine.analyze(custom(bits)).map { it.id })
            checked++
        }
        assertTrue(checked == 128)
    }
}

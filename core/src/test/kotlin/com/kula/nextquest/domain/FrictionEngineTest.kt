package com.kula.nextquest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionEngineTest {

    @Test
    fun everyActivityYieldsAtLeastOneFrictionPoint() {
        // The blandest possible activity (all defaults) must still return something.
        val bland = Activity(name = "Nothing special")
        assertTrue(FrictionEngine.analyze(bland).isNotEmpty())
    }

    @Test
    fun analysisIsDeterministic() {
        val a = Archetypes.all.first { it.id == "big_project" }.activity
        val first = FrictionEngine.analyze(a).map { it.id }
        val second = FrictionEngine.analyze(a).map { it.id }
        assertEquals(first, second)
    }

    @Test
    fun rankingIsSortedByScoreDescending() {
        val a = Archetypes.all.first { it.id == "big_project" }.activity
        val scores = FrictionEngine.analyze(a).map { it.score(a) }
        assertEquals(scores.sortedDescending(), scores)
    }

    @Test
    fun onlyRelevantPointsAppear() {
        // A quick, engaging, well-defined, low-stakes activity: no leaving home, no dread, no wait.
        val easy = Activity(
            name = "Fun quick thing",
            interest = Interest.ENGAGING,
            deadline = Deadline.SOFT,
            structure = Structure.DEFINED,
            duration = Duration.QUICK,
        )
        val ids = FrictionEngine.analyze(easy).map { it.id }
        assertTrue("leaving-home traps must not appear", "beingLate" !in ids && "transition" !in ids)
        assertTrue("wait trap must not appear", "waitGap" !in ids)
        assertTrue("dread trap must not appear", "dread" !in ids)
    }

    // --- Representative archetype tops: the taxonomy must produce intuitive primary traps. ---

    @Test
    fun dreadedTaskTopsAtWallOfAwful() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "boring_admin" }.activity)
        assertEquals("dread", top.id)
        assertEquals(Framework.WALL_OF_AWFUL, top.framework)
    }

    @Test
    fun fixedTimeLeaveHomeTopsAtBeingLate() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "the_appointment" }.activity)
        assertEquals("beingLate", top.id)
        assertEquals(Framework.TIME_BLINDNESS, top.framework)
    }

    @Test
    fun openEndedLongTopsAtNoFinishLine() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "big_project" }.activity)
        assertEquals("noFinishLine", top.id)
    }

    @Test
    fun boringUndreadedTaskTopsAtInitiation() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "chore" }.activity)
        assertEquals("initiation", top.id)
        assertEquals(Framework.ACTIVATION_ENERGY, top.framework)
    }

    @Test
    fun errandTopsAtTransitionCost() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "the_errand" }.activity)
        assertEquals("transition", top.id)
    }

    @Test
    fun longFuzzyDurationTopsAtTimeBlindness() {
        val top = FrictionEngine.topFor(Archetypes.all.first { it.id == "wind_down" }.activity)
        assertEquals("timeEstimation", top.id)
        assertEquals(Framework.TIME_BLINDNESS, top.framework)
    }

    @Test
    fun multiStepSurfacesWorkingMemory() {
        val a = Activity(name = "Sequence", multiStep = true)
        assertTrue("workingMemory" in FrictionEngine.analyze(a).map { it.id })
    }

    @Test
    fun heavyLoadSurfacesEnergyWindow() {
        val a = Activity(name = "Deep work", load = CognitiveLoad.HEAVY)
        assertTrue("cognitiveLoad" in FrictionEngine.analyze(a).map { it.id })
    }

    @Test
    fun openStructureSurfacesDefineDone() {
        val a = Activity(name = "Vague thing", structure = Structure.OPEN)
        assertTrue("defineDone" in FrictionEngine.analyze(a).map { it.id })
    }
}

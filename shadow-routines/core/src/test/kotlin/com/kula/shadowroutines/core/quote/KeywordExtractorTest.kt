package com.kula.shadowroutines.core.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordExtractorTest {

    @Test
    fun `drops stopwords and short tokens, keeps meaningful terms`() {
        val keywords = KeywordExtractor.extract("I'm about to run the weekly team meeting")
        assertTrue("weekly" in keywords)
        assertTrue("team" in keywords)
        assertTrue("meeting" in keywords)
        assertFalse("the" in keywords)
        assertFalse("to" in keywords)
    }

    @Test
    fun `context tags come first and are de-duplicated with text`() {
        val keywords = KeywordExtractor.extract(
            text = "workshop planning",
            contextTags = listOf("workshop", "creativity"),
        )
        assertEquals("workshop", keywords.first())
        assertEquals(keywords.distinct(), keywords)
        assertTrue("creativity" in keywords)
        assertTrue("planning" in keywords)
    }

    @Test
    fun `punctuation and case are normalized`() {
        val keywords = KeywordExtractor.extract("Deep-Work: FOCUS!! (block)")
        assertTrue("deep" in keywords)
        assertTrue("work" in keywords)
        assertTrue("focus" in keywords)
        assertTrue("block" in keywords)
    }

    @Test
    fun `empty input yields empty keywords`() {
        assertTrue(KeywordExtractor.extract("").isEmpty())
        assertTrue(KeywordExtractor.extract("   the a to of ").isEmpty())
    }
}

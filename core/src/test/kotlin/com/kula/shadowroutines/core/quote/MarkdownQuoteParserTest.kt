package com.kula.shadowroutines.core.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownQuoteParserTest {

    @Test
    fun `parses a single quote with full front-matter and strips wrapping quotes`() {
        val md = """
            ---
            id: woolf-room-1
            author: Virginia Woolf
            source: A Room of One's Own
            tags: [work, solitude, creativity, time]
            ---
            "A woman must have money and a room of her own if she is to write fiction."
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("woolf.md", md)

        assertEquals(1, quotes.size)
        val q = quotes.single()
        assertEquals("woolf-room-1", q.id)
        assertEquals("Virginia Woolf", q.author)
        assertEquals("A Room of One's Own", q.source)
        assertEquals(listOf("work", "solitude", "creativity", "time"), q.tags)
        assertEquals(
            "A woman must have money and a room of her own if she is to write fiction.",
            q.text,
        )
        assertEquals("woolf.md", q.sourceFile)
    }

    @Test
    fun `parses multiple quotes in one file`() {
        val md = """
            ---
            id: one
            tags: [a]
            ---
            First quote.

            ---
            id: two
            tags: [b, c]
            ---
            Second quote.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("multi.md", md)

        assertEquals(2, quotes.size)
        assertEquals("one", quotes[0].id)
        assertEquals("First quote.", quotes[0].text)
        assertEquals("two", quotes[1].id)
        assertEquals(listOf("b", "c"), quotes[1].tags)
    }

    @Test
    fun `supports multi-line tag lists and bare comma lists`() {
        val inline = MarkdownQuoteParser.parse(
            "x.md",
            """
            ---
            tags: courage, action
            ---
            Bare list.
            """.trimIndent(),
        ).single()
        assertEquals(listOf("courage", "action"), inline.tags)

        val multiline = MarkdownQuoteParser.parse(
            "y.md",
            """
            ---
            tags:
              - focus
              - calm
            ---
            Multi-line list.
            """.trimIndent(),
        ).single()
        assertEquals(listOf("focus", "calm"), multiline.tags)
    }

    @Test
    fun `body-only quote with no front-matter gets a derived id and no metadata`() {
        val quotes = MarkdownQuoteParser.parse("plain.md", "Just do the next small thing.")
        val q = quotes.single()
        assertTrue(q.id.startsWith("plain-"))
        assertNull(q.author)
        assertNull(q.source)
        assertTrue(q.tags.isEmpty())
        assertEquals("Just do the next small thing.", q.text)
    }

    @Test
    fun `derived id is stable across re-parsing the same content`() {
        val md = "The same words every time."
        val a = MarkdownQuoteParser.parse("f.md", md).single().id
        val b = MarkdownQuoteParser.parse("f.md", md).single().id
        assertEquals(a, b)
    }

    @Test
    fun `blank bodies are skipped`() {
        val md = """
            ---
            id: empty
            ---

            ---
            id: real
            ---
            Real content.
        """.trimIndent()
        val quotes = MarkdownQuoteParser.parse("f.md", md)
        assertEquals(listOf("real"), quotes.map { it.id })
    }

    @Test
    fun `tags are lowercased and de-duplicated`() {
        val q = MarkdownQuoteParser.parse(
            "f.md",
            """
            ---
            tags: [Work, WORK, Focus]
            ---
            text
            """.trimIndent(),
        ).single()
        assertEquals(listOf("work", "focus"), q.tags)
    }
}

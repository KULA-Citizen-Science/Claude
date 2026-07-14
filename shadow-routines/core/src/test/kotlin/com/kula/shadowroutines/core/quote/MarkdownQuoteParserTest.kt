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

    // --- Prose (no per-quote front-matter) ---

    @Test
    fun `each paragraph becomes one whole quote, kept intact`() {
        val prose = """
            Attention is the beginning of devotion. It costs us nothing at all.

            A different paragraph stands on its own.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("prose.md", prose, maxQuoteLength = 260)

        assertEquals(
            listOf(
                "Attention is the beginning of devotion. It costs us nothing at all.",
                "A different paragraph stands on its own.",
            ),
            quotes.map { it.text },
        )
        assertTrue(quotes.all { it.tags.isEmpty() })
    }

    @Test
    fun `a multi-line paragraph is unwrapped into a single quote`() {
        val prose = """
            This paragraph is wrapped
            across several source lines
            but is really one thought.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("wrap.md", prose)

        assertEquals(
            listOf("This paragraph is wrapped across several source lines but is really one thought."),
            quotes.map { it.text },
        )
    }

    @Test
    fun `a paragraph longer than the max is skipped, not truncated`() {
        val longPara = "word ".repeat(80).trim() // ~395 chars, one paragraph
        val prose = "$longPara\n\nBut this short paragraph stays."
        val quotes = MarkdownQuoteParser.parse("p.md", prose, maxQuoteLength = 260)

        assertEquals(listOf("But this short paragraph stays."), quotes.map { it.text })
    }

    @Test
    fun `heading and page-label lines are skipped`() {
        val prose = """
            ARCS OF COHERENCE 167

            This is a normal sentence that should survive parsing intact.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("book.md", prose)

        assertTrue(quotes.none { it.text.contains("ARCS OF COHERENCE") })
        assertTrue(quotes.any { it.text.contains("survive parsing") })
    }

    @Test
    fun `markdown heading prefixed lines are skipped`() {
        val quotes = MarkdownQuoteParser.parse(
            "h.md",
            """
            # Chapter One

            The real sentence lives down here and should be kept.
            """.trimIndent(),
        )
        assertTrue(quotes.none { it.text.contains("Chapter One") })
        assertEquals(1, quotes.size)
    }

    @Test
    fun `a blockquote is kept whole as one quote`() {
        val quotes = MarkdownQuoteParser.parse("bq.md", "> A short marked quote kept whole.")
        assertEquals(listOf("A short marked quote kept whole."), quotes.map { it.text })
    }

    @Test
    fun `a single sentence longer than the max is dropped`() {
        val longSentence = "word ".repeat(80).trim() + "." // ~400 chars, no internal breaks
        val prose = "$longSentence\n\nBut this short one stays."
        val quotes = MarkdownQuoteParser.parse("x.md", prose, maxQuoteLength = 120)

        assertTrue(quotes.none { it.text.length > 120 })
        assertTrue(quotes.any { it.text.contains("short one stays") })
    }

    @Test
    fun `curated front-matter quotes are never split, even when multi-sentence`() {
        val md = """
            ---
            id: c1
            ---
            First sentence here. Second sentence here. Third sentence continues on.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("c.md", md, maxQuoteLength = 20)

        assertEquals(1, quotes.size)
        assertEquals("c1", quotes.single().id)
        assertTrue(quotes.single().text.contains("First sentence"))
        assertTrue(quotes.single().text.contains("Third sentence"))
    }
}

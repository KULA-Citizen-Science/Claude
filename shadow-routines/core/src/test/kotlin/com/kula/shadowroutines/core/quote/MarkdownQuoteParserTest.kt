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

    // --- Prose (no per-quote front-matter): sentence-level extraction ---

    @Test
    fun `prose is split into one quote per sentence`() {
        val prose = "The first idea stands on its own. The second idea also stands alone."
        val quotes = MarkdownQuoteParser.parse("prose.md", prose)

        assertEquals(
            listOf("The first idea stands on its own.", "The second idea also stands alone."),
            quotes.map { it.text },
        )
        assertTrue(quotes.all { it.text.length <= MarkdownQuoteParser.DEFAULT_MAX_QUOTE_LENGTH })
    }

    @Test
    fun `a mid-sentence fragment that starts lowercase is dropped`() {
        // The tail of a sentence broken across a page starts lowercase — discard it.
        val prose = "avoid, and ignore. This one is a proper whole sentence."
        val quotes = MarkdownQuoteParser.parse("frag.md", prose)

        assertEquals(listOf("This one is a proper whole sentence."), quotes.map { it.text })
    }

    @Test
    fun `sentences longer than the max are dropped, not truncated`() {
        val longSentence = "The " + "very ".repeat(40) + "long sentence." // > 130 chars
        val prose = "$longSentence A short one survives here."
        val quotes = MarkdownQuoteParser.parse("len.md", prose, maxQuoteLength = 130)

        assertTrue(quotes.all { it.text.length <= 130 })
        assertEquals(listOf("A short one survives here."), quotes.map { it.text })
    }

    @Test
    fun `running page-label lines between paragraphs are removed`() {
        val prose = """
            The thought begins here and is complete.

            ARCS OF COHERENCE 173

            Another complete thought follows.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("book.md", prose)

        assertTrue(quotes.none { it.text.contains("ARCS OF COHERENCE") })
        assertEquals(
            listOf("The thought begins here and is complete.", "Another complete thought follows."),
            quotes.map { it.text },
        )
    }

    @Test
    fun `markdown heading lines are removed`() {
        val quotes = MarkdownQuoteParser.parse(
            "h.md",
            """
            # Chapter One

            The real sentence lives down here and is kept.
            """.trimIndent(),
        )
        assertEquals(listOf("The real sentence lives down here and is kept."), quotes.map { it.text })
    }

    @Test
    fun `footnote markers are stripped and no longer block the sentence split`() {
        val prose = "A sentence with a footnote marker.²¹ And another sentence here."
        val quotes = MarkdownQuoteParser.parse("fn.md", prose)

        assertTrue(quotes.none { it.text.contains("²") })
        assertEquals(
            listOf("A sentence with a footnote marker.", "And another sentence here."),
            quotes.map { it.text },
        )
    }

    @Test
    fun `a multi-line wrapped paragraph is unwrapped before splitting`() {
        val prose = """
            This paragraph is hard-wrapped
            across several source lines
            but is really one thought.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("wrap.md", prose)

        assertEquals(
            listOf("This paragraph is hard-wrapped across several source lines but is really one thought."),
            quotes.map { it.text },
        )
    }

    @Test
    fun `a blockquote is kept whole even when long`() {
        val long = "> " + "This marked passage runs on and on and on, " .repeat(6).trim()
        val quotes = MarkdownQuoteParser.parse("bq.md", long)
        assertEquals(1, quotes.size)
        assertTrue(quotes.single().text.length > MarkdownQuoteParser.DEFAULT_MAX_QUOTE_LENGTH)
    }

    @Test
    fun `file-level front-matter attributes every prose quote from the file`() {
        val md = """
            ---
            author: Steven Pinker
            source: The Sense of Style
            tags: [writing]
            ---
            A first quotable sentence here. A second quotable sentence here.

            A third one in a new paragraph.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("pinker.md", md)

        assertEquals(3, quotes.size)
        assertTrue(quotes.all { it.author == "Steven Pinker" })
        assertTrue(quotes.all { it.source == "The Sense of Style" })
        assertTrue(quotes.all { it.tags == listOf("writing") })
    }

    @Test
    fun `curated front-matter quotes are never split, even when multi-sentence`() {
        val md = """
            ---
            id: c1
            author: X
            ---
            First sentence here. Second sentence here. Third sentence continues on.
        """.trimIndent()

        val quotes = MarkdownQuoteParser.parse("c.md", md, maxQuoteLength = 20)

        assertEquals(1, quotes.size)
        assertEquals("c1", quotes.single().id)
        assertEquals("X", quotes.single().author)
        assertTrue(quotes.single().text.contains("First sentence"))
        assertTrue(quotes.single().text.contains("Third sentence"))
    }
}

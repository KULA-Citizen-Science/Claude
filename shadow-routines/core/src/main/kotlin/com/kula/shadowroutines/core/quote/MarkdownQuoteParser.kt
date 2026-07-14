package com.kula.shadowroutines.core.quote

/**
 * Parses a single Markdown corpus file into zero or more [ParsedQuote]s.
 *
 * ## Two kinds of input
 *
 * **1. Curated quotes** — a quote fenced by a `---` front-matter block, followed by its body.
 * A file may contain many back-to-back. These are taken *exactly as delimited* (never split),
 * because the author drew the boundaries on purpose:
 *
 * ```
 * ---
 * id: woolf-room-1
 * author: Virginia Woolf
 * source: A Room of One's Own
 * tags: [work, solitude, creativity, time]
 * ---
 * "A woman must have money and a room of her own if she is to write fiction."
 * ```
 *
 * Recognised front-matter keys: `id`, `author`, `source`, `tags`. `tags` accepts an inline
 * list (`[a, b]`), a bare comma list (`a, b`) or a multi-line `- item` list.
 *
 * **2. Prose** — text with *no* per-quote front-matter (e.g. a book or notes sideloaded as
 * `.md`). Dumping a whole chapter as one "quote" is useless, so prose is broken into
 * quote-sized pieces: paragraphs are split into sentences and greedily grouped so each quote is
 * roughly one to two sentences and no longer than [maxQuoteLength]. Markdown headings
 * (`# ...`), page/section labels (short ALL-CAPS lines like `ARCS OF COHERENCE 167`) and other
 * non-prose lines are skipped. A `>` blockquote is kept whole as one quote.
 *
 * ## Limitations (documented rather than hidden)
 * - A prose body line that is exactly `---` is misread as a fence; wrap it or split the file.
 * - Sentence splitting is punctuation-based, not linguistic; an abbreviation like "e.g." can
 *   occasionally cut a sentence early. Acceptable for short reward quotes.
 * - A single prose sentence longer than [maxQuoteLength] is dropped (it can't be shown short).
 */
object MarkdownQuoteParser {

    private const val FENCE = "---"

    /** Default upper bound on a prose quote's length, in characters (~one to two sentences). */
    const val DEFAULT_MAX_QUOTE_LENGTH = 280

    /** Prose quotes shorter than this are merged with the next sentence where possible. */
    private const val MIN_QUOTE_LENGTH = 40

    /** Anything shorter than this is treated as a fragment/label and dropped entirely. */
    private const val FRAGMENT_LENGTH = 15

    fun parse(
        fileName: String,
        content: String,
        maxQuoteLength: Int = DEFAULT_MAX_QUOTE_LENGTH,
    ): List<ParsedQuote> {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val fences = lines.indices.filter { lines[it].trim() == FENCE }
        val quotes = mutableListOf<ParsedQuote>()

        val firstFence = fences.firstOrNull()
        if (firstFence == null) {
            // No front-matter anywhere: the whole file is prose.
            addProse(quotes, fileName, lines.joinToString("\n"), maxQuoteLength)
            return quotes
        }
        // Text before the first fence is prose too.
        if (firstFence > 0) {
            addProse(quotes, fileName, lines.subList(0, firstFence).joinToString("\n"), maxQuoteLength)
        }

        // Walk fence pairs: [open, close] front-matter, then its body up to the next open fence.
        var i = 0
        while (i + 1 < fences.size) {
            val open = fences[i]
            val close = fences[i + 1]
            val front = parseFrontMatter(lines.subList(open + 1, close))
            val nextOpen = fences.getOrNull(i + 2) ?: lines.size
            val bodyLines = lines.subList(minOf(close + 1, lines.size), nextOpen)
            addCuratedQuote(quotes, fileName, front, bodyLines)
            i += 2
        }
        return quotes
    }

    // --- Curated (front-matter-delimited) quotes: taken exactly as written ---

    private fun addCuratedQuote(
        into: MutableList<ParsedQuote>,
        fileName: String,
        front: Map<String, String>,
        bodyLines: List<String>,
    ) {
        val text = cleanBody(bodyLines)
        if (text.isBlank()) return

        val tags = parseTags(front["tags"]).map { it.lowercase() }.distinct()
        val explicitId = front["id"]?.trim()?.takeIf { it.isNotBlank() }

        into += ParsedQuote(
            id = explicitId ?: deriveId(fileName, text),
            text = text,
            author = front["author"]?.trim()?.takeIf { it.isNotBlank() },
            source = front["source"]?.trim()?.takeIf { it.isNotBlank() },
            tags = tags,
            sourceFile = fileName,
        )
    }

    // --- Prose: split into quote-sized pieces ---

    private fun addProse(
        into: MutableList<ParsedQuote>,
        fileName: String,
        rawText: String,
        maxQuoteLength: Int,
    ) {
        for (piece in extractProsePieces(rawText, maxQuoteLength)) {
            into += ParsedQuote(
                id = deriveId(fileName, piece),
                text = piece,
                author = null,
                source = null,
                tags = emptyList(),
                sourceFile = fileName,
            )
        }
    }

    /** Splits prose into display-sized quotes. Visible for testing. */
    internal fun extractProsePieces(rawText: String, maxQuoteLength: Int): List<String> {
        val pieces = mutableListOf<String>()
        // Paragraphs are separated by one or more blank lines.
        for (paragraph in rawText.split(Regex("\\n\\s*\\n"))) {
            val nonBlank = paragraph.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (nonBlank.isEmpty()) continue

            // A `>` blockquote is an explicit quote: keep it whole.
            if (nonBlank.all { it.startsWith(">") }) {
                val quote = nonBlank.joinToString(" ") { it.removePrefix(">").trim() }
                addStripped(pieces, quote, maxQuoteLength, allowLong = true)
                continue
            }
            // Skip a paragraph that is a lone heading/label line.
            if (nonBlank.size == 1 && isHeadingLike(nonBlank[0])) continue
            if (nonBlank.first().startsWith("#")) continue

            val unwrapped = nonBlank.joinToString(" ")
            for (chunk in chunkSentences(unwrapped, maxQuoteLength)) {
                addStripped(pieces, chunk, maxQuoteLength, allowLong = false)
            }
        }
        return pieces
    }

    private fun addStripped(into: MutableList<String>, text: String, maxLen: Int, allowLong: Boolean) {
        val cleaned = stripWrappingQuotes(text.trim())
        if (cleaned.length < FRAGMENT_LENGTH) return
        if (!allowLong && cleaned.length > maxLen) return
        into += cleaned
    }

    /**
     * Splits a paragraph into sentences and greedily groups them so each result is at least
     * [MIN_QUOTE_LENGTH] (where possible) and at most [maxLen]. Sentences longer than [maxLen]
     * on their own are dropped.
     */
    private fun chunkSentences(paragraph: String, maxLen: Int): List<String> {
        val sentences = paragraph
            .split(Regex("(?<=[.!?][\"')”’]?)\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val out = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                out += current.toString().trim()
                current.clear()
            }
        }

        for (sentence in sentences) {
            if (sentence.length > maxLen) {
                flush() // can't shorten this one; drop it
                continue
            }
            when {
                current.isEmpty() -> current.append(sentence)
                current.length + 1 + sentence.length <= maxLen -> current.append(' ').append(sentence)
                else -> { flush(); current.append(sentence) }
            }
            // Keep quotes punchy: once we have enough, stop at the sentence boundary.
            if (current.length >= MIN_QUOTE_LENGTH) flush()
        }
        flush()
        return out
    }

    private fun isHeadingLike(line: String): Boolean {
        if (line.startsWith("#")) return true
        val endsLikeSentence = line.lastOrNull()?.let { it in ".!?\"')”’" } ?: false
        if (endsLikeSentence) return false
        if (line.length >= 60) return false
        val letters = line.count { it.isLetter() }
        if (letters == 0) return true // e.g. "167", "* * *"
        val upper = line.count { it.isUpperCase() }
        return upper.toDouble() / letters >= 0.6 // mostly-caps short line = heading/label
    }

    // --- Shared helpers ---

    private fun parseFrontMatter(lines: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var pendingListKey: String? = null
        val pendingList = mutableListOf<String>()

        fun flushList() {
            pendingListKey?.let { map[it] = pendingList.joinToString(", ") }
            pendingListKey = null
            pendingList.clear()
        }

        for (raw in lines) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            val listItem = line.trimStart()
            if (pendingListKey != null && listItem.startsWith("- ")) {
                pendingList += listItem.removePrefix("- ").trim().trim('"', '\'')
                continue
            }
            flushList()

            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val key = line.substring(0, colon).trim().lowercase()
            val value = line.substring(colon + 1).trim()
            if (value.isEmpty()) {
                pendingListKey = key // a multi-line list may follow
            } else {
                map[key] = value
            }
        }
        flushList()
        return map
    }

    private fun parseTags(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.trim()
            .removePrefix("[").removeSuffix("]")
            .split(',')
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotEmpty() }
    }

    private fun cleanBody(lines: List<String>): String =
        stripWrappingQuotes(lines.joinToString("\n").trim())

    /** Strips one wrapping pair of straight or curly double quotes, if present. */
    private fun stripWrappingQuotes(text: String): String {
        if (text.length < 2) return text
        val first = text.first()
        val last = text.last()
        val wrapped = (first == '"' && last == '"') || (first == '“' && last == '”')
        return if (wrapped) text.substring(1, text.length - 1).trim() else text
    }

    private fun deriveId(fileName: String, text: String): String {
        val base = fileName.substringAfterLast('/').substringBeforeLast('.')
        return "${slug(base)}-${shortHash(text)}"
    }

    private fun slug(value: String): String =
        value.lowercase()
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("")
            .trim('-')
            .replace(Regex("-+"), "-")
            .ifBlank { "quote" }

    /** Stable 8-char FNV-1a hash — independent of JVM String.hashCode specifics. */
    private fun shortHash(text: String): String {
        var hash = -0x7ee3623bL and 0xffffffffL // 2166136261
        for (ch in text) {
            hash = (hash xor ch.code.toLong()) and 0xffffffffL
            hash = (hash * 16777619L) and 0xffffffffL
        }
        return hash.toString(16).padStart(8, '0')
    }
}

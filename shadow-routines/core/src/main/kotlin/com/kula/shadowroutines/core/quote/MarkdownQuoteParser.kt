package com.kula.shadowroutines.core.quote

/**
 * Parses a single Markdown corpus file into zero or more [ParsedQuote]s.
 *
 * ## Two kinds of input
 *
 * **1. Curated quotes** — a quote fenced by a `---` front-matter block followed by a short body.
 * Taken *exactly as written* (never split, never length-filtered), because the author drew the
 * boundaries on purpose:
 *
 * ```
 * ---
 * author: Virginia Woolf
 * source: A Room of One's Own
 * tags: [work, solitude, creativity, time]
 * ---
 * "A woman must have money and a room of her own if she is to write fiction."
 * ```
 *
 * **2. Prose** — a book/notes sideloaded as `.md`, with no per-quote front-matter. Prose is
 * broken into **one quote per sentence**, and each quote must:
 *  - be no longer than [maxQuoteLength] characters (longer sentences are dropped, not truncated),
 *  - **start at a real sentence boundary** (begin with a capital, digit or opening quote) — this
 *    discards mid-sentence fragments left behind when a book breaks a sentence across a page,
 *  - not be a heading or running page-label (`# ...`, or a short mostly-ALL-CAPS line such as
 *    `ARCS OF COHERENCE 173`), which are removed even when they sit between paragraphs.
 * Footnote markers (superscripts, or digits stuck to a sentence's end) are stripped.
 *
 * A `>` blockquote is kept whole as one quote even if long, since it was explicitly marked.
 *
 * ### Attribution for prose
 * A prose file may carry **one file-level front-matter block at the very top** with `author` /
 * `source` / `tags`; that metadata is applied to *every* quote extracted from the file, so
 * sideloaded books can still show their source on the reward card:
 *
 * ```
 * ---
 * author: Steven Pinker
 * source: The Sense of Style
 * tags: [writing]
 * ---
 * <the whole book as prose...>
 * ```
 *
 * ## Limitations (documented rather than hidden)
 * - A prose body line that is exactly `---` is misread as a fence; wrap it or split the file.
 * - Sentence splitting is punctuation-based; an abbreviation like "e.g." can cut early.
 * - A sentence longer than [maxQuoteLength] is dropped. Raise the limit or mark it with `>`.
 */
object MarkdownQuoteParser {

    private const val FENCE = "---"

    /** Default upper bound on a prose quote's length, in characters. */
    const val DEFAULT_MAX_QUOTE_LENGTH = 130

    /** Shorter than this is treated as a fragment/label and dropped. */
    private const val FRAGMENT_LENGTH = 15

    /**
     * A front-matter body up to this length with no blank-line paragraph break is treated as a
     * single curated quote; anything larger is treated as file-level metadata over prose. Set
     * well above [DEFAULT_MAX_QUOTE_LENGTH] so ordinary curated quotes are never prose-split.
     */
    private const val CURATED_MAX_BODY = 400

    private val BLANK_LINE = Regex("\\n\\s*\\n")
    private val SENTENCE_SPLIT = Regex("(?<=[.!?][)\"'”’]?)\\s+")
    private val SUPERSCRIPTS = Regex("[²³¹⁰-⁹]+")
    private val TRAILING_FOOTNOTE = Regex("(?<=[.!?])\\d{1,3}(?=\\s|$)")

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
            addProse(quotes, fileName, content, maxQuoteLength, null, null, emptyList())
            return quotes
        }
        if (firstFence > 0) {
            val lead = lines.subList(0, firstFence).joinToString("\n")
            addProse(quotes, fileName, lead, maxQuoteLength, null, null, emptyList())
        }

        var i = 0
        while (i + 1 < fences.size) {
            val open = fences[i]
            val close = fences[i + 1]
            val front = parseFrontMatter(lines.subList(open + 1, close))
            val nextOpen = fences.getOrNull(i + 2) ?: lines.size
            val bodyLines = lines.subList(minOf(close + 1, lines.size), nextOpen)
            val bodyText = bodyLines.joinToString("\n").trim()

            if (isCuratedBody(bodyText)) {
                addCuratedQuote(quotes, fileName, front, bodyText)
            } else {
                // Large body: treat this block's front-matter as file-level metadata over prose.
                addProse(
                    quotes, fileName, bodyText, maxQuoteLength,
                    author = front["author"]?.trim()?.takeIf { it.isNotBlank() },
                    source = front["source"]?.trim()?.takeIf { it.isNotBlank() },
                    tags = parseTags(front["tags"]),
                )
            }
            i += 2
        }
        return quotes
    }

    private fun isCuratedBody(text: String): Boolean =
        text.isNotBlank() && text.length <= CURATED_MAX_BODY && !BLANK_LINE.containsMatchIn(text)

    // --- Curated (front-matter-delimited) quotes: taken exactly as written ---

    private fun addCuratedQuote(
        into: MutableList<ParsedQuote>,
        fileName: String,
        front: Map<String, String>,
        bodyText: String,
    ) {
        val text = stripWrappingQuotes(bodyText)
        if (text.isBlank()) return
        val explicitId = front["id"]?.trim()?.takeIf { it.isNotBlank() }
        into += ParsedQuote(
            id = explicitId ?: deriveId(fileName, text),
            text = text,
            author = front["author"]?.trim()?.takeIf { it.isNotBlank() },
            source = front["source"]?.trim()?.takeIf { it.isNotBlank() },
            tags = parseTags(front["tags"]).map { it.lowercase() }.distinct(),
            sourceFile = fileName,
        )
    }

    // --- Prose: split into sentence-sized quotes, inheriting any file-level metadata ---

    private fun addProse(
        into: MutableList<ParsedQuote>,
        fileName: String,
        rawText: String,
        maxQuoteLength: Int,
        author: String?,
        source: String?,
        tags: List<String>,
    ) {
        val normalizedTags = tags.map { it.lowercase() }.distinct()
        for (piece in extractProsePieces(rawText, maxQuoteLength)) {
            into += ParsedQuote(
                id = deriveId(fileName, piece),
                text = piece,
                author = author,
                source = source,
                tags = normalizedTags,
                sourceFile = fileName,
            )
        }
    }

    private data class Block(val text: String, val isBlockquote: Boolean)

    /** Splits prose into sentence-sized quote strings. Visible for testing. */
    internal fun extractProsePieces(rawText: String, maxQuoteLength: Int): List<String> {
        val pieces = mutableListOf<String>()
        for (block in buildBlocks(rawText)) {
            if (block.isBlockquote) {
                addStripped(pieces, block.text, maxQuoteLength, allowLong = true, requireSentenceStart = false)
            } else {
                // Clean footnote markers first, so ".²¹ Next" still splits at the sentence.
                for (sentence in SENTENCE_SPLIT.split(cleanFootnotes(block.text))) {
                    addStripped(pieces, sentence, maxQuoteLength, allowLong = false, requireSentenceStart = true)
                }
            }
        }
        return pieces
    }

    /**
     * Groups lines into prose paragraphs and blockquotes. A blank line, a heading, or a running
     * page-label line ends the current paragraph; heading/label lines are dropped entirely so a
     * sentence never fuses across one.
     */
    private fun buildBlocks(rawText: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val prose = StringBuilder()
        val quote = StringBuilder()

        fun flushProse() {
            if (prose.isNotEmpty()) { blocks += Block(prose.toString().trim(), false); prose.clear() }
        }
        fun flushQuote() {
            if (quote.isNotEmpty()) { blocks += Block(quote.toString().trim(), true); quote.clear() }
        }

        for (raw in rawText.split("\n")) {
            val line = raw.trim()
            when {
                line.isEmpty() -> { flushProse(); flushQuote() }
                line.startsWith(">") -> { flushProse(); quote.append(line.removePrefix(">").trim()).append(' ') }
                line.startsWith("#") || isHeadingLike(line) -> { flushProse(); flushQuote() } // drop the heading line
                else -> { flushQuote(); prose.append(line).append(' ') }
            }
        }
        flushProse()
        flushQuote()
        return blocks
    }

    private fun addStripped(
        into: MutableList<String>,
        text: String,
        maxLen: Int,
        allowLong: Boolean,
        requireSentenceStart: Boolean,
    ) {
        val cleaned = stripWrappingQuotes(text.trim()).trim()
        if (cleaned.length < FRAGMENT_LENGTH) return
        if (!allowLong && cleaned.length > maxLen) return
        if (requireSentenceStart && !startsLikeSentence(cleaned)) return
        into += cleaned
    }

    private fun cleanFootnotes(sentence: String): String =
        sentence.replace(SUPERSCRIPTS, "").replace(TRAILING_FOOTNOTE, "").trim()

    /** A real quote starts at a sentence boundary: a capital, a digit, or an opening quote. */
    private fun startsLikeSentence(s: String): Boolean {
        val c = s.first()
        return c.isUpperCase() || c.isDigit() || c in "\"'“‘("
    }

    private fun isHeadingLike(line: String): Boolean {
        if (line.startsWith("#")) return true
        val endsLikeSentence = line.lastOrNull()?.let { it in ".!?\"')”’" } ?: false
        if (endsLikeSentence) return false
        if (line.length >= 60) return false
        val letters = line.count { it.isLetter() }
        if (letters == 0) return true // e.g. "173", "* * *"
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

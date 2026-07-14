package com.kula.shadowroutines.core.quote

/**
 * Parses a single Markdown corpus file into zero or more [ParsedQuote]s.
 *
 * ## Supported format
 * Each quote is an optional YAML-ish front-matter block fenced by `---` lines, followed by
 * the quote body. A file may contain many quotes back-to-back:
 *
 * ```
 * ---
 * id: woolf-room-1
 * author: Virginia Woolf
 * source: A Room of One's Own
 * tags: [work, solitude, creativity, time]
 * ---
 * "A woman must have money and a room of her own if she is to write fiction."
 *
 * ---
 * tags: [courage]
 * ---
 * Do the thing you think you cannot do.
 * ```
 *
 * Recognised front-matter keys: `id`, `author`, `source`, `tags`. `tags` accepts an inline
 * list (`[a, b]`), a bare comma list (`a, b`) or a multi-line `- item` list. Unknown keys are
 * ignored so the format stays forgiving.
 *
 * ## Limitations (by design, documented rather than hidden)
 * - A body line that is exactly `---` would be misread as a fence. Quotes are short, so this
 *   is an accepted trade-off; wrap such content or split the file.
 * - Front-matter values are treated as plain strings; this is not a full YAML parser.
 */
object MarkdownQuoteParser {

    private const val FENCE = "---"

    fun parse(fileName: String, content: String): List<ParsedQuote> {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")

        // Indices of every fence line ("---" possibly with trailing spaces).
        val fences = lines.indices.filter { lines[it].trim() == FENCE }

        val quotes = mutableListOf<ParsedQuote>()

        // Any non-blank text before the first fence is a body-only quote (no metadata).
        val firstFence = fences.firstOrNull()
        if (firstFence == null) {
            addQuote(quotes, fileName, front = emptyMap(), bodyLines = lines)
            return quotes
        }
        if (firstFence > 0) {
            addQuote(quotes, fileName, front = emptyMap(), bodyLines = lines.subList(0, firstFence))
        }

        // Walk fence pairs: [open, close] front-matter, then body up to the next open fence.
        var i = 0
        while (i + 1 < fences.size) {
            val open = fences[i]
            val close = fences[i + 1]
            val front = parseFrontMatter(lines.subList(open + 1, close))

            val nextOpen = fences.getOrNull(i + 2) ?: lines.size
            val bodyLines = lines.subList(minOf(close + 1, lines.size), nextOpen)
            addQuote(quotes, fileName, front, bodyLines)

            i += 2
        }
        return quotes
    }

    private fun addQuote(
        into: MutableList<ParsedQuote>,
        fileName: String,
        front: Map<String, String>,
        bodyLines: List<String>,
    ) {
        val text = cleanBody(bodyLines)
        if (text.isBlank()) return

        val tags = parseTags(front["tags"]).map { it.lowercase() }.distinct()
        val explicitId = front["id"]?.trim()?.takeIf { it.isNotBlank() }
        val id = explicitId ?: deriveId(fileName, text)

        into += ParsedQuote(
            id = id,
            text = text,
            author = front["author"]?.trim()?.takeIf { it.isNotBlank() },
            source = front["source"]?.trim()?.takeIf { it.isNotBlank() },
            tags = tags,
            sourceFile = fileName,
        )
    }

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
                // Possibly a multi-line list follows.
                pendingListKey = key
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

    /** Joins body lines, trims blank edges, and strips one wrapping pair of double quotes. */
    private fun cleanBody(lines: List<String>): String {
        val joined = lines.joinToString("\n").trim()
        if (joined.length >= 2 && joined.first() == '"' && joined.last() == '"') {
            return joined.substring(1, joined.length - 1).trim()
        }
        return joined
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

package com.kula.shadowroutines.core.quote

/**
 * A quote as extracted from the Markdown corpus, before it is stored. The [id] is stable
 * across re-scans (either taken from front-matter or derived from the file + content hash),
 * so per-quote state like "seen count" and "favorite" survives a full corpus rebuild.
 */
data class ParsedQuote(
    val id: String,
    val text: String,
    val author: String?,
    val source: String?,
    val tags: List<String>,
    val sourceFile: String,
)

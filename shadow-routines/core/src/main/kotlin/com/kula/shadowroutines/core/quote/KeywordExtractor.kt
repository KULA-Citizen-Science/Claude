package com.kula.shadowroutines.core.quote

/**
 * Turns free text (a task title/description) plus explicit context tags into a small,
 * normalized set of query keywords: lowercased, de-punctuated, stop-worded, short tokens
 * dropped, de-duplicated. Order is preserved (tags first, then title/description terms) so a
 * caller can weight earlier terms if it wants.
 */
object KeywordExtractor {

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "you", "your", "are", "was", "were", "this", "that",
        "have", "has", "had", "will", "would", "could", "should", "about", "into", "from",
        "out", "off", "over", "under", "then", "than", "them", "they", "our", "his", "her",
        "its", "not", "but", "all", "any", "can", "get", "got", "how", "who", "why", "what",
        "when", "where", "a", "an", "of", "to", "in", "on", "at", "is", "it", "be", "or", "as",
        "im", "ill", "i'm", "i'll", "am", "do", "doing", "before", "after",
    )

    private const val MIN_LENGTH = 3

    /**
     * @param text the task title and/or description, concatenated by the caller.
     * @param contextTags template/context tags that should always be part of the query.
     */
    fun extract(text: String, contextTags: List<String> = emptyList()): List<String> {
        val fromTags = contextTags.flatMap { tokenize(it) }
        val fromText = tokenize(text)
        return (fromTags + fromText)
            .filter { it.length >= MIN_LENGTH && it !in STOP_WORDS }
            .distinct()
    }

    private fun tokenize(value: String): List<String> =
        value.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
}

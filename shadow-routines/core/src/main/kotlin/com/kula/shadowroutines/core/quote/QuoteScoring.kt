package com.kula.shadowroutines.core.quote

/**
 * The single source of truth for keyword relevance scoring, shared by every [QuoteIndex]
 * backend (the in-memory index here and the Room FTS index in :app) so ranking behaves
 * identically regardless of how candidates were fetched. A tag hit counts for more than a
 * body hit; the scheme is deliberately simple and easy to reason about.
 */
object QuoteScoring {

    const val TAG_WEIGHT = 2.0
    const val BODY_WEIGHT = 1.0

    /** Splits body text into lowercase alphanumeric tokens (matches [KeywordExtractor]). */
    fun tokenize(text: String): Set<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()

    /**
     * @param keywords already normalized (see [KeywordExtractor]).
     * @param bodyTokens pre-tokenized body, from [tokenize].
     * @param tags lowercased tags.
     * @return relevance score; 0.0 means no match.
     */
    fun score(keywords: Collection<String>, bodyTokens: Set<String>, tags: List<String>): Double {
        var score = 0.0
        for (k in keywords) {
            if (tags.any { it == k || it.contains(k) }) score += TAG_WEIGHT
            if (k in bodyTokens) score += BODY_WEIGHT
        }
        return score
    }
}

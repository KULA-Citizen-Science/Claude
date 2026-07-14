package com.kula.shadowroutines.core.quote

/**
 * An immutable, capped "recently seen" buffer of quote ids, most-recent-first. Persisted by
 * the app layer so novelty survives app restarts. [capacity] is the strong anti-repetition
 * window: while an id sits in this buffer it is excluded from selection unless constraints are
 * relaxed.
 */
data class RecentlySeen(
    val ids: List<String> = emptyList(),
    val capacity: Int = DEFAULT_CAPACITY,
) {
    fun contains(id: String): Boolean = id in ids

    /** Returns a new buffer with [id] moved to the front, de-duplicated and trimmed. */
    fun withSeen(id: String): RecentlySeen {
        val next = (listOf(id) + ids.filterNot { it == id }).take(capacity)
        return copy(ids = next)
    }

    /**
     * Position from the front (0 = most recent). Larger = seen longer ago. `Int.MAX_VALUE`
     * means "not in the buffer at all" — i.e. safe to show. Used to relax exclusion oldest-first.
     */
    fun recencyRank(id: String): Int {
        val idx = ids.indexOf(id)
        return if (idx < 0) Int.MAX_VALUE else idx
    }

    companion object {
        const val DEFAULT_CAPACITY = 50
    }
}

/**
 * A shuffled deck of quote ids. Walking the deck guarantees every quote is drawn once before
 * any repeat; the deck only reshuffles when exhausted. This is the coverage guarantee used as
 * the fallback when keyword search yields nothing.
 *
 * Persisted as [order] + [position]. If the corpus changes, rebuild with [freshFrom].
 */
data class Deck(
    val order: List<String>,
    val position: Int = 0,
) {
    val isExhausted: Boolean get() = position >= order.size

    /**
     * Draws the next id, reshuffling first if exhausted. Returns the id (or null if the deck
     * is empty) and the advanced deck. Reshuffle uses [shuffle] so tests can inject a seeded
     * ordering.
     */
    fun draw(shuffle: (List<String>) -> List<String>): Pair<String?, Deck> {
        if (order.isEmpty()) return null to this
        val active = if (isExhausted) Deck(shuffle(order), 0) else this
        return active.order[active.position] to active.copy(position = active.position + 1)
    }

    /** True if this deck's contents match [ids] (order-independent), i.e. it is still valid. */
    fun matches(ids: Collection<String>): Boolean = order.toSet() == ids.toSet()

    companion object {
        fun freshFrom(ids: List<String>, shuffle: (List<String>) -> List<String>): Deck =
            Deck(order = shuffle(ids), position = 0)
    }
}

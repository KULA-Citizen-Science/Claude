package com.kula.shadowroutines.data.novelty

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kula.shadowroutines.core.quote.Deck
import com.kula.shadowroutines.core.quote.RecentlySeen
import kotlinx.coroutines.flow.first

/**
 * Persists the anti-repetition state (recently-seen buffer + deck order/position) so novelty
 * survives app restarts. Everything is keyed by stable quote id, so a corpus rescan does not
 * reset it; the [Deck] rebuilds itself if the id set drifts.
 *
 * Lists are stored newline-joined — ids are slugs/hashes with no newlines.
 */
class NoveltyStore(private val dataStore: DataStore<Preferences>) {

    private val recentKey = stringPreferencesKey("recent_ids")
    private val deckOrderKey = stringPreferencesKey("deck_order")
    private val deckPositionKey = intPreferencesKey("deck_position")

    suspend fun loadRecentlySeen(): RecentlySeen {
        val prefs = dataStore.data.first()
        return RecentlySeen(ids = decode(prefs[recentKey]), capacity = RecentlySeen.DEFAULT_CAPACITY)
    }

    suspend fun loadDeck(): Deck {
        val prefs = dataStore.data.first()
        return Deck(order = decode(prefs[deckOrderKey]), position = prefs[deckPositionKey] ?: 0)
    }

    suspend fun save(recentlySeen: RecentlySeen, deck: Deck) {
        dataStore.edit { prefs ->
            prefs[recentKey] = encode(recentlySeen.ids)
            prefs[deckOrderKey] = encode(deck.order)
            prefs[deckPositionKey] = deck.position
        }
    }

    private fun encode(ids: List<String>): String = ids.joinToString("\n")

    private fun decode(raw: String?): List<String> =
        raw?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
}

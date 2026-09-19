package com.kula.anagram.data

import android.content.Context
import com.kula.anagram.core.SavedAnagram

/**
 * Local, offline persistence for the user's saved anagrams, backed by [android.content.SharedPreferences].
 *
 * The app is single-purpose and stores at most a modest list of short words, so a plain preferences
 * blob is plenty — no database or extra dependency needed. Each entry is encoded as one line of
 * `source\ttext\tcreatedAt`; tabs and newlines are stripped from the free-text fields on the way in
 * so a saved value can never corrupt the record separator.
 */
class SavedAnagramStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("anagram_saves", Context.MODE_PRIVATE)

    /** All saved anagrams, newest first. */
    fun load(): List<SavedAnagram> {
        val blob = prefs.getString(KEY, "").orEmpty()
        if (blob.isBlank()) return emptyList()
        return blob.split(RECORD_SEP)
            .mapNotNull { decode(it) }
            .sortedByDescending { it.createdAt }
    }

    /** Persists [items] verbatim, replacing whatever was stored before. */
    fun save(items: List<SavedAnagram>) {
        val blob = items.joinToString(RECORD_SEP) { encode(it) }
        prefs.edit().putString(KEY, blob).apply()
    }

    private fun encode(item: SavedAnagram): String =
        "${item.source.sanitize()}$FIELD_SEP${item.text.sanitize()}$FIELD_SEP${item.createdAt}"

    private fun decode(line: String): SavedAnagram? {
        val parts = line.split(FIELD_SEP)
        if (parts.size != 3) return null
        val createdAt = parts[2].toLongOrNull() ?: return null
        return SavedAnagram(source = parts[0], text = parts[1], createdAt = createdAt)
    }

    private fun String.sanitize(): String =
        replace(FIELD_SEP, " ").replace(RECORD_SEP, " ")

    private companion object {
        const val KEY = "saved"
        const val RECORD_SEP = "\n"
        const val FIELD_SEP = "\t"
    }
}

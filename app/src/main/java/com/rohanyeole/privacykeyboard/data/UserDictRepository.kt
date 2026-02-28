package com.rohanyeole.privacykeyboard.data

import android.content.Context
import android.util.Log
import com.rohanyeole.privacykeyboard.util.isValidWord

class UserDictRepository(context: Context) {
    private val prefs = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
    private val TAG = "UserDictRepository"

    // Always return a fresh mutable copy to avoid Android live-set hazard
    private fun loadRaw(): MutableSet<String> =
        mutableSetOf<String>().also {
            it.addAll(prefs.getStringSet("user_dict", emptySet()) ?: emptySet())
        }

    private fun decodeWord(entry: String): String = entry.substringBefore(":")
    private fun decodeCount(entry: String): Int = entry.substringAfter(":", "0").toIntOrNull() ?: 0
    private fun encodeEntry(word: String, count: Int): String = "$word:$count"

    // Returns bare words — backward-compatible with old bare-word entries
    fun getAll(): Set<String> = loadRaw().map { decodeWord(it) }.toSet()

    fun getAllWithFrequency(): Map<String, Int> =
        loadRaw().associate { decodeWord(it) to decodeCount(it) }

    fun getTopWords(n: Int): List<String> =
        getAllWithFrequency()
            .entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key }

    fun incrementFrequency(word: String) {
        val raw = loadRaw()
        val existing = raw.find { decodeWord(it) == word }
        val newCount = if (existing != null) {
            raw.remove(existing)
            decodeCount(existing) + 1
        } else {
            1
        }
        raw.add(encodeEntry(word, newCount))
        prefs.edit().putStringSet("user_dict", raw).apply()
    }

    fun save(word: String) {
        val trimmed = word.trim()
        val raw = loadRaw()
        val existingWords = raw.map { decodeWord(it) }.toSet()
        if (isValidWord(trimmed) && !existingWords.contains(trimmed)) {
            Log.i(TAG, "Saving word: $trimmed")
            raw.add(encodeEntry(trimmed, 0))
            prefs.edit().putStringSet("user_dict", raw).apply()
        } else {
            Log.i(TAG, "Invalid or duplicate word: $trimmed")
        }
    }
}

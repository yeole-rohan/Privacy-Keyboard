package com.example.privacykeyboard.data

import android.content.Context

class EmojiRepository(context: Context) {
    private val prefs = context.getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_RECENT_EMOJIS = 50
        private const val RECENT_KEY = "recent_emojis"
    }

    fun loadRecent(): List<String> {
        val str = prefs.getString(RECENT_KEY, "") ?: ""
        return if (str.isEmpty()) emptyList() else str.split(",")
    }

    fun addToRecent(emoji: String) {
        val list = loadRecent().toMutableList()
        list.remove(emoji)
        list.add(0, emoji)
        if (list.size > MAX_RECENT_EMOJIS) list.removeAt(list.lastIndex)
        prefs.edit().putString(RECENT_KEY, list.joinToString(",")).apply()
    }
}

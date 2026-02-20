package com.example.privacykeyboard.data

import android.content.Context
import android.util.Log
import com.example.privacykeyboard.util.isValidWord

class UserDictRepository(context: Context) {
    private val prefs = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
    private val TAG = "UserDictRepository"

    fun getAll(): Set<String> =
        prefs.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()

    fun save(word: String) {
        val trimmed = word.trim()
        val existing = prefs.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()
        if (isValidWord(trimmed) && !existing.contains(trimmed)) {
            Log.i(TAG, "Saving word: $trimmed")
            existing.add(trimmed)
            prefs.edit().putStringSet("user_dict", existing).apply()
        } else {
            Log.i(TAG, "Invalid or duplicate word: $trimmed")
        }
    }
}

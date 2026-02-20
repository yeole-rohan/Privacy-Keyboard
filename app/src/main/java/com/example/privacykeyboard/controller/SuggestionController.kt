package com.example.privacykeyboard.controller

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.privacykeyboard.data.UserDictRepository
import com.example.privacykeyboard.trie.Trie

class SuggestionController(
    private val trie: Trie,
    private val userDictRepo: UserDictRepository,
    private val autocompleteArea: LinearLayout,
    private val suggestionViews: List<TextView>,
    private val onWordSelected: (word: String, rawInput: String) -> Unit
) {
    fun update(inputText: String) {
        val rawLastWord = inputText.split(" ").lastOrNull() ?: ""
        val lastInputText = rawLastWord.lowercase()

        if (lastInputText.isEmpty()) {
            hide()
            return
        }

        val userDictSuggestions = userDictRepo.getAll()
            .filter { it.startsWith(lastInputText, ignoreCase = true) }

        val suggestionsToShow = mutableListOf<String>()
        suggestionsToShow.addAll(userDictSuggestions.take(2))

        if (suggestionsToShow.size < 3) {
            trie.searchByPrefix(lastInputText).forEach { suggestion ->
                if (!suggestionsToShow.contains(suggestion)) {
                    suggestionsToShow.add(suggestion)
                }
            }
        }

        for (i in suggestionViews.indices) {
            if (i < suggestionsToShow.size) {
                suggestionViews[i].text = suggestionsToShow[i]
                suggestionViews[i].visibility = View.VISIBLE
            } else {
                suggestionViews[i].visibility = View.GONE
            }
        }

        autocompleteArea.visibility = if (suggestionsToShow.isEmpty()) View.GONE else View.VISIBLE

        suggestionViews.forEach { tv ->
            tv.setOnClickListener {
                val word = tv.text.toString()
                if (word.isNotEmpty()) {
                    onWordSelected(word, rawLastWord)
                    hide()
                }
            }
        }
    }

    fun hide() {
        autocompleteArea.visibility = View.GONE
        suggestionViews.forEach { it.visibility = View.GONE }
    }
}

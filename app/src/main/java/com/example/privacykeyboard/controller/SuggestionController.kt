package com.example.privacykeyboard.controller

import android.view.View
import android.widget.TextView
import com.example.privacykeyboard.data.UserDictRepository
import com.example.privacykeyboard.trie.Trie

class SuggestionController(
    private val trie: Trie,
    private val userDictRepo: UserDictRepository,
    private val suggestionViews: List<TextView>,
    private val dividers: List<View>,           // [divider12, divider23]
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

        // Update suggestion text + visibility
        for (i in suggestionViews.indices) {
            if (i < suggestionsToShow.size) {
                suggestionViews[i].text = suggestionsToShow[i]
                suggestionViews[i].visibility = View.VISIBLE
            } else {
                suggestionViews[i].visibility = View.GONE
            }
        }

        // Divider between suggestion1 and suggestion2: show only when both visible
        if (dividers.size >= 1) {
            dividers[0].visibility =
                if (suggestionViews.size >= 2 &&
                    suggestionViews[0].visibility == View.VISIBLE &&
                    suggestionViews[1].visibility == View.VISIBLE)
                    View.VISIBLE else View.GONE
        }
        // Divider between suggestion2 and suggestion3: show only when both visible
        if (dividers.size >= 2) {
            dividers[1].visibility =
                if (suggestionViews.size >= 3 &&
                    suggestionViews[1].visibility == View.VISIBLE &&
                    suggestionViews[2].visibility == View.VISIBLE)
                    View.VISIBLE else View.GONE
        }

        // Wire tap handlers
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
        suggestionViews.forEach { it.visibility = View.GONE }
        dividers.forEach { it.visibility = View.GONE }
    }
}

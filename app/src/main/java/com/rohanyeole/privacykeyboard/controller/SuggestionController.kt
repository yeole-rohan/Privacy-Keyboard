package com.rohanyeole.privacykeyboard.controller

import android.view.View
import android.widget.TextView
import com.rohanyeole.privacykeyboard.data.UserDictRepository
import com.rohanyeole.privacykeyboard.trie.Trie

class SuggestionController(
    private val trie: Trie,
    private val userDictRepo: UserDictRepository,
    private val suggestionViews: List<TextView>,
    private val dividers: List<View>,           // [divider12, divider23]
    private val onWordSelected: (word: String, rawInput: String) -> Unit
) {
    // QWERTY physical adjacency for all 26 keys
    private val adjacency: Map<Char, String> = mapOf(
        'q' to "was",    'w' to "qeasd",  'e' to "wrsdf",  'r' to "etdfg",
        't' to "ryfgh",  'y' to "tughj",  'u' to "yihjk",  'i' to "uojkl",
        'o' to "ipkl",   'p' to "ol",     'a' to "qwsz",   's' to "awedzx",
        'd' to "serfxc", 'f' to "drtgcv", 'g' to "ftyhvb", 'h' to "gyujbn",
        'j' to "huiknm", 'k' to "jiolm",  'l' to "kop",    'z' to "asx",
        'x' to "zsdc",   'c' to "xdfv",   'v' to "cfgb",   'b' to "vghn",
        'n' to "bhjm",   'm' to "njk"
    )

    fun update(inputText: String) {
        val rawLastWord = inputText.split(" ").lastOrNull() ?: ""

        if (rawLastWord.isEmpty()) {
            if (inputText.isNotEmpty()) {
                showPostSpaceSuggestions()     // Improvement #7
            } else {
                hide()
            }
            return
        }

        val lastInputText = rawLastWord.lowercase()
        val suggestionsToShow = mutableListOf<String>()

        // Improvement #1 + #2: user-dict suggestions sorted by frequency, take ≤2
        val userFreqMap = userDictRepo.getAllWithFrequency()
        userFreqMap.entries
            .filter { it.key.startsWith(lastInputText, ignoreCase = true) }
            .sortedByDescending { it.value }
            .take(2)
            .forEach { suggestionsToShow.add(it.key) }

        // Improvement #1: trie prefix suggestions (already freq-sorted internally)
        if (suggestionsToShow.size < 3) {
            for (s in trie.searchByPrefix(lastInputText)) {
                if (suggestionsToShow.size >= 3) break
                if (!suggestionsToShow.contains(s)) suggestionsToShow.add(s)
            }
        }

        // Improvement #4: adjacency substitution (fat-finger correction)
        if (suggestionsToShow.size < 3 && lastInputText.length > 2) {
            outer@ for (i in lastInputText.indices) {
                val neighbors = adjacency[lastInputText[i]] ?: continue
                for (neighbor in neighbors) {
                    if (suggestionsToShow.size >= 3) break@outer
                    val candidate = lastInputText.substring(0, i) + neighbor + lastInputText.substring(i + 1)
                    if (trie.contains(candidate) && !suggestionsToShow.contains(candidate)) {
                        suggestionsToShow.add(candidate)
                    }
                }
            }
        }

        // Improvement #5: transposition (swap adjacent pair)
        if (suggestionsToShow.size < 3 && lastInputText.length > 2) {
            for (i in 0 until lastInputText.length - 1) {
                if (suggestionsToShow.size >= 3) break
                val chars = lastInputText.toCharArray()
                chars[i] = lastInputText[i + 1]
                chars[i + 1] = lastInputText[i]
                val candidate = String(chars)
                if (trie.contains(candidate) && !suggestionsToShow.contains(candidate)) {
                    suggestionsToShow.add(candidate)
                }
            }
        }

        // Improvement #6: capitalize first suggestion letter at sentence start
        val displayList = if (isSentenceStart(inputText, rawLastWord)) {
            suggestionsToShow.map { it.replaceFirstChar { c -> c.uppercase() } }
        } else {
            suggestionsToShow.toList()
        }

        renderSuggestions(displayList, rawLastWord)
    }

    // Improvement #7: post-space suggestions from personal top words
    private fun showPostSpaceSuggestions() {
        val topWords = userDictRepo.getTopWords(3)
        if (topWords.isEmpty()) {
            hide()
            return
        }
        renderSuggestions(topWords, "")
    }

    private fun isSentenceStart(inputText: String, rawLastWord: String): Boolean {
        val beforeWord = inputText.dropLast(rawLastWord.length)
        return beforeWord.isEmpty() || beforeWord.takeLast(2) in setOf(". ", "! ", "? ")
    }

    private fun renderSuggestions(displayList: List<String>, rawLastWord: String) {
        for (i in suggestionViews.indices) {
            if (i < displayList.size) {
                suggestionViews[i].text = displayList[i]
                suggestionViews[i].visibility = View.VISIBLE
            } else {
                suggestionViews[i].visibility = View.GONE
            }
        }

        // Divider between suggestion1 and suggestion2
        if (dividers.size >= 1) {
            dividers[0].visibility =
                if (suggestionViews.size >= 2 &&
                    suggestionViews[0].visibility == View.VISIBLE &&
                    suggestionViews[1].visibility == View.VISIBLE)
                    View.VISIBLE else View.GONE
        }
        // Divider between suggestion2 and suggestion3
        if (dividers.size >= 2) {
            dividers[1].visibility =
                if (suggestionViews.size >= 3 &&
                    suggestionViews[1].visibility == View.VISIBLE &&
                    suggestionViews[2].visibility == View.VISIBLE)
                    View.VISIBLE else View.GONE
        }

        // Improvement #3: learn from taps — increment frequency on selection
        suggestionViews.forEach { tv ->
            tv.setOnClickListener {
                val word = tv.text.toString()
                if (word.isNotEmpty()) {
                    onWordSelected(word, rawLastWord)
                    userDictRepo.incrementFrequency(word.lowercase())
                    trie.incrementFrequency(word.lowercase())
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

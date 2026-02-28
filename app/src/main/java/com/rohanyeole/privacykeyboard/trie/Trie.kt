package com.rohanyeole.privacykeyboard.trie

class Trie {
    private val root = TrieNode()

    fun insert(word: String, freq: Int = 1) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isEndOfWord = true
        node.frequency = freq
    }

    fun contains(word: String): Boolean {
        var node = root
        for (char in word) {
            node = node.children[char] ?: return false
        }
        return node.isEndOfWord
    }

    fun incrementFrequency(word: String) {
        var node = root
        for (char in word) {
            node = node.children[char] ?: return
        }
        if (node.isEndOfWord) node.frequency++
    }

    fun searchByPrefix(prefix: String, limit: Int = 20): List<String> {
        val result = mutableListOf<Pair<String, Int>>()
        var node = root
        for (char in prefix) {
            node = node.children[char] ?: return emptyList()
        }
        dfs(node, prefix, result, limit)
        return result
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.length })
            .map { it.first }
    }

    private fun dfs(node: TrieNode, prefix: String, result: MutableList<Pair<String, Int>>, limit: Int) {
        if (result.size >= limit) return
        if (node.isEndOfWord) {
            result.add(prefix to node.frequency)
        }
        for ((char, child) in node.children) {
            if (result.size >= limit) return
            dfs(child, prefix + char, result, limit)
        }
    }
}

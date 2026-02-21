package com.example.privacykeyboard.trie

class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isEndOfWord = true
    }

    fun searchByPrefix(prefix: String, limit: Int = 20): List<String> {
        val result = mutableListOf<String>()
        var node = root
        for (char in prefix) {
            node = node.children[char] ?: return result
        }
        dfs(node, prefix, result, limit)
        return result.sortedBy { it.length }
    }

    private fun dfs(node: TrieNode, prefix: String, result: MutableList<String>, limit: Int) {
        if (result.size >= limit) return
        if (node.isEndOfWord) {
            result.add(prefix)
        }
        for ((char, child) in node.children) {
            if (result.size >= limit) return
            dfs(child, prefix + char, result, limit)
        }
    }
}

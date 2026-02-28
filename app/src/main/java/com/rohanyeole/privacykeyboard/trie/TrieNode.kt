package com.rohanyeole.privacykeyboard.trie

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
    var frequency: Int = 0
}

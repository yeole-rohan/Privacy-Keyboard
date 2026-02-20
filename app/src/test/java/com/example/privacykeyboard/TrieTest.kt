package com.example.privacykeyboard

import com.example.privacykeyboard.trie.Trie
import org.junit.Assert.*
import org.junit.Test

class TrieTest {

    @Test
    fun `Trie insert and exact prefix search`() {
        val trie = Trie()
        trie.insert("hello")
        trie.insert("help")
        trie.insert("world")
        val results = trie.searchByPrefix("hel")
        assertTrue(results.contains("hello"))
        assertTrue(results.contains("help"))
        assertFalse(results.contains("world"))
    }

    @Test
    fun `Trie searchByPrefix returns empty for unknown prefix`() {
        val trie = Trie()
        trie.insert("hello")
        assertTrue(trie.searchByPrefix("xyz").isEmpty())
    }

    @Test
    fun `Trie returns full word when prefix equals word`() {
        val trie = Trie()
        trie.insert("hi")
        val results = trie.searchByPrefix("hi")
        assertTrue(results.contains("hi"))
    }

    @Test
    fun `Trie does not return partial words`() {
        val trie = Trie()
        trie.insert("hello")
        val results = trie.searchByPrefix("hell")
        assertFalse(results.contains("hell"))
        assertTrue(results.contains("hello"))
    }

    @Test
    fun `Trie handles empty prefix - returns all words`() {
        val trie = Trie()
        listOf("apple", "banana", "cherry").forEach { trie.insert(it) }
        val results = trie.searchByPrefix("")
        assertEquals(3, results.size)
        assertTrue(results.containsAll(listOf("apple", "banana", "cherry")))
    }

    @Test
    fun `Trie is case-sensitive`() {
        val trie = Trie()
        trie.insert("Hello")
        assertTrue(trie.searchByPrefix("He").contains("Hello"))
        assertTrue(trie.searchByPrefix("he").isEmpty())
    }
}

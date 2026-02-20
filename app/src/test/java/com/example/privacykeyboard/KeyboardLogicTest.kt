package com.example.privacykeyboard

import org.junit.Assert.*
import org.junit.Test

/**
 * JVM unit tests for pure keyboard logic.
 * Run with:  ./gradlew test
 * No device or emulator needed — these execute on your development machine.
 */
class KeyboardLogicTest {

    // -----------------------------------------------------------------------
    // CapsState transitions
    // -----------------------------------------------------------------------

    @Test
    fun `nextCapsState cycles OFF to SHIFT`() {
        assertEquals(CapsState.SHIFT, nextCapsState(CapsState.OFF))
    }

    @Test
    fun `nextCapsState cycles SHIFT to CAPS_LOCK`() {
        assertEquals(CapsState.CAPS_LOCK, nextCapsState(CapsState.SHIFT))
    }

    @Test
    fun `nextCapsState cycles CAPS_LOCK back to OFF`() {
        assertEquals(CapsState.OFF, nextCapsState(CapsState.CAPS_LOCK))
    }

    @Test
    fun `full three-tap cycle returns to original state`() {
        var state = CapsState.OFF
        repeat(3) { state = nextCapsState(state) }
        assertEquals(CapsState.OFF, state)
    }

    // -----------------------------------------------------------------------
    // Auto-off after key press
    // -----------------------------------------------------------------------

    @Test
    fun `shouldAutoOffAfterKey is true only for SHIFT`() {
        assertTrue(shouldAutoOffAfterKey(CapsState.SHIFT))
        assertFalse(shouldAutoOffAfterKey(CapsState.OFF))
        assertFalse(shouldAutoOffAfterKey(CapsState.CAPS_LOCK))
    }

    @Test
    fun `CAPS_LOCK does not auto-off after typing a letter`() {
        var state = CapsState.CAPS_LOCK
        // Simulate 5 key presses — state must stay CAPS_LOCK
        repeat(5) {
            if (shouldAutoOffAfterKey(state)) state = CapsState.OFF
        }
        assertEquals(CapsState.CAPS_LOCK, state)
    }

    @Test
    fun `SHIFT auto-offs after exactly one key press`() {
        var state = CapsState.SHIFT
        if (shouldAutoOffAfterKey(state)) state = CapsState.OFF
        assertEquals(CapsState.OFF, state)
    }

    // -----------------------------------------------------------------------
    // extractCurrentWord  (bug fix: was firstOrNull, now lastOrNull)
    // -----------------------------------------------------------------------

    @Test
    fun `extractCurrentWord returns last word in multi-word text`() {
        assertEquals("world", extractCurrentWord("hello world"))
    }

    @Test
    fun `extractCurrentWord returns the only word when no space`() {
        assertEquals("hello", extractCurrentWord("hello"))
    }

    @Test
    fun `extractCurrentWord returns empty string for empty input`() {
        assertEquals("", extractCurrentWord(""))
    }

    @Test
    fun `extractCurrentWord returns empty string after trailing space`() {
        // User pressed Space — text ends with space, so last token is ""
        assertEquals("", extractCurrentWord("hello "))
    }

    @Test
    fun `extractCurrentWord handles multiple spaces`() {
        assertEquals("world", extractCurrentWord("foo  world"))
    }

    @Test
    fun `regression - was returning first word not current word`() {
        // Old bug: "hello world" → returned "hello" (firstOrNull)
        // Fixed: should return "world" (the word being typed)
        val result = extractCurrentWord("hello world")
        assertNotEquals("hello", result)
        assertEquals("world", result)
    }

    // -----------------------------------------------------------------------
    // isValidWord
    // -----------------------------------------------------------------------

    @Test
    fun `isValidWord accepts normal lowercase word`() {
        assertTrue(isValidWord("hello"))
    }

    @Test
    fun `isValidWord accepts mixed case word`() {
        assertTrue(isValidWord("Hello"))
    }

    @Test
    fun `isValidWord rejects single character`() {
        assertFalse(isValidWord("a"))
    }

    @Test
    fun `isValidWord rejects empty string`() {
        assertFalse(isValidWord(""))
    }

    @Test
    fun `isValidWord rejects word with numbers`() {
        assertFalse(isValidWord("he110"))
    }

    @Test
    fun `isValidWord rejects word with spaces`() {
        assertFalse(isValidWord("hello world"))
    }

    @Test
    fun `isValidWord rejects word longer than 30 chars`() {
        assertFalse(isValidWord("a".repeat(31)))
    }

    @Test
    fun `isValidWord accepts word of exactly 2 chars`() {
        assertTrue(isValidWord("ab"))
    }

    @Test
    fun `isValidWord accepts word of exactly 30 chars`() {
        assertTrue(isValidWord("a".repeat(30)))
    }

    @Test
    fun `isValidWord rejects word with punctuation`() {
        assertFalse(isValidWord("don't"))
        assertFalse(isValidWord("end."))
    }

    // -----------------------------------------------------------------------
    // Trie
    // -----------------------------------------------------------------------

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
        // "hell" was never inserted as a complete word
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

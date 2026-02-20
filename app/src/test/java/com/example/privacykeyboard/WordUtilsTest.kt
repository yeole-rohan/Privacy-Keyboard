package com.example.privacykeyboard

import com.example.privacykeyboard.util.extractCurrentWord
import com.example.privacykeyboard.util.isValidWord
import org.junit.Assert.*
import org.junit.Test

class WordUtilsTest {

    // -----------------------------------------------------------------------
    // extractCurrentWord
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
        assertEquals("", extractCurrentWord("hello "))
    }

    @Test
    fun `extractCurrentWord handles multiple spaces`() {
        assertEquals("world", extractCurrentWord("foo  world"))
    }

    @Test
    fun `regression - was returning first word not current word`() {
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
}

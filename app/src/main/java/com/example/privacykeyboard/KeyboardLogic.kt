package com.example.privacykeyboard

// ---------------------------------------------------------------------------
// Pure (Android-free) logic extracted from MyKeyboard for unit-testability.
// Every function here depends only on Kotlin stdlib — no Android imports.
// ---------------------------------------------------------------------------

/** 3-state caps cycle matching Gboard behaviour. */
enum class CapsState { OFF, SHIFT, CAPS_LOCK }

/** Returns the next CapsState after the user taps the Caps button. */
fun nextCapsState(current: CapsState): CapsState = when (current) {
    CapsState.OFF       -> CapsState.SHIFT
    CapsState.SHIFT     -> CapsState.CAPS_LOCK
    CapsState.CAPS_LOCK -> CapsState.OFF
}

/**
 * Returns true only if [state] is single-shot SHIFT, meaning we should
 * auto-revert to OFF after one letter is typed.
 * CAPS_LOCK is intentionally excluded — it stays until the user taps again.
 */
fun shouldAutoOffAfterKey(state: CapsState): Boolean = state == CapsState.SHIFT

/**
 * Returns the word currently being typed — the token after the last space
 * in [textBeforeCursor].  This is the "correct" word to save to the user
 * dictionary when Space is pressed.
 */
fun extractCurrentWord(textBeforeCursor: String): String =
    textBeforeCursor.split(" ").lastOrNull() ?: ""

/**
 * Returns true when [word] is a saveable dictionary entry:
 * non-empty, 2–30 letters, alphabetic only.
 */
fun isValidWord(word: String): Boolean {
    val wordRegex = "^[a-zA-Z]+$".toRegex()
    return word.isNotEmpty() && word.length in 2..30 && wordRegex.matches(word)
}

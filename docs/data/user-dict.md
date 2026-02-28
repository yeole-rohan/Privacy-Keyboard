# UserDictRepository

**File:** `data/UserDictRepository.kt` (59 lines)
**Package:** `com.rohanyeole.privacykeyboard.data`
**SharedPrefs namespace:** `UserDict`
**Key:** `user_dict` (StringSet)

Stores and retrieves the user's personal word dictionary with per-word tap frequency.

---

## Storage Encoding

Each entry in the StringSet is stored as `"word:count"`:
```
"hello:7"
"kotlin:3"
"rohan:0"
```

**Backward compatibility:** Old bare-word entries (from before the frequency upgrade) have no `:` separator. `decodeCount()` uses `substringAfter(":", "0")` and falls back to `0` when no colon is found.

---

## Public API

### `getAll(): Set<String>`
Returns the set of all saved words (bare strings, no count). Backward-compatible with old storage format.
```kotlin
val words: Set<String> = userDictRepo.getAll()
```

### `getAllWithFrequency(): Map<String, Int>`
Returns `word → count` map for all saved entries.
```kotlin
val freq: Map<String, Int> = userDictRepo.getAllWithFrequency()
// { "hello" to 7, "kotlin" to 3, ... }
```

### `getTopWords(n: Int): List<String>`
Returns the `n` words with the highest tap count, sorted descending. Used by `SuggestionController.showPostSpaceSuggestions()`.
```kotlin
val top3 = userDictRepo.getTopWords(3)  // ["hello", "kotlin", "android"]
```

### `incrementFrequency(word: String)`
Increments the count for `word` by 1. If the word doesn't exist yet, inserts it with count=1.
Called by `SuggestionController` on every suggestion tap.
```kotlin
userDictRepo.incrementFrequency("hello")   // "hello:7" → "hello:8"
userDictRepo.incrementFrequency("newword") // inserts "newword:1"
```

### `save(word: String)`
Validates and saves a new word if it doesn't already exist. Validation uses `isValidWord()` (2–30 letters, alphabetic only). Stored as `"word:0"` (count starts at zero; increases only when tapped as suggestion).

Called by `PrivacyKeyboardService` on every space press:
```kotlin
if (isValidWord(currentWord)) userDictRepo.save(currentWord)
```

---

## Private Helpers

```kotlin
private fun loadRaw(): MutableSet<String>
    // ALWAYS copies the SharedPrefs set into a fresh mutableSetOf()
    // Android's live-set hazard: mutating the returned set directly causes
    // a crash on some devices. The fresh copy prevents this.

private fun decodeWord(entry: String): String = entry.substringBefore(":")
private fun decodeCount(entry: String): Int   = entry.substringAfter(":", "0").toIntOrNull() ?: 0
private fun encodeEntry(word: String, count: Int): String = "$word:$count"
```

---

## Word Validation

Delegated to `isValidWord()` from `util/WordUtils.kt`:
```kotlin
fun isValidWord(word: String): Boolean {
    val wordRegex = "^[a-zA-Z]+$".toRegex()
    return word.isNotEmpty() && word.length in 2..30 && wordRegex.matches(word)
}
```

Numbers, symbols, or mixed-case words shorter than 2 or longer than 30 characters are rejected.

---

## Interaction with SuggestionController

```
User types "hel"
    ↓
userDictRepo.getAllWithFrequency()
    .filter { it.key.startsWith("hel", ignoreCase=true) }
    .sortedByDescending { it.value }
    .take(2)
    → ["hello" (freq 7), "help" (freq 3)]

User taps "hello"
    ↓
userDictRepo.incrementFrequency("hello")   // "hello:8"
trie.incrementFrequency("hello")
```

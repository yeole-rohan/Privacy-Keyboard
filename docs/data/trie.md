# Trie System

**Files:**
- `trie/TrieNode.kt` (7 lines)
- `trie/Trie.kt` (53 lines)
- `trie/DictionaryLoader.kt` (33 lines)

**Package:** `com.rohanyeole.privacykeyboard.trie`

---

## TrieNode

```kotlin
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
    var frequency: Int = 0       // tap count — incremented by trie.incrementFrequency()
}
```

`frequency` is stored at the end-node of each word. Words loaded from the dictionary start at `freq=1` (from `insert(word)` default). Tapping a suggestion calls `incrementFrequency()` to raise it.

---

## Trie

### Methods

#### `insert(word: String, freq: Int = 1)`
Walks/creates nodes for each character, sets `isEndOfWord = true` and `node.frequency = freq` at the terminal node.

Default `freq=1` means `DictionaryLoader` doesn't need changes:
```kotlin
trie.insert(word)        // freq defaults to 1
trie.insert(word, 5)     // explicit frequency
```

#### `contains(word: String): Boolean`
Walks the trie; returns `true` only if the word ends at a node where `isEndOfWord == true`. Used by `SuggestionController` to validate fuzzy-correction candidates.

#### `incrementFrequency(word: String)`
Walks to the word's terminal node. If `isEndOfWord == true`, increments `frequency`. Silently returns if the word is not in the trie (safe to call on any string).

#### `searchByPrefix(prefix: String, limit: Int = 20): List<String>`
1. Walks to the prefix's last node (returns empty list if prefix not found)
2. DFS collects up to `limit` `(word, freq)` pairs
3. Sorts by `freq DESC` then `word.length ASC`
4. Returns the word strings (frequency dropped)

```kotlin
trie.searchByPrefix("th")   // → ["the", "that", "this", "they", ...]
```

### DFS (private)

```kotlin
private fun dfs(node: TrieNode, prefix: String, result: MutableList<Pair<String, Int>>, limit: Int)
```

Recursively appends word+freq pairs to `result` until `result.size >= limit`. Traversal order is map-iteration order (effectively insertion order in Kotlin's `LinkedHashMap` backing `mutableMapOf`).

**Note:** The `limit` caps collection before sorting, so in theory a high-frequency word inserted after the 20th word in DFS order could be missed. In practice this doesn't matter for a keyboard — the most common words appear early in dictionary order.

---

## DictionaryLoader

### `loadDictionaryFromAssets(context: Context): List<String>`

Top-level function (not a class method). Import directly:
```kotlin
import com.rohanyeole.privacykeyboard.trie.loadDictionaryFromAssets
```

Reads `assets/dictionary.zip` via `ZipInputStream` + `BufferedReader`, collecting all non-blank lines (one word per line). Returns `List<String>`. On any exception, logs the error and returns an empty list (keyboard still works, just no suggestions).

### Called in Service

```kotlin
private fun initTrie() {
    trie = Trie()
    loadDictionaryFromAssets(this).forEach { word -> trie.insert(word) }
}
```

Called once in `onCreateInputView()`. The dictionary file is ~7.8 KB compressed.

---

## How Frequency Works End-to-End

```
1. initTrie() → all words inserted with freq=1

2. User types "the" → trie.searchByPrefix("the")
   → returns ["the", "then", "there", ...] sorted by freq, then length

3. User taps "the" suggestion →
   trie.incrementFrequency("the")
   → "the" node frequency becomes 2

4. Next time user types "the" →
   "the" appears before "then" (freq 2 > freq 1, same length order)
```

---

## Unit Tests

`app/src/test/.../TrieTest.kt` — 7 tests covering:
- Insert then searchByPrefix finds the word
- Prefix matching returns all extensions
- No partial-word false positives
- Empty prefix returns all words
- Case-sensitive behavior

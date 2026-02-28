# Emoji Data & Repository

## EmojiData

**File:** `data/EmojiData.kt` (234 lines)
**Package:** `com.rohanyeole.privacykeyboard.data`
**Type:** `object` (singleton, no Android imports)

Pure static data — holds the complete emoji library organized by category.

### Properties

```kotlin
object EmojiData {
    val categoryNames: List<String>          // ordered list of category names
    val byCategory: Map<String, List<String>> // category name → emoji list
}
```

### Categories (in order)

| # | Category | Approximate count |
|---|----------|-------------------|
| 1 | Recent | (dynamic — from EmojiRepository) |
| 2 | Smileys | 100+ |
| 3 | Animals | 60+ |
| 4 | Food | 70+ |
| 5 | Activities | 50+ |
| 6 | Objects | 80+ |
| 7 | Symbols | 70+ |
| 8 | Flags | 50+ |

`categoryNames` contains all categories including "Recent". `EmojiController.render()` filters "Recent" out when iterating `categoryNames` and handles it separately via `EmojiRepository`.

### Usage

```kotlin
EmojiData.categoryNames          // ["Recent", "Smileys", "Animals", ...]
EmojiData.byCategory["Smileys"]  // ["😀", "😃", "😄", ...]
```

---

## EmojiRepository

**File:** `data/EmojiRepository.kt` (25 lines)
**Package:** `com.rohanyeole.privacykeyboard.data`
**SharedPrefs namespace:** `emoji_prefs`
**Key:** `recent_emojis` (single String, comma-separated)

### Constants

```kotlin
const val MAX_RECENT_EMOJIS = 50
```

### Methods

#### `loadRecent(): List<String>`
Returns recently used emojis in most-recent-first order. Returns empty list if nothing saved yet.

```kotlin
val recent = emojiRepo.loadRecent()
// ["👍", "😂", "❤️", ...]
```

#### `addToRecent(emoji: String)`
Prepends the emoji to the front of the list:
1. Loads current list
2. Removes the emoji if it already exists (deduplication)
3. Inserts at index 0
4. Trims to `MAX_RECENT_EMOJIS` (50) if over limit
5. Saves as comma-separated string

```kotlin
emojiRepo.addToRecent("👍")
```

Called by `EmojiController.buildEmojiRows()` on every emoji tap.

### Storage Format

```
"👍,😂,❤️,🎉,🔥,..."
```

Decoded with `str.split(",")`. Emoji characters may be multi-codepoint (e.g. skin tone modifiers) but commas are not valid emoji codepoints, so the delimiter is safe.

---

## How Recent Emojis Are Displayed

`EmojiController.render()` calls `getRecentEmojis()` which calls `emojiRepo.loadRecent()`. If the list is empty, a "No recent emojis" placeholder is shown. Otherwise, emojis are displayed in the same 7-per-row grid as other categories.

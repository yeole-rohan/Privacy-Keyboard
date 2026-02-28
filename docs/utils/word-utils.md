# WordUtils

**File:** `util/WordUtils.kt` (33 lines)
**Package:** `com.rohanyeole.privacykeyboard.util`

Four pure top-level functions with no Android dependencies. Unit-testable without a device.

---

## Functions

### `nextCapsState(current: CapsState): CapsState`

Returns the next caps state when the user taps the caps button:

```kotlin
nextCapsState(CapsState.OFF)        // → SHIFT
nextCapsState(CapsState.SHIFT)      // → CAPS_LOCK
nextCapsState(CapsState.CAPS_LOCK)  // → OFF
```

Used by `CapsController.toggle()`.

---

### `shouldAutoOffAfterKey(state: CapsState): Boolean`

Returns `true` only for `SHIFT`. `CAPS_LOCK` intentionally returns `false` — it stays until manually toggled.

```kotlin
shouldAutoOffAfterKey(CapsState.SHIFT)     // true  → auto-off after one letter
shouldAutoOffAfterKey(CapsState.CAPS_LOCK) // false → stays until tapped
shouldAutoOffAfterKey(CapsState.OFF)       // false
```

Used by `CapsController.makeKeysLowercase()`.

---

### `extractCurrentWord(textBeforeCursor: String): String`

Returns the last whitespace-separated token in `textBeforeCursor`:

```kotlin
extractCurrentWord("hello world")   // → "world"
extractCurrentWord("hello ")        // → ""  (trailing space = empty last token)
extractCurrentWord("")              // → ""
extractCurrentWord("one")           // → "one"
```

Uses `split(" ").lastOrNull() ?: ""`.

Called by `PrivacyKeyboardService.getCurrentWord()` with up to 100 chars before cursor.
Used by `SuggestionController.update()` indirectly via the service.

---

### `isValidWord(word: String): Boolean`

Returns `true` when `word` is suitable for saving to the user dictionary:

```kotlin
isValidWord("hello")  // true
isValidWord("a")      // false (too short, min 2)
isValidWord("ab")     // true
isValidWord("hello123") // false (not alphabetic)
isValidWord("héllo")  // false (non-ASCII)
isValidWord("a".repeat(31)) // false (too long, max 30)
```

Rules:
- Non-empty
- Length 2–30 characters
- Matches `^[a-zA-Z]+$` (alphabetic only, ASCII)

Used by `UserDictRepository.save()` and `PrivacyKeyboardService.setupNormalLayout()` before calling `userDictRepo.save(currentWord)` on space press.

---

## Unit Tests

`app/src/test/.../WordUtilsTest.kt` — 15 tests covering:
- `extractCurrentWord`: single word, multiple words, trailing space, empty string
- `isValidWord`: minimum length, maximum length, non-alphabetic chars, empty string, case-insensitive regex

All tests run JVM-only (`./gradlew test`) — no device required.

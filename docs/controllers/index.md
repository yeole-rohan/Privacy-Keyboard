# Controllers — Overview

Controllers handle all UI logic for keyboard sub-systems. Each receives dependencies via constructor and exposes a clean, focused public API. Controllers never call each other — all inter-controller communication goes through `PrivacyKeyboardService`.

---

## All Four Controllers

| Controller | File | Lines | Purpose |
|------------|------|-------|---------|
| `CapsController` | `controller/CapsController.kt` | 78 | 3-state caps lock (OFF/SHIFT/CAPS_LOCK) |
| `ClipboardController` | `controller/ClipboardController.kt` | 96 | Clipboard chips in suggestion bar |
| `EmojiController` | `controller/EmojiController.kt` | 150 | Emoji picker with category scroll |
| `SuggestionController` | `controller/SuggestionController.kt` | 155 | Autocomplete with frequency ranking + fuzzy |

---

## Constructor Signatures

```kotlin
CapsController(
    binding: KeyboardLayoutBinding
)

ClipboardController(
    context: Context,
    chipsContainer: LinearLayout,
    onTextSelected: (CharSequence) -> Unit
)

EmojiController(
    context: Context,
    emojiBinding: EmojiLayoutBinding,
    emojiRepo: EmojiRepository,
    onEmojiSelected: (String) -> Unit
)

SuggestionController(
    trie: Trie,
    userDictRepo: UserDictRepository,
    suggestionViews: List<TextView>,    // exactly 3 views
    dividers: List<View>,               // [divider12, divider23]
    onWordSelected: (word: String, rawInput: String) -> Unit
)
```

---

## Visibility Rules (managed by PrivacyKeyboardService)

| Condition | Suggestion bar | Clipboard chips |
|-----------|---------------|-----------------|
| Field is empty | Hidden | Shown |
| Field has text | Shown (if matches found) | Hidden |
| Post-space | Shows top personal words | Hidden |

`onUpdateSelection` in the service drives these transitions.

---

## Detailed Docs

- [suggestion.md](suggestion.md) — autocomplete pipeline, 7 improvements, QWERTY adjacency
- [caps.md](caps.md) — state machine, visual indicators
- [emoji.md](emoji.md) — section render, scroll tracking, recent emojis
- [clipboard.md](clipboard.md) — listener registration, chip layout, cleanup

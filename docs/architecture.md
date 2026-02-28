# Architecture

## Package Structure

```
com.rohanyeole.privacykeyboard/
├── PrivacyKeyboardService.kt      ← IME orchestrator (569 lines)
├── MainActivity.kt                ← launcher, 3-state setup status
├── SettingsActivity.kt            ← vibration + theme settings
├── InfoActivity.kt                ← About / Privacy / Terms / Legal
│
├── controller/
│   ├── CapsController.kt          ← 3-state caps UI + transitions
│   ├── ClipboardController.kt     ← clipboard listener + chip row
│   ├── EmojiController.kt         ← category display, recent, scroll
│   └── SuggestionController.kt    ← autocomplete UI (Trie + UserDict)
│
├── data/
│   ├── EmojiData.kt               ← emoji category map (pure data, no Android)
│   ├── EmojiRepository.kt         ← recent emojis SharedPrefs CRUD
│   ├── UserDictRepository.kt      ← user word dictionary (word:count encoding)
│   ├── KeyboardPreferences.kt     ← vibration + theme settings SharedPrefs
│   └── KeyboardTheme.kt           ← 7 built-in theme color definitions
│
├── trie/
│   ├── TrieNode.kt                ← node with children, isEndOfWord, frequency
│   ├── Trie.kt                    ← insert, contains, incrementFrequency, searchByPrefix
│   └── DictionaryLoader.kt        ← loadDictionaryFromAssets() top-level function
│
├── model/
│   └── CapsState.kt               ← enum { OFF, SHIFT, CAPS_LOCK }
│
└── util/
    ├── HapticHelper.kt            ← vibrate wrapper with strength levels
    ├── WordUtils.kt               ← nextCapsState, shouldAutoOffAfterKey,
    │                                 extractCurrentWord, isValidWord (top-level fns)
    ├── ThemeHelper.kt             ← recursive theme application to view tree
    └── AppUpdateHelper.kt         ← Google Play flexible in-app update
```

---

## Architectural Patterns

### 1. Orchestrator + Controllers
`PrivacyKeyboardService` is a thin orchestrator. It wires everything up in `onCreateInputView()` and delegates:
- UI state → controllers (`CapsController`, `SuggestionController`, etc.)
- Persistence → data layer (`UserDictRepository`, `EmojiRepository`, `KeyboardPreferences`)
- Utilities → `HapticHelper`, `ThemeHelper`, `WordUtils`

Controllers never call each other — they only communicate through the service via callbacks.

### 2. Constructor Dependency Injection
Every controller receives its dependencies via its constructor. There is no DI framework. Example:
```kotlin
SuggestionController(
    trie            = trie,
    userDictRepo    = userDictRepo,
    suggestionViews = listOf(normalBinding.suggestion1, ...),
    dividers        = listOf(normalBinding.suggDivider12, ...),
    onWordSelected  = { word, rawInput -> /* commit text */ }
)
```

### 3. Callback-based Communication
Controllers receive lambdas for "output" actions (commit text, hide suggestion bar). This keeps them decoupled from `InputConnection` and easy to unit test.

### 4. View Binding
Three bindings are inflated in `onCreateInputView()`:
- `KeyboardLayoutBinding` — normal keyboard + top bar + functional keys
- `EmojiLayoutBinding` — emoji scroll panel (embedded in normal layout)
- `KeyboardSpecialBinding` — symbol/special character keyboard

All view access goes through these bindings — no `findViewById`.

---

## Data Flow: Key Press → Committed Text

```
User taps letter key
    │
    ▼
setupButtonListeners (PrivacyKeyboardService)
    │  hapticHelper.perform()
    │  capsController.state → uppercase or lowercase
    │  currentInputConnection.commitText(char, 1)
    │  capsController.makeKeysLowercase()  ← auto-off SHIFT if needed
    │
    ▼
onUpdateSelection (fired by Android after cursor moves)
    │  currentInputConnection.getExtractedText()
    │
    ├─ inputText.isEmpty?
    │   ├─ YES → suggestionController.hide() + clipboardController.show()
    │   └─ NO  → clipboardController.hide() + suggestionController.update(inputText)
    │
    ▼
SuggestionController.update(inputText)
    │  rawLastWord = split by space, take last token
    │  userDictRepo.getAllWithFrequency() → prefix-filter → sort freq DESC → take ≤2
    │  trie.searchByPrefix(rawLastWord) → fill to 3
    │  fuzzy: adjacency substitution (if <3 results and len > 2)
    │  fuzzy: transposition (if still <3 results and len > 2)
    │  isSentenceStart? → capitalize
    │  renderSuggestions(displayList, rawLastWord)
    │
    ▼
User taps suggestion chip
    │  onWordSelected(word, rawLastWord)
    │      currentInputConnection.deleteSurroundingText(rawLastWord.length, 0)
    │      currentInputConnection.commitText("$word ", 1)
    │  userDictRepo.incrementFrequency(word.lowercase())
    │  trie.incrementFrequency(word.lowercase())
    │  hide()
```

---

## Data Flow: Space Bar Press

```
User taps space
    │  commitText(" ", 1)
    │  capsController.makeKeysLowercase()
    │  extractCurrentWord(textBefore100Chars) → word before space
    │  isValidWord(word)? → userDictRepo.save(word)
    │
    ▼ (onUpdateSelection fires)
SuggestionController.update(inputText)
    │  rawLastWord = "" (trailing space)
    │  inputText.isNotEmpty?
    │   ├─ YES → showPostSpaceSuggestions()
    │   │         userDictRepo.getTopWords(3) → show personal top words
    │   └─ NO  → hide()
```

---

## Layout Switching

```
Normal keyboard (normalBinding.root)
    ├─ emoji toggle btn → toggleEmojiLayout()
    │       emojiBinding.root  ↔  normalBinding.keyboardRowsContainer
    └─ "!?" btn → switchKeyboardLayout()
            isSpecialKeysEnabled = true
            setInputView(specialBinding.root)

Special keyboard (specialBinding.root)
    └─ "ABC" btn → switchKeyboardLayout()
            isSpecialKeysEnabled = false
            setInputView(normalBinding.root)
```

---

## Theme Application Flow

```
onWindowShown() [called on every keyboard show + return from Settings]
    │  getCurrentTheme() → KeyboardTheme.forId(prefs.themeId)
    │  ThemeHelper.applyToKeyboard(normalBinding.root, theme)
    │  if specialKeyboard visible → ThemeHelper.applyToKeyboard(specialBinding.root, theme)
    │
    ▼
ThemeHelper.applyToKeyboard(root, theme)
    │  root.setBackgroundColor(theme.kbBg)
    │  applyToButtons(root, theme)  ← recursive DFS over ViewGroup tree
    │
    └─ for each Button found (except btnEnter):
           RippleDrawable(theme.rippleColor, roundedShape(theme.keyBg), mask)
           button.setTextColor(theme.keyText)
```

---

## SharedPreferences: Three Namespaces

| Name | Class | Keys |
|------|-------|------|
| `keyboard_settings` | `KeyboardPreferences` | `vibration_enabled` (bool), `vibration_strength` (str), `theme_id` (str) |
| `emoji_prefs` | `EmojiRepository` | `recent_emojis` (comma-separated string, max 50) |
| `UserDict` | `UserDictRepository` | `user_dict` (StringSet of `"word:count"` entries) |

---

## Lifecycle Summary

| Event | What happens |
|-------|-------------|
| `onCreateInputView()` | Inflate bindings, init repos/trie/controllers, wire all button listeners |
| `onStartInputView()` | Reset caps (if not restarting), sync suggestion/clipboard visibility |
| `onWindowShown()` | Re-apply current theme (catches Settings changes) |
| `onUpdateSelection()` | Update suggestion bar with current field text |
| `onDestroy()` | `clipboardController.cleanup()` — removes clipboard listener |

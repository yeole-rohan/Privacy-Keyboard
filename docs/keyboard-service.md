# PrivacyKeyboardService

**File:** `PrivacyKeyboardService.kt` (569 lines)
**Package:** `com.rohanyeole.privacykeyboard`
**Extends:** `InputMethodService`

The main entry point for the keyboard. Acts as a slim orchestrator — it wires layouts to controllers and data repositories, but contains no business logic itself.

---

## Fields

### View Bindings
| Field | Type | Purpose |
|-------|------|---------|
| `normalBinding` | `KeyboardLayoutBinding` | Normal QWERTY keyboard |
| `emojiBinding` | `EmojiLayoutBinding` | Emoji scroll panel (embedded in normal layout at index 0) |
| `specialBinding` | `KeyboardSpecialBinding` | Symbol/special character keyboard |
| `activeView` | `View?` | Currently displayed root view; safe fallback: `activeView ?: normalBinding.root` |

### State
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `isBackspacePressed` | `Boolean` | `false` | Controls continuous-backspace loop |
| `isSpecialKeysEnabled` | `Boolean` | `false` | Which keyboard is shown: normal vs special |
| `handler` | `Handler(Looper.getMainLooper())` | — | Schedules continuous backspace callbacks |

### Repositories / Helpers
| Field | Type |
|-------|------|
| `prefs` | `KeyboardPreferences` |
| `hapticHelper` | `HapticHelper` |
| `emojiRepo` | `EmojiRepository` |
| `userDictRepo` | `UserDictRepository` |
| `trie` | `Trie` |

### Controllers
| Field | Type |
|-------|------|
| `capsController` | `CapsController` |
| `clipboardController` | `ClipboardController` |
| `emojiController` | `EmojiController` |
| `suggestionController` | `SuggestionController` |

---

## Lifecycle Methods

### `onCreateInputView(): View`
Called once when the keyboard is first created. Does all initialization:
1. Inflates all three view bindings
2. Embeds `emojiBinding.root` into `normalBinding.normalContainer` at index 0
3. Creates `prefs`, `hapticHelper`, `emojiRepo`, `userDictRepo`
4. Calls `initTrie()`
5. Creates all four controllers, passing them their wired dependencies
6. Calls `setupNormalLayout()` to wire all button listeners
7. Wires emoji toggle button and emoji backspace
8. Wires settings gear → launches `SettingsActivity`
9. Applies current theme to normal keyboard
10. Sets emoji panel height to 30% of screen height
11. Returns `activeView ?: normalBinding.root`

### `onStartInputView(info, restarting)`
Called when a text field gains focus:
- Calls `capsController.reset()` (sets state to `SHIFT`) unless `restarting`
- Reads current field content via `getExtractedText()`
- If field empty → `suggestionController.hide()` + `clipboardController.show()`
- Else → `clipboardController.hide()`
- Calls `updateSettingsButtonVisibility()`

### `onWindowShown()`
Called every time the keyboard window becomes visible — including after returning from `SettingsActivity`:
- Guard: returns early if `normalBinding` not yet initialized
- Calls `ThemeHelper.applyToKeyboard(normalBinding.root, getCurrentTheme())`
- If special keyboard is active, applies theme there too

### `onUpdateSelection(...)`
Called by Android whenever the cursor position changes:
- Reads current field content via `getExtractedText()`
- If content non-empty: `clipboardController.hide()`, `suggestionController.update(inputText)`
- If content empty: `suggestionController.hide()`, `clipboardController.show()`
- Calls `updateSettingsButtonVisibility()`

### `onDestroy()`
- Calls `clipboardController.cleanup()` to remove clipboard listener and prevent memory leak

---

## Private Methods

### Layout Setup

#### `setupNormalLayout()`
Wires all event handlers for the normal QWERTY keyboard:
- `setupButtonListeners(alphabeticButtons())` — 26 letter keys
- `setupButtonListeners(numericButtons())` — 10 number keys
- Caps lock toggle → `capsController.toggle()`
- Backspace: `ACTION_DOWN` → `performSingleBackspace()` + `startContinuousBackspace()`; `ACTION_UP/CANCEL` → stops loop
- Enter → `sendKeyEvent(KEYCODE_ENTER)`
- Space:
  - Swipe left/right (threshold 30px) → `KEYCODE_DPAD_LEFT/RIGHT` per step
  - Click → `commitText(" ", 1)`, `capsController.makeKeysLowercase()`, `userDictRepo.save(currentWord)` if valid
- Comma → `commitText(",", 1)`, `capsController.makeKeysLowercase()`
- Dot → `commitText(".", 1)`, `capsController.makeKeysLowercase()`
- Apostrophe → `commitText("'", 1)`
- `!?` button → `isSpecialKeysEnabled = !isSpecialKeysEnabled`, `switchKeyboardLayout(...)`

#### `setupSpecialLayout()`
Wires event handlers for the special keyboard:
- 17 special characters: `@ # $ _ & - + ( ) / * " ' : ; ! ?`
- 10 numeric characters: `0–9`
- Backspace, Enter, Space, Comma, Dot (same as normal)
- `ABC` button → `isSpecialKeysEnabled = false`, `switchKeyboardLayout(...)`

Uses `setupCharacterButtons(Array<Pair<Button, String>>)` instead of letter-casing logic.

### Layout Switching

#### `switchKeyboardLayout(button: Button)`
- If `isSpecialKeysEnabled`: calls `setupSpecialLayout()`, applies theme, sets `activeView = specialBinding.root`
- Else: calls `setupNormalLayout()`, applies theme, sets `activeView = normalBinding.root`
- Calls `setInputView(activeView)` to swap the displayed keyboard

#### `toggleEmojiLayout()`
- If emoji visible → hide `emojiBinding.root`, show `keyboardRowsContainer`, hide emoji backspace
- Else → hide `keyboardRowsContainer`, show `emojiBinding.root`, show emoji backspace, call `emojiController.render()`

#### `setEmojiKeyboardHeight()`
Sets `emojiBinding.scrollView.layoutParams.height` to 30% of screen height pixels.

### Trie Initialization

#### `initTrie()`
```kotlin
trie = Trie()
loadDictionaryFromAssets(this).forEach { word -> trie.insert(word) }
```
Inserts all words from `assets/dictionary.zip` with default `freq=1`.

### Button Wiring Helpers

#### `setupButtonListeners(buttons: Array<Button>)`
For each button:
1. `hapticHelper.perform()`
2. Apply caps state: `button.text.uppercase()` if state ≠ OFF, else `.lowercase()`
3. `currentInputConnection?.commitText(inputText, 1)`
4. `capsController.makeKeysLowercase()` — auto-off SHIFT

#### `setupCharacterButtons(buttons: Array<Pair<Button, String>>)`
For each `(button, character)` pair:
1. `hapticHelper.perform()`
2. `currentInputConnection?.commitText(character, 1)`

No caps logic — special chars are case-invariant.

### Backspace Helpers

#### `performSingleBackspace()`
```kotlin
currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
```

#### `startContinuousBackspace()`
Posts a `Runnable` with 200ms initial delay, then repeats every 30ms while `isBackspacePressed == true`.

### Other Helpers

#### `getCurrentTheme(): KeyboardTheme`
```kotlin
KeyboardTheme.forId(prefs.themeId)
```

#### `updateSettingsButtonVisibility()`
Hides `btnKeyboardSettings` (gear icon) when any suggestion is visible; shows it when all suggestions are gone.

#### `getCurrentWord(): String`
Reads up to 100 chars before cursor via `getTextBeforeCursor(100, 0)`, then calls `extractCurrentWord()`.

---

## Button Accessors

#### `alphabeticButtons(): Array<Button>`
Returns all 26 letter buttons (Q–P row, A–L row, Z–M row) from `normalBinding`.

#### `numericButtons(): Array<Button>`
Returns all 10 digit buttons (0–9) from `normalBinding.rowNumeric`.

---

## Controller Wiring (Constructor Arguments)

```kotlin
// CapsController
CapsController(normalBinding)

// ClipboardController
ClipboardController(
    context         = this,
    chipsContainer  = normalBinding.clipboardChipsContainer,
    onTextSelected  = { text -> currentInputConnection?.commitText(text, 1) }
)

// EmojiController
EmojiController(
    context         = this,
    emojiBinding    = emojiBinding,
    emojiRepo       = emojiRepo,
    onEmojiSelected = { emoji ->
        hapticHelper.perform()
        currentInputConnection?.commitText("$emoji ", 1)
    }
)

// SuggestionController
SuggestionController(
    trie            = trie,
    userDictRepo    = userDictRepo,
    suggestionViews = listOf(normalBinding.suggestion1, normalBinding.suggestion2, normalBinding.suggestion3),
    dividers        = listOf(normalBinding.suggDivider12, normalBinding.suggDivider23),
    onWordSelected  = { word, rawInput ->
        currentInputConnection?.deleteSurroundingText(rawInput.length, 0)
        currentInputConnection?.commitText("$word ", 1)
    }
)
```

Note: emoji commits `"$emoji "` (with trailing space). Suggestion commits `"$word "` (with trailing space) after deleting `rawInput`.

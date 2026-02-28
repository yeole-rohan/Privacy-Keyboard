# CapsController

**File:** `controller/CapsController.kt` (78 lines)
**Package:** `com.rohanyeole.privacykeyboard.controller`

Manages the 3-state caps lock system: visual state of the caps button, letter key labels, and auto-off logic.

---

## Constructor

```kotlin
CapsController(binding: KeyboardLayoutBinding)
```

The `binding` reference is used to access all 26 alphabetic buttons and the caps lock button.

---

## Public API

| Method / Property | Signature | Description |
|-------------------|-----------|-------------|
| `state` | `var CapsState` (private set) | Current caps state — readable, not externally settable |
| `toggle()` | `fun toggle()` | Advances state via `nextCapsState()`, then updates UI |
| `reset()` | `fun reset()` | Sets state to `SHIFT`, updates UI. Called on new field focus. |
| `makeKeysLowercase()` | `fun makeKeysLowercase()` | If `shouldAutoOffAfterKey(state)` is true (i.e. SHIFT), transitions to OFF |
| `applyToButtons(buttons)` | `fun applyToButtons(Array<Button>)` | Sets each button text to upper/lowercase based on current state |

---

## State Transitions

```
OFF ──tap──► SHIFT ──tap──► CAPS_LOCK ──tap──► OFF
              │                  │
         (type letter)       (type letter)
              │                  │
              ▼                  ▼
             OFF            CAPS_LOCK (stays)
```

`toggle()` uses `nextCapsState()` from `WordUtils.kt`.
`makeKeysLowercase()` uses `shouldAutoOffAfterKey()` which returns `true` only for `SHIFT`.

---

## Visual Indicators

| State | Button text | Typeface | Underline |
|-------|-------------|----------|-----------|
| `OFF` | `a` | Normal | No |
| `SHIFT` | `A` | Normal | No |
| `CAPS_LOCK` | `A` | **Bold** | Yes |

The visual update happens in `updateUI()` (private), which also calls `applyToButtons()` to sync all 26 letter keys.

---

## Key Code

```kotlin
private fun updateUI() {
    val btn = binding.rowAlphabetic3.capsLock.btnCapsLock
    when (state) {
        CapsState.OFF       -> applyOffState(btn)
        CapsState.SHIFT     -> { btn.text = "A"; btn.setTypeface(null, Typeface.NORMAL); /* clear underline */ }
        CapsState.CAPS_LOCK -> { btn.text = "A"; btn.setTypeface(null, Typeface.BOLD);   /* set underline */ }
    }
    applyToButtons(alphabeticButtons())
}
```

---

## Integration with Service

`PrivacyKeyboardService.setupButtonListeners()` reads `capsController.state` and calls
`capsController.makeKeysLowercase()` after every letter commit. The service calls `capsController.toggle()` from the caps lock button's click listener.

`onStartInputView(restarting=false)` calls `capsController.reset()` so every fresh field
opens in SHIFT (first letter auto-capitalizes).

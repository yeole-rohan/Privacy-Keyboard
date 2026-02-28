# CapsState — Model

**File:** `model/CapsState.kt`
**Package:** `com.rohanyeole.privacykeyboard.model`

---

## Enum Definition

```kotlin
enum class CapsState { OFF, SHIFT, CAPS_LOCK }
```

---

## State Meanings

| State | Meaning | Button label | Button style |
|-------|---------|--------------|--------------|
| `OFF` | Lowercase — no caps active | `a` | Normal weight, no underline |
| `SHIFT` | Single-shot uppercase — auto-reverts to OFF after one letter | `A` | Normal weight, no underline |
| `CAPS_LOCK` | Persistent uppercase — stays until tapped again | `A` | **Bold**, underlined |

---

## Transition Table

| Current state | User action | Next state |
|---------------|-------------|------------|
| `OFF` | Tap caps button | `SHIFT` |
| `SHIFT` | Tap caps button | `CAPS_LOCK` |
| `CAPS_LOCK` | Tap caps button | `OFF` |
| `SHIFT` | Type any letter / space / comma / dot | `OFF` (auto-off via `shouldAutoOffAfterKey`) |
| `CAPS_LOCK` | Type any letter | stays `CAPS_LOCK` |

---

## Related Functions (`util/WordUtils.kt`)

```kotlin
// Returns the next state after the caps button is tapped
fun nextCapsState(current: CapsState): CapsState

// Returns true only for SHIFT — auto-revert after one keystroke
fun shouldAutoOffAfterKey(state: CapsState): Boolean
```

---

## Usage in Service

`PrivacyKeyboardService.setupButtonListeners()` checks `capsController.state` to decide
whether to send the uppercase or lowercase version of each letter:

```kotlin
val inputText = if (capsController.state != CapsState.OFF)
    button.text.toString().uppercase()
else
    button.text.toString().lowercase()
```

After committing the character, `capsController.makeKeysLowercase()` is called, which calls
`shouldAutoOffAfterKey(state)` and only auto-offs for `SHIFT`.

---

## Initial State

`CapsController` starts in `CapsState.SHIFT` and calls `reset()` in `onStartInputView()`,
meaning a fresh field always opens with one-shot shift ready.

# ClipboardController

**File:** `controller/ClipboardController.kt` (96 lines)
**Package:** `com.rohanyeole.privacykeyboard.controller`

Listens to clipboard changes and displays recent clipboard entries as chips in the suggestion bar. Shares the same horizontal space as the autocomplete suggestions.

---

## Constructor

```kotlin
ClipboardController(
    context: Context,
    chipsContainer: LinearLayout,          // normalBinding.clipboardChipsContainer
    onTextSelected: (CharSequence) -> Unit // currentInputConnection?.commitText(text, 1)
)
```

---

## Public API

| Method | Description |
|--------|-------------|
| `setup()` | Registers the `OnPrimaryClipChangedListener`. Call once in `onCreateInputView()`. |
| `cleanup()` | Removes the listener. **Must call in `onDestroy()`** to prevent memory leak. |
| `show()` | Calls `refreshContent()` to populate chips (auto-hides if clipboard empty). |
| `hide()` | Clears all chip views and sets `chipsContainer.visibility = GONE`. |

---

## Chip Layout

`refreshContent()` rebuilds the chip row on every call:

1. Removes all existing views from `chipsContainer`
2. Reads `clipboardManager.primaryClip`
3. For each clipboard item (max 3):
   - Truncates text to 7 chars with `…` if longer: `"${text.take(7)}…"`
   - Creates a `TextView` chip: `textSize=14f`, centered, `weight=1`, ripple background
   - Adds a 1dp divider `View` between chips (semi-transparent `0x22000000`)
   - Chip click → `onTextSelected(fullText)` (commits full, untruncated text)
4. Sets `chipsContainer.visibility = VISIBLE` if any chips added, else `GONE`

---

## Listener Lifecycle

```kotlin
fun setup() {
    listener = ClipboardManager.OnPrimaryClipChangedListener { refreshContent() }
    clipboardManager.addPrimaryClipChangedListener(listener!!)
}

fun cleanup() {
    listener?.let { clipboardManager.removePrimaryClipChangedListener(it) }
}
```

`cleanup()` is called in `PrivacyKeyboardService.onDestroy()`. Forgetting this causes a memory leak — the `ClipboardManager` holds a reference to the service.

---

## Visibility Coordination

The service (`onStartInputView`, `onUpdateSelection`) controls when clipboard is shown vs hidden:

| Condition | Action |
|-----------|--------|
| Field is empty | `clipboardController.show()` |
| Field has content | `clipboardController.hide()` |
| Suggestions are showing | `clipboardController.hide()` (chips and suggestions never coexist) |

---

## Notes

- Clipboard access at API 26+ may show a system toast ("App accessed clipboard") — this is Android OS behavior, not app code.
- Only `primaryClip` items are shown (the most recent clipboard content). Android limits this to 1 item on API 29+.
- Blank or null items are silently skipped.

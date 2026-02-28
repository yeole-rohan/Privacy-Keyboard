# EmojiController

**File:** `controller/EmojiController.kt` (150 lines)
**Package:** `com.rohanyeole.privacykeyboard.controller`

Renders the emoji picker panel, tracks which category is currently visible during scroll, and manages the Recent section.

---

## Constructor

```kotlin
EmojiController(
    context: Context,
    emojiBinding: EmojiLayoutBinding,
    emojiRepo: EmojiRepository,
    onEmojiSelected: (String) -> Unit
)
```

`onEmojiSelected` lambda (in PrivacyKeyboardService):
```kotlin
{ emoji ->
    hapticHelper.perform()
    currentInputConnection?.commitText("$emoji ", 1)   // emoji + trailing space
}
```

---

## Public API

| Method | Description |
|--------|-------------|
| `setup()` | Attaches the scroll listener. Called once in `onCreateInputView()`. |
| `render()` | Rebuilds all emoji rows. Called each time the emoji panel opens. |

---

## `setup()`

Calls `attachScrollListener()` which sets a `setOnScrollChangeListener` on `emojiBinding.scrollView`:
- Finds the last section whose header Y-offset ≤ current scrollY
- Updates `emojiBinding.tvCurrentCategory.text` with that section's name

---

## `render()`

Called each time the emoji panel becomes visible (`toggleEmojiLayout()` in the service):

1. Clears `emojiBinding.emojiContainer` of all views
2. Clears `sectionOffsets`
3. Calls `buildSection("Recent", getRecentEmojis())` first
4. For every `EmojiData.categoryNames` entry (except "Recent"): `buildSection(name, ...)`
5. Posts `collectSectionOffsets()` after layout pass (via `emojiContainer.post {}`)
6. Scrolls to top and sets `tvCurrentCategory` label to "Recent"

---

## Section Layout

Each section is built by `buildSection(name, emojis)`:

1. Adds a **section header** `TextView`:
   - `text = name`, `textSize = 11f`, bold, 8% letter spacing, 55% alpha
   - `tag = name` — used by `collectSectionOffsets()` to find Y-offsets
2. If `emojis.isEmpty()` → adds "No recent emojis" placeholder, returns
3. Else → calls `buildEmojiRows(emojis)`

---

## Emoji Row Layout

`buildEmojiRows(emojis)` groups emojis 7 per row:

- Row: `LinearLayout` (horizontal, `MATCH_PARENT` width)
- Each emoji: `TextView` with `weight=1` (equal distribution), `textSize=39f`, centered
- Background: `R.drawable.ripple_effect` for tap feedback
- On click: calls `onEmojiSelected(emoji)` AND `emojiRepo.addToRecent(emoji)`

---

## Section Offset Tracking

`collectSectionOffsets()` — called after layout pass:
- Iterates all direct children of `emojiContainer`
- For each child whose `tag` is a non-null String → records `(tagName, child.top)` in `sectionOffsets`

Scroll listener then uses `sectionOffsets.lastOrNull { it.second <= scrollY }` to find current category.

---

## Category List

Rendered order:
1. Recent (always first)
2. Smileys
3. Animals
4. Food
5. Activities
6. Objects
7. Symbols
8. Flags

See [data/emoji.md](../data/emoji.md) for the full emoji data source.

---

## Emoji Panel Height

Set in `PrivacyKeyboardService.setEmojiKeyboardHeight()`:
```kotlin
emojiBinding.scrollView.layoutParams.height = (screenHeight * 0.3).toInt()
```
30% of screen height pixels.

# ThemeHelper

**File:** `util/ThemeHelper.kt` (49 lines)
**Package:** `com.rohanyeole.privacykeyboard.util`
**Type:** `object` (singleton)

Applies a `KeyboardTheme`'s colors to the keyboard view tree. Uses recursive DFS to find every `Button` in the hierarchy.

---

## Methods

### `applyToKeyboard(root: View, theme: KeyboardTheme)`
Public entry point. Sets the root background and delegates to `applyToButtons`.

```kotlin
ThemeHelper.applyToKeyboard(normalBinding.root, theme)
ThemeHelper.applyToKeyboard(specialBinding.root, theme)
```

### `applyToButtons(view: View, theme: KeyboardTheme)` *(private)*
Recursive DFS:
- If `view` is a `Button` and its ID is NOT `R.id.btnEnter` → calls `applyKeyStyle(view, theme)`
- If `view` is a `ViewGroup` → recurses into all children

`btnEnter` is excluded because it uses a drawable-based background (`enter_key_arrow.xml`); overwriting it would remove the arrow icon.

### `applyKeyStyle(button: Button, theme: KeyboardTheme)`
Creates a `RippleDrawable` from scratch:

```kotlin
val radius = 8dp in pixels
val shape = GradientDrawable(color = theme.keyBg, cornerRadius = radius)
val mask  = GradientDrawable(color = theme.keyBg, cornerRadius = radius)
button.background = RippleDrawable(ColorStateList.valueOf(theme.rippleColor), shape, mask)
button.setTextColor(theme.keyText)
```

The mask has the same shape as the content layer so the ripple is clipped to the rounded rect.

---

## Called From

| Location | When |
|----------|------|
| `PrivacyKeyboardService.onCreateInputView()` | Once, on normal keyboard after inflation |
| `PrivacyKeyboardService.onWindowShown()` | On every keyboard show (e.g. after returning from Settings) |
| `PrivacyKeyboardService.switchKeyboardLayout()` | When toggling between normal and special keyboards |
| `SettingsActivity.updateThemePreview()` | On the mini preview — not the live keyboard |

---

## Notes

- Each call creates new `GradientDrawable` and `RippleDrawable` instances — no view recycling needed.
- The `8dp` radius is hardcoded. To change key corner radius, update the `8f` value in `applyKeyStyle`.
- Only `Button` views are styled. `TextView`-based suggestion chips and emoji views keep their own backgrounds (`ripple_effect.xml` drawable).

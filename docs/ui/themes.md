# Theme System

The keyboard supports 7 built-in color themes, applied at runtime without restart.

---

## Theme Color Fields

Each `KeyboardTheme` has 4 color values:

| Field | Applied to |
|-------|-----------|
| `keyBg` | Button background (fill of rounded rect) |
| `keyText` | Button text color |
| `kbBg` | Keyboard root background |
| `rippleColor` | Ripple effect color on key press |

---

## All 7 Themes

| ID | Name | keyBg | keyText | kbBg | rippleColor |
|----|------|-------|---------|------|-------------|
| `light` | Light | `#FFFFFF` white | `#1B1B1D` near-black | `#F2F6F8` light grey | `#CCCCCC` grey |
| `dark` | Dark | `#3C4043` dark grey | `#FFFFFF` white | `#202124` very dark | `#666666` mid grey |
| `ocean` | Ocean | `#1976D2` blue | `#FFFFFF` white | `#0D47A1` deep blue | `#42A5F5` light blue |
| `sunset` | Sunset | `#EF6C00` orange | `#FFFFFF` white | `#BF360C` deep orange | `#FFAB40` amber |
| `forest` | Forest | `#388E3C` green | `#FFFFFF` white | `#1B5E20` deep green | `#66BB6A` light green |
| `lavender` | Lavender | `#9575CD` purple | `#FFFFFF` white | `#512DA8` deep purple | `#CE93D8` light purple |
| `midnight` | Midnight | `#1A1A2E` navy | `#E0E0E0` light grey | `#000000` black | `#3D5AFE` indigo |

---

## Runtime Application

Themes are applied by `ThemeHelper.applyToKeyboard(root, theme)`:

```kotlin
// 1. Set keyboard background
root.setBackgroundColor(theme.kbBg)

// 2. Recursively walk all views in the tree
//    For every Button (except btnEnter which keeps its arrow drawable):
val shape = GradientDrawable().apply {
    setColor(theme.keyBg)
    cornerRadius = 8dp
}
val mask = GradientDrawable().apply {
    setColor(theme.keyBg)
    cornerRadius = 8dp
}
button.background = RippleDrawable(ColorStateList.valueOf(theme.rippleColor), shape, mask)
button.setTextColor(theme.keyText)
```

Corner radius is always 8dp. `btnEnter` is excluded because it uses an arrow drawable (`enter_key_arrow.xml`).

---

## When Themes Are Applied

| Trigger | What happens |
|---------|-------------|
| `onCreateInputView()` | `ThemeHelper.applyToKeyboard(normalBinding.root, getCurrentTheme())` |
| `onWindowShown()` | Re-applied to normal keyboard (and special if active) — catches Settings changes |
| `switchKeyboardLayout()` | Applied to whichever keyboard is being switched to |
| `SettingsActivity.updateThemePreview()` | Applied to the mini preview keyboard (not the live keyboard) |

---

## How to Add a New Theme

1. Open `data/KeyboardTheme.kt`
2. Add a new entry to the `all` list:
   ```kotlin
   KeyboardTheme("rose", "Rose",
       Color.parseColor("#E91E63"),   // keyBg
       Color.WHITE,                   // keyText
       Color.parseColor("#880E4F"),   // kbBg
       Color.parseColor("#F48FB1"))   // rippleColor
   ```
3. Done — `SettingsActivity` iterates `KeyboardTheme.all` so the new theme appears automatically.

---

## Theme Storage

The selected theme ID is stored in `KeyboardPreferences` under `keyboard_settings` / `theme_id`. Default is `"light"`. `KeyboardTheme.forId(id)` returns the Light theme if the ID is unrecognized.

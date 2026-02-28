# SettingsActivity

**File:** `SettingsActivity.kt` (192 lines)
**Package:** `com.rohanyeole.privacykeyboard`
**Layout:** `res/layout/activity_settings.xml`

Settings screen for vibration feedback and keyboard theme. All changes persist immediately via `KeyboardPreferences`.

---

## Sections

### Vibration Settings

| Control | View ID | Behaviour |
|---------|---------|-----------|
| On/Off toggle | `switchVibration` | Enables/disables all haptic feedback |
| Strength group | `layoutStrength` | Shown only when vibration is enabled |
| Light option | `rbLight` | 15ms vibration |
| Medium option | `rbMedium` | 30ms vibration (default) |
| Strong option | `rbStrong` | 50ms vibration |

`setupVibration()` loads current prefs, sets the switch and radio group, then wires change listeners that write back to `prefs.vibrationEnabled` and `prefs.vibrationStrength`.

### Theme Settings

A dynamic list of theme rows is built from `KeyboardTheme.all`. Each row is created by `buildThemeRow(theme, isSelected)`.

When a theme row is tapped:
1. `prefs.themeId = theme.id`
2. All rows are visually updated (selected/unselected state)
3. `updateThemePreview(theme)` applies the theme to the preview keyboard

The preview is a mini keyboard built in `previewContainer` using `applyThemeToPreviewView()` and `applyStrengthStyles()`.

---

## Key Methods

| Method | Description |
|--------|-------------|
| `setupVibration()` | Wires vibration toggle + strength radio group |
| `setupThemes()` | Iterates `KeyboardTheme.all`, builds theme rows, shows initial preview |
| `buildThemeRow(theme, isSelected)` | Creates one theme selection row (icon + name + checkmark) |
| `updateThemePreview(theme)` | Refreshes the mini keyboard preview with new theme colors |
| `applyThemeToPreviewView(root, theme)` | Applies `keyBg`, `keyText`, `kbBg` to preview views |
| `applyStrengthStyles(theme)` | Applies `rippleColor` to the strength row buttons in preview |
| `makeRowDivider()` | Creates a 1dp horizontal divider between rows |

---

## Theme Applied at Keyboard Level

When the user returns from `SettingsActivity` to the keyboard, `PrivacyKeyboardService.onWindowShown()` fires and re-applies the newly selected theme to the live keyboard. No restart required.

---

## Layout View IDs

| View ID | Type | Purpose |
|---------|------|---------|
| `switchVibration` | `SwitchCompat` | Vibration on/off |
| `layoutStrength` | `LinearLayout` | Container for strength options (shown/hidden) |
| `rgStrength` | `RadioGroup` | Light / Medium / Strong radio buttons |
| `rbLight` | `RadioButton` | 15ms |
| `rbMedium` | `RadioButton` | 30ms |
| `rbStrong` | `RadioButton` | 50ms |
| `themeListContainer` | `LinearLayout` | Dynamic list of theme rows |
| `previewContainer` | `LinearLayout` | Live theme preview |

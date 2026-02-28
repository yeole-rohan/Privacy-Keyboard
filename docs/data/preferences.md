# KeyboardPreferences & KeyboardTheme

---

## KeyboardPreferences

**File:** `data/KeyboardPreferences.kt` (19 lines)
**Package:** `com.rohanyeole.privacykeyboard.data`
**SharedPrefs namespace:** `keyboard_settings`

Thin wrapper with Kotlin property delegates for clean read/write access to settings.

### Properties

| Property | Type | Key | Default | Valid values |
|----------|------|-----|---------|--------------|
| `vibrationEnabled` | `Boolean` | `vibration_enabled` | `true` | `true` / `false` |
| `vibrationStrength` | `String` | `vibration_strength` | `"medium"` | `"light"` / `"medium"` / `"strong"` |
| `themeId` | `String` | `theme_id` | `"light"` | see KeyboardTheme IDs below |

### Usage

```kotlin
val prefs = KeyboardPreferences(context)
prefs.vibrationEnabled = false
prefs.vibrationStrength = "strong"
prefs.themeId = "ocean"
```

Read by `HapticHelper` (vibration settings) and `PrivacyKeyboardService.getCurrentTheme()` (theme ID).
Written by `SettingsActivity`.

---

## KeyboardTheme

**File:** `data/KeyboardTheme.kt` (54 lines)
**Package:** `com.rohanyeole.privacykeyboard.data`

### Data Class

```kotlin
data class KeyboardTheme(
    val id: String,         // identifier stored in SharedPrefs
    val name: String,       // display name shown in SettingsActivity
    val keyBg: Int,         // key button background color (ARGB int)
    val keyText: Int,       // key button text color (ARGB int)
    val kbBg: Int,          // keyboard background color (ARGB int)
    val rippleColor: Int    // ripple effect color on key press (ARGB int)
)
```

### 7 Built-in Themes

| ID | Name | Key BG | Key Text | KB BG | Ripple |
|----|------|--------|----------|-------|--------|
| `light` | Light | `#FFFFFF` | `#1B1B1D` | `#F2F6F8` | `#CCCCCC` |
| `dark` | Dark | `#3C4043` | `#FFFFFF` | `#202124` | `#666666` |
| `ocean` | Ocean | `#1976D2` | `#FFFFFF` | `#0D47A1` | `#42A5F5` |
| `sunset` | Sunset | `#EF6C00` | `#FFFFFF` | `#BF360C` | `#FFAB40` |
| `forest` | Forest | `#388E3C` | `#FFFFFF` | `#1B5E20` | `#66BB6A` |
| `lavender` | Lavender | `#9575CD` | `#FFFFFF` | `#512DA8` | `#CE93D8` |
| `midnight` | Midnight | `#1A1A2E` | `#E0E0E0` | `#000000` | `#3D5AFE` |

### Companion Object

```kotlin
val all: List<KeyboardTheme>        // all 7 themes in order

fun forId(id: String): KeyboardTheme
    // Returns the theme with matching id, or all[0] (Light) if not found
```

### Adding a New Theme

1. Add a new `KeyboardTheme(...)` entry to the `all` list in `KeyboardTheme.kt`
2. The new theme ID will automatically appear in `SettingsActivity` (it iterates `KeyboardTheme.all`)

### How Themes Are Applied

See [../ui/themes.md](../ui/themes.md) for the full runtime application flow via `ThemeHelper`.

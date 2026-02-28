# MainActivity

**File:** `MainActivity.kt` (151 lines)
**Package:** `com.rohanyeole.privacykeyboard`
**Layout:** `res/layout/activity_main.xml`

The app's launcher screen. Shows the keyboard setup status and provides navigation to all companion screens.

---

## 3-State Setup Status

The activity detects which of 3 states the keyboard is in:

| State | Condition | UI |
|-------|-----------|-----|
| **Disabled** | Keyboard not in enabled IME list | Red status indicator, "Enable" button visible |
| **Enabled, not default** | In enabled list but not the active IME | Yellow/orange indicator, "Set as default" button visible |
| **Active** | Enabled AND is the current default IME | Green indicator, both buttons hidden |

### Detection Methods

```kotlin
private fun isKeyboardEnabled(): Boolean
    // InputMethodManager.enabledInputMethodList.any { it.packageName == packageName }

private fun isKeyboardDefault(): Boolean
    // Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    //     .contains(packageName)
```

`updateKeyboardStatus()` is called in `onResume()` so status refreshes when returning from the system settings screen.

---

## Key Methods

### `onCreate(activity)`
1. Sets up click listeners (enable button → system IME settings, select button → IME picker)
2. Registers `AppUpdateHelper` for in-app update checks
3. Wires navigation cards to their respective activities/intents

### `onResume()`
Calls `updateKeyboardStatus()` to refresh the status UI.

### `onActivityResult(requestCode, resultCode, data)`
Forwarded to `AppUpdateHelper` for handling the update install result.

### `onDestroy()`
Unregisters `AppUpdateHelper`.

---

## Layout View IDs

| View ID | Type | Purpose |
|---------|------|---------|
| `tvStatus` | `TextView` | Short status label ("Active" / "Enabled" / "Disabled") |
| `tvStatusSub` | `TextView` | Longer description of the current state |
| `btnEnableKeyboard` | `Button` | Opens system IME settings to enable the keyboard |
| `btnSelectKeyboard` | `Button` | Opens system IME picker to set as default |
| `cardSettings` | `CardView` | → SettingsActivity |
| `cardAbout` | `CardView` | → InfoActivity (About) |
| `cardPrivacy` | `CardView` | → InfoActivity (Privacy Policy) |
| `cardTerms` | `CardView` | → InfoActivity (Terms of Service) |
| `cardLegal` | `CardView` | → InfoActivity (Legal Notices) |

---

## Update Integration

`AppUpdateHelper` is registered in `onCreate()` and `onActivityResult()` is forwarded to it. If a Play Store update is available, it downloads in the background and prompts the user to restart. See [../utils/app-update.md](../utils/app-update.md).

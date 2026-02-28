# Utilities — Overview

Four utility files in `util/`. They are small, focused helpers with no cross-dependencies on each other.

---

## Summary

| File | Lines | Type | Purpose |
|------|-------|------|---------|
| `HapticHelper.kt` | 26 | class | Vibration wrapper with strength levels |
| `WordUtils.kt` | 33 | top-level fns | CapsState transitions, word extraction, validation |
| `ThemeHelper.kt` | 49 | object | Recursive theme color application |
| `AppUpdateHelper.kt` | 99 | class | Google Play flexible in-app update flow |

---

## Import Patterns

```kotlin
// HapticHelper — class, instantiated
import com.rohanyeole.privacykeyboard.util.HapticHelper
val hapticHelper = HapticHelper(context, prefs)

// WordUtils — top-level functions, import individually
import com.rohanyeole.privacykeyboard.util.nextCapsState
import com.rohanyeole.privacykeyboard.util.shouldAutoOffAfterKey
import com.rohanyeole.privacykeyboard.util.extractCurrentWord
import com.rohanyeole.privacykeyboard.util.isValidWord

// ThemeHelper — object, call directly
import com.rohanyeole.privacykeyboard.util.ThemeHelper
ThemeHelper.applyToKeyboard(root, theme)

// AppUpdateHelper — class, lifecycle managed
import com.rohanyeole.privacykeyboard.util.AppUpdateHelper
```

---

## Detailed Docs

- [haptic.md](haptic.md)
- [word-utils.md](word-utils.md)
- [theme-helper.md](theme-helper.md)
- [app-update.md](app-update.md)

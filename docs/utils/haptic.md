# HapticHelper

**File:** `util/HapticHelper.kt` (26 lines)
**Package:** `com.rohanyeole.privacykeyboard.util`

Thin wrapper around `Vibrator` with vibration strength levels controlled by `KeyboardPreferences`.

---

## Constructor

```kotlin
class HapticHelper(context: Context, private val prefs: KeyboardPreferences)
```

Retrieves `Vibrator` via `context.getSystemService(Context.VIBRATOR_SERVICE)`.

---

## `perform()`

Call on every key press for tactile feedback:

```kotlin
hapticHelper.perform()
```

Behaviour:
1. If `prefs.vibrationEnabled == false` → returns immediately (no vibration)
2. Maps `prefs.vibrationStrength` to a duration:
   - `"light"` → 15ms
   - `"strong"` → 50ms
   - anything else (including `"medium"`) → 30ms
3. API 26+ → `VibrationEffect.createOneShot(duration, DEFAULT_AMPLITUDE)`
4. API < 26 → deprecated `vibrator.vibrate(duration)` with `@Suppress("DEPRECATION")`

---

## Required Permission

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

**This permission must be in `AndroidManifest.xml`.** Missing it causes a crash on every key press.

---

## Usage in Service

`PrivacyKeyboardService` calls `hapticHelper.perform()` at the start of every button click/touch handler before committing text. Also called in `EmojiController`'s `onEmojiSelected` lambda.

---

## Known Warning

```
w: Parameter 'button' is never used
```
This warning appears in `PrivacyKeyboardService` (unrelated to `HapticHelper`) and is expected — it's safe to ignore.

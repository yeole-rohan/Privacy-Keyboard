# Activities — Overview

The app has three activities. None of them are part of the keyboard service — they are companion UI screens launched from the keyboard or from the app launcher.

---

## Activity Summary

| Activity | File | Lines | Purpose | Exported |
|----------|------|-------|---------|----------|
| `MainActivity` | `MainActivity.kt` | 151 | App launcher, 3-state keyboard setup status | Yes (LAUNCHER) |
| `SettingsActivity` | `SettingsActivity.kt` | 192 | Vibration + theme settings | No |
| `InfoActivity` | `InfoActivity.kt` | 311 | About / Privacy / Terms / Legal pages | No |

---

## Navigation

```
Launcher icon
    └─► MainActivity
            ├─ "Settings" card    → SettingsActivity
            ├─ "About" card       → InfoActivity (page=ABOUT)
            ├─ "Privacy" card     → InfoActivity (page=PRIVACY)
            ├─ "Terms" card       → InfoActivity (page=TERMS)
            └─ "Legal" card       → InfoActivity (page=LEGAL)

Normal keyboard (gear icon)
    └─► SettingsActivity (FLAG_ACTIVITY_NEW_TASK — launched from a Service)
```

---

## AndroidManifest Registration

```xml
<activity android:name=".MainActivity" android:exported="true"
    android:windowSoftInputMode="adjustResize">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity android:name=".SettingsActivity" android:exported="false"
    android:label="Settings" />

<activity android:name=".InfoActivity" android:exported="false"
    android:label="Info" />
```

---

## Detailed Docs

- [main.md](main.md) — 3-state setup status, enable/select keyboard flows
- [settings.md](settings.md) — vibration + theme picker with live preview
- [info.md](info.md) — dynamically built content pages

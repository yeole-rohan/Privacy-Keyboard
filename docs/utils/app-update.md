# AppUpdateHelper

**File:** `util/AppUpdateHelper.kt` (99 lines)
**Package:** `com.rohanyeole.privacykeyboard.util`

Handles Google Play **flexible** in-app updates. Downloads the update in the background while the user continues using the app, then prompts them to install.

---

## Dependency

```kotlin
implementation("com.google.android.play:app-update-ktx:2.1.0")
```

---

## Lifecycle Integration (in MainActivity)

```kotlin
private lateinit var appUpdateHelper: AppUpdateHelper

override fun onCreate(...) {
    appUpdateHelper = AppUpdateHelper(this)
    appUpdateHelper.register()
}

override fun onResume() {
    super.onResume()
    appUpdateHelper.checkForUpdate()   // checks Play Store on every resume
}

override fun onActivityResult(requestCode, resultCode, data) {
    super.onActivityResult(requestCode, resultCode, data)
    appUpdateHelper.onActivityResult(requestCode, resultCode, data)
}

override fun onDestroy() {
    super.onDestroy()
    appUpdateHelper.unregister()
}
```

---

## Public API

| Method | Description |
|--------|-------------|
| `register()` | Registers the install state listener with Play Core |
| `unregister()` | Removes the listener (call in `onDestroy()`) |
| `checkForUpdate()` | Queries the Play Store; if update available with `FLEXIBLE` type, starts the download |
| `onActivityResult(requestCode, resultCode, data)` | Forwards result to `AppUpdateManager`; resumes pending updates |

---

## Update Flow

```
checkForUpdate()
    │  AppUpdateManager.appUpdateInfo
    │
    ├─ updateAvailability == UPDATE_AVAILABLE
    │  AND isUpdateTypeAllowed(FLEXIBLE)
    │       └─ startUpdateFlowForResult(FLEXIBLE, activity, REQUEST_CODE)
    │               ↓ (download happens in background)
    │
    ├─ installStatus == DOWNLOADED
    │       └─ showRestartPrompt()
    │               ↓ AlertDialog: "Restart to apply update"
    │               └─ completeUpdate() on confirm
    │
    └─ installStatus == FAILED → log error
```

### Install State Listener

Registered in `register()`. Fires when download status changes:
- `DOWNLOADED` → calls `showRestartPrompt()`
- `FAILED` → logs the error code

---

## `showRestartPrompt()`

Shows an `AlertDialog`:
- **Title:** "Update Ready"
- **Message:** "Restart to apply the update"
- **Confirm** → `appUpdateManager.completeUpdate()` (triggers install + restart)
- **Dismiss** → dialog closes (update installs on next app launch)

---

## Notes

- `REQUEST_CODE` is a private constant (e.g. `1001`) used to identify the update result in `onActivityResult`.
- If the Play Store is unavailable (e.g. device without Play Services), `checkForUpdate()` silently catches the exception and does nothing.
- This is the **flexible** flow — the **immediate** (full-screen mandatory) flow is not used.

# Build & Test

---

## Build Configuration (`app/build.gradle.kts`)

| Setting | Value |
|---------|-------|
| `namespace` | `com.rohanyeole.privacykeyboard` |
| `applicationId` | `com.rohanyeole.privacykeyboard` |
| `compileSdk` | 35 |
| `targetSdk` | 34 |
| `minSdk` | 26 (Android 8.0 Oreo) |
| `versionCode` | 1 |
| `versionName` | `"1.0"` |
| Kotlin JVM target | 1.8 |
| View Binding | enabled |
| Minification (release) | disabled (`isMinifyEnabled = false`) |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `androidx.core.ktx` | Kotlin extensions for Android core |
| `androidx.appcompat` | AppCompat backwards compat |
| `material` | Material Design components |
| `androidx.activity` | Activity KTX |
| `androidx.constraintlayout` | ConstraintLayout |
| `com.google.android.play:app-update-ktx:2.1.0` | Google Play flexible in-app updates |
| `junit` | Unit testing (JVM) |
| `androidx.test.junit` | AndroidX test JUnit runner |
| `androidx.test.espresso.core` | Espresso UI tests |

---

## Release Signing

Signing credentials are read from `local.properties` (not committed to git):

```
KEYSTORE_PATH=path/to/keystore.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

`local.properties` must exist for a release build. Debug builds don't require it.

---

## Common Build Commands

```bash
# Run all JVM unit tests (fast, no device needed)
./gradlew test

# Run only debug unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build + install on connected device
./gradlew installDebug

# Build release APK (requires local.properties with signing config)
./gradlew assembleRelease

# Clean build
./gradlew clean
```

---

## Unit Tests

All unit tests are JVM-only — run with `./gradlew test`, no device required.

### Test Files

| File | Tests | Covers |
|------|-------|--------|
| `CapsStateTest.kt` | 8 | `nextCapsState()`, `shouldAutoOffAfterKey()`, state cycle |
| `WordUtilsTest.kt` | 15 | `extractCurrentWord()`, `isValidWord()` |
| `TrieTest.kt` | 7 | `insert()`, `searchByPrefix()`, contains semantics |
| `ExampleUnitTest.kt` | 1 | Template (always passes) |

**Total:** 31 JVM unit tests

### Test Imports

Tests use the new package paths after the Feb 2026 refactor:
```kotlin
import com.rohanyeole.privacykeyboard.model.CapsState
import com.rohanyeole.privacykeyboard.util.nextCapsState
import com.rohanyeole.privacykeyboard.util.shouldAutoOffAfterKey
import com.rohanyeole.privacykeyboard.util.extractCurrentWord
import com.rohanyeole.privacykeyboard.util.isValidWord
import com.rohanyeole.privacykeyboard.trie.Trie
```

### Instrumented Tests

`ExampleInstrumentedTest.kt` — template test that runs on a device/emulator. Run with:
```bash
./gradlew connectedAndroidTest
```

Android-dependent code (views, `InputConnection`, SharedPreferences) requires Robolectric or a device to test.

---

## Gradle Properties (`gradle.properties`)

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`-Xmx2048m` gives the Gradle daemon 2 GB of heap — important for the Kotlin compiler during incremental builds.

---

## After Installing on Device

1. Open **Settings → General management → Keyboard → On-screen keyboard → Manage keyboards**
2. Enable "Privacy Keyboard"
3. Open any text field → tap the globe/keyboard icon in the system bar → select Privacy Keyboard

**Note:** After a build where the service class was renamed, you must re-enable the keyboard in Settings (it appears as a new keyboard to Android even if the package name is the same).

---

## Known Build Warnings

The following warning is expected and safe to ignore:
```
w: Parameter 'button' is never used
   at PrivacyKeyboardService.kt:417:38
```

The deprecated `VIBRATOR_SERVICE` warning is also expected — suppressed with `@Suppress("DEPRECATION")` in `HapticHelper` for API < 26 code path.

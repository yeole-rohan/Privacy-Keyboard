# PrivacyKeyboard — Knowledge Base Index

Android custom keyboard (IME) app written in Kotlin. No ML/NLP libraries — autocomplete uses a Trie + SharedPreferences user dictionary. Minimum SDK 26 (Android 8.0).

## Quick Navigation

| Topic | File | What it covers |
|-------|------|----------------|
| **Architecture** | [architecture.md](architecture.md) | Package structure, patterns, data-flow diagram |
| **Main Service** | [keyboard-service.md](keyboard-service.md) | `PrivacyKeyboardService` — full method reference |
| **Controllers** | [controllers/index.md](controllers/index.md) | All 4 controllers at a glance |
| → Suggestion | [controllers/suggestion.md](controllers/suggestion.md) | Autocomplete pipeline, fuzzy, frequency |
| → Caps | [controllers/caps.md](controllers/caps.md) | 3-state caps lock state machine |
| → Emoji | [controllers/emoji.md](controllers/emoji.md) | Emoji picker, scroll tracking, recent |
| → Clipboard | [controllers/clipboard.md](controllers/clipboard.md) | Clipboard chips, listener lifecycle |
| **Data Layer** | [data/index.md](data/index.md) | All 5 data files, SharedPrefs namespaces |
| → Trie | [data/trie.md](data/trie.md) | Trie, TrieNode, DictionaryLoader |
| → User Dict | [data/user-dict.md](data/user-dict.md) | `UserDictRepository`, word:count encoding |
| → Emoji Data | [data/emoji.md](data/emoji.md) | `EmojiData` categories, `EmojiRepository` |
| → Preferences | [data/preferences.md](data/preferences.md) | `KeyboardPreferences`, `KeyboardTheme` |
| **Activities** | [activities/index.md](activities/index.md) | All 3 activities overview |
| → MainActivity | [activities/main.md](activities/main.md) | 3-state setup status screen |
| → SettingsActivity | [activities/settings.md](activities/settings.md) | Vibration + theme settings |
| → InfoActivity | [activities/info.md](activities/info.md) | About / Privacy / Terms / Legal |
| **UI Layouts** | [ui/layouts.md](ui/layouts.md) | All 15 XML layout files + key view IDs |
| **Themes** | [ui/themes.md](ui/themes.md) | 7-theme color system, runtime application |
| **Utilities** | [utils/index.md](utils/index.md) | All 4 utility files overview |
| → HapticHelper | [utils/haptic.md](utils/haptic.md) | Vibration wrapper |
| → WordUtils | [utils/word-utils.md](utils/word-utils.md) | Pure word/caps logic functions |
| → ThemeHelper | [utils/theme-helper.md](utils/theme-helper.md) | Recursive theme application |
| → AppUpdateHelper | [utils/app-update.md](utils/app-update.md) | Google Play flexible update flow |
| **CapsState** | [model.md](model.md) | `CapsState` enum + transition table |
| **Build & Test** | [build-and-test.md](build-and-test.md) | Gradle config, deps, signing, test commands |

---

## Project Stats

| Category | Count |
|----------|-------|
| Kotlin source files (main) | 21 |
| Kotlin test files | 4 |
| XML layout files | 15 |
| XML drawable files | 14 |
| SharedPreferences namespaces | 3 |
| Keyboard themes | 7 |
| Unit tests | 30 (CapsState: 8, WordUtils: 15, Trie: 7) |

---

## Key Entry Points

- **Adding a feature to the keyboard** → start at [keyboard-service.md](keyboard-service.md)
- **Changing autocomplete behaviour** → [controllers/suggestion.md](controllers/suggestion.md)
- **Changing how words are stored** → [data/user-dict.md](data/user-dict.md)
- **Adding a new theme** → [ui/themes.md](ui/themes.md)
- **Changing vibration / haptics** → [utils/haptic.md](utils/haptic.md)
- **Understanding caps lock** → [controllers/caps.md](controllers/caps.md) + [model.md](model.md)
- **Running tests** → [build-and-test.md](build-and-test.md)

---

## SharedPreferences Namespaces (quick ref)

| Namespace | File | What's stored |
|-----------|------|---------------|
| `keyboard_settings` | `KeyboardPreferences` | vibration on/off, strength, themeId |
| `emoji_prefs` | `EmojiRepository` | recent emojis (comma-separated) |
| `UserDict` | `UserDictRepository` | user words as `"word:count"` entries |

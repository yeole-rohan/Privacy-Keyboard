# Data Layer — Overview

All persistence and static data lives in `data/` and `trie/`. No Room, no SQLite — everything uses SharedPreferences or in-memory data structures.

---

## Files at a Glance

| File | Lines | Purpose |
|------|-------|---------|
| `data/KeyboardPreferences.kt` | 19 | Settings: vibration, theme |
| `data/KeyboardTheme.kt` | 54 | 7 built-in theme color definitions |
| `data/UserDictRepository.kt` | 59 | User word dictionary with frequency |
| `data/EmojiData.kt` | 234 | Static emoji category map (pure data) |
| `data/EmojiRepository.kt` | 25 | Recent emojis SharedPrefs |
| `trie/TrieNode.kt` | 7 | Trie node (children, isEndOfWord, frequency) |
| `trie/Trie.kt` | 53 | Trie: insert, contains, searchByPrefix, incrementFrequency |
| `trie/DictionaryLoader.kt` | 33 | Loads dictionary.zip → `List<String>` |

---

## SharedPreferences Namespaces

| Namespace | Class | Keys |
|-----------|-------|------|
| `keyboard_settings` | `KeyboardPreferences` | `vibration_enabled` (bool, default true), `vibration_strength` (str, default "medium"), `theme_id` (str, default "light") |
| `emoji_prefs` | `EmojiRepository` | `recent_emojis` (comma-separated emoji string, max 50 chars = 50 emoji) |
| `UserDict` | `UserDictRepository` | `user_dict` (StringSet of `"word:count"` entries; backward-compat: bare words decoded as count=0) |

---

## Detailed Docs

- [trie.md](trie.md) — Trie data structure and dictionary loading
- [user-dict.md](user-dict.md) — UserDictRepository, word:count encoding, frequency API
- [emoji.md](emoji.md) — EmojiData categories, EmojiRepository recent-list
- [preferences.md](preferences.md) — KeyboardPreferences, KeyboardTheme color system

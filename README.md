# Privacy Keyboard

A free, open-source Android keyboard that works entirely on your device.
Your keystrokes, words, and clipboard items never leave your phone — not to us, not to anyone.

**Website:** [privacy-keyboard.rohanyeole.com](https://privacy-keyboard.rohanyeole.com)

---

## Why

Your keyboard is the most sensitive app on your phone. It sees your passwords before you tap submit, your messages before you hit send, and your bank account numbers. Most popular keyboards send some or all of this data to remote servers.

Privacy Keyboard has **no internet permission** — Android will not allow any network access without it. There is nothing to opt out of, because nothing is collected.

---

## Features

- Word suggestions from a built-in dictionary — computed entirely on-device
- Personal word learning — saves words you use to improve suggestions locally
- Emoji picker with recently used emoji
- Clipboard manager — held in memory only, never written to disk
- 3-state Caps Lock: off / shift (one letter) / caps lock (stays on)
- Adjustable haptic feedback: Light, Medium, or Strong
- Seven colour themes: Light, Dark, Ocean, Sunset, Forest, Lavender, Midnight
- Spacebar swipe to move the cursor left or right
- Special-character keyboard with symbols, brackets, and punctuation
- No internet permission — the app cannot connect to the network

---

## Install & Enable

1. Install the app from the Google Play Store
2. Open Privacy Keyboard and tap **Enable Keyboard** — Android opens the keyboard settings list
3. Find Privacy Keyboard in the list and toggle it **ON**, then tap OK
4. Tap **Select as Active Keyboard** and pick Privacy Keyboard from the system picker

Full visual guide: [privacy-keyboard.rohanyeole.com/#install](https://privacy-keyboard.rohanyeole.com/#install)

---

## Build from Source

**Requirements:** Android Studio Hedgehog or newer, JDK 17, Android SDK 34

```bash
git clone https://github.com/rohanyeole/privacy-keyboard.git
cd privacy-keyboard
./gradlew installDebug        # build + install on connected device
./gradlew test                # run unit tests (no device needed)
```

After installing from source you need to re-enable the keyboard in Settings
(Android treats the newly installed package as a new input method).

---

## Project Structure

```
app/src/main/java/com/rohanyeole/privacykeyboard/
├── PrivacyKeyboardService.kt   ← keyboard service (orchestrator)
├── MainActivity.kt             ← setup / enable flow
├── SettingsActivity.kt         ← theme + vibration settings
├── InfoActivity.kt             ← about / privacy / terms / legal pages
├── controller/
│   ├── CapsController.kt       ← 3-state caps lock logic
│   ├── ClipboardController.kt  ← clipboard listener + UI
│   ├── EmojiController.kt      ← emoji picker + recent emoji
│   └── SuggestionController.kt ← word suggestion bar
├── data/
│   ├── EmojiData.kt            ← emoji category map
│   ├── EmojiRepository.kt      ← recent emoji (SharedPreferences)
│   ├── KeyboardTheme.kt        ← theme definitions
│   └── UserDictRepository.kt   ← personal word list (SharedPreferences)
├── trie/
│   ├── Trie.kt                 ← prefix trie for word lookup
│   ├── TrieNode.kt
│   └── DictionaryLoader.kt     ← loads dictionary from assets/dictionary.zip
├── model/
│   └── CapsState.kt            ← enum: OFF / SHIFT / CAPS_LOCK
└── util/
    ├── AppUpdateHelper.kt      ← Google Play in-app update flow
    ├── HapticHelper.kt         ← vibration wrapper
    ├── ThemeHelper.kt          ← applies colour theme to all key buttons
    └── WordUtils.kt            ← word extraction + caps helpers
```

---

## Privacy

Privacy Keyboard stores three things locally on your device — and nothing else:

| Data | Purpose | Leaves device? |
|---|---|---|
| Personal word list | Improve word suggestions | Never |
| Recent emoji | Show at top of emoji picker | Never |
| App settings | Theme and vibration strength | Never |

Clipboard items are held in memory only while the keyboard is open and are discarded when it closes.

Full policy: [privacy-keyboard.rohanyeole.com/privacy.html](https://privacy-keyboard.rohanyeole.com/privacy.html)

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions, code style notes, and how to submit a pull request.

---

## Author

Made by **Rohan Yeole** — [rohanyeole.com](https://rohanyeole.com)

---

## License

Free for personal, non-commercial use. See [Terms of Service](https://privacy-keyboard.rohanyeole.com/terms.html) for details.
Open-source attributions: [privacy-keyboard.rohanyeole.com/legal.html](https://privacy-keyboard.rohanyeole.com/legal.html)

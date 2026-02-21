# Contributing to Privacy Keyboard

Thanks for your interest in improving Privacy Keyboard.

---

## Getting Started

**Requirements**
- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Android SDK 34
- A physical Android device or emulator (API 26+)

**Clone and open**
```bash
git clone https://github.com/rohanyeole/privacy-keyboard.git
```
Open the root folder in Android Studio. Let Gradle sync finish before running.

**Run unit tests** (no device needed)
```bash
./gradlew test
```

**Build and install on device**
```bash
./gradlew installDebug
```
After installing, go to **Settings → General Management → Keyboard list and default** and re-enable Privacy Keyboard (Android requires this after each new install from source).

---

## Code Style

- Kotlin only; follow standard Kotlin conventions
- View Binding is used throughout — do not use `findViewById`
- Keep `PrivacyKeyboardService.kt` as a slim orchestrator; move logic into the appropriate controller or utility
- No internet, analytics, or third-party SDKs — keep the dependency list minimal
- Unit-testable logic (word utils, trie, caps state) lives in `util/`, `trie/`, and `model/` — keep Android imports out of those packages

---

## Project Layout

| Package | Responsibility |
|---|---|
| `controller/` | UI state managers for caps, clipboard, emoji, suggestions |
| `data/` | SharedPreferences repositories + theme definitions |
| `trie/` | Prefix trie for word lookup; `DictionaryLoader` reads `assets/dictionary.zip` |
| `model/` | Pure data types (e.g. `CapsState` enum) |
| `util/` | Stateless helpers: haptic, theme application, word extraction, in-app updates |

---

## Submitting a Pull Request

1. Fork the repository and create a branch from `main`
   ```bash
   git checkout -b fix/your-description
   ```
2. Make your changes; run `./gradlew test` to verify nothing is broken
3. Keep the PR focused — one fix or feature per PR
4. Write a clear description of what changed and why
5. Open the pull request against `main`

---

## Reporting Bugs

Open a GitHub Issue with:
- Android version and device model
- Steps to reproduce
- What you expected vs. what happened
- A screenshot or screen recording if relevant

---

## What Not to Add

- Internet access, analytics, or any form of data collection
- Third-party keyboard SDKs or AI cloud features
- Features that require new dangerous permissions

If unsure whether a change fits the project's goals, open an Issue first to discuss before writing code.

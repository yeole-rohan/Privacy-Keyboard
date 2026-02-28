# XML Layouts Reference

All layouts are in `app/src/main/res/layout/`.

---

## Keyboard Layouts

### `keyboard_layout.xml` — Normal Keyboard
Root: `LinearLayout` (vertical)

The main keyboard UI. Includes all rows and the suggestion strip.

| View ID | Type | Purpose |
|---------|------|---------|
| `btnKeyboardSettings` | `Button` | Gear icon → opens SettingsActivity (hidden when suggestions showing) |
| `suggestion1` | `TextView` | First autocomplete suggestion |
| `suggestion2` | `TextView` | Second autocomplete suggestion |
| `suggestion3` | `TextView` | Third autocomplete suggestion |
| `suggDivider12` | `View` | Thin vertical divider between suggestion 1 and 2 |
| `suggDivider23` | `View` | Thin vertical divider between suggestion 2 and 3 |
| `clipboardChipsContainer` | `LinearLayout` | Container for clipboard chips (replaces suggestion bar) |
| `btnEmoji` | `Button` | Emoji panel toggle |
| `btnEmojiBackspace` | `Button` | Backspace visible only in emoji mode |
| `normalContainer` | `LinearLayout` | Parent for emoji panel + keyboard rows |
| `keyboardRowsContainer` | `LinearLayout` | Parent for all letter/number rows |
| `rowNumeric` | `include` | → `row_numeric.xml` |
| `rowAlphabetic1` | `include` | → `row_alphabetic_1.xml` (Q–P) |
| `rowAlphabetic2` | `include` | → `row_alphabetic_2.xml` (A–L) |
| `rowAlphabetic3` | `include` | → `row_alphabetic_3.xml` (Z–M + caps + backspace) |
| `functionalKeys` | `include` | → `functional_keys.xml` |

`emojiBinding.root` is **programmatically inserted** at index 0 inside `normalContainer` in `onCreateInputView()`.

---

### `keyboard_special.xml` — Symbol Keyboard
Root: `LinearLayout` (vertical)

| View IDs | Purpose |
|----------|---------|
| `btnAt`, `btnHash`, `btnDollar`, `btnUnderscore`, `btnAmpersand`, `btnMinus`, `btnPlus`, `btnLeftParen`, `btnRightParen`, `btnSlash` | Row 1 special chars: `@ # $ _ & - + ( ) /` |
| `btnAsterisk`, `btnQuote`, `btnApostrophe`, `btnColon`, `btnSemicolon`, `btnExclamation`, `btnQuestion` | Row 2 special chars: `* " ' : ; ! ?` |
| `backSpace` | `include` → `back_space_btn.xml` |
| `rowNumeric` | `include` → `row_numeric.xml` |
| `btnEnter`, `btnSpace`, `btnComma`, `btnDot`, `btnSpecialKeys` | Bottom row (direct IDs, no include) |

---

### `emoji_layout.xml` — Emoji Panel
Root: `LinearLayout` (vertical)

| View ID | Type | Purpose |
|---------|------|---------|
| `tvCurrentCategory` | `TextView` | Current visible category label (updated by scroll listener) |
| `scrollView` | `ScrollView` | Scrollable container; height = 30% of screen height |
| `emojiContainer` | `LinearLayout` | All dynamically built emoji rows and section headers |

Height is set programmatically: `(screenHeight * 0.3).toInt()`.

---

## Alphabetic Row Layouts

### `row_alphabetic_1.xml` — Q to P (10 keys)
| View IDs | Keys |
|----------|------|
| `btnQ` `btnW` `btnE` `btnR` `btnT` `btnY` `btnU` `btnI` `btnO` `btnP` | Q W E R T Y U I O P |

### `row_alphabetic_2.xml` — A to L (9 keys)
18dp side margins to center the shorter row.

| View IDs | Keys |
|----------|------|
| `btnA` `btnS` `btnD` `btnF` `btnG` `btnH` `btnJ` `btnK` `btnL` | A S D F G H J K L |

### `row_alphabetic_3.xml` — Z to M + Caps + Backspace
| View IDs | Purpose |
|----------|---------|
| `capsLock` | `include` → `caps_lock_btn.xml` |
| `btnZ` `btnX` `btnC` `btnV` `btnB` `btnN` `btnM` | Z X C V B N M |
| `backSpace` | `include` → `back_space_btn.xml` |

### `row_numeric.xml` — 0 to 9 (10 keys)
| View IDs | Keys |
|----------|------|
| `btn0` `btn1` `btn2` `btn3` `btn4` `btn5` `btn6` `btn7` `btn8` `btn9` | 0 1 2 3 4 5 6 7 8 9 |

---

## Functional Keys

### `functional_keys.xml` — Bottom Row
Included in normal keyboard layout.

| View ID | Type | Purpose |
|---------|------|---------|
| `btnSpace` | `Button` | Space (tap) + cursor move left/right (swipe, 30px threshold) |
| `btnComma` | `Button` | `,` |
| `btnDot` | `Button` | `.` |
| `btnApostrophe` | `Button` | `'` |
| `btnEnter` | `Button` | Enter (sends `KEYCODE_ENTER`) |
| `btnSpecialKeys` | `Button` | Toggle to `!?` special keyboard |

---

## Component Layouts

### `caps_lock_btn.xml`
| View ID | Purpose |
|---------|---------|
| `btnCapsLock` | Caps button. Text: `a` (OFF), `A` (SHIFT), `A` bold+underline (CAPS_LOCK) |

### `back_space_btn.xml`
| View ID | Purpose |
|---------|---------|
| `btnBackSpace` | Backspace (touch — continuous delete on hold) |

### `top_layout.xml`
| View ID | Purpose |
|---------|---------|
| `btnKeyboardSettings` | Gear icon for settings (same ID as in keyboard_layout.xml but this is a separate component file) |

---

## Activity Layouts

### `activity_main.xml`
Root: `ScrollView` containing a `LinearLayout` (vertical).

Key view IDs: `tvStatus`, `tvStatusSub`, `btnEnableKeyboard`, `btnSelectKeyboard`, `cardSettings`, `cardAbout`, `cardPrivacy`, `cardTerms`, `cardLegal`.

### `activity_settings.xml`
Root: `LinearLayout` (vertical).

Key view IDs: `switchVibration`, `layoutStrength`, `rgStrength`, `rbLight`, `rbMedium`, `rbStrong`, `themeListContainer`, `previewContainer`.

### `activity_info.xml`
Root: `ScrollView` containing `contentContainer` (LinearLayout) + `btnWebsite`.

---

## Style Notes

- Key buttons: `btnLightTheme` style defined in `values/themes.xml` — 52dp height, 48dp min height
- Side margins on row 2: `layout_marginStart/End = 18dp` (dp values, not percentages)
- Emoji rows: `MATCH_PARENT` width + `weight=1` TextViews for equal distribution
- Button backgrounds are overridden at runtime by `ThemeHelper.applyKeyStyle()` with `RippleDrawable`

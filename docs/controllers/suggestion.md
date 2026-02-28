# SuggestionController

**File:** `controller/SuggestionController.kt` (155 lines)
**Package:** `com.rohanyeole.privacykeyboard.controller`

Drives the 3-slot autocomplete suggestion bar. Implements 7 improvements over the original length-only ranking.

---

## Constructor

```kotlin
SuggestionController(
    trie: Trie,
    userDictRepo: UserDictRepository,
    suggestionViews: List<TextView>,    // [suggestion1, suggestion2, suggestion3]
    dividers: List<View>,               // [suggDivider12, suggDivider23]
    onWordSelected: (word: String, rawInput: String) -> Unit
)
```

`onWordSelected` lambda (in PrivacyKeyboardService):
```kotlin
{ word, rawInput ->
    currentInputConnection?.deleteSurroundingText(rawInput.length, 0)
    currentInputConnection?.commitText("$word ", 1)
}
```

---

## Public API

| Method | Description |
|--------|-------------|
| `update(inputText: String)` | Main entry — called from `onUpdateSelection` with full field text |
| `hide()` | Hides all suggestion views and dividers |

---

## `update()` Full Pipeline

```
rawLastWord = inputText.split(" ").lastOrNull() ?: ""

Case A: rawLastWord.isEmpty() AND inputText.isNotEmpty()
    → showPostSpaceSuggestions()                    [Improvement #7]

Case B: rawLastWord.isEmpty() AND inputText.isEmpty()
    → hide()

Case C: rawLastWord is non-empty
    lastInputText = rawLastWord.lowercase()

    Step 1 — User dict (freq-sorted, take ≤2)       [Improvements #1, #2]
        userDictRepo.getAllWithFrequency()
            .filter { it.key.startsWith(lastInputText, ignoreCase = true) }
            .sortedByDescending { it.value }
            .take(2)

    Step 2 — Trie prefix (freq-sorted internally)   [Improvement #1]
        trie.searchByPrefix(lastInputText)
            → fill to 3 (skip duplicates)

    Step 3 — Fuzzy: adjacency substitution          [Improvement #4]
        (only if still <3 AND lastInputText.length > 2)
        for each position i: replace char with each adjacent key
        candidate = substituted word → trie.contains(candidate)?

    Step 4 — Fuzzy: transposition                   [Improvement #5]
        (only if still <3 AND lastInputText.length > 2)
        swap each adjacent pair
        candidate = transposed word → trie.contains(candidate)?

    Step 5 — Capitalize if sentence start            [Improvement #6]
        isSentenceStart(inputText, rawLastWord)?
            YES → map { replaceFirstChar { c -> c.uppercase() } }

    Step 6 → renderSuggestions(displayList, rawLastWord)
```

---

## The 7 Improvements

| # | What | Where |
|---|------|-------|
| 1 | Frequency-ranked suggestions (trie + user dict) | Steps 1–2 |
| 2 | User-dict words shown before trie words | Step 1 takes ≤2 slots first |
| 3 | Learn from taps — increment freq on selection | `renderSuggestions` tap handler |
| 4 | Fat-finger correction (adjacency substitution) | Step 3 |
| 5 | Transposition correction (swap adjacent chars) | Step 4 |
| 6 | Sentence-start capitalization | Step 5 |
| 7 | Post-space personal word suggestions | Case A |

---

## `renderSuggestions(displayList, rawLastWord)`

Shows up to 3 suggestions, updates dividers, and wires tap handlers:

```kotlin
tv.setOnClickListener {
    val word = tv.text.toString()   // already capitalized if #6 applied
    onWordSelected(word, rawLastWord)
    userDictRepo.incrementFrequency(word.lowercase())   // Improvement #3
    trie.incrementFrequency(word.lowercase())           // Improvement #3
    hide()
}
```

**Post-space taps:** `rawLastWord = ""` → `deleteSurroundingText(0, 0)` is a no-op; word appends after existing space.

---

## `showPostSpaceSuggestions()` — Improvement #7

```kotlin
val topWords = userDictRepo.getTopWords(3)
if (topWords.isEmpty()) { hide(); return }
renderSuggestions(topWords, "")
```

Shows the user's 3 most-tapped words when the cursor is after a space.

---

## `isSentenceStart(inputText, rawLastWord): Boolean`

```kotlin
val beforeWord = inputText.dropLast(rawLastWord.length)
return beforeWord.isEmpty() || beforeWord.takeLast(2) in setOf(". ", "! ", "? ")
```

Triggers at: empty field, after period+space, after exclamation+space, after question+space.

---

## QWERTY Adjacency Map (Improvement #4)

Used to generate fat-finger candidates. For each char in the typed word, substitute it with each adjacent key and check `trie.contains(candidate)`.

```
q→was      w→qeasd    e→wrsdf    r→etdfg
t→ryfgh    y→tughj    u→yihjk    i→uojkl
o→ipkl     p→ol       a→qwsz     s→awedzx
d→serfxc   f→drtgcv   g→ftyhvb   h→gyujbn
j→huiknm   k→jiolm    l→kop      z→asx
x→zsdc     c→xdfv     v→cfgb     b→vghn
n→bhjm     m→njk
```

---

## Divider Visibility Logic

```
dividers[0] (between slot 1 and 2): VISIBLE iff suggestion1 AND suggestion2 both VISIBLE
dividers[1] (between slot 2 and 3): VISIBLE iff suggestion2 AND suggestion3 both VISIBLE
```

---

## Notes

- Suggestions always show the display string (possibly capitalized) to `onWordSelected`; `rawLastWord.length` handles deletion correctly regardless of case difference.
- Fuzzy candidates only come from the trie (not user dict) because `trie.contains()` is the validator.
- User dict words filtered with `ignoreCase = true` but sorted lowercase for consistency.

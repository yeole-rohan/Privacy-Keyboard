# InfoActivity

**File:** `InfoActivity.kt` (311 lines)
**Package:** `com.rohanyeole.privacykeyboard`
**Layout:** `res/layout/activity_info.xml`

Displays one of four informational pages. The page type is determined by an Intent extra — the same Activity class is reused for all four pages.

---

## Pages

| Page | Intent extra value | Content |
|------|-------------------|---------|
| About | `"ABOUT"` | App description, version, author info |
| Privacy Policy | `"PRIVACY"` | Privacy commitments (no data collection) |
| Terms of Service | `"TERMS"` | Usage terms |
| Legal Notices | `"LEGAL"` | Open source attributions and legal text |

Launched from `MainActivity` via intent extra:
```kotlin
intent.putExtra("page", "PRIVACY")
startActivity(intent)
```

---

## Dynamic UI Construction

The activity doesn't use static XML layouts for content — it builds all text blocks programmatically in `renderContent()`, then adds them to `contentContainer` (a `LinearLayout` inside a `ScrollView`).

### Content Builder Helpers

| Method | Adds |
|--------|------|
| `addTitle(text)` | Large bold title |
| `addBody(text)` | Regular paragraph text |
| `addBullet(text)` | `• text` indented bullet point |
| `addNumbered(n, text)` | `n. text` numbered item |
| `addSection(heading)` | Medium bold section heading |
| `addSpacer()` | Vertical whitespace |
| `addDivider()` | Thin horizontal line |

Each method creates a `TextView` (or `View` for divider/spacer) with appropriate typography and appends it to `contentContainer`.

---

## Content Methods

| Method | Returns |
|--------|---------|
| `aboutContent()` | List of UI elements for the About page |
| `privacyContent()` | List of UI elements for the Privacy Policy |
| `termsContent()` | List of UI elements for the Terms of Service |
| `legalContent()` | List of UI elements for the Legal Notices |

`titleFor(page)` returns the activity title string shown in the toolbar for each page type.

---

## Layout View IDs

| View ID | Type | Purpose |
|---------|------|---------|
| `contentContainer` | `LinearLayout` | Parent for all dynamically added text blocks |
| `btnWebsite` | `Button` | Opens the app's website (visible on About page) |

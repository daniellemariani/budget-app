---
paths:
  - "android/**/*.kt"
  - "web/**/*.tsx"
  - "web/**/*.ts"
  - "web/**/*.css"
---

# Design System Rules

Full reference: `specs/design/design.md`

## Typography

- **Primary font: Inter** — not a system font on Android; must be bundled explicitly via the Google Fonts downloadable font provider or as a local asset in `res/font/`. Never assume it is available as a system font.
- **All text sizes in `sp` on Android** — never `dp` for text. Layouts must remain functional at up to 200% system font scale without truncation or overlap.
- **Web:** Inter loaded from Google Fonts using the variable font (`Inter:wght@100..900`).

Typography scale (base at 1× system scale):

| Role | Size | Weight | Usage |
|---|---|---|---|
| Display | 32sp | 600 | Net worth, hero financial amounts |
| Headline | 22sp | 600 | Screen titles, bottom sheet titles |
| Title | 17sp | 600 | Card titles, section headers |
| Body | 15sp | 400 | Standard content, transaction notes |
| Label | 13sp | 500 | Buttons, form labels, chips, tags |
| Caption | 11sp | 400 | Timestamps, secondary metadata, currency codes |

---

## Financial Number Formatting

- Currency format: `$#,##0.00` (e.g. `$1,234.56`)
- Never abbreviate amounts in transaction lists, account detail, or budget detail views
- Dashboard summary blocks may abbreviate ≥ $1,000,000 as `$1.2M` only
- **Negative amounts:** always use the typographic minus `−` (U+2212), not a hyphen `-`. Format: `−$48.00`
- **Never rely on color alone** to indicate a negative value — the `−` sign is mandatory
- **Percentages:** always one decimal place — `68.4%`, `100.0%`. Goal progress may exceed 100% — display actual value (e.g. `112.3%`)
- **Amount alignment:** right-align and decimal-align all amounts within lists and tables. Currency symbols align with the leftmost character of the amount column.

---

## Color Independence Rule

Color alone must never communicate financial meaning. Every semantic state must include a secondary signal.

| State | Color | Required secondary signal |
|---|---|---|
| Overspent budget | `color.semantic.error` | `−` sign + "over" label |
| Negative balance | `color.semantic.error` | `−` sign prefix |
| Warning budget | `color.semantic.warning` | Icon or "!" indicator |
| Income transaction | `color.semantic.success` | `+` sign prefix |
| Sync conflict | `color.semantic.warning` | Conflict icon + label |

---

## Progress Bar Thresholds

Budget progress bars and remaining amount labels use this three-tier color logic:

| Spend level | Bar fill color | Amount text color |
|---|---|---|
| < 85% of budget | `color.accent.primary` | `color.accent.primary` |
| 85–99% of budget | `color.semantic.warning` | `color.semantic.warning` |
| ≥ 100% (overspent) | `color.semantic.error` | `color.semantic.error` |

Goal progress bars always use `color.accent.primary` — goals have no overspent concept.

Applies to: Budget List, Budget Detail, Dashboard budget rows.

---

## Spacing

All spacing must use the defined token system. Arbitrary spacing values are not permitted.

| Token | Value |
|---|---|
| `spacing.xxs` | 2dp |
| `spacing.xs` | 4dp |
| `spacing.sm` | 8dp |
| `spacing.md` | 16dp |
| `spacing.lg` | 24dp |
| `spacing.xl` | 32dp |
| `spacing.xxl` | 48dp |

---

## Dark Mode

- Dark mode follows the system setting (Android system dark mode / OS preference on web)
- **Every screen and component must support both light and dark mode from initial implementation** — no component may be light-only
- Use semantic token names (`color.accent.primary`, `color.text.secondary`, etc.) — never hardcode hex values in UI code

---

## Iconography

- **Android:** Material Symbols, Outlined style
- **Web:** Tabler Icons, Outlined style
- Filled style is used exclusively for selected/active states (e.g. active bottom nav item)
- Icon sizes: 20dp inline with body text, 24dp for standalone actions and nav bar items, 16dp for caption/metadata context
- Every icon must have a text label or accessible content description — `contentDescription` on Android, `aria-label` on web
- Custom icons are not permitted in Phase 1

---

## What to Avoid

Never generate UI that includes:
- Glassmorphism or heavy transparency effects
- Gradients (backgrounds, cards, buttons)
- Neon or overly saturated colors
- Heavy drop shadows — use `elevation.sm` (1dp) for cards only
- Bounce or spring physics in animations
- Transitions longer than 400ms
- Hardcoded hex color values — always use design tokens
- Hardcoded spacing values — always use spacing tokens

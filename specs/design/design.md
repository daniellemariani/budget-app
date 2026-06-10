# Design System — Capital

**Version:** 0.3.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-05-27
**Last Updated:** 2026-06-08

---

## Overview

This document defines the visual design system, UX principles, theming strategy, and cross-platform UI guidelines for Capital.

The goal of the design system is to create a calm, trustworthy, and highly readable financial experience that scales consistently across Android, Web, and future Flutter/KMP clients.

This document acts as the canonical visual and UX reference for the product. All feature specifications, component implementations, and platform-specific UI layers must align with the principles and constraints defined here.

This document is intentionally implementation-agnostic. It defines visual and interaction standards, not platform-specific code.

---

## Related Documents

| Document | Purpose |
|---|---|
| SPEC.md | Global business rules and feature index |
| PRODUCT.md | Product vision and roadmap |
| ARCHITECTURE.md | Technical architecture and platform decisions |
| specs/design/navigation.md | Navigation flows and screen relationships |
| specs/technical/offline-sync.md | Sync behavior and sync status concepts |
| specs/features/dashboard/spec.md | Dashboard requirements and data presentation |

---

## Goals

- Create a minimalist and professional financial management experience
- Reduce cognitive load when viewing financial information
- Prioritize readability and information hierarchy over decoration
- Establish a consistent cross-platform visual identity
- Define a reusable color system and typography scale
- Standardize spacing, elevation, and component behavior
- Ensure accessibility and dark mode parity from the beginning
- Prevent long-term design drift as features and platforms expand

---

## Non-Goals

- Define implementation-specific Compose, React, or Flutter widgets
- Replace platform design systems (Material 3 remains the Android foundation)
- Provide pixel-perfect mockups for every screen
- Define marketing website branding or promotional assets
- Define illustration systems or mascot/brand character guidelines

---

## Design Philosophy

Capital is designed around the concept of:

> Calm Financial Control

The product should help users feel organized, informed, and in control of their finances without creating unnecessary visual stress.

The app should feel:

- Stable
- Trustworthy
- Clean
- Structured
- Calm
- Modern
- Professional

The app should avoid:

- Aggressive fintech aesthetics
- Trading-style dashboards
- Excessive gradients and bright colors
- Visual clutter
- Overly playful motion
- Dense unreadable layouts
- Decorative UI with weak information hierarchy

The user experience should emphasize clarity, consistency, and long-term usability over novelty.

---

## Design Principles

### Clarity Over Decoration

UI elements exist to improve financial understanding.

Visual decoration must never reduce readability or increase cognitive load. Information hierarchy should be achieved primarily through spacing, typography, and layout rather than excessive color usage or ornamentation.

---

### Calm Financial Experience

The product should reduce financial anxiety, not amplify it.

The interface should feel calm and approachable even when displaying negative financial states such as overspending or debt.

Semantic colors should communicate status without dominating the interface.

---

### Neutral-First Design

Most UI surfaces should remain neutral.

Accent and semantic colors should be reserved for:

- Actions
- Progress indicators
- Financial status
- Warnings
- Charts
- Highlights

Neutral layouts improve readability and allow financial data to remain the primary focus.

---

### Information Hierarchy Through Spacing

Whitespace and layout structure are preferred over heavy visual separators.

The UI should feel breathable even when displaying data-dense financial screens.

Spacing consistency is mandatory across all platforms.

---

### Trustworthy and Stable

Financial software must feel reliable.

Animations, transitions, and interactions should reinforce stability and predictability rather than entertainment.

The app should feel dependable during:

- Offline usage
- Sync states
- Error states
- Data loading
- Dashboard refreshes

---

### Cross-Platform Consistency

Android, Web, and future Flutter/KMP clients should feel like members of the same product family.

Platform-specific conventions may differ, but typography, colors, layout philosophy, spacing, semantic meaning, and component behavior must remain consistent.

---

## Visual Identity

### Overall Style

The visual style is minimalist, modern, soft, professional, data-focused, and productivity-oriented.

The UI should use:

- Soft elevated surfaces
- Rounded cards
- Restrained shadows
- Spacious layouts
- High readability typography
- Limited accent usage

The UI should avoid:

- Glassmorphism
- Neon aesthetics
- Heavy gradients
- Excessive transparency
- Overly saturated palettes
- Decorative textures

---

### Reference Applications

The following applications represent strong visual references for Budget App:

- Copilot Money — visual polish, spacing, premium calm aesthetic
- YNAB — budgeting workflows and information organization
- Monarch Money — dashboard composition and financial summaries
- Wallet by BudgetBakers — information architecture and budgeting UX

These references are directional inspiration only. Capital maintains its own identity and implementation.

---

## Color System

### Color Philosophy

The color system is intentionally restrained. Most of the interface should use neutral surfaces with accent colors reserved for meaningful states and interactions.

The primary accent color uses a muted teal palette because it communicates trust, stability, modernity, and calmness without the aggressiveness commonly associated with bright fintech greens.

The accent is consistent across light and dark modes — the same teal family adapted for contrast, not a different hue. This preserves a single brand identity across themes.

---

### Light Theme

| Role | Token | Hex |
|---|---|---|
| Background | `color.background.primary` | `#F7F8FA` |
| Surface / Card | `color.surface.card` | `#FFFFFF` |
| Surface Alt | `color.surface.alt` | `#F1F3F5` |
| Border | `color.border.default` | `#E2E8F0` |
| Primary Text | `color.text.primary` | `#111827` |
| Secondary Text | `color.text.secondary` | `#6B7280` |
| Primary Accent | `color.accent.primary` | `#0D7377` |
| On Primary Accent | `color.accent.on` | `#FFFFFF` |
| Positive / Income | `color.semantic.success` | `#16A34A` |
| Negative / Overspent | `color.semantic.error` | `#DC2626` |
| Warning | `color.semantic.warning` | `#D97706` |
| Info | `color.semantic.info` | `#2563EB` |

---

### Dark Theme

| Role | Token | Hex |
|---|---|---|
| Background | `color.background.primary` | `#111827` |
| Surface / Card | `color.surface.card` | `#1F2937` |
| Surface Alt | `color.surface.alt` | `#1C2434` |
| Border | `color.border.default` | `#374151` |
| Primary Text | `color.text.primary` | `#F3F4F6` |
| Secondary Text | `color.text.secondary` | `#9CA3AF` |
| Primary Accent | `color.accent.primary` | `#2EB5AC` |
| On Primary Accent | `color.accent.on` | `#0A2E2D` |
| Positive / Income | `color.semantic.success` | `#4ADE80` |
| Negative / Overspent | `color.semantic.error` | `#F87171` |
| Warning | `color.semantic.warning` | `#FBBF24` |
| Info | `color.semantic.info` | `#60A5FA` |

**Notes:**
- `color.accent.on` is the text/icon color used on top of filled accent surfaces (buttons, filled progress bars, chips). Never use primary text on an accent background.
- Surface Alt dark (`#1C2434`) is a neutral dark tone in the same gray family as the other dark surfaces. It avoids the blue cast present in earlier drafts.
- The accent shift from `#0D7377` (light) to `#2EB5AC` (dark) stays within the same teal family — adapted for dark contrast, not a different hue.

---

### Dark Mode Strategy

Dark mode follows the system setting (Android system dark mode, OS-level preference on web) in Phase 1. A manual user toggle is deferred to a future phase.

All feature implementations must support both modes from the start. No screen or component may be light-only.

---

### Semantic Color Usage

#### Positive / Income

Used for positive balances, income indicators, goal completion, healthy financial states, and positive trends. Must not dominate the interface.

#### Negative / Overspent

Used for overspent budgets, debt emphasis, errors, financial warnings, and negative trends. Should remain readable and calm — not alarming.

#### Warning

Used for approaching budget limits, pending issues, partial sync problems, and important informational alerts.

#### Info

Used for informational states, neutral charts, guidance, and status messaging.

---

### Color Independence Rule

Color alone must never communicate important financial meaning. Every semantic state must include a secondary signal — an icon, a text label, a sign character, or a combination.

| State | Color | Secondary Signal |
|---|---|---|
| Overspent budget | Negative / Red | `−` sign + "over" label |
| Negative balance | Negative / Red | `−` sign prefix |
| Warning budget | Warning / Amber | Icon or "!" indicator |
| Income transaction | Positive / Green | `+` sign prefix |
| Sync conflict | Warning / Amber | Conflict icon + label |

---

## Typography

### Typography Philosophy

Typography is a primary tool for information hierarchy. Financial data must remain readable at all times.

The system prioritizes legibility, numeric readability, consistent spacing, clear hierarchy, and moderate density.

---

### Primary Font

**Inter** is the primary font across all platforms.

Fallback stack: `Inter, Roboto, system-ui, sans-serif`

**Android note:** Inter is not a system font on Android. It must be bundled explicitly — either as a downloadable font via the Google Fonts provider (preferred for bundle size) or as a local asset in the `res/font/` directory. Do not assume it will be available as a system font on any Android version.

**Web note:** Inter is loaded from Google Fonts. Include the variable font (`Inter:wght@100..900`) to avoid loading multiple weight files.

---

### Display / Brand Font

**Borel** is the display font used exclusively for the app name "Capital" in the `AppHeader` on the Dashboard tab. It is not used anywhere else in the UI.

Borel is a cursive display typeface that gives the brand name visual personality and distinction from the Inter-based UI. It must not be applied to any general UI text, labels, or financial data.

**Android registration:**

1. Download the Borel font file (`Borel-Regular.ttf`) from Google Fonts.
2. Place it at `android/app/src/main/res/font/borel_regular.ttf`.
3. Create a font resource XML file at `android/app/src/main/res/font/borel.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <font
        android:font="@font/borel_regular"
        android:fontStyle="normal"
        android:fontWeight="400"
        app:font="@font/borel_regular"
        app:fontStyle="normal"
        app:fontWeight="400" />
</font-family>
```

4. Define the `FontFamily` in the design system's typography file (`core/ui/theme/Type.kt`):

```kotlin
val BorelFontFamily = FontFamily(
    Font(R.font.borel_regular, FontWeight.Normal)
)
```

5. `BorelFontFamily` is imported from `core/ui/theme/` wherever needed. It must not be redeclared inline in individual composables.

**Usage:** `AppHeader` applies `BorelFontFamily` at `26.sp` for the "Capital" label on the Dashboard tab only. This value may be adjusted at implementation time after visual QA.

**Web note:** Borel is available on Google Fonts. Load it as a display font alongside Inter. Apply only to the equivalent of the app name header element.

**Minimum size:** Do not render Borel at less than `20.sp` / `20px` — the cursive letterforms lose legibility at small sizes.

---

### Typography Scale

All sizes are in `sp` on Android (respects system font scaling) and `rem` on web. The values below express the base size at 1× system scale.

| Role | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| Display | 32sp | 600 | 1.2 | Net worth figure, hero financial amounts |
| Headline | 22sp | 600 | 1.3 | Screen titles, bottom sheet titles |
| Title | 17sp | 600 | 1.4 | Card titles, section headers |
| Body | 15sp | 400 | 1.6 | Standard readable content, transaction notes |
| Label | 13sp | 500 | 1.4 | Buttons, form labels, chips, tags |
| Caption | 11sp | 400 | 1.4 | Timestamps, secondary metadata, currency codes |

---

### Financial Number Formatting

Financial values follow consistent formatting rules across all screens and platforms.

**Currency amounts:**
- Default format: `$#,##0.00` (e.g. `$1,234.56`)
- Amounts ≥ $10,000 always display with full comma-separated digits in detail views (e.g. `$12,500.00`)
- Dashboard summary blocks may abbreviate amounts ≥ $1,000,000 as `$1.2M`
- Never abbreviate amounts in transaction lists, account detail, or budget detail views

**Negative amounts:**
- Always include a minus sign: `−$48.00`
- Never rely on color alone to indicate a negative value
- Use the typographic minus `−` (U+2212), not a hyphen

**Percentages:**
- Always show one decimal place: `68.4%`, `100.0%`
- Goal progress may exceed 100% if over-contributed — display the actual value (e.g. `112.3%`)

**Number alignment:**
- Amounts within lists and tables must right-align and use consistent decimal alignment
- Currency symbols align with the leftmost character of the amount column

---

## Spacing & Layout

### Spacing System

The design system uses an **8dp base grid**. All spacing values are multiples of 4dp (half the base), providing a consistent and predictable rhythm.

| Token | Value | Usage |
|---|---|---|
| `spacing.xxs` | 2dp | Micro-gaps: icon-to-label, currency symbol to amount |
| `spacing.xs` | 4dp | Tight internal padding, icon margins |
| `spacing.sm` | 8dp | Component internal spacing, list item gaps |
| `spacing.md` | 16dp | Standard content padding, card internal padding |
| `spacing.lg` | 24dp | Section separation, card gaps |
| `spacing.xl` | 32dp | Large section gaps, screen top padding |
| `spacing.xxl` | 48dp | Hero section spacing, bottom sheet top padding |

All layouts must align consistently to this system. Arbitrary spacing values are not permitted.

---

### Layout Philosophy

The interface should feel spacious but efficient, data-dense but breathable, and structured without rigidity.

The app should avoid cramped dashboards, excessively large empty areas, deeply nested card structures, and inconsistent padding.

---

### Card Design

Cards are the primary organizational container.

| Property | Value |
|---|---|
| Corner radius | `radius.lg` (16dp) |
| Shadow | Soft, low-elevation (`elevation.sm`) |
| Border | `color.border.default` at 0.5px, or none |
| Internal padding | `spacing.md` (16dp) |
| Gap between cards | `spacing.lg` (24dp) |

Cards should group related information clearly without excessive visual separation. Avoid nesting cards inside cards more than one level deep.

---

### Web Layout Guidance

The web dashboard should use responsive layouts with a maximum content width of 1280px. Content should not stretch to fill ultra-wide displays. Spacing hierarchy from mobile is preserved on web, with information density increased appropriately for larger viewports.

Responsive breakpoints are defined at Phase 3 kickoff.

---

## Iconography

### Icon Library

| Platform | Library | Style |
|---|---|---|
| Android | Material Symbols | Outlined |
| Web | Tabler Icons | Outlined |
| Flutter/KMP (Phase 4) | TBD — align with chosen platform | Outlined |

Outlined style is the default. Filled style is used exclusively for selected or active states (for example, the active bottom navigation item on Android).

---

### Icon Sizing

| Context | Size |
|---|---|
| Inline with text (body) | 20dp / 20px |
| Standalone action (button, FAB) | 24dp / 24px |
| Navigation bar items | 24dp / 24px |
| Small metadata / caption context | 16dp / 16px |

---

### Icon Usage Rules

- Icons must always be accompanied by a text label or accessible content description. Icon-only buttons require an `aria-label` (web) or `contentDescription` (Android).
- Icons must not be used as the sole indicator of financial meaning. Pair icons with labels for all semantic states.
- Custom icons are not permitted in Phase 1. All icons must come from the designated library.

---

## Components

This section defines behavioral and visual expectations for common UI patterns. Implementation-specific code is not included here — component-level specs are defined per feature.

---

### Lists

Lists are heavily used throughout the application for transactions, budgets, goals, and accounts.

Lists must:
- Prioritize scanability with consistent row heights
- Right-align and decimal-align all numeric data
- Use restrained dividers (`color.border.default` at 0.5px) or rely on spacing alone
- Avoid excessive visual noise — icons, labels, and amounts only

Transaction lists must remain readable with large datasets spanning multiple years.

---

### Progress Bars

Progress bars are used for budget spending status, goal contribution progress, and sync progress.

**Appearance:**
- Track height: 6dp
- Corner radius: fully rounded (pill shape)
- Track color: matches semantic state at reduced opacity (12%) or uses `color.surface.alt`
- Fill color: determined by spend level (see threshold table below)

**Budget spending thresholds:**

| Spend level | Bar fill color | Amount text color |
|---|---|---|
| < 85% of budget | `color.accent.primary` | `color.accent.primary` |
| 85–99% of budget | `color.semantic.warning` | `color.semantic.warning` |
| ≥ 100% (overspent) | `color.semantic.error` | `color.semantic.error` |

These thresholds apply to budget progress bars and remaining amount labels throughout the app. The same rule applies on the Dashboard budget status rows and the Budget List screen.

**Goal progress bars** always use `color.accent.primary` regardless of percentage — there is no "overspent" concept for goals.

---

### Charts

Charts support comprehension, not decoration. They are secondary to the core financial data displayed in lists and cards.

Charts must:
- Use the defined semantic and accent color palette only
- Minimize chart junk (gridlines, borders, tick marks should be subtle or absent)
- Favor readability over novelty — bar charts and line charts are preferred over complex visualizations
- Avoid gradients, drop shadows, and heavy fills
- Remain accessible in dark mode

Bar charts comparing two periods (e.g. planned vs. actual spending) use `color.accent.primary` for the primary series and `color.surface.alt` with a border for the secondary series.

---

### Empty States

Every list screen must define an empty state. The anatomy is: a lightweight illustration placeholder (or icon), a short headline, one sentence of subtext, and a primary CTA button.

Empty states should feel encouraging, not apologetic. They should point directly to the action that resolves the empty state.

See `specs/design/navigation.md` for the two-level empty state pattern (dependency not met vs. no data yet).

---

### Loading States

Loading states must be subtle and preserve layout stability. Skeleton screens are preferred over spinners for list content — they prevent layout jump when data loads.

Skeletons use `color.surface.alt` as the base and a shimmer animation in the same neutral family. Avoid full-screen loading states — load content progressively where possible.

---

### Sync Indicators

Sync status indicators must remain low-noise and non-intrusive. The sync system is background-oriented and must not interrupt primary workflows.

Visual emphasis levels by sync state:

| Sync state | Indicator style |
|---|---|
| Syncing (background) | Subtle spinner or no indicator |
| Last synced (idle) | Caption-style timestamp, secondary text color |
| Sync failed | Warning-colored icon + brief label |
| Conflict detected | Warning-colored banner or badge, requires user action |
| Prolonged offline | Passive offline badge, non-blocking |

Full sync UI specification is defined in `specs/features/sync/spec.md`.

---

## Motion & Animation

### Motion Philosophy

Animations support comprehension and continuity. Motion must never feel playful or distracting. The app should feel responsive, stable, predictable, and calm.

### Motion Guidelines

Recommended:
- Short transitions (150–250ms)
- Subtle fades for state changes
- Gentle slide transitions for screen navigation
- Minimal scale effects for confirmation feedback

Avoid:
- Bounce or spring physics
- Large parallax effects
- Long-duration transitions (> 400ms)
- Decorative motion unrelated to user intent
- Animations that block access to content

---

## Accessibility

Accessibility is a first-class requirement. Financial applications must remain usable across a wide range of users and visual conditions.

### Contrast

All text and critical UI elements must meet **WCAG AA** minimum contrast requirements:
- Normal text: 4.5:1 contrast ratio minimum
- Large text (≥ 18sp regular or ≥ 14sp bold): 3:1 minimum
- UI components and graphical elements: 3:1 minimum

### Touch Targets

All interactive elements must meet the minimum touch target size defined in `SPEC.md` (NFR-AC-01):
- Minimum: 48dp × 48dp on all mobile platforms
- Visual size may be smaller; the touch target area must meet the minimum

### Font Scaling

All typography must use `sp` units on Android to respect system font size scaling (NFR-AC-02). Layouts must remain functional and readable at up to 200% system font scale without truncation or overlap.

### Color Independence

Color alone must never communicate financial meaning. See the Color Independence Rule section above.

### Screen Reader Compatibility

All interactive elements require descriptive labels. Financial amounts must be announced with context — a screen reader should announce "Groceries budget, $364 spent of $500, 68% used" not just "$364".

---

## Platform Adaptation

### Android

Android uses **Material 3** as the foundational design system. Budget App customizes the Material 3 color scheme, typography, shape, and elevation tokens while preserving Android-native interaction expectations (ripple effects, back gesture, bottom sheets, etc.).

The Material 3 `ColorScheme` maps to Budget App tokens as follows:

| Material 3 Role | Budget App Token |
|---|---|
| `primary` | `color.accent.primary` |
| `onPrimary` | `color.accent.on` |
| `background` | `color.background.primary` |
| `surface` | `color.surface.card` |
| `surfaceVariant` | `color.surface.alt` |
| `onBackground` | `color.text.primary` |
| `onSurface` | `color.text.primary` |
| `onSurfaceVariant` | `color.text.secondary` |
| `outline` | `color.border.default` |
| `error` | `color.semantic.error` |

---

### Web

The web platform adapts the same design language to larger layouts using the same token values expressed as CSS custom properties. TanStack Query and shadcn/ui are used as the component foundation; shadcn/ui components are customized to match Budget App tokens via Tailwind configuration.

The web UI must preserve visual consistency with mobile, increase information density appropriately for larger viewports, and maintain consistent semantic meaning across all states.

---

### Flutter / KMP (Phase 4)

Future Flutter/KMP implementations must preserve the typography hierarchy, color system, component behavior, motion philosophy, spacing rules, and accessibility standards defined here. Platform differences are acceptable as long as the overall product identity remains consistent.

---

## Design Tokens

The token naming strategy below is the canonical reference for implementation. Platform-specific implementations (Compose `MaterialTheme`, CSS custom properties, Flutter `ThemeData`) must map to these token names.

### Color Tokens

```
color.background.primary
color.surface.card
color.surface.alt
color.border.default
color.text.primary
color.text.secondary
color.accent.primary
color.accent.on
color.semantic.success
color.semantic.error
color.semantic.warning
color.semantic.info
```

### Spacing Tokens

```
spacing.xxs   — 2dp
spacing.xs    — 4dp
spacing.sm    — 8dp
spacing.md    — 16dp
spacing.lg    — 24dp
spacing.xl    — 32dp
spacing.xxl   — 48dp
```

### Radius Tokens

```
radius.sm     — 6dp   (chips, tags, badges)
radius.md     — 10dp  (input fields, small cards)
radius.lg     — 16dp  (primary cards, list containers)
radius.xl     — 24dp  (bottom sheets, dialogs)
```

### Elevation Tokens

```
elevation.none   — 0dp
elevation.sm     — 1dp  (cards, subtle lift)
elevation.md     — 4dp  (bottom sheets, menus)
elevation.lg     — 8dp  (dialogs, modals)
```

---

## Future Considerations

The design system is expected to evolve alongside the product.

Potential future additions:
- Dedicated component specification document
- Chart visualization standards and color palette for multi-series charts
- Illustration or icon spot-art guidelines for empty states
- Motion timing token definitions
- Responsive web breakpoint definitions (Phase 3 kickoff)
- Advanced accessibility auditing and WCAG AAA targets
- Multi-brand or white-label theming support

Future additions must remain consistent with the core philosophy defined in this document.

---

## Guiding Principle

Capital should help users understand their finances clearly and confidently.

The interface should reduce mental friction, support long-term usage, and communicate financial information in a calm, structured, and trustworthy way.

The product should feel dependable before it feels impressive.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-27 | Danielle Mariani | Initial draft — design philosophy, principles, color system, typography scale (roles only), spacing, component patterns, motion, accessibility, platform adaptation, and design tokens. |
| 0.2.0 | 2026-05-27 | Danielle Mariani | Revise primary accent: `#0F766E` → `#0D7377` (light), `#2DD4BF` → `#2EB5AC` (dark) — same teal family, consistent brand identity across modes. Fix Surface Alt dark: `#273244` → `#1C2434` (neutral, no blue cast). Add `color.accent.on` token to both themes. Add Dark Mode Strategy section. Add token column to color tables. Add Iconography section (Material Symbols / Tabler, sizing, usage rules). Expand Typography Scale with concrete sizes, weights, and line heights. Add Android font bundling note and web variable font note. Add Financial Number Formatting section (currency, negative values, percentages, alignment). Add `spacing.xxs` (2dp) micro-spacing token. Make Radius tokens concrete with values and usage. Add Progress Bar spending threshold table (accent / warning / error at 85% and 100%). Add Color Independence Rule table with secondary signals. Add Material 3 color role mapping table. Add Sync Indicators state table. Add changelog. |
| 0.3.0 | 2026-06-08 | Danielle Mariani | Add Display / Brand Font section: Borel, Android registration steps (`res/font/borel_regular.ttf`, `borel.xml`, `BorelFontFamily` val in `core/ui/theme/Type.kt`), usage constraints (Dashboard AppHeader only, 26sp, minimum 20sp), web note. Rename document title and all "Budget App" references to "Capital". |
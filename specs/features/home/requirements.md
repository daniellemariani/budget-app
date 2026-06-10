# Home — Requirements

**Version:** 0.4.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-06-05
**Last Updated:** 2026-06-08

---

## Introduction

Home is the persistent shell of the app. It is the first screen a user reaches after completing onboarding, and it remains the structural container for all post-onboarding experiences across the app's lifetime. Its responsibilities are narrow but foundational: display the app header, render the bottom navigation bar, host each feature tab's root screen, and manage system back navigation consistently.

Home does not contain business logic. It does not own the content of any tab — each tab's content is defined and owned by its respective feature spec. Home defines the container, the navigation rules, and the shared chrome that appears on every tab root screen within `MainActivity`.

This spec covers Phase 1 (Android, offline). Web navigation structure (Phase 3) is covered in `navigation.md`.

---

## Scope Boundaries

### In Scope

- `MainActivity` shell (activity entry point after onboarding)
- App header: context-sensitive label and optional settings gear icon
- Bottom navigation bar: 5 destinations with icon only (no labels)
- Placeholder screens for each tab (rendered until each feature is implemented)
- Back navigation behavior at all levels within `MainActivity`
- Light and dark theme support for all shell UI elements

### Out of Scope

- Content of any individual tab (owned by each feature spec)
- Settings screen content and behavior (owned by `specs/features/settings/`)
- Workspace Management screen (owned by Dashboard feature spec)
- Sub-screen headers and back navigation chrome (global pattern defined in `navigation.md`, owned per feature spec)
- Internal navigation within each feature (owned by each feature spec)
- Web navigation shell (Phase 3)
- Authentication or session management (Phase 2)
- Borel font registration and FontFamily definition (defined in `specs/design/design.md`)

---

## Requirements

### Entry Point

**RQ-HM-01 — MainActivity launch condition**
`MainActivity` must be launched when `onboarding_completed = true` is present in SharedPreferences. This is the only entry condition in Phase 1. No authentication check is performed. See `navigation.md` — App Launch Logic for the full decision tree.

**RQ-HM-02 — Default tab on launch**
When `MainActivity` launches, the Dashboard tab must be the active tab. No other tab is selected by default.

---

### Header

**RQ-HM-03 — Header is present on all tab root screens**
The header must be present on every tab root screen within `MainActivity`. The header is part of the `MainActivity` shell and is not controlled by individual features at the tab root level. Sub-screens (e.g. Account Detail, Budget Detail, creation and edit forms) are displayed full-screen and replace the shell header with a feature-specific top bar — this pattern is defined in `navigation.md` and owned per feature spec.

**RQ-HM-04 — Header label: Dashboard tab**
When the Dashboard tab is active, the header must display the word "Capital" as a centered text label rendered in the Borel font. This is a `Text` composable — not a static image asset. The label carries no tap action. The Borel `FontFamily` is defined in `specs/design/design.md` and registered as a font resource in `res/font/`.

**RQ-HM-05 — Header label: non-Dashboard tabs**
When any tab other than Dashboard is active (Accounts, Transactions, Budgets, Goals), the header must display the tab's name as a centered text label rendered in the Inter font. The label text matches the tab name exactly: "Accounts", "Transactions", "Budgets", or "Goals". The label carries no tap action.

**RQ-HM-06 — Settings entry point**
When the Dashboard tab is active, the right side of the header must display a gear icon. Tapping the gear icon navigates to the Settings screen. The gear icon must not be visible on any non-Dashboard tab. The Settings screen content and behavior are defined in `specs/features/settings/`.

**RQ-HM-07 — Header scroll behavior**
The header must auto-hide when the user scrolls down and reappear when the user scrolls up, on all five tabs. This mirrors the scroll behavior of the X (formerly Twitter) timeline header. The header is not a fixed element — it participates in the scroll interaction via `enterAlwaysScrollBehavior`. This behavior applies to all tab root screens. Full functional verification of this behavior on each tab is deferred to the point at which each feature's real list content is implemented; it must be fully functional across all tabs by the end of Phase 1.

**RQ-HM-08 — Header theme adaptation**
The header background, label text, and gear icon must adapt to the active system theme (light/dark) using the design system's surface and on-surface tokens. No hardcoded colors.

---

### Bottom Navigation Bar

**RQ-HM-09 — Five destinations**
The bottom navigation bar must contain exactly five destinations in the following order, left to right:

| Position | Tab | Icon style |
|---|---|---|
| 1 | Dashboard | Home icon |
| 2 | Accounts | Wallet icon |
| 3 | Transactions | Swap / arrows icon |
| 4 | Budgets | Chart / plan icon |
| 5 | Goals | Target / flag icon |

Icon selection follows the design system's iconography tokens. Final icon assets are confirmed at implementation time.

**RQ-HM-10 — Icon-only navigation**
The bottom navigation bar must display icons only — no text labels below the icons. This applies to all five destinations in all states (active and inactive).

**RQ-HM-11 — Active tab indicator**
The active tab icon must be rendered in its fully filled variant. Non-active tab icons must be rendered in their outline variant (fill color transparent, border only). This distinction visually communicates the selected destination at a glance.

**RQ-HM-12 — Icon colors**
Icon color must reflect the active/inactive state using the design system's color tokens. The bottom nav bar background must use the design system's surface token. No hardcoded colors.

**RQ-HM-13 — Theme adaptation**
The bottom navigation bar must adapt to the active system theme (light/dark). Background color, active icon color, and inactive icon color must all use design system tokens.

**RQ-HM-14 — Active tab re-selection scrolls to top**
Tapping a destination that is already the active tab must scroll the tab's current list to the top, but only if the list is not already at the top. If the list is already scrolled to the top, no action is triggered. Applies only when the user is at the root screen of the tab — not when a sub-screen is active.

---

### Tab Placeholder Screens

**RQ-HM-15 — Placeholder per tab**
Until each feature is implemented, each tab must display a minimal placeholder screen. The placeholder must show the tab name centered on the screen. The placeholder must not crash, must respect the shell header and bottom nav chrome, and must be navigable via the bottom nav bar. All five tab placeholders are served by a single reusable composable that accepts the tab label as a parameter.

**RQ-HM-16 — Placeholder replacement**
Each placeholder is replaced by its full feature implementation when that feature's spec is complete and the feature is built. Once all features are implemented the placeholder composable is deleted entirely — it is not kept alongside real implementations.

---

### Back Navigation

**RQ-HM-17 — Back behavior at Dashboard tab root**
When the user is at the Dashboard tab root screen and presses the system back button or performs the back gesture, the app must exit to the Android home screen. The user is not prompted before exiting.

**RQ-HM-18 — Back behavior at non-Dashboard tab root**
When the user is at the root screen of any tab other than Dashboard (Accounts list, Transactions screen, Budget list, Goals list) and presses the system back button or performs the back gesture, the app must navigate to the Dashboard tab. The app does not exit.

**RQ-HM-19 — Back behavior within a tab's sub-screen stack**
When the user has navigated within a tab to a sub-screen (e.g. Account Detail, Budget Detail, Transaction edit form), the system back button or back gesture must follow the natural screen stack: each back press pops one screen until the tab's root screen is reached. Once at the root screen, RQ-HM-17 or RQ-HM-18 applies.

**RQ-HM-20 — Back behavior is consistent across all tabs**
The back navigation rules defined in RQ-HM-17 through RQ-HM-19 apply uniformly across all five tabs. No tab has special back behavior beyond what is described here.

---

## Acceptance Criteria

**AC-HM-01 — Launch lands on Dashboard**
Given `onboarding_completed = true`,
When the app is launched,
Then `MainActivity` opens with the Dashboard tab active, the header displaying "Capital" in Borel font centered, the gear icon visible on the right, and the bottom nav bar showing the Dashboard icon in the filled/active state.

**AC-HM-02 — Tab switching**
Given the user is on any tab,
When the user taps a different tab in the bottom nav bar,
Then the newly tapped tab becomes active, its icon switches to the filled variant, the previously active tab's icon switches to the outline variant, and the tapped tab's root screen is displayed.

**AC-HM-03 — Header on Dashboard tab**
Given the Dashboard tab is active,
Then the header displays "Capital" centered in Borel font and the gear icon is visible on the right.

**AC-HM-04 — Header on non-Dashboard tabs**
Given any non-Dashboard tab is active (Accounts, Transactions, Budgets, or Goals),
Then the header displays the tab name centered in Inter font and no gear icon is visible.

**AC-HM-05 — Active tab icon is filled**
Given any tab is active,
Then its icon is displayed in the fully filled variant in the active color token.

**AC-HM-06 — Inactive tab icons are outline**
Given any tab is not active,
Then its icon is displayed in the outline variant with transparent fill in the inactive color token.

**AC-HM-07 — No tab labels in bottom nav**
Given any state of the app,
Then no text label is displayed below any icon in the bottom navigation bar.

**AC-HM-08 — Re-selecting active tab scrolls to top**
Given the user is at the root screen of the active tab,
When the user taps the same tab in the bottom nav bar,
Then if the list is scrolled down, it scrolls back to the top.
If the list is already at the top, no action is triggered.

**AC-HM-09 — Header hides on scroll down**
Given the user is on any tab root screen with scrollable content,
When the user scrolls down,
Then the header slides out of view.

**AC-HM-10 — Header reappears on scroll up**
Given the header is hidden (user has scrolled down),
When the user scrolls up,
Then the header slides back into view.

**AC-HM-11 — Back on Dashboard tab exits app**
Given the user is at the Dashboard tab root screen,
When the user presses the system back button or performs the back gesture,
Then the app exits to the Android home screen.

**AC-HM-12 — Back on non-Dashboard tab goes to Dashboard**
Given the user is at the root screen of the Accounts, Transactions, Budgets, or Goals tab,
When the user presses the system back button or performs the back gesture,
Then the Dashboard tab becomes active and the Dashboard root screen is displayed. The app does not exit.

**AC-HM-13 — Back within sub-screen stack**
Given the user navigated from Accounts list → Account Detail,
When the user presses the system back button or performs the back gesture,
Then the app navigates back to Account List.
When the user presses back again from Account List,
Then the Dashboard tab becomes active (per AC-HM-12).

**AC-HM-14 — Gear icon navigates to Settings**
Given the user is on the Dashboard tab root screen,
When the user taps the gear icon in the header,
Then the app navigates to the Settings screen.

**AC-HM-15 — Light theme renders correctly**
Given the device is set to light theme,
Then the header background, label text, gear icon, bottom nav background, active icon, and inactive icon colors all use the design system light theme tokens with no hardcoded colors.

**AC-HM-16 — Dark theme renders correctly**
Given the device is set to dark theme,
Then all shell elements adapt to the design system dark theme tokens. No element is invisible or illegible against its background.

---

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| `onboarding_completed` SharedPreferences flag | Internal | Set by Onboarding. Read by `MainActivity` to determine whether to launch |
| Borel font resource | Design | Registered in `res/font/` and defined as a `FontFamily` in `specs/design/design.md`. Required for the "Capital" header label on the Dashboard tab |
| Gear icon asset | Design | From the design system's iconography tokens |
| Bottom nav icon set | Design | Filled and outline variants for all 5 destinations |
| Design system tokens | Design | Surface, on-surface, primary, and icon color tokens for light and dark themes |
| `MainNavHost` | Internal | Jetpack Compose Navigation host defined in `AppNavGraph.kt`. Each feature registers its own nested nav graph |
| Feature root Composables | Internal | Each feature exposes one public root Composable as its tab entry point. Defined per feature spec |
| Settings screen | Internal | Destination owned by `specs/features/settings/`. The gear icon navigates to it; content is not defined here |

---

## Open Questions

| ID | Question | Status |
|---|---|---|
| OQ-HM-01 | Should full-screen creation and edit forms suppress the bottom nav bar? | Resolved — sub-screens are full-screen; bottom nav is not present. Global pattern to be documented in `navigation.md`. |
| OQ-HM-02 | Who owns the gear icon CTA behavior — this spec or Settings spec? | Resolved — this spec owns the navigation action (gear icon → Settings). Settings spec owns the destination screen content. |
| OQ-HM-03 | Should the wordmark be tappable? | Resolved — no tap action. |
| OQ-HM-04 | Should the header label use a static image asset or a Text composable? | Resolved — Text composable for all tabs. Borel font for Dashboard ("Capital"), Inter for all other tabs (tab name). No image asset required for the header. |
| OQ-HM-05 | Should bottom nav tabs display labels? | Resolved — icon only, no labels. Labels are replaced by the context-sensitive header label. |

---

## Platform: Web (Phase 3)

Web navigation uses a persistent left sidebar in place of the bottom navigation bar. The five destinations (Dashboard, Accounts, Transactions, Budgets, Goals) are present in both platforms. The header is replaced by a top bar with the app wordmark and user avatar / workspace switcher. Full web shell spec is defined at Phase 3 kickoff.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-06-05 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-06-05 | Danielle Mariani | AC-HM-05: scroll-to-top only triggers if list is not already at top. RQ-HM-05 and AC-HM-09: gear icon has no action in Phase 1; Settings wired up when Settings spec is written. RQ-HM-03: clarify header is present on tab root screens only; sub-screens are full-screen with feature-owned top bar — pattern delegated to navigation.md. Removed former AC-HM-10 (sub-screen header visibility). Renumbered AC-HM-10 and AC-HM-11 (formerly AC-HM-11 and AC-HM-12). OQ-HM-01, OQ-HM-02, OQ-HM-03 all resolved. |
| 0.3.0 | 2026-06-05 | Danielle Mariani | RQ-HM-05 and AC-HM-09: gear icon navigates to Settings in Phase 1. Settings content and behavior delegated to specs/features/settings/. Settings added to Dependencies table. RQ-HM-12: note that all five tab placeholders are served by a single reusable composable. RQ-HM-13: placeholder composable deleted entirely once all features are implemented. Scope Boundaries: Settings screen content explicitly called out as out of scope. |
| 0.4.0 | 2026-06-08 | Danielle Mariani | Header redesign: replace static wordmark image asset with context-sensitive Text composable. Dashboard tab displays "Capital" in Borel font (centered); non-Dashboard tabs display tab name in Inter (centered). Gear icon visible on Dashboard only. Add RQ-HM-07 (header scroll behavior — enterAlwaysScrollBehavior, auto-hide on scroll down, reappear on scroll up, all tabs). Bottom nav redesign: icon-only, no tab labels (RQ-HM-10). Renumbered requirements and acceptance criteria throughout. Add OQ-HM-04 and OQ-HM-05 (resolved). Update Dependencies table: remove wordmark asset, add Borel font resource. Update Scope Boundaries: add Borel font deferral note. |
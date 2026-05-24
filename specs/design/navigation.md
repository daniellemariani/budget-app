# Navigation Spec — Budget App

**Version:** 0.1.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-05-23
**Last Updated:** 2026-05-23

---

## Overview

This document defines the navigation structure, screen inventory, and flow for the Budget App across platforms. It serves as the reference for feature spec authors — every screen referenced in a feature spec should trace back to this document.

Phase 1 covers the Android app. Phase 3 covers the web dashboard. iOS and cross-platform navigation are deferred to Phase 4.

This document is intentionally lightweight — it captures structure and flow, not visual design or component-level UI detail. Those are defined per feature spec.

---

## Related Documents

| Document | Purpose |
|---|---|
| SPEC.md | Feature index and global business rules |
| ARCHITECTURE.md | Android stack and navigation implementation (Jetpack Compose Navigation) |
| specs/features/onboarding/spec.md | Onboarding feature detail |
| specs/features/dashboard/spec.md | Dashboard feature detail |
| specs/features/transactions/spec.md | Transactions feature detail |
| specs/features/transfers/spec.md | Transfers feature detail |
| specs/features/accounts/spec.md | Accounts feature detail |
| specs/features/budgets/spec.md | Budgets feature detail |
| specs/features/goals/spec.md | Goals feature detail |

---

## Global Navigation Patterns

These patterns apply across all screens unless a feature spec explicitly overrides them.

### Detail Screen vs Direct Edit

Entity types follow one of two interaction patterns on row tap:

| Entity | Tap behavior | Edit access |
|---|---|---|
| Account | Opens Account Detail screen | Pencil icon on detail screen |
| Budget | Opens Budget Detail screen | Pencil icon on detail screen |
| Goal | Opens Goal Detail screen | Pencil icon on detail screen |
| Transaction | Opens Transaction Edit form directly | — |
| Transfer | Opens Transfer Edit form directly | — |
| Goal Contribution | Opens Goal Contribution Edit form directly | — |

**Rationale:** Accounts, Budgets, and Goals have rich contextual detail (charts, history, computed totals) that warrants a dedicated detail screen. Transactions, Transfers, and Goal Contributions are simple entry forms — opening the edit form directly is faster and more appropriate for frequent edits.

### FAB Patterns

Two FAB variants are used, consistent with Material Design 3:

**Standard FAB** — single primary action. Used on Budget list and Goal list screens.

**Speed Dial FAB** — expands on tap to reveal 2–5 labeled sub-actions. Used on Transactions screen (Add Transaction, Add Transfer) and Accounts screen (Add Cash, Add Savings, Add Checking, Add Credit Card). Speed Dial is only used when there are multiple distinct creation actions on the same screen.

### Soft Delete Confirmation

All destructive actions (delete transaction, delete account, etc.) require a confirmation dialog before executing. No silent deletes anywhere in the app.

### Month Navigator

A reusable component used on Transactions and Budget list screens. Displays the selected month and year (e.g. "May 2026") with a left arrow (previous month) and a right arrow (next month). The right arrow is disabled when the selected month is the current month — the user cannot navigate into the future.

TBD: Tapping the month label may open a month picker for fast navigation to any past month. Deferred — mark as open for Phase 1 implementation decision.

---

## Android Navigation

### App Structure

The Android app uses two distinct activity contexts:

```
OnboardingActivity         — shown only on first launch; no bottom nav, no header gear icon
    └── OnboardingNavHost  — manages onboarding screen flow

MainActivity               — all post-onboarding screens
    ├── Header             — app name ([App Name]) left, gear icon (Settings) right
    ├── BottomNavigationBar — 5 destinations, each with icon + label
    └── MainNavHost        — manages all feature screen flows
```

**OnboardingActivity** is a self-contained navigation scope. It is dismissed permanently after onboarding is completed or after the Name + Account Setup is complete. It does not share the navigation back stack with MainActivity.

**MainActivity** is the persistent shell for all post-onboarding screens. The header and bottom nav bar are always visible in MainActivity, regardless of which feature screen is active. Exception: screens that open full-screen forms (e.g. Add Transaction, Add Account) may suppress the bottom nav — TBD per feature spec.

### Header

Present on all MainActivity screens. Not present during onboarding.

| Element | Position | Behavior |
|---|---|---|
| App name ([App Name]) | Left | Static label — no action |
| Gear icon | Right | Navigates to Settings screen |

### Bottom Navigation Bar

Five destinations. Each has an icon and a text label.

| Position | Label | Icon | Destination |
|---|---|---|---|
| 1 | Dashboard | Home icon | Dashboard screen |
| 2 | Accounts | Wallet icon | Accounts list screen |
| 3 | Transactions | Swap/arrows icon | Transactions screen |
| 4 | Budgets | Chart/plan icon | Budget list screen |
| 5 | Goals | Target/flag icon | Goals list screen |

Selecting a bottom nav item that is already active scrolls the current list to the top (standard Material 3 behavior).

---

## Screen Inventory — Android

### Onboarding Flow (OnboardingActivity)

```
App Launch (first time)
    │
    └──► Feature Slides (3 slides, skippable)
              │
              ├── Skip ──────────────────────────────────┐
              │                                           │
              └── Next / Complete ──────────────────────►│
                                                          │
                                                    Set Your Name screen
                                                          │
                                                          └──► Account Setup screen
                                                                    │
                                                                    └──► MainActivity (Dashboard)
```

#### Feature Slides screen

3 swipeable slides. A "Skip" button is visible on every slide. A "Get Started" button appears on the last slide.

| Slide | Title | Key message |
|---|---|---|
| 1 | Know where your money goes | Track every expense and income across all your accounts. See your full financial picture at a glance. |
| 2 | Plan with intention | Set monthly spending budgets by category. Save toward the things that matter most. |
| 3 | All your accounts, one place. Works offline, always. | Add checking, savings, credit cards, and cash. Your data stays on your device — no account required. |

Skipping goes directly to the Set Your Name screen. Feature slides are never shown again after this first launch.

#### Set Your Name screen

Single text input for Display Name. Stored in SharedPreferences. Not synced to any server in Phase 1.

Privacy copy: *"Your display name is stored only on this device. No account required."*

Continuing goes to Account Setup.

#### Account Setup screen

Prompts the user to add at least one account before proceeding. Uses the same account creation form as the Accounts feature. A "Skip for now" option is available but discouraged via copy (e.g. *"Add an account to get the most out of [App Name]"*).

Completing or skipping goes to MainActivity, landing on the Dashboard.

---

### Dashboard (Home)

Entry point: Bottom nav — Dashboard.

**Screen: Dashboard**

| Element | Description |
|---|---|
| Greeting | Contextual welcome message using Display Name and device time (e.g. "Good morning, Dani", "Good evening, Dani"). |
| Net worth block | Total assets, total liabilities, net worth. |
| Current vs previous month | Income and expense totals for this month vs last month. |
| Budget status | All budgets for the current period, sorted by most overspent first. Each shows category, planned, spent, remaining. |
| Top spending categories | Top 5 categories by spend this month. |
| Goal progress | All active goals with progress percentage and target date. |
| Recent transactions | Last 10 transactions across all accounts. |

No FAB on Dashboard. No month navigator — Dashboard always shows the current period.

Tapping a budget row → Budget Detail screen.
Tapping a goal row → Goal Detail screen.
Tapping a transaction row → Transaction Edit form.

---

### Accounts

Entry point: Bottom nav — Accounts.

```
Accounts List screen
    │
    ├──► [Speed Dial FAB] ──► Add Cash / Add Savings / Add Checking / Add Credit Card
    │                               └──► Account Creation form ──► back to Accounts List
    │
    └──► [Tap account row] ──► Account Detail screen
                                    │
                                    ├──► [Pencil icon] ──► Account Edit form ──► back to Account Detail
                                    │
                                    └──► [Tap transaction row] ──► Transaction Edit form
```

#### Accounts List screen

Accounts displayed sorted by type in this order: Cash → Savings → Checking → Credit Card.

Each account row shows: account name, account type, currency code, current balance.

Speed Dial FAB with four sub-actions: Add Cash, Add Savings, Add Checking, Add Credit Card. Each opens the Account Creation form pre-filled for that account type.

#### Account Detail screen

Shows account name, type, balance, and transaction history for that account (all time, newest first). Pencil icon in the top right opens the Account Edit form.

#### Account Creation / Edit form

Fields: name, type (pre-selected from Speed Dial), currency code, initial balance, credit limit (Credit Card only).

---

### Transactions

Entry point: Bottom nav — Transactions.

Alternative considered: "Activity." Settled on "Transactions" for scannability — users instinctively look for this label when logging an expense. The Speed Dial FAB surfaces the Transaction/Transfer distinction at point of creation.

```
Transactions screen
    │
    ├──► [Month navigator] ──► previous / next month
    │
    ├──► [Type filter chips] ──► All | Income | Expense | Transfer
    │
    ├──► [Speed Dial FAB]
    │         ├──► Add Transaction ──► Transaction Creation form ──► back to Transactions
    │         └──► Add Transfer ──► Transfer Creation form ──► back to Transactions
    │
    └──► [Tap transaction/transfer row] ──► Transaction Edit form / Transfer Edit form
```

#### Transactions screen

Month navigator at the top (see Global Patterns). Type filter chips below the month navigator: All | Income | Expense | Transfer. Default: All.

List is mixed — transactions and transfers interleaved, sorted by date descending within the selected month.

Speed Dial FAB with two sub-actions: Add Transaction, Add Transfer.

TBD: Tapping the month label may open a month picker for fast navigation to a specific past month. Decision deferred to Phase 1 implementation.

#### Transaction Creation / Edit form

Fields: account, category, merchant (optional), type (Income/Expense), amount, date, notes.

#### Transfer Creation / Edit form

Fields: from account, to account, amount, date, notes.

---

### Budgets

Entry point: Bottom nav — Budgets.

```
Budget List screen
    │
    ├──► [Month navigator] ──► previous / next month
    │
    ├──► [Standard FAB] ──► Budget Creation form ──► back to Budget List
    │
    └──► [Tap budget row] ──► Budget Detail screen
                                    │
                                    ├──► [Pencil icon] ──► Budget Edit form ──► back to Budget Detail
                                    │
                                    └──► [Tap transaction row] ──► Transaction Edit form
```

#### Budget List screen

Month navigator at the top. Lists all budgets for the selected month, sorted by remaining amount ascending (most overspent first). Each row shows: category icon, category name, planned amount, spent amount, remaining amount, progress bar.

Standard FAB: Add Budget → Budget Creation form.

If no budgets exist for the selected month: empty state with prompt to create the first budget.

#### Budget Detail screen

Shows for the selected category:

- Current month: planned amount, spent amount, remaining amount (large display)
- Bar chart: planned vs spent for the last 6 months
- Transaction list: all transactions in this category for the selected month

Pencil icon opens Budget Edit form (editable fields: amount, carry_forward).

#### Budget Creation / Edit form

Fields: category (creation only — immutable after creation), amount, carry forward toggle.

---

### Goals

Entry point: Bottom nav — Goals.

```
Goals List screen
    │
    ├──► [Standard FAB] ──► Goal Creation form ──► back to Goals List
    │
    └──► [Tap goal row] ──► Goal Detail screen
                                    │
                                    ├──► [Pencil icon] ──► Goal Edit form ──► back to Goal Detail
                                    │
                                    └──► [Contribute button] ──► Goal Contribution form ──► back to Goal Detail
```

#### Goals List screen

Lists all active goals. Each row shows: goal name, target amount, contributed amount, progress bar, progress percentage, target date (if set).

Standard FAB: Add Goal → Goal Creation form.

If no goals exist: empty state with prompt to create the first goal.

#### Goal Detail screen

Shows for the selected goal:

- Goal name, target amount, contributed amount, progress percentage (large display)
- Bar chart: contributions over time (monthly, last 6 months)
- Target date and days remaining (if target date is set)
- Contribution history list (all contributions, newest first)

Pencil icon opens Goal Edit form (editable fields: name, target amount, target date, notes).

Contribute button (prominent, below progress display) opens the Goal Contribution form.

Tapping a contribution row in the history list opens the Goal Contribution Edit form.

#### Goal Creation / Edit form

Fields: name, target amount, currency code (creation only), target date (optional), notes.

#### Goal Contribution Creation / Edit form

Fields: amount, date, notes.

---

### Settings

Entry point: Gear icon in header (top right). Available from any MainActivity screen.

```
Settings screen
    │
    ├──► Edit Display Name ──► inline edit or modal ──► back to Settings
    ├──► About ──► About screen (app name, version, brief description)
    ├──► Version ──► displayed inline (not a navigable item)
    └──► Clear Data ──► Confirmation dialog ──► clears Room DB + SharedPreferences ──► OnboardingActivity
```

#### Settings screen

Simple list screen. No FAB.

| Item | Type | Behavior |
|---|---|---|
| Display Name | Editable field | Inline edit or modal. Updates SharedPreferences. |
| About | Navigable row | Opens About screen. |
| Version | Static label | Shows current app version (e.g. 1.0.0). Not tappable. |
| Clear Data | Destructive action | Shows confirmation dialog: "This will permanently delete all your data. This cannot be undone." Confirm → wipes Room DB and SharedPreferences → launches OnboardingActivity. |

**Phase 2 note:** Display Name editing in Settings is replaced by full user account management (name, email, avatar) when Supabase Auth is introduced.

---

## Web Navigation (Phase 3)

The web dashboard connects to the Phase 2 backend. There is no offline mode on web — all data comes from the API. The web navigation mirrors the Android destination structure but uses patterns appropriate for a desktop/tablet browser.

### Layout

```
┌─────────────────────────────────────────────┐
│  Sidebar  │  Main Content Area              │
│           │                                 │
│ [App Name]│                                 │
│           │                                 │
│ Dashboard │                                 │
│ Accounts  │                                 │
│ Transact. │                                 │
│ Budgets   │                                 │
│ Goals     │                                 │
│           │                                 │
│ [Gear]    │                                 │
│ Settings  │                                 │
└─────────────────────────────────────────────┘
```

### Sidebar

Left-side persistent navigation. Two states:

- **Expanded** — shows icon + label for each destination. Default on wide viewports.
- **Collapsed** — shows icon only. Default on narrower viewports or user preference. Tooltips on hover reveal labels.

Sidebar collapse/expand is toggled by the user (hamburger/chevron button at the top of the sidebar).

Settings and gear icon live at the bottom of the sidebar (below the five main destinations), consistent with common web app patterns (Linear, Notion, etc.).

No onboarding flow on web in Phase 3 — users authenticate via Supabase and their workspace data is loaded from the backend. First-time web setup is handled by the existing Android onboarding or a lightweight web-specific onboarding (TBD at Phase 3 kickoff).

### Destinations

Same five destinations as Android:

| Label | Description |
|---|---|
| Dashboard | Workspace summary — powered by GET /api/v1/workspaces/{workspace_id}/summary |
| Accounts | Account list and detail |
| Transactions | Money activity — transactions and transfers, with type filters and date range filters |
| Budgets | Budget list and detail with spending charts |
| Goals | Goals list and detail with contribution history |

### Web-specific behaviors

- No FABs — web uses standard buttons and form modals or dedicated form pages
- No month navigator component — web uses date range filters and a period preset selector (this month, last month, YTD, etc.)
- No Speed Dial — account and transaction creation uses button menus or split buttons
- Sidebar replaces bottom nav — all navigation is sidebar-driven
- Responsive breakpoints and exact layout details defined in Phase 3 feature specs

---

## Open Questions

- **Transactions month label — month picker:** Tapping the month label to open a month picker for fast navigation is noted as TBD. Decision at Phase 1 implementation.
- **Full-screen forms — bottom nav visibility:** Should the bottom nav be suppressed when a creation/edit form is open full-screen? Common Android pattern is to suppress it. Decision per feature spec.
- **Accounts sort order:** Current spec uses Cash → Savings → Checking → Credit Card. Alternative: Checking → Savings → Cash → Credit Card (most active first). Decision at Phase 1 implementation — can be user-configurable in a future phase.
- **Account Detail — transaction history scope:** All-time or limited to a rolling window (e.g. last 12 months)? Decision at Phase 1 implementation.
- **Web onboarding:** First-time web user experience is TBD. Decision at Phase 3 kickoff.
- **App name:** Placeholder `[App Name]` used throughout. To be decided before Phase 1 build.
- **Goal Contribution — FAB vs button:** Goal Detail uses a dedicated Contribute button rather than a FAB. If a FAB is preferred for consistency, revisit at Phase 1 implementation.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-23 | Danielle Mariani | Initial draft. Covers Android onboarding flow, MainActivity shell, all five bottom nav destinations, Settings, and Web Phase 3 sidebar structure. |
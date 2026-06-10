# Onboarding — Requirements

**Version:** 0.5.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-05-29
**Last Updated:** 2026-06-08

---

## Introduction

Onboarding is the first experience a user has with the app. It runs on first launch and is never shown again unless the user clears all app data via Settings. Its purpose is threefold: introduce the app's key value propositions through illustrated feature slides, collect a display name used throughout the app for personalization, and guide the user to set up their first financial account(s) before entering the main experience.

The onboarding flow is self-contained within `OnboardingActivity` and shares no back stack with `MainActivity`. Once completed, the user lands on the Dashboard and the onboarding flow is permanently suppressed.

This spec covers Phase 1 (Android, offline). Web onboarding (Phase 3) is covered in the Platform: Web section.

---

## Scope Boundaries

### In Scope

- Feature Slides screen (3 slides, swipeable, skippable)
- Set Your Name screen (display name input, stored in SharedPreferences)
- Add an Account screen (account creation form, skippable)
- Background initialization tasks on first launch (Workspace creation, default Category seeding)
- SharedPreferences flag management (`onboarding_completed`)
- Back navigation behavior across all onboarding screens
- Android Phase 1 implementation only

### Out of Scope

- User authentication or account registration (Phase 2)
- Currency picker during onboarding (Phase 2 — base currency defaults to USD in Phase 1)
- Cloud sync of display name or any onboarding data (Phase 2)
- Multi-workspace setup during onboarding (Phase 4)
- Web onboarding registration flow (Phase 3 — see Platform: Web section)
- Re-onboarding flows beyond Clear Data reset
- In-app language selection
- Onboarding analytics or event tracking

---

## Requirements

### Feature Slides

**RQ-ON-01 — Display Feature Slides pager**
The app must display a full-screen swipeable pager with exactly 3 slides when `onboarding_completed` is `false` (or not set) in SharedPreferences. The pager uses a slide animation that follows the direction of the user's swipe gesture (swipe left advances, swipe right goes back). Transition between slides is animated.

**RQ-ON-02 — Feature Slides visual layout**
Each slide must follow this layout from top to bottom:
- The word "Capital" rendered as a centered `Text` composable using `BorelFontFamily` at the top of the screen. This is not an image asset — it uses the same font and implementation as the Dashboard tab header in `MainActivity`.
- A large illustration centered in the screen, covering the majority of the display area, representing the slide's key message
- Slide title in white, bold
- Slide key message in white, regular weight
- Page indicator (3 dots) centered horizontally above the bottom action area
- Action button(s) at the bottom of the screen

The screen background is the app's accent color. All text and UI elements use white. The layout is full-screen with no system bars obscuring content (edge-to-edge).

**RQ-ON-03 — Feature Slides content**
The three slides must display the following content:

| Slide | Illustration | Title | Key Message |
|---|---|---|---|
| 1 | Illustration representing full financial picture | Know where your money goes | Track every expense and income across all your accounts. See your full financial picture at a glance. |
| 2 | Illustration representing budget planning | Plan with intention | Set monthly spending budgets by category. Save toward the things that matter most. |
| 3 | Illustration representing offline, multi-account use | All your accounts, one place. Works offline, always. | Add checking, savings, credit cards, and cash. Your data stays on your device — no account required. |

Illustrations are placeholders in Phase 1 and will be replaced with final assets before public release.

**RQ-ON-04 — Page indicator**
A row of 3 dots must be displayed near the bottom of the screen, above the action buttons. The dot representing the current slide is a filled white circle. The dots representing other slides are dimmed/outline white circles. The indicator updates in sync with slide transitions.

**RQ-ON-05 — Skip button**
A "Skip" text button must be visible on slides 1 and 2 only. It is positioned in the bottom-right corner of the screen, within the action area, for ergonomic thumb reach. Tapping Skip navigates the user directly to the Set Your Name screen. The `onboarding_completed` SharedPreferences flag is NOT set when the user taps Skip.

**RQ-ON-06 — Get Started button**
On slide 3 only, the Skip button is hidden and replaced by a "Get Started" button. Get Started is a distinct element from Skip — it is a prominent filled/primary button centered at the bottom of the screen, carrying more visual weight than the Skip text button. Tapping Get Started navigates the user to the Set Your Name screen. The `onboarding_completed` SharedPreferences flag is NOT set at this point.

**RQ-ON-07 — Feature Slides suppression**
Once the user has navigated past the Feature Slides (via Skip or Get Started), the Feature Slides are never shown again on subsequent launches. This is controlled by the `onboarding_completed` SharedPreferences flag. Since this flag is set to true only after Set Your Name is completed, the slides are suppressed for any launch where `onboarding_completed = true`. No separate SharedPreferences flag is used for the slides.

**RQ-ON-08 — Feature Slides back navigation**
There is no explicit back button on the Feature Slides screen. On slide 1, the OS back gesture or back button exits the app entirely — it does not navigate anywhere within the onboarding flow. On slides 2 and 3, the OS back gesture or back button navigates to the previous slide within the pager.

---

### Set Your Name

**RQ-ON-09 — Display Set Your Name screen**
After the Feature Slides (via Skip or Get Started), the app must display a full-screen Set Your Name screen. This screen uses the app's default (light) background color and default text color, signaling a visual transition from the onboarding slides into the app experience. The word "Capital" is displayed centered horizontally at the top of the screen as a `Text` composable using `BorelFontFamily` — not an image asset.

**RQ-ON-10 — Display Name input field**
A single text input field must be displayed for the user to enter their Display Name. The field has a transparent background. The font size is considerably larger than the surrounding screen copy. The field accepts a maximum of 30 characters. Once the 30-character limit is reached, the field must not accept additional input. The minimum valid length is 2 characters.

**RQ-ON-11 — Continue button state**
A "Continue" button must be displayed at the bottom of the screen. The button is disabled and visually dimmed when the Display Name field is empty or contains fewer than 2 characters. The button becomes enabled only when the field contains between 2 and 30 characters (inclusive). If the user clears the field after entering a valid name, the button returns to the disabled/dimmed state immediately.

**RQ-ON-12 — Display Name persistence**
When the user taps Continue with a valid Display Name, leading and trailing whitespace is trimmed and the result is stored in SharedPreferences under the key `display_name`. The `onboarding_completed` SharedPreferences flag is set to `true` under the key `onboarding_completed` at this point. These two writes happen atomically before navigation proceeds.

**RQ-ON-13 — Navigation to Account Setup**
After a successful Continue action (valid name stored, flag set), the app navigates to the Add an Account screen.

**RQ-ON-14 — Privacy copy**
The following privacy copy must be displayed below the Display Name input field:
*"Your display name is stored only on this device. No account required."*

**RQ-ON-15 — Set Your Name is not skippable**
There is no Skip button or any alternative to proceeding without entering a valid Display Name. The user must enter a name of 2–30 characters to continue.

**RQ-ON-16 — Set Your Name back navigation**
There is no explicit back button on this screen. The OS back gesture or back button exits the app entirely. It does not navigate back to the Feature Slides.

---

### Add an Account

**RQ-ON-17 — Display Add an Account screen**
After Set Your Name, the app must display a full-screen Add an Account screen. The screen uses the app's default (light) background and default text color. The word "Capital" is displayed at the top, centered horizontally, as a `Text` composable using `BorelFontFamily` — not an image asset.

**RQ-ON-18 — Encouraging copy**
The following copy must be displayed at the top of the form, below the logo:
*"Add an account to get the most out of [App Name]"*
This copy is always visible and is not removed after the user adds an account.

**RQ-ON-19 — Account creation form fields**
The account creation form must include the following fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| Name | Text input | Yes | Max 100 characters |
| Type | Dropdown | Yes | Options: Checking, Savings, Cash, Credit Card |
| Currency | Text (read-only) | N/A | Dimmed. Displays workspace base currency (USD in Phase 1). Not editable. |
| Initial Balance | Numeric input | Yes | Formatted as currency. Accepts decimals. Must be ≥ 0. |
| Credit Limit | Numeric input | Conditional | Only visible when Type = Credit Card. Must be > 0 when visible. |

**RQ-ON-20 — Credit Limit field visibility**
The Credit Limit field is only shown when the Account Type is set to Credit Card. Switching to any other type hides the field and clears its value. Switching back to Credit Card makes the field visible again (empty). This toggle behavior applies only to Credit Limit — no other field is conditionally shown.

**RQ-ON-21 — Amount field constraints**
Initial Balance and Credit Limit are stored as integers in cents (BR-DI-03). The maximum value for both fields is 999,999,999 cents ($9,999,999.99). Values exceeding this must not be accepted. Both fields must display amounts formatted according to the workspace currency (e.g. $1,234.56 for USD). Initial Balance accepts 0 as a valid value. Credit Limit must be greater than 0 when visible.

For Credit Card accounts, the Credit Limit must be greater than or equal to the Initial Balance (BR-AC-04). If the user enters a Credit Limit that is less than the Initial Balance, an inline error is shown below the Credit Limit field on blur: *"Credit limit must be greater than or equal to the initial balance."* The Save button remains disabled until this error is resolved.

**RQ-ON-22 — Save button state**
The Save button is disabled until all mandatory fields are completed and valid. Mandatory fields are: Name (non-empty, ≤ 100 characters), Type (selected), Initial Balance (valid numeric value ≥ 0). When Type = Credit Card, Credit Limit (> 0) is also mandatory and must be greater than or equal to Initial Balance (BR-AC-04). The button becomes enabled as soon as all required fields are valid and returns to disabled if any required field is cleared or becomes invalid.

**RQ-ON-23 — Save and continue dialog**
When the user taps the Save button with a valid form, the account is saved to the local Room database and a dialog is presented with two options:

- **"Add Another Account"** — dismisses the dialog, clears all form fields (Name, Initial Balance, Credit Limit if visible), resets Type to the default (Checking), and keeps the user on the Add an Account screen to enter another account. No screen transition occurs.
- **"Go to Home"** — dismisses the dialog and navigates the user to MainActivity (Dashboard). Onboarding is complete.

The dialog title is *"Account saved!"* and the body copy is *"Would you like to add another account or go to the app's Home?"*

**RQ-ON-24 — Skip button**
A "Skip for now" text button must always be visible on this screen, regardless of whether an account has been saved. Tapping it navigates the user directly to MainActivity (Dashboard). This is the only way to proceed without saving at least one account. The button is never disabled.

**RQ-ON-25 — Add an Account back navigation**
There is no explicit back button on this screen. The OS back gesture or back button exits the app entirely. It does not navigate back to Set Your Name.

**RQ-ON-26 — Account uniqueness**
Account names must be unique within the workspace (case-insensitive, excluding soft-deleted accounts). If the user attempts to save an account with a name that already exists in the current workspace, the save is blocked and an inline error is shown below the Name field: *"An account with this name already exists."* This validation applies when the user adds multiple accounts during onboarding.

---

### Background Initialization

**RQ-ON-27 — Workspace creation**
On first launch, before the Feature Slides are displayed, the app must create a default Workspace in the local Room database with the following values:

| Field | Value |
|---|---|
| id | UUID v4 (generated client-side) |
| name | "Personal" |
| base_currency | "USD" |

This Workspace is never exposed in the UI in Phase 1. All subsequent entities created during onboarding (accounts, categories) are associated with this Workspace via `workspace_id`.

**RQ-ON-28 — Default category seeding**
On first launch, after the Workspace is created, the app must seed the following default categories into the local Room database. All categories have `is_default = true`, `is_hidden = false`, and inherit the `workspace_id` from the default Workspace.

| Category | Icon | Notes / Examples |
|---|---|---|
| Groceries | 🛒 | Supermarkets, food stores, wholesale clubs (e.g. Costco, Trader Joe's) |
| Dining Out | 🍽️ | Restaurants, cafes, coffee shops, fast food, takeout (e.g. Starbucks, McDonald's) |
| Transport | 🚗 | Public transit, rideshare, parking, tolls (e.g. Uber, Metro, parking meters) |
| Fuel | ⛽ | Gas stations, EV charging (e.g. Shell, Chevron, Tesla Supercharger) |
| Utilities | 💡 | Electricity, water, gas, internet, phone bills |
| Housing | 🏠 | Rent, mortgage payments, HOA fees, home insurance |
| Health | 💊 | Doctor visits, prescriptions, medical bills, health insurance |
| Fitness & Sports | 🏋️ | Gym memberships, sports equipment, classes (e.g. Equinox, yoga studio) |
| Entertainment | 🎬 | Movies, theme parks, concerts, live sports, streaming (e.g. Disneyland, Dodgers game) |
| Shopping | 🛍️ | Clothing, electronics, home goods, one-time purchases (e.g. Amazon, Target, car down payment) |
| Education | 📚 | Tuition, books, courses, school supplies |
| Travel | ✈️ | Flights, hotels, vacation packages, travel insurance |
| Personal Care | 🪥 | Haircuts, cosmetics, toiletries, spa |
| Subscriptions | 📱 | Recurring digital services (e.g. Netflix, Spotify, iCloud, app subscriptions) |
| Gifts & Donations | 🎁 | Birthday gifts, holiday gifts, charitable donations |
| Pets | 🐾 | Pet food, vet visits, grooming, pet supplies |
| Taxes & Fees | 🧾 | Income tax, property tax, government fees, fines |
| Savings | 🏦 | Transfers to savings goals, emergency fund contributions |
| Income | 💰 | Salary, freelance payments, cashback, rewards, side income |
| Other | 📦 | Anything that doesn't fit another category |

**RQ-ON-29 — Initialization timing**
Workspace creation and category seeding must complete before the Feature Slides are shown. These operations run on a background thread and must not block the UI. If initialization fails (e.g. due to a database error), the app must display a generic error state with a retry option rather than proceeding with an uninitialized database.

**RQ-ON-30 — Initialization idempotency**
Workspace creation and category seeding must be idempotent. If the app is killed mid-initialization and relaunched, re-running the initialization must not create duplicate Workspaces or Categories.

---

## Non-Functional Requirements

**NFR-ON-01 — Onboarding screens must render in under 300ms** (consistent with NFR-PE-01).

**NFR-ON-02 — All onboarding operations are fully offline.** No network call is made at any point during onboarding in Phase 1 (consistent with NFR-OF-01, NFR-OF-02).

**NFR-ON-03 — No financial data is transmitted externally during onboarding** (consistent with NFR-DS-01).

**NFR-ON-04 — Display Name input must support system font size scaling** (consistent with NFR-AC-02).

**NFR-ON-05 — All interactive elements must meet the 48×48dp minimum touch target size** (consistent with NFR-AC-01).

**NFR-ON-06 — Database initialization (Workspace + Categories) must complete in under 500ms** on target devices to avoid a perceptible delay before the Feature Slides appear.

---

## Edge Cases

**EC-ON-01 — App killed between Feature Slides and Set Your Name**
If the user navigates past the slides (Skip or Get Started) and the app is killed before completing Set Your Name, `onboarding_completed` remains `false`. On next launch, the app shows the Feature Slides again from slide 1. This is the correct behavior — the user has not completed onboarding.

**EC-ON-02 — App killed between Set Your Name and Add an Account**
If the user completes Set Your Name (name stored, `onboarding_completed = true`) and the app is killed before reaching or completing Add an Account, the next launch goes directly to MainActivity (Dashboard). The user skips Add an Account entirely. This is acceptable — the flag is set after Set Your Name, not after account setup.

**EC-ON-03 — User adds multiple accounts and kills the app mid-session**
Any accounts saved to Room before the app is killed are persisted. On next launch the user goes to Dashboard (since `onboarding_completed = true`). No accounts are lost.

**EC-ON-04 — OS back on slide 1**
On slide 1, the OS back gesture or button exits the app entirely. This is consistent with the back behavior on Set Your Name and Add an Account — the first screen of each onboarding section exits the app on back.

**EC-ON-05 — Duplicate account name during multi-account entry**
If the user adds two accounts with the same name in the same onboarding session, the second save attempt is blocked with an inline error (see RQ-ON-26). The first account remains saved. The user must change the name before saving.

**EC-ON-06 — Display Name at exactly 2 or 30 characters**
Both boundary values are valid. A name of exactly 2 characters enables the Continue button. A name of exactly 30 characters is accepted and the field enforces the limit by preventing any further input — the 31st character is silently rejected at the input field level. No error message is shown. The field itself enforces the constraint.

**EC-ON-07 — Initial Balance of zero**
An Initial Balance of 0 is valid for all account types. This represents an account with no funds at setup time (e.g. a new empty checking account). The Save button must not block on a zero balance.

**EC-ON-08 — Workspace or category initialization failure**
If Room fails to create the Workspace or seed categories on first launch, the app must not proceed to the Feature Slides. A full-screen error state is shown with the message *"Something went wrong. Please try again."* and a "Retry" button. Tapping Retry re-runs the initialization. The app does not display partial onboarding UI on top of an uninitialized database.

**EC-ON-09 — Clear Data resets onboarding**
When the user taps Clear Data in Settings and confirms, both the Room database and all SharedPreferences (including `onboarding_completed` and `display_name`) are wiped. The next launch runs the full onboarding flow from the beginning, including re-initialization of the Workspace and default categories. The same outcome occurs if the user clears app data via Android device Settings → Apps → [App Name] → Clear Data — this is an OS-level operation that produces an identical result to the in-app Clear Data action.

---

## Business Rules

**BR-ON-01 — onboarding_completed flag**
`onboarding_completed` is a boolean stored in SharedPreferences. It defaults to `false` (missing key treated as false). It is set to `true` once and only once: when the user taps Continue on the Set Your Name screen with a valid Display Name. It is never set by Feature Slides completion or Add an Account completion or skipping.

**BR-ON-02 — Display Name constraints**
Display Name must be between 2 and 30 characters inclusive. Leading and trailing whitespace is trimmed before validation and storage. A name consisting entirely of whitespace is treated as empty and does not satisfy the minimum length requirement.

**BR-ON-03 — Display Name storage**
Display Name is stored in SharedPreferences only. It is not written to the Room database in Phase 1. It is not synced to any server in Phase 1.

**BR-ON-04 — Default Workspace**
The default Workspace is identified by being the first (and only) Workspace created on first launch, not by a hardcoded ID (BR-WS-02). Its UUID is generated client-side at creation time. It is not visible in the UI in Phase 1.

**BR-ON-05 — Default categories are system-owned**
All seeded categories have `is_default = true`. Default categories cannot be deleted, only hidden (BR-CA-02). They are created once during initialization and never re-seeded on subsequent launches (idempotency — RQ-ON-30).

**BR-ON-06 — Account currency during onboarding**
Accounts created during onboarding inherit the workspace `base_currency` (USD in Phase 1). The currency field is read-only and not editable during onboarding (BR-CU-02). Currency picker is introduced in Phase 2.

**BR-ON-07 — Account amounts**
Initial Balance and Credit Limit are stored as integers in cents (BR-DI-03). Maximum storable value is 999,999,999 cents ($9,999,999.99). Amounts are always positive or zero; negative Initial Balance is not supported during onboarding.

**BR-ON-08 — Back navigation behavior across onboarding**
The OS back gesture or back button exits the app on the first screen of each onboarding section: slide 1 of Feature Slides, Set Your Name, and Add an Account. On slides 2 and 3 of Feature Slides, back navigates to the previous slide. Once the user has left the Feature Slides (via Skip or Get Started), back navigation never returns them to the slides within the same session.

**BR-ON-09 — Credit Card balance and limit constraint**
For Credit Card accounts, the Initial Balance must not exceed the Credit Limit. This is enforced as a cross-field validation rule: the Credit Limit field shows an inline error on blur when Credit Limit < Initial Balance. References BR-AC-04.

---

## Acceptance Criteria

### Happy Path

**AC-ON-01 — Complete onboarding end-to-end**
Given the app is launched for the first time,
When the user swipes through all 3 Feature Slides, enters a valid Display Name, saves one account, and taps "Go to Home",
Then the app navigates to MainActivity (Dashboard), `onboarding_completed = true` is stored in SharedPreferences, the Display Name is stored in SharedPreferences, the account exists in Room, the Workspace exists in Room, and all 20 default categories exist in Room.

**AC-ON-02 — Skip Feature Slides on slide 1**
Given the user is on Feature Slide 1,
When the user taps Skip,
Then the app navigates to Set Your Name, the Feature Slides are not shown again on the next launch (controlled by `onboarding_completed` being set later), and `onboarding_completed` remains `false`.

**AC-ON-03 — Get Started on slide 3**
Given the user is on Feature Slide 3,
When the user taps Get Started,
Then the app navigates to Set Your Name. The Skip button is not shown on slide 3. The Get Started button is shown in its place.

**AC-ON-04 — Continue enabled only with valid name**
Given the user is on Set Your Name,
When the user types a name of 1 character,
Then the Continue button is disabled.
When the user types a second character (total: 2),
Then the Continue button becomes enabled.
When the user clears the field entirely,
Then the Continue button returns to disabled.

**AC-ON-05 — Display Name stored and flag set on Continue**
Given the user is on Set Your Name with a valid name entered,
When the user taps Continue,
Then the Display Name is written to SharedPreferences under key `display_name`, `onboarding_completed` is set to `true` in SharedPreferences, and the app navigates to Add an Account.

**AC-ON-06 — Save and Add Another Account**
Given the user is on Add an Account with a valid form,
When the user taps Save and the dialog appears,
And the user taps "Add Another Account",
Then the dialog dismisses, the account is persisted in Room, all form fields are cleared, and the user remains on the Add an Account screen ready to enter another account.

**AC-ON-07 — Save and Go to Home**
Given the user is on Add an Account with a valid form,
When the user taps Save and the dialog appears,
And the user taps "Go to Home",
Then the dialog dismisses, the account is persisted in Room, and the app navigates to MainActivity (Dashboard).

**AC-ON-08 — Skip Add an Account**
Given the user is on Add an Account,
When the user taps "Skip for now",
Then the app navigates to MainActivity (Dashboard) with no account created. No error or warning is shown.

**AC-ON-09 — Credit Limit field visibility toggle**
Given the user is on Add an Account,
When the user selects Credit Card as the account type,
Then the Credit Limit field appears.
When the user switches to any other account type,
Then the Credit Limit field is hidden and its value is cleared.
When the user switches back to Credit Card,
Then the Credit Limit field appears again, empty.

**AC-ON-10 — Subsequent launch skips onboarding**
Given `onboarding_completed = true` is stored in SharedPreferences,
When the app is launched,
Then the app navigates directly to MainActivity (Dashboard). The onboarding flow is not shown.

---

### Edge Cases

**AC-ON-11 — App killed after slides, before Set Your Name completion**
Given the user navigated past Feature Slides but did not complete Set Your Name (`onboarding_completed = false`),
When the app is relaunched,
Then the Feature Slides are shown from slide 1.

**AC-ON-12 — App killed after Set Your Name, before Add an Account**
Given the user completed Set Your Name (`onboarding_completed = true`) and the app was killed on Add an Account,
When the app is relaunched,
Then the app navigates directly to Dashboard. Add an Account is not shown again.

**AC-ON-13 — OS back on slide 1**
Given the user is on Feature Slide 1,
When the user presses the OS back button or performs the back gesture,
Then the app exits to the Android home screen.

**AC-ON-14 — OS back on Set Your Name**
Given the user is on Set Your Name,
When the user presses the OS back button or performs the back gesture,
Then the app exits to the Android home screen. The Feature Slides are not shown.

**AC-ON-15 — OS back on Add an Account**
Given the user is on Add an Account,
When the user presses the OS back button or performs the back gesture,
Then the app exits to the Android home screen. Set Your Name is not shown.

**AC-ON-16 — Duplicate account name**
Given the user has saved an account named "Chase Checking" during onboarding,
When the user attempts to save another account also named "Chase Checking",
Then the save is blocked and an inline error appears below the Name field: *"An account with this name already exists."* The previously saved account is unaffected.

**AC-ON-17 — Display Name with only whitespace**
Given the user enters only spaces in the Display Name field,
When validation runs,
Then the field is treated as empty after trimming, the Continue button remains disabled, and no name is stored.

**AC-ON-18 — Initial Balance of zero**
Given the user enters 0 as the Initial Balance,
When the Save button state is evaluated,
Then zero is accepted as a valid value and the Save button is enabled (assuming other required fields are valid).

**AC-ON-19 — Database initialization failure on first launch**
Given the app is launched for the first time and Room fails to initialize,
When the initialization error occurs,
Then a full-screen error state is shown with a Retry button. The Feature Slides are not shown. Tapping Retry re-attempts initialization.

**AC-ON-20 — Clear Data resets to full onboarding**
Given the user has previously completed onboarding,
When the user clears all data from Settings (or via Android device Settings → Apps → [App Name] → Clear Data) and relaunches,
Then the Feature Slides are shown from slide 1, `onboarding_completed` is `false`, the Room database is empty, and initialization runs again.

**AC-ON-21 — Credit Limit below Initial Balance shows inline error**
Given the user has selected Credit Card as the account type,
And has entered an Initial Balance of $4,500.00,
When the user enters a Credit Limit of $4,000.00 and moves focus away from the field (blur),
Then an inline error appears below the Credit Limit field: *"Credit limit must be greater than or equal to the initial balance."*
And the Save button remains disabled.
When the user corrects the Credit Limit to $4,500.00 or more,
Then the error is cleared and the Save button becomes enabled (assuming all other fields are valid).

---

## Error Handling

| Scenario | Error Type | User-Facing Message | Recovery |
|---|---|---|---|
| Database initialization failure | Full-screen error state | "Something went wrong. Please try again." | Retry button re-runs initialization |
| Duplicate account name | Inline field error | "An account with this name already exists." | User corrects the name field |
| Account save failure (Room write error) | Inline error below Save button | "Couldn't save account. Please try again." | User taps Save again |
| Display Name at character limit | Input silently stops accepting characters | None — field simply stops | User is informed by field behavior only |
| Initial Balance exceeds maximum | Inline field error | "Maximum amount is $9,999,999.99." | User corrects the amount |
| Credit Limit exceeds maximum | Inline field error | "Maximum amount is $9,999,999.99." | User corrects the amount |
| Credit Limit of zero or negative | Inline field error | "Credit limit must be greater than $0." | User corrects the amount |
| Credit Limit less than Initial Balance | Inline field error on blur | "Credit limit must be greater than or equal to the initial balance." | User corrects the Credit Limit field |

---

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Room database | Internal | Workspace, Category, and Account entities must be fully defined before onboarding can be implemented |
| SharedPreferences | Internal | `onboarding_completed` and `display_name` keys |
| Workspace entity | Internal | Must exist before Account or Category can be created (BR-WS-01) |
| Category entity | Internal | Default categories seeded during initialization |
| Account entity and creation form | Internal | Add an Account reuses the same form as the Accounts feature. The Accounts feature spec must define the canonical form — onboarding references it. |
| Borel font resource | Design | `BorelFontFamily` defined in `core/ui/theme/Type.kt`, font file at `res/font/borel_regular.ttf`. Required for the "Capital" label on Feature Slides, Set Your Name, and Add an Account screens. See `specs/design/design.md` for registration details. |
| Slide illustrations | Design | 3 illustration assets (one per slide). Placeholders acceptable for Phase 1. |
| Accent color token | Design | Used as background for Feature Slides |
| AppNavGraph | Internal | OnboardingActivity has its own NavHost. Navigation to MainActivity is handled via Intent, not Compose Navigation. |

---

## Platform: Web (Phase 3)

Web onboarding differs from Android in the following ways:

- **Authentication replaces Set Your Name.** The "Set Your Name" screen is replaced by a Supabase Auth registration form (name, email, password). Display Name is stored in the User record on the backend, not in localStorage.
- **No SharedPreferences.** Onboarding state is derived from the presence of a valid Supabase session and a completed User record.
- **Feature Slides are the same three slides**, adapted for web layout and viewport sizes.
- **Account Setup is the same in intent**, but the form connects to `POST /api/v1/accounts` via the feature API rather than writing directly to Room.
- **Privacy copy is adapted:** *"Your financial data is encrypted and stored securely. No third parties."*

Full web onboarding spec is defined at Phase 3 kickoff.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-29 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-05-30 | Danielle Mariani | RQ-ON-01: add onboarding_completed = false gate. RQ-ON-05: move Skip to bottom-right corner. RQ-ON-06: clarify Get Started as a distinct filled button separate from Skip. RQ-ON-08: slide 1 back exits the app (was no-op); slides 2–3 back navigates to previous slide. RQ-ON-12: specify SharedPreferences key names (display_name, onboarding_completed). RQ-ON-23: update dialog body copy to "go to the app's Home". EC-ON-04: updated to reflect slide 1 back exits the app. EC-ON-06: clarified field enforces max length silently, no error message. EC-ON-09: add note about OS-level data clear via Android device Settings. BR-ON-08: rewritten to cover back behavior across all onboarding screens consistently. AC IDs: removed HP/EC distinction — all IDs follow AC-ON-NN format (AC-ON-01 through AC-ON-20). |
| 0.3.0 | 2026-06-05 | Danielle Mariani | RQ-ON-21: add cross-field validation — Credit Limit must be ≥ Initial Balance for Credit Card accounts; inline error on blur below Credit Limit field. RQ-ON-22: add Credit Limit ≥ Initial Balance to Save button validity conditions. Error Handling table: add Credit Limit < Initial Balance row. Add BR-ON-09: Credit Card balance and limit constraint (references BR-AC-04 in SPEC.md). Add AC-ON-21: inline Credit Limit error when balance exceeds limit. |
| 0.4.0 | 2026-06-08 | Danielle Mariani | RQ-ON-02, RQ-ON-09, RQ-ON-17: replace app logo with Capital wordmark (centered, top of screen) across all three onboarding screens. Dependencies table: rename "App logo asset" to "Capital wordmark asset"; expand note to include Add an Account screen. |
| 0.5.0 | 2026-06-08 | Danielle Mariani | RQ-ON-02, RQ-ON-09, RQ-ON-17: clarify that "Capital" label is a `Text` composable using `BorelFontFamily` — not an image asset. Consistent with AppHeader implementation in home spec. Dependencies table: replace "Capital wordmark asset" with "Borel font resource" row referencing `core/ui/theme/Type.kt` and `design.md` registration. |
# Onboarding — Design

**Version:** 0.3.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-05-30
**Last Updated:** 2026-05-31

---

## Overview

This document defines the technical solution design for the Onboarding feature on Android (Phase 1). It covers the component structure, data models, screen-level composable and ViewModel design, shared components, navigation implementation, dependency injection, and testing strategy.

This document is the implementation reference for the Onboarding feature. All decisions here must be consistent with `specs/features/onboarding/requirements.md` (functional requirements) and `specs/design/design.md` (visual design system). Where this document references design tokens, those tokens are defined in `specs/design/design.md` and mapped to Material 3 via the app's `MaterialTheme`.

---

## Related Documents

| Document | Purpose |
|---|---|
| specs/features/onboarding/requirements.md | Functional requirements and acceptance criteria |
| specs/design/design.md | Design system — color tokens, typography, spacing, elevation |
| specs/design/navigation.md | Navigation flows and OnboardingActivity structure |
| specs/technical/data-model.md | Canonical schema for Workspace, Category, Account |
| ARCHITECTURE.md | Android stack, MVVM + Clean Architecture, package structure |

---

## Architecture Overview

Onboarding runs entirely within `OnboardingActivity`, a self-contained Activity with its own `NavHost`. It has no shared back stack with `MainActivity`. When onboarding completes (or is skipped past the point of no return), the app starts `MainActivity` via an explicit `Intent` and calls `finish()` on `OnboardingActivity`.

All data operations are offline-only in Phase 1. No network calls are made during onboarding.

```
OnboardingActivity
    └── OnboardingNavHost
            ├── FeatureSlidesScreen        ──► route: "feature_slides"
            ├── SetYourNameScreen          ──► route: "set_your_name"
            └── AddAnAccountScreen         ──► route: "add_an_account"

SharedPreferences
    ├── onboarding_completed: Boolean
    └── display_name: String

Room Database (AppDatabase)
    ├── WorkspaceDao       ──► Workspace entity
    ├── CategoryDao        ──► Category entity
    └── AccountDao         ──► Account entity

MainActivity  ◄── started via Intent on onboarding completion
```

**Key architectural constraints:**
- `OnboardingActivity` calls `PreferencesDataSource.isOnboardingCompleted()` on `onCreate`. If `true`, it immediately starts `MainActivity` and finishes itself without inflating any UI. No direct SharedPreferences access anywhere in the onboarding feature — all reads and writes go through `PreferencesDataSource`.
- Database initialization (Workspace + Category seeding) runs once, on a background coroutine, before `FeatureSlidesScreen` is shown.
- All three screens share a single `OnboardingViewModel` scoped to `OnboardingActivity`. This avoids re-running initialization logic and allows state (e.g. saved accounts count) to persist across screen transitions within the flow.

---

## Component Structure

```
feature/onboarding/
├── ui/
│   ├── OnboardingActivity.kt              # Activity shell, PreferencesDataSource gate, NavHost
│   ├── OnboardingNavGraph.kt              # NavHost destinations and route constants
│   ├── OnboardingViewModel.kt             # Single ViewModel scoped to OnboardingActivity
│   ├── screens/
│   │   ├── FeatureSlidesScreen.kt         # Slide pager composable
│   │   ├── SetYourNameScreen.kt           # Display name input composable
│   │   └── AddAnAccountScreen.kt          # Account creation form composable
│   └── components/
│       ├── OnboardingPageIndicator.kt     # 3-dot page indicator component
│       ├── OnboardingSlide.kt             # Single slide layout composable
│       └── AccountSavedDialog.kt          # "Add Another / Go to Home" dialog
├── di/
│   └── OnboardingModule.kt                # Hilt @Binds and @Provides for onboarding
├── domain/
│   ├── OnboardingRepository.kt            # Interface
│   ├── InitializeWorkspaceUseCase.kt      # Creates default Workspace + seeds Categories
│   ├── SaveDisplayNameUseCase.kt          # Delegates to PreferencesDataSource
│   └── CreateAccountUseCase.kt            # Persists a new Account to Room
└── data/
    ├── local/
    │   └── OnboardingLocalDataSource.kt   # Wraps WorkspaceDao, CategoryDao, AccountDao
    └── OnboardingRepositoryImpl.kt        # Implements OnboardingRepository
```

**Notes:**
- `CreateAccountUseCase` is defined in the onboarding feature for Phase 1. When the Accounts feature is implemented, a shared `CreateAccountUseCase` may be extracted to `core/domain/` if the logic is identical. This decision is deferred to the Accounts feature spec.
- `AccountFormFields.kt` is a shared composable defined in `core/ui/` — see Shared Components section.
- `OnboardingNavGraph.kt` defines route constants as an object to avoid stringly-typed navigation.
- `PreferencesDataSource` lives in `core/data/` — it is not onboarding-specific and is shared across features.

---

## Constants

### SharedPreferences Keys

All SharedPreferences keys are defined as constants in a single object located in `core/data/`. This prevents typos and ensures that any feature reading or writing these values references the same key string.

```kotlin
// core/data/PreferenceKeys.kt
object PreferenceKeys {
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val DISPLAY_NAME = "display_name"
}
```

**Usage in `SaveDisplayNameUseCase`:**
```kotlin
prefs.edit {
    putString(PreferenceKeys.DISPLAY_NAME, trimmedName)
    putBoolean(PreferenceKeys.ONBOARDING_COMPLETED, true)
}
```

**Usage in `OnboardingActivity`:**
```kotlin
prefs.getBoolean(PreferenceKeys.ONBOARDING_COMPLETED, false)
```

`PreferenceKeys` lives in `core/data/` because SharedPreferences is a data-layer concern and these keys are not onboarding-specific — `DISPLAY_NAME` is read by the Dashboard greeting and `ONBOARDING_COMPLETED` is read on every app launch.

---

### String Resources

All user-facing copy is defined in `res/values/strings.xml`. Composables reference strings via `stringResource(R.string.*)` — no hardcoded strings in Kotlin files. This ensures the app is ready for localization (e.g. Spanish) without requiring code changes.

**Onboarding string keys:**

```xml
<!-- res/values/strings.xml — Onboarding -->

<!-- Feature Slides -->
<string name="onboarding_slide_1_title">Know where your money goes</string>
<string name="onboarding_slide_1_message">Track every expense and income across all your accounts. See your full financial picture at a glance.</string>
<string name="onboarding_slide_2_title">Plan with intention</string>
<string name="onboarding_slide_2_message">Set monthly spending budgets by category. Save toward the things that matter most.</string>
<string name="onboarding_slide_3_title">All your accounts, one place. Works offline, always.</string>
<string name="onboarding_slide_3_message">Add checking, savings, credit cards, and cash. Your data stays on your device — no account required.</string>
<string name="onboarding_skip">Skip</string>
<string name="onboarding_get_started">Get Started</string>

<!-- Set Your Name -->
<string name="onboarding_name_hint">Your name</string>
<string name="onboarding_name_privacy_copy">Your display name is stored only on this device. No account required.</string>
<string name="onboarding_name_continue">Continue</string>

<!-- Add an Account -->
<string name="onboarding_account_encouraging_copy">Add an account to get the most out of %s</string>
<string name="onboarding_account_name_label">Account name</string>
<string name="onboarding_account_type_label">Account type</string>
<string name="onboarding_account_type_checking">Checking</string>
<string name="onboarding_account_type_savings">Savings</string>
<string name="onboarding_account_type_cash">Cash</string>
<string name="onboarding_account_type_credit_card">Credit Card</string>
<string name="onboarding_account_currency_label">Currency</string>
<string name="onboarding_account_initial_balance_label">Initial balance</string>
<string name="onboarding_account_credit_limit_label">Credit limit</string>
<string name="onboarding_account_save">Save</string>
<string name="onboarding_account_skip">Skip for now</string>

<!-- Account Saved Dialog -->
<string name="onboarding_dialog_title">Account saved!</string>
<string name="onboarding_dialog_body">Would you like to add another account or go to the app\'s Home?</string>
<string name="onboarding_dialog_add_another">Add Another Account</string>
<string name="onboarding_dialog_go_home">Go to Home</string>

<!-- Validation and Error Messages -->
<string name="onboarding_error_duplicate_account_name">An account with this name already exists.</string>
<string name="onboarding_error_account_save_failed">Couldn\'t save account. Please try again.</string>
<string name="onboarding_error_max_amount">Maximum amount is $9,999,999.99.</string>
<string name="onboarding_error_credit_limit_zero">Credit limit must be greater than $0.</string>

<!-- Initialization Error -->
<string name="onboarding_error_init_title">Something went wrong.</string>
<string name="onboarding_error_init_body">Please try again.</string>
<string name="onboarding_error_retry">Retry</string>
```

**Notes:**
- `onboarding_account_encouraging_copy` uses a `%s` placeholder for the app name. This is resolved at runtime via `stringResource(R.string.onboarding_account_encouraging_copy, stringResource(R.string.app_name))`.
- The app name (`app_name`) is defined separately in `strings.xml` and used throughout the app — not duplicated here.
- Error message strings for amounts (`onboarding_error_max_amount`) use a hardcoded USD value for Phase 1. If multi-currency formatting is introduced, these strings will be generated programmatically rather than from static resources.

---

## Data Models

Onboarding interacts with three Room entities. Full schema is defined in `specs/technical/data-model.md`. This section summarizes the fields written during onboarding.

### Workspace

| Field | Value written during onboarding |
|---|---|
| id | UUID v4, generated client-side |
| name | `"Personal"` |
| base_currency | `"USD"` |
| created_at | Current UTC Unix timestamp |
| updated_at | Current UTC Unix timestamp |
| deleted_at | `null` |
| last_synced_at | `null` (Phase 1) |
| sync_status | `null` (Phase 1) |

### Category (× 20)

| Field | Value written during onboarding |
|---|---|
| id | UUID v4, generated client-side (one per category) |
| workspace_id | FK → Workspace.id (the default workspace) |
| name | See default category table in requirements.md (RQ-ON-28) |
| icon | See default category table in requirements.md (RQ-ON-28) |
| is_default | `1` (true) |
| is_hidden | `0` (false) |
| created_at | Current UTC Unix timestamp |
| updated_at | Current UTC Unix timestamp |
| deleted_at | `null` |
| last_synced_at | `null` (Phase 1) |
| sync_status | `null` (Phase 1) |

### Account (0 or more)

| Field | Value written during onboarding |
|---|---|
| id | UUID v4, generated client-side |
| workspace_id | FK → Workspace.id (the default workspace) |
| name | User-entered (max 100 chars) |
| type | User-selected: `CHECKING`, `SAVINGS`, `CASH`, or `CREDIT_CARD` |
| currency_code | Inherited from Workspace.base_currency (`"USD"` in Phase 1) |
| initial_balance | User-entered in dollars, stored as cents (integer) |
| credit_limit | User-entered in dollars, stored as cents; `null` if type ≠ `CREDIT_CARD` |
| is_pinned | `0` (false) |
| pinned_at | `null` |
| created_at | Current UTC Unix timestamp |
| updated_at | Current UTC Unix timestamp |
| deleted_at | `null` |
| last_synced_at | `null` (Phase 1) |
| sync_status | `null` (Phase 1) |

### SharedPreferences

All keys are defined in `PreferenceKeys` (see Constants section). Direct string literals must not be used.

| Key constant | Key string | Type | Set when |
|---|---|---|---|
| `PreferenceKeys.ONBOARDING_COMPLETED` | `"onboarding_completed"` | Boolean | After user taps Continue on Set Your Name with a valid name |
| `PreferenceKeys.DISPLAY_NAME` | `"display_name"` | String | After user taps Continue on Set Your Name with a valid name |

Both keys are written atomically in a single SharedPreferences `edit { ... }` block before navigation proceeds.

---

## Screen Specifications

### Feature Slides

#### Composable Breakdown

```
FeatureSlidesScreen
    ├── HorizontalPager (Compose Accompanist or native Compose)
    │       └── OnboardingSlide (×3)
    │               ├── Image / illustration placeholder (centered, large)
    │               ├── Text — slide title (Headline style)
    │               └── Text — slide key message (Body style)
    ├── OnboardingPageIndicator
    │       └── Row of 3 dot indicators
    ├── SkipButton (visible on pages 0 and 1 only)
    └── GetStartedButton (visible on page 2 only)
```

`HorizontalPager` manages swipe state. The page index drives the visibility of Skip vs Get Started and the active dot in `OnboardingPageIndicator`.

#### ViewModel — State & Events

`OnboardingViewModel` exposes the following for Feature Slides:

**State (part of `OnboardingUiState`):**

```kotlin
data class OnboardingUiState(
    val isInitializing: Boolean = true,
    val initializationError: Boolean = false,
    // Set Your Name state
    val displayName: String = "",
    val isDisplayNameValid: Boolean = false,
    // Add an Account state
    val accountName: String = "",
    val accountType: AccountType = AccountType.CHECKING,
    val initialBalance: String = "",
    val creditLimit: String = "",
    val isAccountFormValid: Boolean = false,
    val accountSaveError: String? = null,
    val showAccountSavedDialog: Boolean = false,
    val savedAccountsCount: Int = 0
)
```

**Events:**

```kotlin
sealed class OnboardingEvent {
    object InitializationRetried : OnboardingEvent()
    object SlidesCompleted : OnboardingEvent()          // Skip or Get Started tapped
    data class DisplayNameChanged(val name: String) : OnboardingEvent()
    object ContinueWithName : OnboardingEvent()
    data class AccountNameChanged(val name: String) : OnboardingEvent()
    data class AccountTypeChanged(val type: AccountType) : OnboardingEvent()
    data class InitialBalanceChanged(val value: String) : OnboardingEvent()
    data class CreditLimitChanged(val value: String) : OnboardingEvent()
    object SaveAccount : OnboardingEvent()
    object AddAnotherAccount : OnboardingEvent()        // Dialog: Add Another
    object GoToHome : OnboardingEvent()                 // Dialog: Go to Home / Skip
}
```

**Side effects** (one-shot navigation events emitted via `Channel<OnboardingSideEffect>`):

```kotlin
sealed class OnboardingSideEffect {
    object NavigateToSetYourName : OnboardingSideEffect()
    object NavigateToAddAnAccount : OnboardingSideEffect()
    object NavigateToHome : OnboardingSideEffect()      // starts MainActivity, finishes OnboardingActivity
}
```

#### UI States

| State | Trigger | UI behavior |
|---|---|---|
| Initializing | App first launch, before Workspace + Categories are created | Full-screen loading indicator over accent background. Feature Slides pager not yet shown. |
| Initialization error | Room write fails during setup | Full-screen error state: message + Retry button (see Error Handling section) |
| Idle (slides) | Initialization complete | Pager shown, user can swipe or tap Skip / Get Started |

#### Design Tokens Applied

| Element | Token | Notes |
|---|---|---|
| Screen background | `color.accent.primary` | Full-screen fill. Applies to all 3 slides uniformly. |
| All text (title, body, buttons) | `color.accent.on` | White on accent background |
| App logo top padding | `spacing.xl` (32dp) | From status bar to top of logo |
| Logo bottom margin | `spacing.lg` (24dp) | Between logo and illustration |
| Illustration area | Fills remaining space between logo and copy | Centered, scales with screen height |
| Slide title | Typography: Headline (22sp, weight 600) | `color.accent.on` |
| Slide key message | Typography: Body (15sp, weight 400) | `color.accent.on`, `spacing.xs` below title |
| Horizontal padding (copy) | `spacing.lg` (24dp) | Left and right |
| Page indicator top margin | `spacing.lg` (24dp) | Above indicator, below key message |
| Page indicator dot size | 8dp | Active dot: filled `color.accent.on`. Inactive: `color.accent.on` at 40% alpha |
| Gap between dots | `spacing.sm` (8dp) | |
| Skip button position | Bottom-right corner | Typography: Label (13sp, weight 500), `color.accent.on` |
| Skip button inset | `spacing.md` (16dp) | Right and bottom inset from screen edge |
| Get Started button | Bottom center, full-width minus horizontal padding | Filled button, background `color.accent.on`, text `color.accent.primary` |
| Get Started bottom padding | `spacing.lg` (24dp) | From button bottom to screen bottom |
| Get Started horizontal padding | `spacing.md` (16dp) | Left and right screen inset |
| Slide transition animation | Horizontal slide | Follows swipe direction. Duration: 250ms (per `design.md` motion guidelines) |

---

### Set Your Name

#### Composable Breakdown

```
SetYourNameScreen
    ├── Image — app logo (top, centered)
    ├── BasicTextField — Display Name input
    │       └── Large, transparent background, bottom border only
    ├── Text — privacy copy (Caption style, secondary color)
    └── Button — Continue (bottom, full-width, disabled until valid)
```

No `Scaffold` or `TopAppBar` — fully custom layout using a `Column` with `fillMaxSize`.

#### ViewModel — State & Events

Handled by `OnboardingViewModel` (shared). Relevant state fields:

- `displayName: String` — current input value
- `isDisplayNameValid: Boolean` — true when trimmed length is between 2 and 30 characters

Relevant events: `DisplayNameChanged`, `ContinueWithName`

On `ContinueWithName`:
1. Trim `displayName`
2. Write `display_name` and `onboarding_completed = true` to SharedPreferences atomically
3. Emit `NavigateToAddAnAccount` side effect

#### UI States

| State | Trigger | UI behavior |
|---|---|---|
| Empty (initial) | Screen first shown | Name field empty, Continue button disabled and dimmed |
| Typing — invalid | Input length < 2 (after trim) | Continue button disabled and dimmed |
| Typing — valid | Input length 2–30 (after trim) | Continue button enabled |
| At max length | Input reaches 30 characters | Field silently stops accepting input. No error shown. |
| Cleared after valid | User deletes all characters | Continue button returns to disabled and dimmed |

#### Design Tokens Applied

| Element | Token | Notes |
|---|---|---|
| Screen background | `color.background.primary` | Light background — signals transition into the app |
| App logo top padding | `spacing.xl` (32dp) | From status bar to top of logo |
| Logo bottom margin | `spacing.xxl` (48dp) | Generous space between logo and name field |
| Display Name field font size | 28sp, weight 400 | Larger than Body scale — intentionally prominent. Uses Inter. |
| Display Name field text color | `color.text.primary` | |
| Display Name field background | Transparent | Bottom border only: `color.border.default` at 1dp |
| Display Name field focused border | `color.accent.primary` | Bottom border color when field is focused |
| Display Name horizontal padding | `spacing.md` (16dp) | Left and right screen inset |
| Privacy copy | Typography: Caption (11sp, weight 400) | `color.text.secondary`, `spacing.sm` below name field |
| Privacy copy horizontal padding | `spacing.md` (16dp) | |
| Continue button | Bottom of screen, full-width minus insets | Filled button, `color.accent.primary` background |
| Continue button — enabled | `color.accent.primary` background, `color.accent.on` text | |
| Continue button — disabled | `color.accent.primary` at 38% alpha, `color.accent.on` at 38% alpha | Standard Material 3 disabled state |
| Continue button bottom padding | `spacing.lg` (24dp) | From button bottom to screen bottom |
| Continue button horizontal inset | `spacing.md` (16dp) | Left and right |

---

### Add an Account

#### Composable Breakdown

```
AddAnAccountScreen
    ├── Image — app logo (top, centered)
    ├── Text — encouraging copy (Body style)
    ├── AccountFormFields                   # shared composable from core/ui/
    │       ├── OutlinedTextField — Account Name
    │       ├── ExposedDropdownMenu — Account Type
    │       ├── OutlinedTextField — Currency (read-only, dimmed)
    │       ├── OutlinedTextField — Initial Balance (numeric, currency-formatted)
    │       └── OutlinedTextField — Credit Limit (conditional, numeric)
    ├── Button — Save (disabled until form valid)
    ├── TextButton — "Skip for now" (always visible, always enabled)
    └── AccountSavedDialog (shown after successful save)
            ├── Text — title: "Account saved!"
            ├── Text — body: "Would you like to add another account or go to the app's Home?"
            ├── TextButton — "Add Another Account"
            └── Button — "Go to Home"
```

#### ViewModel — State & Events

Handled by `OnboardingViewModel` (shared). Relevant state fields:

- `accountName: String`
- `accountType: AccountType` — enum: `CHECKING`, `SAVINGS`, `CASH`, `CREDIT_CARD`
- `initialBalance: String` — raw string from input, parsed to cents on save
- `creditLimit: String` — raw string from input, parsed to cents on save; only relevant when `accountType == CREDIT_CARD`
- `isAccountFormValid: Boolean` — derived: name non-empty and ≤ 100 chars, type selected, initialBalance ≥ 0, creditLimit > 0 if type is CREDIT_CARD
- `accountSaveError: String?` — inline error message (e.g. duplicate name)
- `showAccountSavedDialog: Boolean` — drives dialog visibility
- `savedAccountsCount: Int` — incremented on each successful save

Relevant events: `AccountNameChanged`, `AccountTypeChanged`, `InitialBalanceChanged`, `CreditLimitChanged`, `SaveAccount`, `AddAnotherAccount`, `GoToHome`

On `SaveAccount`:
1. Validate form fields
2. Parse `initialBalance` and `creditLimit` strings to cents (integers)
3. Build `Account` domain object with a client-generated UUID v4
4. Call `CreateAccountUseCase`
5. On success: increment `savedAccountsCount`, set `showAccountSavedDialog = true`, clear `accountSaveError`
6. On failure (duplicate name): set `accountSaveError = "An account with this name already exists."`
7. On failure (Room write error): set `accountSaveError = "Couldn't save account. Please try again."`

On `AddAnotherAccount`:
1. Set `showAccountSavedDialog = false`
2. Clear `accountName`, `initialBalance`, `creditLimit`
3. Reset `accountType` to `CHECKING`
4. Clear `accountSaveError`

On `GoToHome`:
1. Set `showAccountSavedDialog = false`
2. Emit `NavigateToHome` side effect → `OnboardingActivity` starts `MainActivity`, calls `finish()`

#### UI States

| State | Trigger | UI behavior |
|---|---|---|
| Empty (initial) | Screen first shown | All fields empty, Save button disabled |
| Filling form | User interacts with fields | Save button enables when all required fields are valid |
| Credit Card selected | `accountType == CREDIT_CARD` | Credit Limit field appears below Initial Balance |
| Other type selected | `accountType != CREDIT_CARD` | Credit Limit field hidden, its value cleared |
| Save attempted — duplicate name | Room returns unique constraint violation | Inline error below Name field. Save button re-enables. |
| Save attempted — Room error | Unexpected write failure | Inline error below Save button. |
| Save success | Account written to Room | `AccountSavedDialog` shown |
| Dialog — Add Another | User taps "Add Another Account" | Dialog dismisses, form clears, user stays on screen |
| Dialog — Go to Home | User taps "Go to Home" | Dialog dismisses, MainActivity started |

#### Design Tokens Applied

| Element | Token | Notes |
|---|---|---|
| Screen background | `color.background.primary` | |
| App logo top padding | `spacing.xl` (32dp) | |
| Logo bottom margin | `spacing.lg` (24dp) | Between logo and encouraging copy |
| Encouraging copy | Typography: Body (15sp, weight 400) | `color.text.secondary`, centered |
| Encouraging copy bottom margin | `spacing.lg` (24dp) | Between copy and form |
| Form horizontal padding | `spacing.md` (16dp) | Left and right screen inset |
| Form field gap | `spacing.md` (16dp) | Between each field |
| Input field style | `OutlinedTextField` (Material 3) | Corner radius: `radius.md` (10dp) |
| Input label color | `color.text.secondary` | Floating label |
| Input focused border | `color.accent.primary` | |
| Currency field | Dimmed: `color.text.secondary` text, `color.surface.alt` background | Read-only, not focusable |
| Inline error text | Typography: Caption (11sp) | `color.semantic.error` |
| Save button — enabled | `color.accent.primary` background, `color.accent.on` text | Full-width, `radius.md` |
| Save button — disabled | `color.accent.primary` at 38% alpha | Standard Material 3 disabled |
| Save button bottom margin | `spacing.md` (16dp) | Between Save and Skip |
| Skip button | `color.accent.primary` text, no background | Typography: Label (13sp, weight 500) |
| Skip button bottom padding | `spacing.lg` (24dp) | From button to screen bottom |
| Dialog corner radius | `radius.xl` (24dp) | Standard bottom sheet / dialog radius |
| Dialog elevation | `elevation.lg` (8dp) | |
| Dialog title | Typography: Title (17sp, weight 600) | `color.text.primary` |
| Dialog body | Typography: Body (15sp, weight 400) | `color.text.secondary` |
| Dialog "Go to Home" button | Filled, `color.accent.primary` | |
| Dialog "Add Another" button | Text button, `color.accent.primary` text | |

---

## Shared Components

### AccountFormFields

**Location:** `core/ui/components/AccountFormFields.kt`

A reusable `@Composable` that renders the account form fields (Name, Type, Currency, Initial Balance, Credit Limit). It accepts all field values and callbacks as parameters — it holds no state of its own.

```kotlin
@Composable
fun AccountFormFields(
    accountName: String,
    onAccountNameChanged: (String) -> Unit,
    accountType: AccountType,
    onAccountTypeChanged: (AccountType) -> Unit,
    currencyCode: String,           // read-only display
    initialBalance: String,
    onInitialBalanceChanged: (String) -> Unit,
    creditLimit: String,            // only relevant when type == CREDIT_CARD
    onCreditLimitChanged: (String) -> Unit,
    accountNameError: String?,      // null = no error
    initialBalanceError: String?,
    creditLimitError: String?,
    modifier: Modifier = Modifier
)
```

The Credit Limit field is rendered conditionally inside this composable based on `accountType`. This centralizes the toggle logic so both onboarding and the Accounts feature (when implemented) share identical behavior.

**Used by:**
- `AddAnAccountScreen.kt` (onboarding)
- `AccountCreationScreen.kt` (Accounts feature — Phase 1)
- `AccountEditScreen.kt` (Accounts feature — Phase 1)

---

### OnboardingPageIndicator

**Location:** `feature/onboarding/ui/components/OnboardingPageIndicator.kt`

Renders a row of `count` dots. The dot at `activePage` index is filled; all others are dimmed.

```kotlin
@Composable
fun OnboardingPageIndicator(
    count: Int,
    activePage: Int,
    modifier: Modifier = Modifier
)
```

Dot size: 8dp. Active dot: filled circle, `color.accent.on`. Inactive dot: `color.accent.on` at 40% alpha. Gap between dots: `spacing.sm` (8dp).

---

### OnboardingSlide

**Location:** `feature/onboarding/ui/components/OnboardingSlide.kt`

Renders a single slide's content: illustration, title, and key message.

```kotlin
@Composable
fun OnboardingSlide(
    illustration: Int,          // @DrawableRes
    title: String,
    message: String,
    modifier: Modifier = Modifier
)
```

---

### AccountSavedDialog

**Location:** `feature/onboarding/ui/components/AccountSavedDialog.kt`

Renders the post-save dialog with "Add Another Account" and "Go to Home" actions.

```kotlin
@Composable
fun AccountSavedDialog(
    onAddAnother: () -> Unit,
    onGoToHome: () -> Unit
)
```

This dialog is onboarding-specific and is not a shared component.

---

## Navigation Implementation

### OnboardingActivity

`OnboardingActivity` is responsible for:

1. Injecting `PreferencesDataSource` via field injection
2. Calling `preferencesDataSource.isOnboardingCompleted()` on `onCreate`
3. If `true`: immediately start `MainActivity` via `Intent`, call `finish()`, return — no UI is inflated
4. If `false`: set content to `OnboardingNavGraph`

No direct SharedPreferences access in this class — all reads go through `PreferencesDataSource`.

```kotlin
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (preferencesDataSource.isOnboardingCompleted()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent {
            BudgetAppTheme {
                OnboardingNavGraph(
                    onOnboardingComplete = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
```

### OnboardingNavGraph

Three destinations, no bottom nav, no top app bar. Routes defined as constants:

```kotlin
object OnboardingRoutes {
    const val FEATURE_SLIDES = "feature_slides"
    const val SET_YOUR_NAME = "set_your_name"
    const val ADD_AN_ACCOUNT = "add_an_account"
}
```

The `NavHost` starts at `FEATURE_SLIDES`. Navigation between destinations is triggered by side effects emitted from `OnboardingViewModel` and collected in `OnboardingActivity` or the `NavGraph` composable.

### Back Navigation Overrides

Back behavior is overridden per screen using `BackHandler`:

**Feature Slides — slide 1:**
```kotlin
// In FeatureSlidesScreen, when pagerState.currentPage == 0
BackHandler {
    (context as? Activity)?.finish()
}
```

**Feature Slides — slides 2 and 3:**
No `BackHandler` override — the `HorizontalPager` handles back to the previous page natively via `pagerState`.

**Set Your Name:**
```kotlin
BackHandler {
    (context as? Activity)?.finish()
}
```

**Add an Account:**
```kotlin
BackHandler {
    (context as? Activity)?.finish()
}
```

### Navigation to MainActivity

`OnboardingActivity` passes an `onOnboardingComplete` lambda into the `NavGraph`. The `NavGraph` collects `OnboardingSideEffect.NavigateToHome` from the ViewModel and calls this lambda. This keeps navigation logic out of the ViewModel and composables.

```
OnboardingSideEffect.NavigateToHome
    └── collected in OnboardingNavGraph
            └── calls onOnboardingComplete()
                    └── OnboardingActivity starts MainActivity + finishes itself
```

---

## Dependency Injection

Onboarding DI is split across two Hilt modules:

- **`DatabaseModule`** (`core/data/di/`) — provides the Room database singleton and all DAOs. Installed in `SingletonComponent`.
- **`OnboardingModule`** (`feature/onboarding/di/`) — provides and binds all onboarding-specific classes. Installed in `SingletonComponent`. Uses `@Binds` for interfaces (abstract module) and `@Provides` where needed.

```kotlin
// feature/onboarding/di/OnboardingModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository

    companion object {

        @Provides
        fun provideOnboardingLocalDataSource(
            workspaceDao: WorkspaceDao,
            categoryDao: CategoryDao,
            accountDao: AccountDao
        ): OnboardingLocalDataSource =
            OnboardingLocalDataSource(workspaceDao, categoryDao, accountDao)

        @Provides
        fun providePreferencesDataSource(
            @ApplicationContext context: Context
        ): PreferencesDataSource =
            PreferencesDataSource(context)
    }
}
```

Use cases (`InitializeWorkspaceUseCase`, `SaveDisplayNameUseCase`, `CreateAccountUseCase`) are annotated with `@Inject constructor(...)` and do not require explicit `@Provides` entries — Hilt resolves them automatically via constructor injection.

`OnboardingViewModel` is annotated with `@HiltViewModel` and injected via `hiltViewModel()` scoped to `OnboardingActivity`. All three screens obtain the ViewModel via `hiltViewModel()` from the Activity's scope — this ensures a single shared instance across the navigation graph.

`SaveDisplayNameUseCase` injects `PreferencesDataSource` — not `@ApplicationContext` directly. All SharedPreferences access is centralized through `PreferencesDataSource`.

---

## Key Implementation Notes

### Initialization Idempotency

`InitializeWorkspaceUseCase` must be safe to run multiple times without creating duplicates. The implementation checks for the existence of a Workspace before creating one:

```
1. Query WorkspaceDao for any existing non-deleted Workspace
2. If found: skip Workspace creation, skip Category seeding, return success
3. If not found: create Workspace, then seed all 20 default Categories in a single transaction
```

Category seeding is wrapped in a Room `@Transaction` to ensure all 20 categories are inserted atomically. If any insert fails, the entire seed is rolled back and the error is surfaced to the ViewModel.

### Currency Input Parsing

Initial Balance and Credit Limit are collected as raw strings from the user and parsed to integer cents before saving. The parsing logic:

1. Strip currency symbols and formatting characters (e.g. `$`, `,`)
2. Parse to `Double`
3. Multiply by 100, round to nearest integer
4. Validate: result must be ≥ 0 for Initial Balance, > 0 for Credit Limit, ≤ 999,999,999 for both

This parsing runs in the ViewModel on `SaveAccount` event, not on every keystroke. Field-level formatting (displaying `$1,234.56` as the user types) is handled by a custom `VisualTransformation` on the `OutlinedTextField`.

### Single ViewModel Across Screens

`OnboardingViewModel` is scoped to `OnboardingActivity` (not to individual destinations). All three screens call `hiltViewModel<OnboardingViewModel>()` — Compose resolves this to the Activity-scoped instance. This ensures:

- Initialization state (`isInitializing`, `initializationError`) is computed once and survives navigation between screens
- `savedAccountsCount` persists if the user adds multiple accounts and then navigates within onboarding
- No repeated database reads between screen transitions

### HorizontalPager Back Behavior on Slide 1

The `HorizontalPager` does not consume back events by default. A `BackHandler` is conditionally registered only when `pagerState.currentPage == 0` to exit the app. On pages 1 and 2, no `BackHandler` is registered — Compose Navigation's default back behavior navigates within the pager via `pagerState.animateScrollToPage(pagerState.currentPage - 1)`.

### Atomic SharedPreferences Write

`display_name` and `onboarding_completed` are written atomically through `PreferencesDataSource.saveDisplayName()`. No caller writes these keys directly — the atomic write is centralized in one place:

```kotlin
// core/data/PreferencesDataSource.kt
fun saveDisplayName(name: String) {
    prefs.edit {
        putString(PreferenceKeys.DISPLAY_NAME, name)
        putBoolean(PreferenceKeys.ONBOARDING_COMPLETED, true)
    }
}
```

`apply()` (async write) is used internally by the Kotlin `edit { }` extension. This is acceptable because navigation is triggered after the call returns — the values will be committed before the next Activity read occurs. `SaveDisplayNameUseCase` trims the name before passing it to `PreferencesDataSource`.

---

## Error Handling

| Error scenario | Detection point | UiState change | User-facing behavior |
|---|---|---|---|
| Workspace/Category initialization failure | `InitializeWorkspaceUseCase` throws | `initializationError = true` | Full-screen error composable with message and Retry button. Slides not shown. |
| Account save — duplicate name | Room `SQLiteConstraintException` on unique index | `accountSaveError = "An account with this name already exists."` | Inline error below Name field. Dialog not shown. |
| Account save — Room write error | Unexpected exception from DAO | `accountSaveError = "Couldn't save account. Please try again."` | Inline error below Save button. |
| Initial Balance exceeds maximum | Validation in ViewModel on SaveAccount | `initialBalanceError = "Maximum amount is $9,999,999.99."` | Inline error below Initial Balance field. |
| Credit Limit exceeds maximum | Validation in ViewModel on SaveAccount | `creditLimitError = "Maximum amount is $9,999,999.99."` | Inline error below Credit Limit field. |
| Credit Limit zero or negative | Validation in ViewModel on SaveAccount | `creditLimitError = "Credit limit must be greater than $0."` | Inline error below Credit Limit field. |

All errors are surfaced as fields in `OnboardingUiState` — no exceptions are thrown to the UI layer. The ViewModel catches all exceptions from use cases and maps them to state. No Snackbars are used during onboarding — all errors are inline, close to the relevant field or action.

**Initialization error screen:**

```
FullScreenErrorState
    ├── Icon — error / warning (Material Symbols, 48dp)
    ├── Text — "Something went wrong." (Title style)
    ├── Text — "Please try again." (Body style, secondary color)
    └── Button — "Retry" (filled, accent)
```

Tapping Retry dispatches `OnboardingEvent.InitializationRetried`, which re-runs `InitializeWorkspaceUseCase` and resets `initializationError = false, isInitializing = true`.

---

## Testing Strategy

### Unit Tests

| Class | What to test |
|---|---|
| `OnboardingViewModel` | `isDisplayNameValid` transitions (empty → 1 char → 2 chars → 30 chars → cleared). `isAccountFormValid` with all field combinations. Credit Limit field visibility logic. `SaveAccount` happy path. `SaveAccount` with duplicate name. `SaveAccount` with Room write failure. `AddAnotherAccount` clears form state. `ContinueWithName` emits correct side effect. |
| `InitializeWorkspaceUseCase` | Creates Workspace and 20 Categories on empty DB. Is idempotent on second call (no duplicates). Returns error on DAO failure. |
| `SaveDisplayNameUseCase` | Delegates correctly to `PreferencesDataSource.saveDisplayName`. Trims whitespace before delegating. |
| `CreateAccountUseCase` | Happy path: Account persisted to Room with correct field values. Duplicate name: throws or returns error result. Amount parsing: correct cents conversion for boundary values (0, 1, 999999999). |

### Integration Tests

| Scope | What to test |
|---|---|
| Room (in-memory DB) | `InitializeWorkspaceUseCase` creates exactly 1 Workspace and 20 Categories. Second call is a no-op. `CreateAccountUseCase` persists Account with correct `workspace_id`. Duplicate Account name is rejected by unique constraint. |
| SharedPreferences | `PreferencesDataSource.saveDisplayName` writes both keys atomically. `isOnboardingCompleted` returns correct value. Values survive a simulated process restart (read back from a fresh `PreferencesDataSource` instance). |

### UI Tests

Deferred — consistent with the global testing strategy in `ARCHITECTURE.md`. UI tests for onboarding are not required for Phase 1.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-30 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-05-31 | Danielle Mariani | Add Constants section with `PreferenceKeys` object (`ONBOARDING_COMPLETED`, `DISPLAY_NAME`) in `core/data/`. Add `strings.xml` string resource definitions for all onboarding copy, labels, and error messages. Update SharedPreferences table in Data Models to reference `PreferenceKeys` constants. Update `OnboardingActivity` and `SaveDisplayNameUseCase` snippets to use `PreferenceKeys`. Update Atomic SharedPreferences Write note. |
| 0.3.0 | 2026-05-31 | Danielle Mariani | Introduce `PreferencesDataSource` as centralized SharedPreferences wrapper in `core/data/`. Update Architecture Overview: all SharedPreferences access goes through `PreferencesDataSource`, no direct access anywhere. Update Component Structure: add `di/OnboardingModule.kt`, update `OnboardingActivity` and `SaveDisplayNameUseCase` comments. Update `OnboardingActivity` snippet: inject `PreferencesDataSource` via `@Inject`, call `isOnboardingCompleted()`. Rewrite Dependency Injection section: split into `DatabaseModule` and `OnboardingModule` (abstract class with `@Binds` + companion `@Provides`); use cases use constructor injection. Update Atomic SharedPreferences Write: centralized in `PreferencesDataSource.saveDisplayName`. Update Testing Strategy rows for `SaveDisplayNameUseCase` and SharedPreferences integration. |
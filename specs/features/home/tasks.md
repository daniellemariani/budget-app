# Home — Tasks

**Version:** 0.1.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-06-05
**Last Updated:** 2026-06-05

---

## Overview

This file defines the implementation task breakdown for the Home feature. Tasks are organized into groups that follow the natural dependency order: foundation first, then resources, then navigation, then UI shell, then placeholders, then tests.

The Home feature has no data layer, no repository, and no ViewModel. All tasks are app-shell and UI concerns.

Note: Group 0 (Project Foundation) tasks are defined in `specs/features/onboarding/tasks.md` (TSK-ON-01 through TSK-ON-04). Home tasks begin after those are complete.

Each task specifies its requirements coverage, acceptance criteria, dependencies on other tasks, files to create, and an effort estimate (S = ~1–2h, M = ~2–4h, L = ~4–8h).

**Group execution order:**

| Group | Name | Can start when |
|---|---|---|
| 1 | Resources | Onboarding TSK-ON-01 done |
| 2 | Navigation Foundation | TSK-HM-01 done |
| 3 | UI Shell | TSK-HM-02, TSK-HM-03 done |
| 4 | Placeholder Screen | TSK-HM-04 done |
| 5 | MainActivity Assembly | TSK-HM-03, TSK-HM-05, TSK-HM-06 done |
| 6 | Testing | TSK-HM-07 done |

---

## Task Summary

| ID | Title | Group | Phase | Effort | Status |
|---|---|---|---|---|---|
| TSK-HM-01 | Define Home string resources | Resources | 1 | S | Not Started |
| TSK-HM-02 | Define TopLevelDestination sealed class | Navigation Foundation | 1 | S | Not Started |
| TSK-HM-03 | Implement AppNavGraph shell | Navigation Foundation | 1 | M | Not Started |
| TSK-HM-04 | Implement BottomNavigationBar composable | UI Shell | 1 | M | Not Started |
| TSK-HM-05 | Implement AppHeader composable | UI Shell | 1 | S | Not Started |
| TSK-HM-06 | Implement TabPlaceholder composable | Placeholder Screen | 1 | S | Not Started |
| TSK-HM-07 | Assemble MainActivity | MainActivity Assembly | 1 | M | Not Started |
| TSK-HM-08 | Unit tests for Home shell | Testing | 1 | M | Not Started |
| TSK-HM-09 | Integration tests for MainActivity navigation | Testing | 1 | M | Not Started |

---

## Task Format

Each task follows this structure:

```
**TSK-HM-XX — Title**
- Effort: S / M / L
- Phase: 1
- Group: Group name
- Requirements: RQ-HM-XX, ...
- Acceptance Criteria: AC-HM-XX, ...
- Status: Not Started / Done
- Depends on: TSK-HM-XX, ... / None
- Creates:
  - full/path/to/File.kt
- Details:
  What to implement, key constraints, and any non-obvious decisions.
```

---

## Group 1 — Resources

String resources and drawable assets that all Home shell composables depend on. Must be in place before any UI task begins.

---

**TSK-HM-01 — Define Home string resources**
- Effort: S
- Phase: 1
- Group: Resources
- Requirements: RQ-HM-04, RQ-HM-05, RQ-HM-07
- Acceptance Criteria: —
- Status: Not Started
- Depends on: TSK-ON-01 (project skeleton exists)
- Creates:
  - Additions to `android/app/src/main/res/values/strings.xml`
  - `android/app/src/main/res/drawable/ic_wordmark.xml` (light theme wordmark)
  - `android/app/src/main/res/drawable-night/ic_wordmark.xml` (dark theme wordmark)
- Details:
  Add the following string entries to `strings.xml`. Do not create a separate file — append to the existing onboarding strings file under a `<!-- Home / Navigation -->` comment block:

  ```xml
  <!-- Home / Navigation -->
  <string name="nav_dashboard">Dashboard</string>
  <string name="nav_accounts">Accounts</string>
  <string name="nav_transactions">Transactions</string>
  <string name="nav_budgets">Budgets</string>
  <string name="nav_goals">Goals</string>
  <string name="settings">Settings</string>
  <string name="app_name">Capital</string>
  ```

  Wordmark assets (`ic_wordmark.xml`): placeholder vector drawables at this stage — a simple text path or rectangle is acceptable. Final branded assets are provided by design before the public launch. Two variants are required from day one so the theme-switching mechanism works correctly:
  - `res/drawable/ic_wordmark.xml` — for light theme
  - `res/drawable-night/ic_wordmark.xml` — for dark theme

  Android resolves the correct variant automatically based on `UiMode`. No runtime switching logic is needed in code.

---

## Group 2 — Navigation Foundation

Route constants and the `AppNavGraph` shell. These are the structural backbone of `MainActivity` — all UI shell tasks and feature nav graph registrations depend on these being in place.

---

**TSK-HM-02 — Define TopLevelDestination sealed class**
- Effort: S
- Phase: 1
- Group: Navigation Foundation
- Requirements: RQ-HM-07
- Acceptance Criteria: AC-HM-01, AC-HM-02
- Status: Not Started
- Depends on: TSK-ON-01
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/TopLevelDestination.kt`
- Details:
  Single source of truth for the five tab routes. Defined as a sealed class in the `app/` package — not inside any feature package.

  ```kotlin
  sealed class TopLevelDestination(val route: String) {
      object Dashboard    : TopLevelDestination("dashboard")
      object Accounts     : TopLevelDestination("accounts")
      object Transactions : TopLevelDestination("transactions")
      object Budgets      : TopLevelDestination("budgets")
      object Goals        : TopLevelDestination("goals")
  }
  ```

  Settings is intentionally excluded — it is a non-tab destination and its route is defined in `feature/settings/ui/SettingsDestination.kt` (Settings feature spec). Feature-internal sub-routes (e.g. `AccountsDestination.Detail`) are also defined inside their respective feature packages and must not appear here.

---

**TSK-HM-03 — Implement AppNavGraph shell**
- Effort: M
- Phase: 1
- Group: Navigation Foundation
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-07, RQ-HM-12
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-06, AC-HM-07
- Status: Not Started
- Depends on: TSK-HM-02, TSK-HM-06
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/AppNavGraph.kt`
- Details:
  `AppNavGraph` is a `@Composable` function that hosts the `NavHost` and registers all feature nested nav graphs. At this stage, all five tab destinations are wired to `TabPlaceholder`. Feature nav graph registrations are replaced one by one as each feature is implemented.

  ```kotlin
  @Composable
  fun AppNavGraph(
      navController: NavHostController,
      modifier: Modifier = Modifier
  ) {
      NavHost(
          navController = navController,
          startDestination = TopLevelDestination.Dashboard.route,
          modifier = modifier
      ) {
          // Tab destinations — replaced with real feature nav graphs as features are built
          composable(TopLevelDestination.Dashboard.route) {
              TabPlaceholder(labelRes = R.string.nav_dashboard)
          }
          composable(TopLevelDestination.Accounts.route) {
              TabPlaceholder(
                  labelRes = R.string.nav_accounts,
                  onNavigateToDashboard = {
                      navController.navigate(TopLevelDestination.Dashboard.route) {
                          popUpTo(TopLevelDestination.Dashboard.route) { inclusive = false }
                      }
                  }
              )
          }
          composable(TopLevelDestination.Transactions.route) {
              TabPlaceholder(
                  labelRes = R.string.nav_transactions,
                  onNavigateToDashboard = {
                      navController.navigate(TopLevelDestination.Dashboard.route) {
                          popUpTo(TopLevelDestination.Dashboard.route) { inclusive = false }
                      }
                  }
              )
          }
          composable(TopLevelDestination.Budgets.route) {
              TabPlaceholder(
                  labelRes = R.string.nav_budgets,
                  onNavigateToDashboard = {
                      navController.navigate(TopLevelDestination.Dashboard.route) {
                          popUpTo(TopLevelDestination.Dashboard.route) { inclusive = false }
                      }
                  }
              )
          }
          composable(TopLevelDestination.Goals.route) {
              TabPlaceholder(
                  labelRes = R.string.nav_goals,
                  onNavigateToDashboard = {
                      navController.navigate(TopLevelDestination.Dashboard.route) {
                          popUpTo(TopLevelDestination.Dashboard.route) { inclusive = false }
                      }
                  }
              )
          }
          // Settings — registered here; destination composable owned by Settings feature spec
          composable(SettingsDestination.route) {
              // Placeholder until Settings feature is implemented
              TabPlaceholder(labelRes = R.string.settings)
          }
      }
  }
  ```

  When each feature's nav graph extension function is ready (e.g. `accountsNavGraph()`), its corresponding `composable()` block above is replaced with the nested graph call:
  ```kotlin
  accountsNavGraph(navController)
  ```

---

## Group 3 — UI Shell

The `BottomNavigationBar` and `AppHeader` composables. Both are app-level UI components that live in `app/ui/` and are rendered directly by `MainActivity`.

---

**TSK-HM-04 — Implement BottomNavigationBar composable**
- Effort: M
- Phase: 1
- Group: UI Shell
- Requirements: RQ-HM-07, RQ-HM-08, RQ-HM-09, RQ-HM-10, RQ-HM-11
- Acceptance Criteria: AC-HM-02, AC-HM-03, AC-HM-04, AC-HM-05, AC-HM-10, AC-HM-11
- Status: Not Started
- Depends on: TSK-HM-01, TSK-HM-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/ui/BottomNavigationBar.kt`
- Details:
  Implement `BottomNavItem` data class and `BottomNavigationBar` composable as specified in `design.md`.

  ```kotlin
  data class BottomNavItem(
      val destination: TopLevelDestination,
      @StringRes val labelRes: Int,
      val selectedIcon: ImageVector,
      val unselectedIcon: ImageVector
  )
  ```

  The five `BottomNavItem` entries are defined inside the composable as a remembered list. Icon identifiers (filled and outlined variants from Material Symbols) are confirmed at implementation time — use the closest available Material Icons as stand-ins if the exact Symbols variants are not yet available.

  Active tab detection must use `currentDestination?.hierarchy` (not direct route equality) so nested destinations within a tab (e.g. Account Detail) keep the correct tab highlighted.

  All colors must use `MaterialTheme.colorScheme` tokens — no hardcoded values:
  - `selectedIconColor` → `MaterialTheme.colorScheme.primary`
  - `selectedTextColor` → `MaterialTheme.colorScheme.primary`
  - `unselectedIconColor` → `MaterialTheme.colorScheme.onSurfaceVariant`
  - `unselectedTextColor` → `MaterialTheme.colorScheme.onSurfaceVariant`
  - `indicatorColor` → `MaterialTheme.colorScheme.secondaryContainer`
  - `containerColor` (NavigationBar) → `MaterialTheme.colorScheme.surface`

  Scroll-to-top on re-selection: `onTabSelected` is always called on tap (including when the tab is already active). `launchSingleTop = true` in the nav graph prevents a duplicate back stack entry. The scroll-to-top side effect is handled inside each feature's root list composable via `LaunchedEffect` — not here.

---

**TSK-HM-05 — Implement AppHeader composable**
- Effort: S
- Phase: 1
- Group: UI Shell
- Requirements: RQ-HM-03, RQ-HM-04, RQ-HM-05, RQ-HM-06
- Acceptance Criteria: AC-HM-09, AC-HM-10, AC-HM-11
- Status: Not Started
- Depends on: TSK-HM-01
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/ui/AppHeader.kt`
- Details:
  Implement `AppHeader` as a `TopAppBar` composable. Accepts `onSettingsClick: () -> Unit` lambda — the composable has no knowledge of the `NavController`.

  ```kotlin
  @Composable
  fun AppHeader(
      onSettingsClick: () -> Unit,
      modifier: Modifier = Modifier
  )
  ```

  Wordmark: `Image` composable using `painterResource(R.drawable.ic_wordmark)`. Height fixed at `24.dp`. `contentDescription` = `stringResource(R.string.app_name)`. No click action.

  Gear icon: `Icons.Outlined.Settings`. `contentDescription` = `stringResource(R.string.settings)`. `onClick` invokes `onSettingsClick`.

  Colors via `TopAppBarDefaults.topAppBarColors`:
  - `containerColor` → `MaterialTheme.colorScheme.surface`
  - `titleContentColor` → `MaterialTheme.colorScheme.onSurface`
  - `actionIconContentColor` → `MaterialTheme.colorScheme.onSurfaceVariant`

---

## Group 4 — Placeholder Screen

The single reusable placeholder composable. Must exist before `AppNavGraph` can compile (it is referenced in TSK-HM-03).

---

**TSK-HM-06 — Implement TabPlaceholder composable**
- Effort: S
- Phase: 1
- Group: Placeholder Screen
- Requirements: RQ-HM-12, RQ-HM-13, RQ-HM-15
- Acceptance Criteria: AC-HM-07
- Status: Not Started
- Depends on: TSK-HM-01
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/home/ui/placeholders/TabPlaceholder.kt`
- Details:
  Single reusable composable that serves as the temporary tab content for all five destinations (and temporarily for Settings) until real screens are implemented.

  ```kotlin
  @Composable
  fun TabPlaceholder(
      @StringRes labelRes: Int,
      onNavigateToDashboard: (() -> Unit)? = null
  ) {
      if (onNavigateToDashboard != null) {
          BackHandler { onNavigateToDashboard() }
      }
      Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
      ) {
          Text(
              text = stringResource(labelRes),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
          )
      }
  }
  ```

  `onNavigateToDashboard` must be non-null for all non-Dashboard tab destinations. The Dashboard tab and the Settings placeholder pass `null` — the Dashboard exit behavior is handled by Compose Navigation's default start destination back behavior; Settings has no special back behavior.

  This file is deleted entirely once all five feature root Composables are implemented and wired into `AppNavGraph`.

---

## Group 5 — MainActivity Assembly

Assembles the full `MainActivity` shell using all components built in Groups 1–4. This is the final wiring step before the app is runnable end-to-end.

---

**TSK-HM-07 — Assemble MainActivity**
- Effort: M
- Phase: 1
- Group: MainActivity Assembly
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-03, RQ-HM-05, RQ-HM-14, RQ-HM-15
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-06, AC-HM-07, AC-HM-08, AC-HM-09, AC-HM-10, AC-HM-11
- Status: Not Started
- Depends on: TSK-HM-03, TSK-HM-04, TSK-HM-05, TSK-HM-06
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/MainActivity.kt` (replaces onboarding-era placeholder)
- Details:
  This task replaces the temporary `MainActivity.kt` placeholder created during onboarding (TSK-ON-15) with the full implementation.

  `MainActivity` is annotated `@AndroidEntryPoint`. Injects `PreferencesDataSource` via field injection for the onboarding guard.

  `onCreate` logic:
  1. Call `super.onCreate(savedInstanceState)`
  2. Check `preferencesDataSource.isOnboardingCompleted()`. If `false`: start `OnboardingActivity`, call `finish()`, return.
  3. Call `setContent { AppTheme { ... } }`

  Inside `setContent`:
  - `rememberNavController()` for the `NavHostController`
  - `currentBackStackEntryAsState()` to observe `currentDestination`
  - `Scaffold` with:
    - `topBar`: `AppHeader(onSettingsClick = { navController.navigate(SettingsDestination.route) })`
    - `bottomBar`: `BottomNavigationBar(currentDestination, onTabSelected = { ... })`
    - Content: `AppNavGraph(navController, Modifier.padding(innerPadding))`

  Tab selection in `onTabSelected`:
  ```kotlin
  navController.navigate(destination.route) {
      popUpTo(navController.graph.findStartDestination().id) {
          saveState = true
      }
      launchSingleTop = true
      restoreState = true
  }
  ```

  Update `AndroidManifest.xml`: ensure `MainActivity` is the launcher activity (replacing or confirming `OnboardingActivity` launch logic — the manifest must declare the correct entry point; app launch routing is handled in code via the SharedPreferences guard, not via separate manifest intent filters per activity).

  **Phase 2 note:** add session check after the onboarding guard — no structural changes needed, just an additional `if` branch.

---

## Group 6 — Testing

---

**TSK-HM-08 — Unit tests for Home shell composables**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-HM-07, RQ-HM-08, RQ-HM-09, RQ-HM-04, RQ-HM-05
- Acceptance Criteria: AC-HM-02, AC-HM-03, AC-HM-04, AC-HM-09
- Status: Not Started
- Depends on: TSK-HM-04, TSK-HM-05, TSK-HM-06
- Creates:
  - `android/app/src/test/java/com/dmariani/capital/app/ui/BottomNavigationBarTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/app/ui/AppHeaderTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/feature/home/ui/TabPlaceholderTest.kt`
- Details:
  Use Compose testing APIs (`createComposeRule`, `onNodeWithContentDescription`, `performClick`, `assertIsDisplayed`).

  **`BottomNavigationBarTest`:**
  - Active tab item shows filled icon and primary color token
  - Inactive tab items show outlined icon and `onSurfaceVariant` color token
  - `onTabSelected` lambda is invoked with the correct `TopLevelDestination` when a tab is tapped
  - Tapping the already-active tab still invokes `onTabSelected` (scroll-to-top responsibility lives upstream)

  **`AppHeaderTest`:**
  - Wordmark image is displayed (`contentDescription` = app name string)
  - Gear icon button is present (`contentDescription` = settings string)
  - `onSettingsClick` lambda is invoked when gear icon is tapped

  **`TabPlaceholderTest`:**
  - Label string is rendered for a given `labelRes`
  - `BackHandler` is active when `onNavigateToDashboard` is non-null
  - No `BackHandler` registered when `onNavigateToDashboard` is `null`

---

**TSK-HM-09 — Integration tests for MainActivity navigation**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-11, RQ-HM-14, RQ-HM-15, RQ-HM-16, RQ-HM-17
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-05, AC-HM-06, AC-HM-07, AC-HM-08
- Status: Not Started
- Depends on: TSK-HM-07
- Creates:
  - `android/app/src/androidTest/java/com/dmariani/capital/app/MainActivityNavigationTest.kt`
- Details:
  Instrumented tests using `ActivityScenario<MainActivity>` and Compose testing APIs. Requires `onboarding_completed = true` in SharedPreferences before each test — set via a `@Before` rule that writes directly to SharedPreferences using the test application context.

  Test cases:
  - App launches with Dashboard tab active and shell elements visible (header, bottom nav)
  - Tapping each non-active tab switches to that tab's placeholder screen
  - Re-selecting the active tab does not add a new entry to the back stack (`navController.backQueue.size` unchanged)
  - Back press on a non-Dashboard tab navigates to Dashboard tab
  - Back press on Dashboard tab exits the app (activity finishes)
  - Navigating Accounts → back → Dashboard, then Accounts again restores Accounts tab state (back stack preserved via `restoreState`)
  - Tapping gear icon navigates to Settings placeholder

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-06-05 | Danielle Mariani | Initial draft. 9 tasks across 6 groups. No data layer tasks — Home is UI-only. Group 0 (Project Foundation) delegated to onboarding tasks. |
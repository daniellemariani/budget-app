# Home — Tasks

**Version:** 0.2.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-06-05
**Last Updated:** 2026-06-08

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

String resources that all Home shell composables depend on. Must be in place before any UI task begins.

Note: The Borel font asset and `FontFamily` definition are defined in `specs/design/design.md` and are registered as part of the design system setup, not here. `AppHeader` imports `BorelFontFamily` from the design system's typography definitions in `core/ui/theme/`.

---

**TSK-HM-01 — Define Home string resources**
- Effort: S
- Phase: 1
- Group: Resources
- Requirements: RQ-HM-04, RQ-HM-05, RQ-HM-06, RQ-HM-09
- Acceptance Criteria: —
- Status: Not Started
- Depends on: TSK-ON-01 (project skeleton exists)
- Creates:
  - Additions to `android/app/src/main/res/values/strings.xml`
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

  `app_name` is used as the text content for the Dashboard header label and as the `contentDescription` for accessibility on that label. All other strings serve as both tab accessibility descriptions (bottom nav icons) and header labels for non-Dashboard tabs.

---

## Group 2 — Navigation Foundation

Route constants and the `AppNavGraph` shell. These are the structural backbone of `MainActivity` — all UI shell tasks and feature nav graph registrations depend on these being in place.

---

**TSK-HM-02 — Define TopLevelDestination sealed class**
- Effort: S
- Phase: 1
- Group: Navigation Foundation
- Requirements: RQ-HM-09
- Acceptance Criteria: AC-HM-01, AC-HM-02
- Status: Not Started
- Depends on: TSK-ON-01
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/TopLevelDestination.kt`
- Details:
  Single source of truth for the five tab routes and their string resource references. Defined as a sealed class in the `app/` package — not inside any feature package.

  ```kotlin
  sealed class TopLevelDestination(
      val route: String,
      @StringRes val labelRes: Int
  ) {
      object Dashboard    : TopLevelDestination("dashboard",    R.string.nav_dashboard)
      object Accounts     : TopLevelDestination("accounts",     R.string.nav_accounts)
      object Transactions : TopLevelDestination("transactions", R.string.nav_transactions)
      object Budgets      : TopLevelDestination("budgets",      R.string.nav_budgets)
      object Goals        : TopLevelDestination("goals",        R.string.nav_goals)
  }
  ```

  `labelRes` is used by `AppHeader` to resolve the tab name for non-Dashboard tabs, and by `BottomNavigationBar` for icon `contentDescription`. Settings is intentionally excluded — it is a non-tab destination and its route is defined in `feature/settings/ui/SettingsDestination.kt` (Settings feature spec).

---

**TSK-HM-03 — Implement AppNavGraph shell**
- Effort: M
- Phase: 1
- Group: Navigation Foundation
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-09, RQ-HM-15
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-11, AC-HM-12
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
- Requirements: RQ-HM-09, RQ-HM-10, RQ-HM-11, RQ-HM-12, RQ-HM-13, RQ-HM-14
- Acceptance Criteria: AC-HM-02, AC-HM-05, AC-HM-06, AC-HM-07, AC-HM-08, AC-HM-15, AC-HM-16
- Status: Not Started
- Depends on: TSK-HM-01, TSK-HM-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/ui/BottomNavigationBar.kt`
- Details:
  Implement `BottomNavItem` data class and `BottomNavigationBar` composable as specified in `design.md`. Icon-only — no text labels.

  ```kotlin
  data class BottomNavItem(
      val destination: TopLevelDestination,
      @StringRes val labelRes: Int,
      val selectedIcon: ImageVector,
      val unselectedIcon: ImageVector
  )
  ```

  The five `BottomNavItem` entries are defined inside the composable as a remembered list. Icon identifiers (filled and outlined variants from Material Symbols) are confirmed at implementation time — use the closest available Material Icons as stand-ins if the exact Symbols variants are not yet available.

  Set `label = null` on every `NavigationBarItem` — this suppresses tab text labels entirely. The `contentDescription` on the `Icon` composable (sourced from `item.labelRes`) ensures accessibility is maintained for screen readers.

  Active tab detection must use `currentDestination?.hierarchy` (not direct route equality) so nested destinations within a tab (e.g. Account Detail) keep the correct tab highlighted.

  All colors must use `MaterialTheme.colorScheme` tokens — no hardcoded values:
  - `selectedIconColor` → `MaterialTheme.colorScheme.primary`
  - `unselectedIconColor` → `MaterialTheme.colorScheme.onSurfaceVariant`
  - `indicatorColor` → `MaterialTheme.colorScheme.secondaryContainer`
  - `containerColor` (NavigationBar) → `MaterialTheme.colorScheme.surface`

  Do not set `selectedTextColor` or `unselectedTextColor` — labels are not rendered.

  Scroll-to-top on re-selection: `onTabSelected` is always called on tap (including when the tab is already active). `launchSingleTop = true` in the nav graph prevents a duplicate back stack entry. The scroll-to-top side effect is handled inside each feature's root list composable via `LaunchedEffect` — not here.

---

**TSK-HM-05 — Implement AppHeader composable**
- Effort: S
- Phase: 1
- Group: UI Shell
- Requirements: RQ-HM-03, RQ-HM-04, RQ-HM-05, RQ-HM-06, RQ-HM-07, RQ-HM-08
- Acceptance Criteria: AC-HM-03, AC-HM-04, AC-HM-09, AC-HM-10, AC-HM-14, AC-HM-15, AC-HM-16
- Status: Not Started
- Depends on: TSK-HM-01, TSK-HM-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/app/ui/AppHeader.kt`
- Details:
  Implement `AppHeader` as a `TopAppBar` composable. Accepts `currentDestination: NavDestination?`, `onSettingsClick: () -> Unit`, and `scrollBehavior: TopAppBarScrollBehavior`. The composable has no knowledge of `NavController` directly.

  ```kotlin
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun AppHeader(
      currentDestination: NavDestination?,
      onSettingsClick: () -> Unit,
      scrollBehavior: TopAppBarScrollBehavior,
      modifier: Modifier = Modifier
  )
  ```

  **Active tab detection:**
  ```kotlin
  val isDashboard = currentDestination?.hierarchy?.any {
      it.route == TopLevelDestination.Dashboard.route
  } == true
  ```

  **Title slot — Dashboard:**
  Wrap in `Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center)`.
  Render `Text(text = stringResource(R.string.app_name), fontFamily = BorelFontFamily, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)`.
  `BorelFontFamily` is imported from the design system — do not construct it inline.

  **Title slot — non-Dashboard tabs:**
  Resolve the label resource from `TopLevelDestination.entries` by matching against `currentDestination?.hierarchy`. Fall back to `R.string.app_name` if no match is found.
  Render `Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)`.

  **Actions slot:**
  Render `IconButton` with `Icons.Outlined.Settings` only when `isDashboard == true`. When `isDashboard == false`, the `actions` block is empty — no invisible placeholder rendered.
  `contentDescription` for gear icon = `stringResource(R.string.settings)`.

  **Colors via `TopAppBarDefaults.topAppBarColors`:**
  - `containerColor` → `MaterialTheme.colorScheme.surface`
  - `titleContentColor` → `MaterialTheme.colorScheme.onSurface`
  - `actionIconContentColor` → `MaterialTheme.colorScheme.onSurfaceVariant`

  **Scroll behavior:** Pass `scrollBehavior` directly to `TopAppBar`. The `nestedScroll` connection is wired at the `Scaffold` level in `MainActivity` — no additional setup needed here.

  **Label centering note:** `TopAppBar`'s `title` slot left-aligns by default. The `Box(fillMaxWidth, Alignment.Center)` wrapper achieves true screen-centered alignment regardless of whether the gear icon is present or absent in the `actions` slot.

---

## Group 4 — Placeholder Screen

The single reusable placeholder composable. Must exist before `AppNavGraph` can compile (it is referenced in TSK-HM-03).

---

**TSK-HM-06 — Implement TabPlaceholder composable**
- Effort: S
- Phase: 1
- Group: Placeholder Screen
- Requirements: RQ-HM-15, RQ-HM-16, RQ-HM-18
- Acceptance Criteria: AC-HM-12
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
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-03, RQ-HM-06, RQ-HM-07, RQ-HM-17, RQ-HM-18
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-03, AC-HM-04, AC-HM-09, AC-HM-10, AC-HM-11, AC-HM-12, AC-HM-13, AC-HM-14, AC-HM-15, AC-HM-16
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
  - `TopAppBarDefaults.enterAlwaysScrollBehavior()` for `scrollBehavior`
  - `Scaffold` with:
    - `modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`
    - `topBar`: `AppHeader(currentDestination, onSettingsClick = { navController.navigate(SettingsDestination.route) }, scrollBehavior)`
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
- Requirements: RQ-HM-09, RQ-HM-10, RQ-HM-11, RQ-HM-12, RQ-HM-04, RQ-HM-05, RQ-HM-06
- Acceptance Criteria: AC-HM-02, AC-HM-03, AC-HM-04, AC-HM-05, AC-HM-06, AC-HM-07, AC-HM-14
- Status: Not Started
- Depends on: TSK-HM-04, TSK-HM-05, TSK-HM-06
- Creates:
  - `android/app/src/test/java/com/dmariani/capital/app/ui/BottomNavigationBarTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/app/ui/AppHeaderTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/feature/home/ui/TabPlaceholderTest.kt`
- Details:
  Use Compose testing APIs (`createComposeRule`, `onNodeWithContentDescription`, `onNodeWithText`, `performClick`, `assertIsDisplayed`, `assertDoesNotExist`).

  **`BottomNavigationBarTest`:**
  - Active tab item shows filled icon and primary color token
  - Inactive tab items show outlined icon and `onSurfaceVariant` color token
  - No text label node is present for any tab item
  - `onTabSelected` lambda is invoked with the correct `TopLevelDestination` when a tab is tapped
  - Tapping the already-active tab still invokes `onTabSelected` (scroll-to-top responsibility lives upstream)

  **`AppHeaderTest`:**
  - When `currentDestination` is Dashboard: "Capital" text node is present; gear icon button is present; `onSettingsClick` is invoked on gear icon tap
  - When `currentDestination` is a non-Dashboard tab: tab name text node is present; gear icon is not present; `onSettingsClick` is not invoked on any tap

  **`TabPlaceholderTest`:**
  - Label string is rendered for a given `labelRes`
  - `BackHandler` is active when `onNavigateToDashboard` is non-null
  - No `BackHandler` registered when `onNavigateToDashboard` is `null`

---

**TSK-HM-09 — Integration tests for MainActivity navigation**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-HM-01, RQ-HM-02, RQ-HM-14, RQ-HM-17, RQ-HM-18, RQ-HM-19, RQ-HM-20
- Acceptance Criteria: AC-HM-01, AC-HM-02, AC-HM-03, AC-HM-04, AC-HM-08, AC-HM-11, AC-HM-12, AC-HM-13
- Status: Not Started
- Depends on: TSK-HM-07
- Creates:
  - `android/app/src/androidTest/java/com/dmariani/capital/app/MainActivityNavigationTest.kt`
- Details:
  Instrumented tests using `ActivityScenario<MainActivity>` and Compose testing APIs. Requires `onboarding_completed = true` in SharedPreferences before each test — set via a `@Before` rule that writes directly to SharedPreferences using the test application context.

  Test cases:
  - App launches with Dashboard tab active; header shows "Capital" and gear icon; bottom nav shows Dashboard icon in filled state
  - Tapping each non-active tab switches to that tab's placeholder screen; header updates to show tab name; gear icon is not visible
  - Returning to Dashboard tab: header shows "Capital" and gear icon again
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
| 0.2.0 | 2026-06-08 | Danielle Mariani | TSK-HM-01: remove wordmark drawable asset creation — header now uses Text composable, no image asset required. Add note re: Borel font deferral to design.md. TSK-HM-02: add `labelRes` property to `TopLevelDestination` sealed class. TSK-HM-04: update `BottomNavigationBar` — set `label = null` on all `NavigationBarItem`s; remove `selectedTextColor`/`unselectedTextColor` from color tokens. Update requirements and AC references. TSK-HM-05: full rewrite of `AppHeader` — replace `Image` wordmark with context-sensitive `Text`; add `currentDestination` and `scrollBehavior` parameters; add Dashboard vs non-Dashboard label/icon logic; add `enterAlwaysScrollBehavior` wiring; add centering note. TSK-HM-07: add `scrollBehavior` creation and `Modifier.nestedScroll` to `Scaffold`; update `AppHeader` call signature. TSK-HM-08: update `AppHeaderTest` cases for new context-sensitive behavior; add no-label assertion to `BottomNavigationBarTest`. TSK-HM-09: update integration test cases to cover header label switching and gear icon visibility per tab. |
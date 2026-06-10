# Home — Design

**Version:** 0.3.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-06-05
**Last Updated:** 2026-06-08

---

## Overview

This document defines the technical solution design for the Home feature on Android (Phase 1). It covers the `MainActivity` shell, the navigation architecture, the `AppNavGraph` nested graph composition, the `BottomNavigationBar` and `Header` composables, back navigation implementation, placeholder screen structure, dependency injection, and testing strategy.

This document is the implementation reference for the Home feature. All decisions here must be consistent with `specs/features/home/requirements.md` (functional requirements), `specs/design/design.md` (visual design system), and `navigation.md` (navigation flows and screen inventory). Where this document references design tokens, those tokens are defined in `specs/design/design.md` and mapped to Material 3 via the app's `MaterialTheme`.

---

## Related Documents

| Document | Purpose |
|---|---|
| specs/features/home/requirements.md | Functional requirements and acceptance criteria |
| specs/design/design.md | Design system — color tokens, typography, spacing, iconography, font registration (Borel) |
| navigation.md | Navigation flows, app launch logic, screen inventory |
| ARCHITECTURE.md | Android stack, MVVM + Clean Architecture, package structure |

---

## Architecture Overview

Home runs entirely within `MainActivity`. Unlike `OnboardingActivity`, `MainActivity` is not self-contained — it is the permanent host for all post-onboarding feature screens. The shell provides the `AppHeader`, the `BottomNavigationBar`, and the `MainNavHost`. All five tab destinations are registered in `AppNavGraph` as nested navigation graphs, each owned by its respective feature. Settings is also registered in `AppNavGraph` as a non-tab destination reachable from the header gear icon.

```
MainActivity
    ├── AppHeader
    │       ├── "Capital" label (Borel, centered) — Dashboard tab only
    │       ├── Tab name label (Inter, centered) — non-Dashboard tabs
    │       └── Gear icon (right) — Dashboard tab only, navigates to Settings
    ├── BottomNavigationBar
    │       ├── Dashboard    (tab 1 — default, icon only)
    │       ├── Accounts     (tab 2, icon only)
    │       ├── Transactions (tab 3, icon only)
    │       ├── Budgets      (tab 4, icon only)
    │       └── Goals        (tab 5, icon only)
    └── MainNavHost
            ├── dashboardNavGraph()     ──► feature/dashboard/
            ├── accountsNavGraph()      ──► feature/accounts/
            ├── transactionsNavGraph()  ──► feature/transactions/
            ├── budgetsNavGraph()       ──► feature/budgets/
            ├── goalsNavGraph()         ──► feature/goals/
            └── settingsNavGraph()      ──► feature/settings/
```

**Key architectural constraints:**

- `MainActivity` reads `PreferencesDataSource.isOnboardingCompleted()` on `onCreate`. If `false`, it immediately starts `OnboardingActivity`, finishes itself, and returns. This guard is a safety net only — the normal path always arrives at `MainActivity` from `OnboardingActivity` after `onboarding_completed = true` is set.
- The `BottomNavigationBar` and `AppHeader` are rendered by `MainActivity` directly, not by any individual feature. Features do not control shell visibility at the tab root level.
- Sub-screens within each feature navigate into a full-screen destination that suppresses the shell (bottom nav and shell header not present). Sub-screen top bars are defined and owned per feature spec. See `navigation.md` — Global Navigation Patterns for the sub-screen top bar convention.
- No ViewModel is scoped to `MainActivity` in Phase 1. The shell has no business logic or data dependencies.
- `AppNavGraph` is the single place that imports and wires all feature nav graphs. Features never import each other.
- `AppHeader` receives the current active destination as a parameter and derives its label text and gear icon visibility from it — it does not observe `NavController` directly.

---

## Component Structure

```
app/
├── MainActivity.kt                    # Activity shell, onboarding guard, NavHost host
├── AppNavGraph.kt                     # Composes all feature nested nav graphs
├── TopLevelDestination.kt             # Sealed class for the 5 tab routes
├── ui/
│   ├── BottomNavigationBar.kt         # Bottom nav composable (icon-only)
│   └── AppHeader.kt                   # Context-sensitive header composable

feature/home/
├── ui/
│   └── placeholders/
│       └── TabPlaceholder.kt          # Single reusable placeholder composable (accepts label string resource)
```

**Notes:**

- `MainActivity.kt`, `AppNavGraph.kt`, `TopLevelDestination.kt`, `BottomNavigationBar.kt`, and `AppHeader.kt` live in the `app/` package — they are app-level concerns, not feature-level.
- A single `TabPlaceholder` composable covers all five tab placeholder screens. It accepts a `@StringRes labelRes: Int` parameter and renders the tab name centered on the screen. One file replaces five — deleted entirely once all features are implemented.
- Each feature's root Composable (e.g. `AccountsScreen.kt`) lives in its own feature package (`feature/accounts/ui/`). `AppNavGraph` imports it directly.
- No interface or abstract class is defined for feature root Composables. The contract is enforced by convention: each feature exposes one public `@Composable` function that accepts navigation lambdas (no `NavController` passed directly into the Composable).

---

## Navigation Architecture

### Nested Nav Graphs

Each feature defines its own `NavGraphBuilder` extension function. `AppNavGraph` calls each one to compose the full navigation graph. This keeps each feature's internal routes self-contained and prevents `AppNavGraph` from growing with every sub-screen.

```kotlin
// AppNavGraph.kt
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
        dashboardNavGraph(navController)
        accountsNavGraph(navController)
        transactionsNavGraph(navController)
        budgetsNavGraph(navController)
        goalsNavGraph(navController)
        settingsNavGraph(navController)
    }
}
```

Each feature's nav graph extension is defined in its own feature package:

```kotlin
// feature/accounts/ui/AccountsNavGraph.kt
fun NavGraphBuilder.accountsNavGraph(navController: NavHostController) {
    navigation(
        startDestination = AccountsDestination.List.route,
        route = TopLevelDestination.Accounts.route
    ) {
        composable(AccountsDestination.List.route) {
            AccountsScreen(
                onNavigateToDetail = { accountId ->
                    navController.navigate(AccountsDestination.Detail.route(accountId))
                }
            )
        }
        composable(AccountsDestination.Detail.route) { backStackEntry ->
            AccountDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { accountId ->
                    navController.navigate(AccountsDestination.Edit.route(accountId))
                }
            )
        }
        // ...additional account destinations
    }
}
```

### Top-Level Destinations

All top-level destinations (one per tab) are defined as a sealed class in the `app/` package. This is the single source of truth for tab routes — feature nav graphs reference these constants when declaring their root `navigation()` block.

```kotlin
// app/TopLevelDestination.kt
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

`labelRes` is used by `AppHeader` to derive the tab name for non-Dashboard tabs. It is also retained in `BottomNavItem` for accessibility `contentDescription` on icon-only nav items.

Settings is not a tab destination and is therefore not included in `TopLevelDestination`. Its route is defined in its own feature package (`feature/settings/ui/SettingsDestination.kt`) and registered directly in `AppNavGraph` via `settingsNavGraph()`.

Feature-internal routes (e.g. `AccountsDestination.Detail`) are defined inside each feature package and are not exposed to `AppNavGraph` directly — only the nav graph extension function is exposed.

### Back Stack Per Tab

Each tab uses a separate back stack entry. When the user switches tabs, the current tab's back stack is saved and restored on return. This is achieved via `NavController` `saveState` and `restoreState` options on tab selection:

```kotlin
navController.navigate(destination.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

This ensures that navigating from Accounts tab → Transactions tab → back to Accounts tab restores the Accounts tab exactly where the user left it (e.g. still on Account Detail if they had navigated there).

---

## Back Navigation Implementation

Back navigation is handled via `BackHandler` at two levels:

**Level 1 — Non-Dashboard tab root → Dashboard:**

Each non-Dashboard tab root screen registers a `BackHandler` that navigates to the Dashboard tab instead of exiting the app. This is implemented inside each feature's root Composable, not in `MainActivity`.

```kotlin
// Example: AccountsScreen.kt
@Composable
fun AccountsScreen(
    onNavigateToDashboard: () -> Unit,
    // ...
) {
    BackHandler {
        onNavigateToDashboard()
    }
    // screen content
}
```

`onNavigateToDashboard` is a lambda passed from `AppNavGraph`, which calls:

```kotlin
navController.navigate(TopLevelDestination.Dashboard.route) {
    popUpTo(TopLevelDestination.Dashboard.route) { inclusive = false }
}
```

**Level 2 — Dashboard tab root → exit app:**

No `BackHandler` is registered on the Dashboard root screen. The default Compose Navigation behavior at the start destination exits the app via the system back mechanism.

**Level 3 — Sub-screens → pop back stack:**

Sub-screen destinations use `navController.popBackStack()` via the `onNavigateBack` lambda. No custom `BackHandler` is needed — the default Compose Navigation back behavior handles this correctly.

---

## BottomNavigationBar

Icon-only navigation — no text labels rendered. The `labelRes` on each `BottomNavItem` is retained solely for accessibility `contentDescription`.

```kotlin
// app/ui/BottomNavigationBar.kt

data class BottomNavItem(
    val destination: TopLevelDestination,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BottomNavigationBar(
    currentDestination: NavDestination?,
    onTabSelected: (TopLevelDestination) -> Unit
) {
    val items = listOf(
        BottomNavItem(TopLevelDestination.Dashboard,    R.string.nav_dashboard,    /* filled icon */, /* outlined icon */),
        BottomNavItem(TopLevelDestination.Accounts,     R.string.nav_accounts,     /* filled icon */, /* outlined icon */),
        BottomNavItem(TopLevelDestination.Transactions, R.string.nav_transactions, /* filled icon */, /* outlined icon */),
        BottomNavItem(TopLevelDestination.Budgets,      R.string.nav_budgets,      /* filled icon */, /* outlined icon */),
        BottomNavItem(TopLevelDestination.Goals,        R.string.nav_goals,        /* filled icon */, /* outlined icon */)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.destination.route
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
```

**Icon-only:** `label = null` suppresses the text label entirely. The `contentDescription` on each `Icon` ensures screen reader accessibility is maintained.

**Icon selection:** Material Symbols provides filled and outlined variants for all five icons. The `selectedIcon` uses the filled variant; `unselectedIcon` uses the outlined variant. Final icon identifiers are confirmed at implementation time.

**Active tab detection:** `currentDestination?.hierarchy` is used (not a simple route equality check) so that nested destinations within a tab (e.g. Account Detail) still highlight the correct tab in the bottom nav.

**Scroll-to-top on re-selection:** When the user taps the already-active tab, `onTabSelected` is called with the same destination. The nav graph's `launchSingleTop = true` option prevents a new back stack entry. The scroll-to-top behavior is triggered via a `LaunchedEffect` in the feature's root list Composable, which observes the `NavBackStackEntry` reselection. Implementation detail deferred to each feature spec.

---

## AppHeader

The header is context-sensitive: its label text and the visibility of the gear icon both depend on which tab is currently active.

The header uses `TopAppBarScrollBehavior` with `enterAlwaysScrollBehavior()` — it hides when the user scrolls down and re-enters when the user scrolls up. The `scrollBehavior` is created in `MainActivity` and passed down to both `AppHeader` and the `Scaffold`'s `topBar`. The `Scaffold` content must apply `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` for the scroll interaction to wire correctly.

**Scroll behavior note:** In Phase 1, placeholder tab screens have no scrollable content — the auto-hide behavior will not visually trigger on placeholder screens. This is acceptable. The behavior is structurally in place from the start; full functional verification is deferred to the point at which each feature's real list content is implemented. The scroll behavior must be fully functional across all tabs by the end of Phase 1.

```kotlin
// app/ui/AppHeader.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentDestination: NavDestination?,
    onSettingsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    val isDashboard = currentDestination?.hierarchy?.any {
        it.route == TopLevelDestination.Dashboard.route
    } == true

    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isDashboard) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = BorelFontFamily,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    val labelRes = TopLevelDestination.entries
                        .firstOrNull { dest ->
                            currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        }?.labelRes ?: R.string.app_name
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (isDashboard) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
```

**Label centering:** The `title` slot of `TopAppBar` does not center its content by default — it left-aligns. Wrapping the `Text` in a `Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth())` achieves true screen-centered alignment independent of the gear icon's presence or absence.

**Borel font:** `BorelFontFamily` is defined in `specs/design/design.md` and imported from the design system's typography definitions. It must not be constructed inline in this file.

**Label size:** "Capital" in Borel is rendered at `26.sp`. Non-Dashboard tab names use `MaterialTheme.typography.titleLarge` (Inter). Both values may be adjusted at implementation time to match visual QA.

**Gear icon visibility:** `actions` block renders the `IconButton` only when `isDashboard` is `true`. On non-Dashboard tabs the `actions` slot is empty — no invisible placeholder is rendered.

**`BorelFontFamily` import:** The `BorelFontFamily` val is defined once in the design system (`core/ui/theme/`) and imported here. Do not redeclare it in `AppHeader.kt`.

---

## MainActivity

```kotlin
// app/MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety guard: if onboarding not completed, redirect
        if (!preferencesDataSource.isOnboardingCompleted()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        AppHeader(
                            currentDestination = currentDestination,
                            onSettingsClick = {
                                navController.navigate(SettingsDestination.route)
                            },
                            scrollBehavior = scrollBehavior
                        )
                    },
                    bottomBar = {
                        BottomNavigationBar(
                            currentDestination = currentDestination,
                            onTabSelected = { destination ->
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

**`scrollBehavior`** is created at the `MainActivity` level so that both `AppHeader` and `Scaffold` share the same instance. The `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on `Scaffold` connects the scroll interaction from the tab content up through to the header.

**Phase 2 note:** When Supabase Auth is introduced, `onCreate` gains a second condition: check for a valid session after `isOnboardingCompleted()`. If no valid session exists, the user is navigated to the Login screen. No structural change to `MainActivity` is needed — the guard chain is extended.

---

## Tab Placeholder

A single reusable composable covers all five tab placeholder screens:

```kotlin
// feature/home/ui/placeholders/TabPlaceholder.kt
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

`onNavigateToDashboard` is passed for all non-Dashboard tabs to register the correct back behavior from day one. The Dashboard tab passes `null` — its back behavior (exit app) is handled by Compose Navigation's default start destination behavior.

Wired in `AppNavGraph` until each feature's real root Composable is available:

```kotlin
composable(TopLevelDestination.Accounts.route) {
    TabPlaceholder(
        labelRes = R.string.nav_accounts,
        onNavigateToDashboard = { navController.navigate(TopLevelDestination.Dashboard.route) }
    )
}
```

Each placeholder is deleted and replaced when its feature is implemented.

---

## Dependency Injection

The Home feature shell has no Repository, no use case, and no ViewModel — there is no feature-specific Hilt module. `MainActivity` injects `PreferencesDataSource` (defined in `core/data/`) via field injection for the onboarding guard only.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferencesDataSource: PreferencesDataSource
}
```

`PreferencesDataSource` is provided by the `CoreModule` (or equivalent module in `core/di/`) — it is shared infrastructure, not home-specific.

---

## Key Implementation Notes

### Shell Visibility on Sub-Screens

Sub-screens (Account Detail, Budget Detail, creation forms, etc.) must be displayed full-screen with no bottom nav and no shell header. This is achieved by registering sub-screen destinations within the `NavHost` — they are full-screen composables that draw edge-to-edge and define their own `TopAppBar` (back arrow + screen title). The shell `AppHeader` and `BottomNavigationBar` remain in the composition at the `Scaffold` level but are visually covered by the sub-screen.

The exact implementation strategy (e.g. whether to conditionally suppress shell elements or rely on visual coverage) is deferred to the Accounts feature spec, which introduces the first sub-screen. The same pattern applies to Budgets, Transactions, and Goals.

### Theme Application

`AppTheme` is applied at the `MainActivity` level, wrapping the entire `setContent` block. All composables below this point inherit the active theme (light or dark) via `MaterialTheme`. No individual composable applies its own theme — all color and typography decisions use `MaterialTheme.colorScheme` and `MaterialTheme.typography` tokens.

### String Resources

All user-facing labels (tab names, accessibility descriptions) are defined in `res/values/strings.xml`. No hardcoded strings appear in Kotlin files.

```xml
<!-- res/values/strings.xml — Home / Navigation -->
<string name="nav_dashboard">Dashboard</string>
<string name="nav_accounts">Accounts</string>
<string name="nav_transactions">Transactions</string>
<string name="nav_budgets">Budgets</string>
<string name="nav_goals">Goals</string>
<string name="settings">Settings</string>
<string name="app_name">Capital</string>
```

---

## Error Handling

The Home shell has no data operations and therefore no error states in Phase 1. The only failure scenario is the onboarding guard in `onCreate` — if `PreferencesDataSource` throws unexpectedly, the app will crash. This is acceptable in Phase 1: `SharedPreferences` access is synchronous and does not fail under normal conditions.

---

## Testing Strategy

### Unit Tests

| Class | What to test |
|---|---|
| `BottomNavigationBar` | Active tab shows filled icon and primary color. Inactive tabs show outlined icon and `onSurfaceVariant` color. No text label is rendered for any tab item. `onTabSelected` lambda is called with the correct `TopLevelDestination` on tap. |
| `AppHeader` | On Dashboard: "Capital" label is rendered in Borel font, centered; gear icon is present. On non-Dashboard tab: tab name label is rendered in Inter, centered; no gear icon is present. `onSettingsClick` lambda is invoked when gear icon is tapped (Dashboard only). |
| `TabPlaceholder` | Label string is rendered correctly. `BackHandler` is registered when `onNavigateToDashboard` is non-null. No `BackHandler` when `null` is passed. |

### Integration Tests

| Scope | What to test |
|---|---|
| `MainActivity` + `AppNavGraph` | Tapping each tab renders the correct placeholder (or real screen). Header label updates to match the active tab. Gear icon visible on Dashboard, hidden on all other tabs. Re-selecting active tab does not add to the back stack. Back press on non-Dashboard tab navigates to Dashboard. Back press on Dashboard tab exits the app. Back stack per tab is saved and restored on tab switch. Tapping gear icon navigates to Settings. |

### UI Tests

Deferred — consistent with the global testing strategy in `ARCHITECTURE.md`.

---

## Phase 2 Additions

- `onCreate` onboarding guard is extended with a Supabase session check. If `onboarding_completed = true` but no valid session exists, the user is navigated to the Login screen.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-06-05 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-06-05 | Danielle Mariani | Settings is Phase 1: gear icon navigates to Settings screen; `settingsNavGraph()` added to `AppNavGraph`; `onSettingsClick` lambda added to `AppHeader`. Replaced five individual placeholder composables with a single reusable `TabPlaceholder` accepting `@StringRes labelRes` and optional `onNavigateToDashboard` lambda. Removed "(Option A)" from Nested Nav Graphs section title. Renamed `CapitalTheme` to `AppTheme` throughout for app-name agnosticism. |
| 0.3.0 | 2026-06-08 | Danielle Mariani | Header redesign: replace static `Image` wordmark with context-sensitive `Text` composable. Dashboard tab renders "Capital" in `BorelFontFamily` at 26sp, centered. Non-Dashboard tabs render tab name using `MaterialTheme.typography.titleLarge` (Inter), centered. Gear icon rendered in `actions` slot on Dashboard only; `actions` slot empty on non-Dashboard tabs. Add `TopAppBarScrollBehavior` with `enterAlwaysScrollBehavior()` — header hides on scroll down, reappears on scroll up, all tabs. `scrollBehavior` created in `MainActivity`, passed to `AppHeader` and `Scaffold`. `Modifier.nestedScroll` applied to `Scaffold`. Add scroll behavior note re: Phase 1 placeholder screens. `TopLevelDestination` sealed class updated to include `labelRes` property. `BottomNavigationBar`: set `label = null` on all `NavigationBarItem`s (icon-only); remove `selectedTextColor` and `unselectedTextColor` from `NavigationBarItemDefaults.colors`. `AppHeader` receives `currentDestination: NavDestination?` parameter (replaces no context parameter). Architecture diagram updated. Component structure: `Header.kt` renamed to `AppHeader.kt`. Unit and integration test tables updated. |
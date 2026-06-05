# Home — Design

**Version:** 0.2.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-06-05
**Last Updated:** 2026-06-05

---

## Overview

This document defines the technical solution design for the Home feature on Android (Phase 1). It covers the `MainActivity` shell, the navigation architecture, the `AppNavGraph` nested graph composition, the `BottomNavigationBar` and `Header` composables, back navigation implementation, placeholder screen structure, dependency injection, and testing strategy.

This document is the implementation reference for the Home feature. All decisions here must be consistent with `specs/features/home/requirements.md` (functional requirements), `specs/design/design.md` (visual design system), and `navigation.md` (navigation flows and screen inventory). Where this document references design tokens, those tokens are defined in `specs/design/design.md` and mapped to Material 3 via the app's `MaterialTheme`.

---

## Related Documents

| Document | Purpose |
|---|---|
| specs/features/home/requirements.md | Functional requirements and acceptance criteria |
| specs/design/design.md | Design system — color tokens, typography, spacing, iconography |
| navigation.md | Navigation flows, app launch logic, screen inventory |
| ARCHITECTURE.md | Android stack, MVVM + Clean Architecture, package structure |

---

## Architecture Overview

Home runs entirely within `MainActivity`. Unlike `OnboardingActivity`, `MainActivity` is not self-contained — it is the permanent host for all post-onboarding feature screens. The shell provides the `Header`, the `BottomNavigationBar`, and the `MainNavHost`. All five tab destinations are registered in `AppNavGraph` as nested navigation graphs, each owned by its respective feature. Settings is also registered in `AppNavGraph` as a non-tab destination reachable from the header gear icon.

```
MainActivity
    ├── Header
    │       ├── App wordmark (left) — static, no action
    │       └── Gear icon (right) — navigates to Settings
    ├── BottomNavigationBar
    │       ├── Dashboard    (tab 1 — default)
    │       ├── Accounts     (tab 2)
    │       ├── Transactions (tab 3)
    │       ├── Budgets      (tab 4)
    │       └── Goals        (tab 5)
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
- The `BottomNavigationBar` and `Header` are rendered by `MainActivity` directly, not by any individual feature. Features do not control shell visibility at the tab root level.
- Sub-screens within each feature navigate into a full-screen destination that suppresses the shell (bottom nav and shell header not present). Sub-screen top bars are defined and owned per feature spec. See `navigation.md` — Global Navigation Patterns for the sub-screen top bar convention.
- No ViewModel is scoped to `MainActivity` in Phase 1. The shell has no business logic or data dependencies.
- `AppNavGraph` is the single place that imports and wires all feature nav graphs. Features never import each other.

---

## Component Structure

```
app/
├── MainActivity.kt                    # Activity shell, onboarding guard, NavHost host
├── AppNavGraph.kt                     # Composes all feature nested nav graphs
├── TopLevelDestination.kt             # Sealed class for the 5 tab routes
├── ui/
│   ├── BottomNavigationBar.kt         # Bottom nav composable
│   └── Header.kt                      # App wordmark + gear icon composable

feature/home/
├── ui/
│   └── placeholders/
│       └── TabPlaceholder.kt          # Single reusable placeholder composable (accepts label string resource)
```

**Notes:**

- `MainActivity.kt`, `AppNavGraph.kt`, `TopLevelDestination.kt`, `BottomNavigationBar.kt`, and `Header.kt` live in the `app/` package — they are app-level concerns, not feature-level.
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
sealed class TopLevelDestination(val route: String) {
    object Dashboard    : TopLevelDestination("dashboard")
    object Accounts     : TopLevelDestination("accounts")
    object Transactions : TopLevelDestination("transactions")
    object Budgets      : TopLevelDestination("budgets")
    object Goals        : TopLevelDestination("goals")
}
```

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
                label = { Text(stringResource(item.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
```

**Icon selection:** Material Symbols provides filled and outlined variants for all five icons. The `selectedIcon` uses the filled variant; `unselectedIcon` uses the outlined variant. Final icon identifiers are confirmed at implementation time.

**Active tab detection:** `currentDestination?.hierarchy` is used (not a simple route equality check) so that nested destinations within a tab (e.g. Account Detail) still highlight the correct tab in the bottom nav.

**Scroll-to-top on re-selection:** When the user taps the already-active tab, `onTabSelected` is called with the same destination. The nav graph's `launchSingleTop = true` option prevents a new back stack entry. The scroll-to-top behavior is triggered via a `LaunchedEffect` in the feature's root list Composable, which observes the `NavBackStackEntry` reselection. Implementation detail deferred to each feature spec.

---

## Header

```kotlin
// app/ui/Header.kt
@Composable
fun AppHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Image(
                painter = painterResource(id = R.drawable.ic_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.height(24.dp)
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
```

`onSettingsClick` is passed down from `MainActivity`, which calls `navController.navigate(SettingsDestination.route)`.

**Wordmark asset:** `R.drawable.ic_wordmark` — a vector or PNG logotype asset. Two variants are required: `res/drawable/ic_wordmark.xml` (light theme) and `res/drawable-night/ic_wordmark.xml` (dark theme). Android resolves the correct variant automatically based on the active system theme.

**Gear icon:** `Icons.Outlined.Settings` from Material Symbols. Navigates to the Settings screen. The `contentDescription` ensures accessibility compliance.

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

                Scaffold(
                    topBar = {
                        AppHeader(
                            onSettingsClick = {
                                navController.navigate(SettingsDestination.route)
                            }
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

Sub-screens (Account Detail, Budget Detail, creation forms, etc.) must be displayed full-screen with no bottom nav and no shell header. This is achieved by registering sub-screen destinations within the `NavHost` — they are full-screen composables that draw edge-to-edge and define their own `TopAppBar` (back arrow + screen title). The shell `Header` and `BottomNavigationBar` remain in the composition at the `Scaffold` level but are visually covered by the sub-screen.

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
| `BottomNavigationBar` | Active tab shows filled icon and primary color. Inactive tabs show outlined icon and `onSurfaceVariant` color. `onTabSelected` lambda is called with the correct `TopLevelDestination` on tap. |
| `AppHeader` | Wordmark image is rendered. Gear icon button is present. `onSettingsClick` lambda is invoked on gear icon tap. |
| `TabPlaceholder` | Label string is rendered correctly. `BackHandler` is registered when `onNavigateToDashboard` is non-null. No `BackHandler` when `null` is passed. |

### Integration Tests

| Scope | What to test |
|---|---|
| `MainActivity` + `AppNavGraph` | Tapping each tab renders the correct placeholder (or real screen). Re-selecting active tab does not add to the back stack. Back press on non-Dashboard tab navigates to Dashboard. Back press on Dashboard tab exits the app. Back stack per tab is saved and restored on tab switch. Tapping gear icon navigates to Settings. |

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
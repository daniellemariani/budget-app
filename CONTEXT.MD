# AI Session Context — Budget App

## Purpose
Paste this file at the start of any new Claude or Cursor session to restore project context. Update after each significant session.

## Last Updated
2026-05-31

## Current Status
- PRODUCT.md — complete v1.0.0
- SPEC.md — complete v1.0.4
- ARCHITECTURE.md — complete v1.0.2
- ROADMAP.md — complete v1.0.0
- specs/technical/data-model.md — complete v0.7.0
- specs/technical/offline-sync.md — complete v0.4.0
- specs/technical/api-contract.md — complete v0.5.0
- specs/design/navigation.md — complete v0.5.0
- specs/design/design.md — complete (design system tokens, typography, spacing, elevation, Material 3 mapping)
- specs/features/onboarding/requirements.md — complete v0.2.0
- specs/features/onboarding/design.md — complete v0.3.0
- specs/features/onboarding/tasks.md — complete v0.3.0
- All other feature specs — not started

## Key Technical Decisions Made

### Stack
- Android: Kotlin + Jetpack Compose + Hilt + Room + Coroutines/Flow
- Android Network (Phase 2): Retrofit + Kotlin Serialization + OkHttp Interceptor
- Android Image (Phase 2): Coil (`coil-compose`)
- Backend (Phase 2): Python 3.12+ + FastAPI (monolith) + SQLModel + Alembic + Uvicorn
- Auth + Database: Supabase (Auth + PostgreSQL)
- File Storage (Phase 2): Supabase Storage (user avatars)
- Web (Phase 3): React + TypeScript + TanStack Query + shadcn/ui + Tailwind + Vite
- Mobile Phase 4: Flutter or KMP — decision deferred to Phase 4

### Architecture
- Offline-first: Room is the single source of truth in Phase 1
- MVVM + Clean Architecture on Android
- ViewModel depends on use cases, not directly on Repository
- Each use case represents a single business operation
- Use cases are injected into ViewModels via Hilt
- Repositories are injected into use cases via Hilt
- Feature-based package structure (not layer-based)
- Each feature organized internally by layers: ui/, domain/, data/
- ViewModels live in ui/
- Use cases and Repository interface live in domain/
- Domain entities are pure Kotlin with no framework dependencies
- Room entities and network DTOs live in the data layer, not domain
- Mappers convert between domain models, Room entities, and DTOs at layer boundaries
- Business entity models live in core/domain/
- DAOs and Room database definition live in core/data/
- Feature data layer organized per source: local/ from Phase 1, remote/ added in Phase 2
- Repository implementation lives in feature/[name]/data/
- Hilt for dependency injection throughout
- Monorepo structure: android/ backend/ web/ specs/

### Android Package Structure
```
android/src/main/java/com/budgetapp/
├── core/
│   ├── ui/                            # shared Composables, theme, design tokens
│   ├── domain/                        # domain entities: Transaction.kt, Budget.kt, Account.kt, ...
│   └── data/
│       ├── AppDatabase.kt             # Room database definition
│       ├── TransactionDao.kt
│       ├── BudgetDao.kt
│       └── ...                        # one DAO per entity
├── feature/
│   ├── onboarding/
│   ├── dashboard/
│   ├── categories/
│   ├── accounts/
│   ├── transactions/
│   ├── merchants/
│   ├── transfers/
│   ├── budgets/
│   ├── goals/
│   └── ...
└── app/
    ├── MainActivity.kt
    └── AppNavGraph.kt
```

Each feature folder follows the structure:
```
feature/transactions/
├── ui/
│   ├── TransactionScreen.kt           # Composable UI
│   └── TransactionViewModel.kt        # UI state + events
├── domain/
│   ├── TransactionRepository.kt       # interface only
│   ├── CreateTransactionUseCase.kt
│   ├── UpdateTransactionUseCase.kt
│   ├── DeleteTransactionUseCase.kt
│   └── GetTransactionsUseCase.kt
└── data/
    ├── local/
    │   └── TransactionLocalDataSource.kt    # wraps DAO calls (Phase 1)
    ├── remote/
    │   └── TransactionRemoteDataSource.kt   # wraps Retrofit calls (Phase 2)
    └── TransactionRepositoryImpl.kt         # implements domain interface
```

Note: Feature Data package includes a `local/` directory from Phase 1. A `remote/` directory is added in Phase 2 when backend sync is introduced.

### Android Layer Diagram
```
UI (Composables)
    └── ViewModel              # UI state, user events
        └── UseCase            # single business operation
            └── Repository     # data access interface
                └── DAO        # Room DAOs, raw SQL queries
                    └── Room Database
```

### Domain Model Principles
- core/domain/ contains pure Kotlin domain entities — no framework dependencies, no annotations
- Room entities (@Entity) live in core/data/ — map to database columns
- Network DTOs live in feature/data/remote/ — mirror API response structure (Phase 2)
- Mappers convert between domain models, Room entities, and DTOs at layer boundaries
- UI state models (e.g. TransactionUiState) live in feature/ui/

### Backend Architecture
- Monolith — single FastAPI app exposing all APIs
- RESTful JSON API, versioned under /api/v1/
- API contract defined in specs/technical/api-contract.md, exposed at runtime via OpenAPI
- Connects to Supabase PostgreSQL via direct connection string (DATABASE_URL)
- Auth delegated to Supabase — FastAPI validates JWTs locally using SUPABASE_JWT_SECRET
- No network call required per JWT validation
- Hosting: Render or Railway — decision deferred to Phase 2 kickoff
- CI/CD: GitHub Actions

### Backend Package Structure
```
backend/
├── app/
│   ├── api/
│   │   └── v1/
│   │       ├── routes/          # One file per feature
│   │       └── router.py        # Aggregates all routes
│   ├── core/
│   │   ├── config.py            # Settings via pydantic-settings
│   │   ├── security.py          # JWT logic
│   │   └── database.py          # SQLModel engine + session
│   ├── models/                  # SQLModel table models
│   ├── schemas/                 # Pydantic request/response schemas
│   ├── services/                # Business logic layer
│   └── main.py                  # FastAPI app instantiation
├── alembic/                     # Migration scripts
├── tests/
└── pyproject.toml
```

### Web Architecture
- Feature-based folder structure mirroring Android
- TanStack Query for all API data fetching, caching, and invalidation
- No global client-side state manager unless a specific need arises
- Supabase JS SDK for auth, session management, JWT storage in localStorage
- JWT passed in Authorization header to FastAPI on every request
- Hosting: Vercel — strong candidate, decision deferred to Phase 3 kickoff

### Web Package Structure
```
web/src/
├── features/
│   ├── transactions/
│   │   ├── components/          # feature-specific components
│   │   ├── hooks/               # feature-specific hooks (useTransactions.ts)
│   │   ├── types.ts             # feature-specific TypeScript types
│   │   └── index.ts             # public API of the feature
│   └── ...
├── components/
│   └── ui/                      # shadcn/ui components (shared)
├── hooks/                       # shared hooks (useAuth.ts, useWorkspace.ts)
├── lib/
│   ├── api.ts                   # fetch wrapper, attaches JWT
│   ├── supabase.ts              # Supabase client instance
│   └── utils.ts
├── types/                       # shared TypeScript types
├── pages/                       # route-level components (one per route)
└── main.tsx
```

### Cross-Platform (Phase 4)
- Decision between Flutter and KMP deferred to Phase 4 kickoff
- Flutter: strong cross-platform rendering consistency, larger UI rewrite
- KMP: shared business logic only, native UI per platform
- Decision criteria: native look and feel priority, Dart vs Kotlin, community maturity, Phase 1 codebase reuse
- Introduces: iOS support, multi-workspace UI

### Workspace Strategy
- Workspace is the top-level data owner (not User)
- Default Workspace is created on first launch with a newly generated UUID — no hardcoded id
- Default Workspace name is `"Personal"` — set at creation time, not configurable in Phase 1
- The default Workspace is identified by being the first Workspace created, not by its id value
- workspace_id FK on all direct financial entities from Phase 1 — no migration needed later
- GoalContribution carries workspace_id for query performance consistency, even though it is reachable via Goal
- Multi-workspace UI introduced in Phase 4
- BR-WS-01 through BR-WS-05 define workspace rules

### Currency Strategy
- Level 1 multi-currency only (no exchange rate conversion)
- base_currency on Workspace (default: USD, set during Phase 2 onboarding)
- currency_code on Account (independent per account, defaults to base_currency)
- currency_code on Transaction (inherited from Account at creation, immutable)
- Currency picker deferred to Phase 2 onboarding
- Aggregated totals group by currency in Phase 1, no conversion

### Data Model Principles
- All primary keys are UUID v4 (TEXT), generated client-side at creation time
- UUID v4 chosen over ULID — ordering handled via created_at, no extra dependency needed
- All foreign keys are TEXT to match UUID primary keys
- All amounts stored as integers in cents (never floats)
- All dates stored as Unix timestamps (INTEGER) in UTC, displayed in device local time
- Soft delete on all entities (deleted_at timestamp)
- created_at and updated_at on all entities
- Transfers are immutable once created
- Transfers between different currency accounts not supported in Phase 1
- Derived values are never persisted: Account current balance, Goal progress, Budget spent amount, Spending variance, Net worth — all calculated at query time
- Unique constraint on (workspace_id, name) where deleted_at IS NULL for Account, Category, and Merchant

### Sync Strategy
Full strategy defined in specs/technical/offline-sync.md v0.3.0. Key decisions:

- All entities carry last_synced_at (nullable INTEGER) and sync_status (nullable TEXT ENUM) from Phase 1 to avoid future migrations
- Both fields are null in Phase 1, active in Phase 2
- In Phase 2, default value for sync_status on new records is PENDING
- sync_status ENUM values: PENDING, SYNCED, FAILED, CONFLICT
- Phase 1 records with sync_status IS NULL are treated as PENDING on first Phase 2 sync
- CONFLICT state requires explicit user resolution in the UI (Phase 2)
- FAILED records retried with exponential backoff (Phase 2)

**updated_at is client-authoritative:**
- updated_at represents when the user made the change (device clock, UTC)
- The server stores client-provided updated_at as-is on push — never overwrites it
- Conflict detection compares client updated_at values on both sides (same scale)
- Device clock skew is a known limitation, not corrected in Phase 2
- server_received_at is a backend-only field (not synced to clients) for audit, telemetry, and debugging

**Semantic equality check (pull flow):**
- Before flagging a conflict on a PENDING/FAILED/NULL record, business fields are compared
- Business fields = all fields except id, sync_status, last_synced_at, updated_at, deleted_at
- If all business fields are identical: mark SYNCED, reconcile timestamps to server values — no conflict surfaced
- If any business field differs: mark CONFLICT, surface to user

**Sync concurrency:**
- Exactly one sync cycle must be active at any given time per workspace
- Enforced via a Kotlin Mutex in the SyncOrchestrator, released via finally on completion, failure, or cancellation
- WorkManager uses ExistingPeriodicWorkPolicy.KEEP to prevent duplicate workers
- Background and connectivity-restoration triggers are dropped if a sync is already in flight
- Manual user refresh is queued as a single boolean follow-up flag if a sync is in flight

**Cancellation safety:**
- Sync writes to Room record-by-record — partial progress is always preserved on interruption
- Watermark only advances on full successful completion of a pull cycle
- Safe across graceful app close, process kill (OOM/force-stop), and WorkManager worker cancellation
- On next launch or trigger, sync resumes cleanly from the last valid watermark

**Sync triggers (Phase 2):**
- App foreground via DefaultLifecycleObserver.onStart()
- Manual pull-to-refresh (queued as follow-up if sync in flight)
- Periodic background sync via WorkManager (every 15–30 min, CONNECTED constraint)
- Connectivity restoration via ConnectivityManager.NetworkCallback

**Conflict resolution:**
- Server is the authoritative source for conflict resolution
- Last-write-wins (server) applied silently when local sync_status = SYNCED
- Explicit user resolution required when local record is PENDING/FAILED/NULL and business fields differ
- User chooses: keep local (re-queued as PENDING with force: true) or accept server (marked SYNCED)
- Soft-deleted records are synced so deletions propagate across devices
- Deleted vs updated conflicts surface to user with restore or confirm deletion choice

**Watermark approach:**
- Single watermark per workspace (not per entity), stored in SyncMetadata table keyed by workspace_id
- Client sends watermark as since query parameter on pull
- Server returns records where updated_at > since, including soft-deleted records
- Cursor-based pagination on pull (stable under concurrent inserts); offset pagination avoided
- Watermark only advanced after all pages of a pull cycle are successfully applied
- First full sync (no watermark): server returns all records for the workspace

**Batch processing:**
- Push: 100 records per request (default), split into sequential batches if exceeded
- Pull: 500 records per page (default)
- Batch processing is record-level, not batch-level — a failure on one record does not affect others
- No business sort order applied to sync payloads; dependency order only
- Push dependency order: Workspace → WorkspaceMember → Account → Category → Merchant → Goal → Transaction → Transfer → Budget → GoalContribution → RecurringTransaction

**SyncOrchestrator open question:**
- Per-entity sync status vs single workspace-level sync state — not yet decided
- Pragmatic middle ground: workspace-level status in UI, per-entity detail in a sync log or debug screen
- Decision deferred to Phase 2 feature spec (specs/features/sync/spec.md)

### Budget Philosophy
- Budgets are optional spending plans, not hard caps
- Transactions are never blocked by budget status
- Overspending is allowed and visible
- UI language: Planned / Spent / Remaining (not "Envelope")
- Spending Status = Budget - Spent (positive = underspent, negative = overspent)
- carry_forward flag (default: 1) on Budget causes the app to auto-generate next month's budget row on first access of a new period
- Auto-generated budget rows are independent — editing or deleting one does not affect other months
- If a month is skipped, all missing months are generated in sequence up to the current period

### Auth Flow (Phase 2+)
```
User → Android App or Web (Supabase SDK, gets JWT)
     → FastAPI (validates JWT using SUPABASE_JWT_SECRET)
     → PostgreSQL on Supabase (via direct connection string)
```
Note: The same JWT pattern applies in Phase 3.

### API Contract Decisions
Full contract defined in specs/technical/api-contract.md v0.4.1. Key decisions:

- Supabase owns identity (register, login, token refresh, logout via SDK); FastAPI owns workspace authorization
- X-Workspace-ID header required on all authenticated requests — workspace derived from header, not JWT claims; enables multi-workspace switching in Phase 4 without token exchange
- Offset-based pagination for standard list endpoints; cursor-based pagination for sync pull only
- Push payload is entity-keyed (arrays grouped by entity type); response is entity-keyed and outcome-keyed: accepted / conflicts / rejected arrays per entity type, plus a top-level summary
- sync_status and last_synced_at are never included in push payloads or pull responses — client-only fields
- invite_token and invite_expires_at on WorkspaceMember are server-only — never returned in any API response or sync payload
- Computed fields (Budget spending, Goal progress) are server-calculated on feature API responses; omitted from sync payloads; Android recalculates locally from Room using the same formulas
- Dashboard summary served by GET /api/v1/workspaces/{workspace_id}/summary (BFF endpoint for web); Android generates equivalent data locally from Room
- PATCH semantics: omit = unchanged, value = update, null = clear (nullable fields only)
- initial_balance is editable via PATCH Account; changes affect derived current balance and net worth immediately
- WorkspaceMember invite flow: backend-issued signed JWT token (7-day TTL); POST /workspace-members/invite → email → POST /workspace-members/accept; accept endpoint exempt from X-Workspace-ID validation

**Role Permission Matrix (Phase 2+):**
- VIEWER: read-only; sees account name/type/currency but not balances, credit limits, or net worth
- MEMBER: VIEWER + create/edit/delete transactions, transfers, budgets, goals, contributions, recurring transactions
- ADMIN: MEMBER + create/edit/delete accounts, categories, merchants, workspace settings, invite/manage members
- OWNER: ADMIN + ownership transfer; workspace must always have exactly one active OWNER (BR-WS-05)

**Client source of truth rules:**
- Android: Room first; feature APIs secondary; computed fields recalculated locally
- Web: feature APIs first; TanStack Query cache is ephemeral; computed fields from server response
- Flutter/KMP (Phase 4): expected to mirror Android — local DB first, sync engine for propagation

### Navigation Decisions
Navigation structure defined in specs/design/navigation.md v0.5.0. Key decisions:

- Android uses two activity contexts: OnboardingActivity (first launch only) and MainActivity (all post-onboarding screens)
- `onboarding_completed` boolean stored in SharedPreferences; read on every app launch via `PreferencesDataSource` to determine whether to show OnboardingActivity or go directly to MainActivity
- `onboarding_completed` set to true only when the user taps Continue on the Set Your Name screen — not on feature slide skip, not on Account Setup completion or skip
- `display_name` stored in SharedPreferences; written atomically with `onboarding_completed` on Set Your Name completion
- All SharedPreferences access is centralized through `PreferencesDataSource` (`core/data/`) — no direct SharedPreferences reads or writes anywhere in the app
- SharedPreferences key constants defined in `PreferenceKeys` object (`core/data/`) — no raw strings
- Clear Data wipes Room DB AND all SharedPreferences (including `onboarding_completed` and `display_name`) → relaunches OnboardingActivity
- Phase 2 launch logic adds a second gate: valid Supabase session check after `onboarding_completed = true`
- Bottom nav: Dashboard | Accounts | Transactions | Budgets | Goals (5 destinations, icon + label each)
- Header: app name left, gear icon (Settings) right — not shown during onboarding
- Workspace Management screen accessible via Manage button on Dashboard: Categories, Merchants, Currency (read-only Phase 1), Members (Phase 2)
- Settings is personal only: Display Name, About, Version, Clear Data
- Speed Dial FAB on Transactions (Add Transaction, Add Transfer) and Accounts (Add Checking, Add Savings, Add Cash, Add Credit Card)
- Standard FAB on Budgets (Add Budget) and Goals (Add Goal)
- Pin button on Account Detail, Budget Detail, Goal Detail — toggles is_pinned; pinned items sort first in their list screens by pinned_at ascending
- Detail screens for Account, Budget, Goal; direct edit form for Transaction, Transfer, GoalContribution, Category, Merchant
- Display Name stored in SharedPreferences (Phase 1); used for contextual greeting on Dashboard (Good morning/afternoon/evening, [Name])
- Web: left sidebar (collapsed = icons only, expanded = icons + labels); same 5 destinations; Settings at sidebar bottom
- Web onboarding: feature slides → Supabase registration (Create Account) → Account Setup → Dashboard

### Feature Spec Conventions

Each feature spec lives in `specs/features/[feature]/` and always contains exactly four files:

```
specs/features/[feature]/
├── requirements.md   # Functional requirements, business rules, ACs, error handling
├── design.md         # Technical solution design (components, ViewModel, DI, tokens)
└── tasks.md          # Implementation task breakdown with file paths and dependencies
```

**`requirements.md` structure:**
```
# [Feature] — Requirements
## Introduction
## Scope Boundaries (In Scope / Out of Scope)
## Requirements (RQ-[FEAT]-XX)
## Non-Functional Requirements
## Screens & UI
    ### [Screen Name]
        #### Fields & Layout
        #### Validation
        #### States
        #### UX Copy
## Edge Cases
## Business Rules (BR-[FEAT]-XX)
## Acceptance Criteria
    ### Happy Path
    ### Edge Cases
## Error Handling
## Dependencies
## Changelog
```

**`design.md` structure:**
```
# [Feature] — Design
## Overview
## Related Documents
## Architecture Overview
## Component Structure
## Constants
    ### SharedPreferences Keys (if applicable)
    ### String Resources
## Data Models
## Screen Specifications
    ### [Screen Name]
        #### Composable Breakdown
        #### ViewModel — State & Events
        #### UI States
        #### Design Tokens Applied
## Shared Components
## Navigation Implementation
## Dependency Injection
## Key Implementation Notes
## Error Handling
## Testing Strategy
## Changelog
```

**`tasks.md` structure:**
```
# [Feature] — Tasks
## Overview (group execution order table)
## Task Summary (all tasks: ID, title, group, phase, effort, status)
## Task Format
## Group 0 — Project Foundation (first feature only)
## Group N — [Group Name]
    TSK-[FEAT]-XX — [Title]
    - Effort: S / M / L
    - Phase: N
    - Group: [name]
    - Requirements: RQ-[FEAT]-XX
    - Acceptance Criteria: AC-[FEAT]-XX
    - Status: Not Started / Done
    - Depends on: TSK-[FEAT]-XX / None
    - Creates: [full file paths]
    - Details: [implementation guidance]
## Changelog
```

**ID conventions:**
- Requirements: `RQ-[FEAT]-NN` (e.g. `RQ-ON-01`)
- Business Rules: `BR-[FEAT]-NN` (e.g. `BR-ON-01`)
- Acceptance Criteria: `AC-[FEAT]-NN` (e.g. `AC-ON-01`) — no HP/EC suffix in ID; Happy Path and Edge Cases are section headers only
- Tasks: `TSK-[FEAT]-NN` (e.g. `TSK-ON-01`)
- IDs are never renumbered once assigned

**Key principles:**
- `requirements.md` defines *what* — platform-agnostic functional behavior
- `design.md` defines *how* — Android implementation, file paths, ViewModel/DI wiring, design tokens applied per screen
- `tasks.md` defines *when and what to create* — granular, ordered, with exact file paths to prevent AI improvisation
- Feature BRs reference global BRs by ID only — never copy global rule text
- All UX copy lives in `strings.xml` — no hardcoded strings in Kotlin files
- All SharedPreferences keys defined as constants in `PreferenceKeys` (`core/data/`)
- All SharedPreferences access goes through `PreferencesDataSource` (`core/data/`)
- Design tokens in `design.md` reference `specs/design/design.md` — never duplicated inline
| Platform | Unit                     | Integration                        | UI       |
|----------|--------------------------|------------------------------------|----------|
| Android  | JUnit + Hilt test runner | Room in-memory database tests      | Deferred |
| Backend  | pytest                   | FastAPI TestClient + test database | N/A      |
| Web      | Vitest                   | TanStack Query mock handlers       | Deferred |

Test coverage targets and CI configuration defined at Phase 2 kickoff.

### Naming Conventions
- Business rules: BR-[SECTION]-[NUMBER] in SPEC.md
- Feature rules: BR-[FEATURE]-[NUMBER] in feature specs
- Global rules referenced in feature specs by ID only, never copied
- Entities: singular PascalCase (Transaction, not Transactions)
- Feature folders: lowercase hyphenated (recurring-transactions/)

### Git Strategy
- Conventional Commits format
- Feature branches: feature/xxx
- Spec changes and code changes in separate commits
- Never renumber BR or NFR IDs once assigned

### Cursor Rules
- Rules live in .cursor/rules/ as .mdc files (not deprecated .cursorrules)
- core.mdc — alwaysApply: true
- android.mdc — globs: android/**/*.kt
- fastapi.mdc — globs: backend/**/*.py
- react.mdc — globs: web/**/*.tsx
- spec-workflow.mdc — agent-requested

## Open Technical Decisions
- Backend hosting: Render or Railway — decision at Phase 2 kickoff
- Web hosting: Vercel is strong candidate — decision at Phase 3 kickoff
- Remote Config: custom Supabase config table vs third-party (e.g. Firebase Remote Config) — decision TBD post Phase 4
- Local database sync scope: Phase 1 and 2 use full local mirroring of all user data. For long-term scaling, consider windowed sync (e.g. last 12 months on device, older data fetched on demand). Revisit at Phase 2 kickoff.
- Pull page size: configurable from start vs hardcoded in Phase 2 — decision at Phase 2 kickoff
- Permanently failed records (after max retries): require manual user action to reset, or auto-reset on app restart — decision at Phase 2 kickoff
- Soft-delete purge retention threshold — define at Phase 2 kickoff (Permanent Cleanup Policy)
- SyncOrchestrator granularity: workspace-level vs per-entity sync status — decision at Phase 2 feature spec (specs/features/sync/spec.md)
- Conflict resolution UI: inline on record detail screen vs dedicated conflict queue screen — decision at Phase 2 feature spec (specs/features/sync/spec.md)
- Transactional email provider for workspace invites: Resend or SendGrid — decision at Phase 2 kickoff
- Certificate pinning on Android (OkHttp): implement from Phase 2 start or defer — decision at Phase 2 kickoff
- X-Workspace-ID multi-workspace switching: confirm header approach or evaluate URL prefix (/workspaces/{id}/) at Phase 4 kickoff
- Dashboard summary endpoint content: validate against UI mockups once specs/features/dashboard/spec.md is written
- Goal cascade on delete: soft-delete goal cascades to contributions or leaves them orphaned — decision before Phase 2 implementation
- Timezone handling for period filter presets: currently resolved in UTC; consider Workspace.timezone field (IANA) before Phase 3 web implementation
- Budget spending on list endpoint: computing spending totals for every budget row may be expensive at scale — revisit with benchmarks at Phase 2 implementation
- Transactions month label — month picker: tapping month label to open fast-navigation picker is TBD — decision at Phase 1 implementation
- Full-screen forms — bottom nav visibility: suppress bottom nav when form is open? Decision per feature spec
- Account Detail — transaction history scope: all-time or rolling window — decision at Phase 1 implementation
- App name: placeholder [App Name] used throughout — decide before Phase 1 build
- Category/Merchant quick-add during transaction entry: inline creation without navigating to Workspace Management — decision at Transactions feature spec
- Web onboarding full spec: sketched in specs/design/navigation.md; full spec at Phase 3 kickoff

## Open Questions (from PRODUCT.md)
- Free, freemium, paid, or ad-supported for public launch?
- Receipt scanning for public launch?
- Spanish language support?
- Product analytics strategy?

## Phase Roadmap
- Phase 1: Android offline, no backend, no auth
- Phase 2: FastAPI backend, Supabase (Auth + PostgreSQL), sync, recurring transactions
- Phase 3: React web dashboard + reports, hosted on Vercel (TBD)
- Phase 4: Flutter or KMP cross-platform mobile, iOS support, multi-workspace UI

## Roadmap Summary

### Phase 1 — Android Offline (Current)
- [ ] Project setup (monorepo, Gradle, Hilt, Room, base package structure)
- [ ] onboarding/ — see specs/features/onboarding/tasks.md
- [ ] accounts/ — see specs/features/accounts/tasks.md
- [ ] categories/ — see specs/features/categories/tasks.md
- [ ] merchants/ — see specs/features/merchants/tasks.md
- [ ] transactions/ — see specs/features/transactions/tasks.md
- [ ] transfers/ — see specs/features/transfers/tasks.md
- [ ] budgets/ — see specs/features/budgets/tasks.md
- [ ] goals/ — see specs/features/goals/tasks.md
- [ ] dashboard/ — see specs/features/dashboard/tasks.md

### Phase 2 — Backend + Sync
- [ ] Project setup (FastAPI, Supabase, Alembic, GitHub Actions)
- [ ] auth/ — see specs/features/auth/tasks.md
- [ ] sync/ — see specs/features/sync/tasks.md
- [ ] recurring-transactions/ — see specs/features/recurring-transactions/tasks.md
- [ ] onboarding/ — see specs/features/onboarding/tasks.md
- [ ] accounts/ — see specs/features/accounts/tasks.md
- [ ] categories/ — see specs/features/categories/tasks.md
- [ ] merchants/ — see specs/features/merchants/tasks.md
- [ ] transactions/ — see specs/features/transactions/tasks.md
- [ ] transfers/ — see specs/features/transfers/tasks.md
- [ ] budgets/ — see specs/features/budgets/tasks.md
- [ ] goals/ — see specs/features/goals/tasks.md
- [ ] dashboard/ — see specs/features/dashboard/tasks.md

### Phase 3 — Web
- [ ] Project setup (React, Vite, TanStack Query, shadcn/ui, Vercel)
- [ ] onboarding/ — see specs/features/onboarding/tasks.md
- [ ] accounts/ — see specs/features/accounts/tasks.md
- [ ] categories/ — see specs/features/categories/tasks.md
- [ ] merchants/ — see specs/features/merchants/tasks.md
- [ ] transactions/ — see specs/features/transactions/tasks.md
- [ ] transfers/ — see specs/features/transfers/tasks.md
- [ ] budgets/ — see specs/features/budgets/tasks.md
- [ ] goals/ — see specs/features/goals/tasks.md
- [ ] dashboard/ — see specs/features/dashboard/tasks.md
- [ ] web-dashboard/ — see specs/features/web-dashboard/tasks.md
- [ ] reports/ — see specs/features/reports/tasks.md

### Phase 4 — Cross-Platform Mobile
- [ ] Flutter or KMP decision
- [ ] Project setup (TBD based on Flutter or KMP decision)
- [ ] Migration of Phase 1 Android features to cross-platform
- [ ] Multi-workspace UI

## Next Session
Start a new chat. Paste CONTEXT.md to restore full context.

**Current state:** Onboarding feature spec is complete (requirements.md v0.2.0, design.md v0.3.0, tasks.md v0.3.0). All cross-file updates from the onboarding session are applied (navigation.md v0.5.0, SPEC.md v1.0.4, ARCHITECTURE.md v1.0.2).

**Immediate next steps:**
1. Begin Accounts feature spec — recommended next feature after Onboarding
   - `specs/features/accounts/requirements.md`
   - `specs/features/accounts/design.md`
   - `specs/features/accounts/tasks.md`

**Remaining feature spec order:**
- Accounts → Categories → Merchants → Transactions → Transfers → Budgets → Goals → Dashboard

**When ready to code:**
- All onboarding tasks are defined in `specs/features/onboarding/tasks.md` v0.3.0
- Start from TSK-ON-01 (Project Foundation) and work through groups in order
- App ID `com.dmariani.budgetapp` is temporary — decide final identifier before Phase 1 build
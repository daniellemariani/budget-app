# AI Session Context — Budget App

## Purpose
Paste this file at the start of any new Claude or Cursor session to restore project context. Update after each significant session.

## Last Updated
2026-05-12

## Current Status
- PRODUCT.md — complete v1.0.0
- SPEC.md — complete v1.0.1
- ARCHITECTURE.md — complete v1.0.1
- ROADMAP.md — complete v1.0.0
- specs/technical/data-model.md — complete v0.5.0
- specs/technical/offline-sync.md — next to write
- specs/technical/api-contract.md — not started (depends on offline-sync.md)
- Feature specs — not started

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
- All entities carry last_synced_at (nullable INTEGER) and sync_status (nullable TEXT ENUM) from Phase 1 to avoid future migrations
- Both fields are null in Phase 1, active in Phase 2
- In Phase 2, default value for sync_status on new records is PENDING
- sync_status ENUM values: PENDING, SYNCED, FAILED, CONFLICT
- CONFLICT state requires explicit user resolution in the UI (Phase 2)
- FAILED records retried with exponential backoff (Phase 2)

**Sync triggers (Phase 2):**
- App foreground (connectivity check on resume)
- Manual pull-to-refresh
- Periodic background sync via WorkManager

**Conflict resolution (Phase 2):**
- Server is the authoritative source for conflict resolution
- Soft-deleted records are synced so deletions propagate across devices

**Watermark approach (Phase 2):**
- Client tracks last_synced_at per entity
- Records where updated_at > last_synced_at have pending local changes
- Server returns all records modified after the client's last_synced_at watermark

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

### Testing Strategy
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
- Local database sync scope: Phase 1 and 2 use full local mirroring of all user data. For long-term scaling, consider windowed sync (e.g. last 12 months on device, older data fetched on demand) to reduce initial sync time and device storage on new installs. Revisit at Phase 2 kickoff based on data volume projections and target device specs.

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
- Write specs/technical/offline-sync.md
- Cover: sync triggers, conflict resolution strategy, watermark approach, failure handling, CONFLICT state user resolution flow
- Reference data-model.md v0.5.0 for last_synced_at and sync_status field definitions
- Reference ARCHITECTURE.md sync strategy section as starting point
- api-contract.md to follow after offline-sync.md is complete — sync endpoints depend on sync strategy decisions
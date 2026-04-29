# ARCHITECTURE.md — Budget App

**Version:** 1.0.0
**Status:** In Progress
**Owner:** Danielle Mariani
**Created at:** 2026-04-23
**Last Updated:** 2026-04-28

## Overview

Budget App is a personal finance management tool built offline-first for Android, with a backend sync layer and web dashboard introduced incrementally across four phases. The primary architectural constraint is offline-first: Room is the single source of truth in Phase 1 and all features must function without network access. Backend, web, and cross-platform layers are introduced in later phases to avoid over-engineering before the core product is validated. This document describes the technical architecture, stack decisions, and cross-cutting concerns across all four phases.

## Repository Structure

Monorepo layout. All platforms and shared specs live in a single repository.

```
budget-app/
├── android/          # Kotlin + Jetpack Compose app (Phase 1)
├── backend/          # Python + FastAPI server (Phase 2)
├── web/              # React + TypeScript dashboard (Phase 3)
├── specs/
│   ├── features/     # One subfolder per feature (e.g. specs/features/transactions/)
│   └── technical/    # data-model.md, api-contract.md, offline-sync.md
├── .cursor/
│   └── rules/        # Cursor AI rules (.mdc files)
├── PRODUCT.md
├── SPEC.md
├── ARCHITECTURE.md
└── ROADMAP.md
```

## Phase Overview


| Phase | Platform              | Backend              | Status      |
| ----- | --------------------- | -------------------- | ----------- |
| 1     | Android offline       | None                 | In Progress |
| 2     | Android + sync        | FastAPI + PostgreSQL | Not Started |
| 3     | Web dashboard         | FastAPI + PostgreSQL | Not Started |
| 4     | Cross-platform mobile | FastAPI + PostgreSQL | Not Started |


---

## Android Architecture

### Stack


| Layer                | Technology                                                    |
| -------------------- | ------------------------------------------------------------- |
| Language             | Kotlin                                                        |
| UI                   | Jetpack Compose                                               |
| Architecture         | MVVM + Repository                                             |
| Dependency Injection | Hilt                                                          |
| Local Database       | Room                                                          |
| Async                | Coroutines + Flow                                             |
| Build                | Gradle (Kotlin DSL)                                           |
| Network              | Retrofit + Kotlin Serialization + OkHttp Interceptor (Phase 2) |
| Image                | Coil (Phase 2)                                                |


### Architecture

MVVM with a Repository layer separating the ViewModel from data access.

```
UI (Composables)
    └── ViewModel          # UI state, user events, business logic coordination
        └── Repository     # Data access interface, abstracts Room
            └── DAO        # Room DAOs, raw SQL queries
                └── Room Database
```

- ViewModels are scoped to composable destinations via `hiltViewModel()`
- Repositories are injected into ViewModels via Hilt
- DAOs are injected into Repositories via Hilt
- All data access is asynchronous using Coroutines and Flow
- The UI layer observes StateFlow from the ViewModel and never accesses the Repository directly

### Package Structure

Feature-based. Each feature is organized layer-based internally. Each feature owns its own UI, ViewModel, Repository, and DAO files.

```
android/src/main/java/com/budgetapp/
├── core/
│   ├── ui/                            # shared Composables, theme, design tokens
│   ├── domain/                        # Transaction.kt, Budget.kt, Account.kt, ...
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
    └── AppNavGraph.kt    # Navigation graph
```

Each feature folder follows the structure:

```
feature/transactions/
├── ui/
│   ├── TransactionScreen.kt           # Composable UI
│   └── TransactionViewModel.kt        # UI state + events
├── domain/
│   └── TransactionRepository.kt       # interface only
└── data/
    └── TransactionRepositoryImpl.kt   # implements domain interface
```

### Local Database

Room is the single source of truth in Phase 1. All data lives on-device.

- Database name: `budget_app.db`
- All entities include `created_at`, `updated_at`, and `deleted_at` (soft delete)
- Amounts stored as `INTEGER` (cents), never `REAL`
- Dates stored as `INTEGER` (Unix timestamp, UTC)
- `workspace_id` foreign key present on all financial entities from Phase 1
- Full schema: `specs/technical/data-model.md`

### Navigation

Jetpack Compose Navigation with a single `NavHost` defined in `AppNavGraph.kt`. All destinations are defined as a sealed class or object hierarchy.

### Dependency Injection

Hilt provides the DI graph throughout. Modules are defined per layer:

- `DatabaseModule` — provides Room DB and all DAOs
- `RepositoryModule` — binds Repository interfaces to implementations
- Feature-level modules where needed

### Offline-First Guarantee

Phase 1 has no network dependency of any kind. No feature may depend on network availability (NFR-OF-01, NFR-OF-02). Local database backup is included in Android Auto Backup (NFR-DS-02).

---

## Backend Architecture (Python + FastAPI)

### Stack


| Layer        | Technology                      |
| ------------ | ------------------------------- |
| Language     | Python 3.12+                    |
| Framework    | FastAPI                         |
| ORM          | SQLModel                        |
| Database     | PostgreSQL (hosted on Supabase) |
| Migrations   | Alembic                         |
| Auth         | JWT (via Supabase)              |
| Runtime      | Uvicorn                         |
| Hosting      | TBD                             |
| CICD         | GitHub Actions                  |
| File Storage | Supabase Storage                |


### Architecture

The backend is a Python monolith built with FastAPI, exposing all APIs consumed by the Android app (Phase 2) and the web dashboard (Phase 3). It connects directly to a PostgreSQL database hosted on Supabase via a connection string, keeping the application code database-agnostic. Authentication is delegated to Supabase Auth — FastAPI validates Supabase-issued JWTs locally on every request using `SUPABASE_JWT_SECRET`, with no network call required per validation. The API contract is defined in `specs/technical/api-contract.md` and exposed at runtime via OpenAPI.

```
[ Android App (Kotlin · MVVM · Compose) ]
            |
            |  HTTPS
            v
[ Backend API (FastAPI) ]
            |
            |  SQL
            v
[ Supabase Postgres (PostgreSQL · Auth) ]
            |
            +--> Auth (Supabase)
```

Note: This diagram only reflects High-Level Architecture for Phase 2 (Android + Backend).

### API Design (ref to `api-contract.md`)

RESTful JSON API. Endpoint definitions: `specs/technical/api-contract.md`.

- Endpoints names are lowercase hyphenated
- All endpoints are versioned under /api/v1/
- Auth endpoints under /api/v1/auth/
- Financial data endpoints scoped by workspace_id in the URL or request body
- Pagination, filtering, and sorting defined per feature in the API contract

### Auth (Supabase)

Supabase JWT secret is used by FastAPI to validate tokens issued by Supabase Auth. Stored as an environment variable (`SUPABASE_JWT_SECRET`). FastAPI uses this to verify the JWT on every incoming request without calling Supabase on each request, validation happens locally.

Android stores tokens in `EncryptedSharedPreferences`. Phase 1 data is migrated to the default Workspace on first authenticated sync.

Full Auth + database flow across all layers:

```
User → Android App (Supabase SDK, gets JWT)
     → FastAPI (validates JWT using SUPABASE_JWT_SECRET)
     → PostgreSQL on Supabase (via direct connection string)
```
Note: The same JWT pattern applies in Phase 3.

### Database Integration (Supabase)

FastAPI connects to Supabase's PostgreSQL database directly via a connection string. SQLModel handles the ORM, Alembic handles migrations. Supabase just hosts the database. The goal is to keep FastAPI code database-agnostic.

Database connection string includes host, port, database name, user, and password. Provided by Supabase in the project dashboard. Stored as an environment variable (`DATABASE_URL`).

### Sync Strategy (ref to `offline-sync.md`)

Bidirectional sync between the Android client and the backend. Full strategy and conflict resolution: `specs/technical/offline-sync.md`.

High-level approach:

- Client tracks a `last_synced_at` watermark per entity type
- Sync is triggered on app foreground when connectivity is available
- Server is the authoritative source for conflict resolution
- Soft-deleted records are synced so deletions propagate across devices

### Package Structure

```
backend/
├── app/
│   ├── api/
│   │   └── v1/
│   │       ├── routes/          # One file per feature (transactions.py, budgets.py, ...)
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

---

## Web Architecture (React)

### Tech Stack


| Layer         | Technology      |
| ------------- | --------------- |
| Language      | TypeScript      |
| Framework     | React           |
| Server State  | TanStack Query  |
| UI Components | shadcn/ui       |
| Styling       | Tailwind CSS    |
| Build         | Vite            |
| Auth          | Supabase JS SDK |
| Hosting       | TBD             |


### Architecture

- Feature-based folder structure mirroring the Android side
- TanStack Query handles all API data fetching, caching, and invalidation
- No global client-side state manager (Redux/Zustand) unless a specific need arises
- shadcn/ui components are customized via Tailwind and kept in `web/components/ui/`
- JWT passed in Authorization header to FastAPI on every request

### Package Structure

```
web/src/
├── features/
│   ├── transactions/
│   │   ├── components/          # feature-specific components
│   │   │   ├── TransactionList.tsx
│   │   │   └── TransactionForm.tsx
│   │   ├── hooks/               # feature-specific hooks (useTransactions.ts)
│   │   ├── types.ts             # feature-specific TypeScript types
│   │   └── index.ts             # public API of the feature
│   ├── budgets/
│   └── ...
├── components/
│   └── ui/                      # shadcn/ui components (shared)
├── hooks/                       # shared hooks (useAuth.ts, useWorkspace.ts)
├── lib/
│   ├── api.ts                   # Axios or fetch wrapper, attaches JWT
│   ├── supabase.ts              # Supabase client instance
│   └── utils.ts
├── types/                       # shared TypeScript types
├── pages/                       # route-level components (one per route)
│   ├── DashboardPage.tsx
│   ├── TransactionsPage.tsx
│   └── ...
└── main.tsx
```

---

## Cross-Platform Architecture (Flutter/KMP)

The decision between `Flutter` and `Kotlin Multiplatform (KMP)` is deferred to Phase 4. Key considerations at decision time:

- Flutter: larger UI rewrite, strong cross-platform rendering consistency
- KMP: shared business logic only, native UI per platform

iOS and Multi-workspace UI are introduced in Phase 4.

Decision Criteria:

- Native look and feel priority
- Dart vs Kotlin
- Community maturity
- Phase 1 codebase reuse

---

## Cross-Cutting Concerns

### Data Integrity

Enforced at the database layer across all platforms:

- **Soft delete** — all entities use a `deleted_at` timestamp. Hard deletes are never performed (BR-DI-01).
- **Audit timestamps** — `created_at` and `updated_at` on every entity (BR-DI-02).
- **Amounts as integers** — all monetary values stored in cents to avoid floating-point precision errors (BR-DI-03).
- **UTC dates** — all timestamps stored in UTC, converted to device local time in the UI layer (BR-DI-04).

### Workspace Isolation

- `workspace_id` is a foreign key on every financial entity from Phase 1 forward (BR-WS-01).
- A default Workspace (`id: 1, name: "default"`) is seeded on first launch and is not exposed in the UI in Phase 1 (BR-WS-02).
- This design avoids a migration when multi-workspace support is introduced in Phase 4.

### Currency

- Level 1 multi-currency: multiple currencies per workspace, no conversion performed (BR-CU-04).
- `base_currency` on Workspace (ISO 4217 code, defaults to `USD`).
- `currency_code` on Account (defaults to `base_currency`, overridable per account).
- `currency_code` on Transaction (inherited from Account at creation, immutable after creation — BR-CU-03).
- Aggregated totals group by currency; cross-currency conversion is out of scope for all phases.
- Currency picker introduced in Phase 2 onboarding (BR-CU-01).

### Error Handling

- Android: errors surfaced to the UI via sealed `UiState` classes (Loading, Success, Error). No silent failures.
- Backend: consistent error response schema across all endpoints. HTTP status codes used semantically.
- Web: TanStack Query error states handled per feature; no global error boundary swallowing errors silently.

### Security

- Phase 1: no data leaves the device (NFR-DS-01). No analytics, no crash reporting, no remote logging.
- Phase 2+: HTTPS only. Tokens stored in `EncryptedSharedPreferences` on Android. No secrets in source code. User identity stored in Supabase Auth.
- Financial data never transmitted to third parties in any phase.

### Testing Strategy


| Platform | Unit                     | Integration                        | UI       |
| -------- | ------------------------ | ---------------------------------- | -------- |
| Android  | JUnit + Hilt test runner | Room in-memory database tests       | Deferred |
| Backend  | pytest                   | FastAPI TestClient + test database | N/A      |
| Web      | Vitest                   | TanStack Query mock handlers       | Deferred |


Test coverage targets and CI configuration defined at Phase 2 kickoff.

---

## Open Technical Decisions

- Use Render or Railway to host the backend
- Use a database table for Configurations/Feature Flagging or identify a third-party solution to manage Remote Config (e.g. Firebase Remote Config)
- Determine hosting platform for the Web solution. Vercel is a strong candidate.

## Related Documents


| Document                        | Purpose                                        |
| ------------------------------- | ---------------------------------------------- |
| PRODUCT.md                      | Vision, users, success criteria                |
| SPEC.md                         | Feature index, glossary, global business rules |
| ROADMAP.md                      | Phase timeline and progress                    |
| specs/technical/data-model.md   | Full database schema                           |
| specs/technical/api-contract.md | API endpoint definitions                       |
| specs/technical/offline-sync.md | Sync strategy and conflict resolution          |



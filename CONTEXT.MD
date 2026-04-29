# AI Session Context — Budget App

## Purpose
Paste this file at the start of any new Claude or Cursor session to restore project context. Update after each significant session.

## Last Updated
2026-04-28

## Current Status
- PRODUCT.md — complete v1.0.0
- SPEC.md — complete v1.0.0
- ARCHITECTURE.md — complete v1.0.0
- ROADMAP.md — complete v1.0.0
- specs/technical/data-model.md — next to write
- specs/technical/api-contract.md — not started
- specs/technical/offline-sync.md — not started
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
- MVVM + Repository pattern on Android
- Feature-based package structure (not layer-based)
- Each feature organized internally by layers: ui/, domain/, data/
- ViewModels live in ui/
- Business entity models live in core/domain/
- DAOs and Room database definition live in core/data/
- Repository interface lives in feature/[name]/domain/
- Repository implementation lives in feature/[name]/data/
- Hilt for dependency injection throughout
- Monorepo structure: android/ backend/ web/ specs/

### Android Package Structure
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
    └── AppNavGraph.kt
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
- Default Workspace (id: 1, name: "default") seeded on first launch, transparent in Phase 1
- workspace_id FK on all entities from Phase 1 — no migration needed later
- Multi-workspace UI introduced in Phase 4
- BR-WS-01 through BR-WS-04 define workspace rules

### Currency Strategy
- Level 1 multi-currency only (no exchange rate conversion)
- base_currency on Workspace (default: USD, set during Phase 2 onboarding)
- currency_code on Account (independent per account, defaults to base_currency)
- currency_code on Transaction (inherited from Account at creation, immutable)
- Currency picker deferred to Phase 2 onboarding
- Aggregated totals group by currency in Phase 1, no conversion

### Data Model Principles
- All amounts stored as integers in cents (never floats)
- All dates stored in UTC, displayed in device local time
- Soft delete on all entities (deleted_at timestamp)
- created_at and updated_at on all entities
- Transfers are immutable once created
- Transfers between different currency accounts not supported in Phase 1

### Auth Flow (Phase 2+)
```
User → Android App or Web (Supabase SDK, gets JWT)
     → FastAPI (validates JWT using SUPABASE_JWT_SECRET)
     → PostgreSQL on Supabase (via direct connection string)
```

### Testing Strategy
| Platform | Unit                     | Integration                        | UI       |
|----------|--------------------------|------------------------------------|----------|
| Android  | JUnit + Hilt test runner | Room in-memory database tests      | Deferred |
| Backend  | pytest                   | FastAPI TestClient + test database | N/A      |
| Web      | Vitest                   | TanStack Query mock handlers       | Deferred |

Test coverage targets and CI configuration defined at Phase 2 kickoff.

### Budget Philosophy
- Budgets are optional spending plans, not hard caps
- Transactions are never blocked by budget status
- Overspending is allowed and visible
- UI language: Planned / Spent / Remaining (not "Envelope")
- Spending Status = Budget - Spent (positive = underspent, negative = overspent)

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
- Write specs/technical/data-model.md
- Full database schema for all Phase 1 entities
- Reference SPEC.md Data Model Summary and Global Business Rules as the source of truth
- Entities to cover: Workspace, Account, Category, Transaction, Budget, Merchant, Transfer, Goal, GoalContribution
- Phase 2 entities to stub: User, RecurringTransaction
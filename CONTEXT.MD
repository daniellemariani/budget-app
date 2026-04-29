# AI Session Context — Budget App

## Purpose
Paste this file at the start of any new Claude or Cursor session to restore project context. Update after each significant session.

## Last Updated
2026-04-23

## Current Status
- PRODUCT.md — complete v0.1.0
- SPEC.md — complete v0.1.0
- ARCHITECTURE.md — next to write
- specs/technical/data-model.md — not started
- Feature specs — not started

## Key Technical Decisions Made

### Stack
- Android: Kotlin + Jetpack Compose + Hilt + Room + Coroutines/Flow
- Backend (Phase 2): Python + FastAPI + PostgreSQL + SQLModel + Alembic
- Web (Phase 3): React + TypeScript + TanStack Query + shadcn/ui + Tailwind
- Mobile Phase 4: Flutter or KMP — decision deferred to Phase 4

### Architecture
- Offline-first: Room is the single source of truth in Phase 1
- MVVM + Repository pattern on Android
- Feature-based package structure (not layer-based)
- Hilt for dependency injection throughout
- Monorepo structure: android/ backend/ web/ shared/ specs/

### Workspace Strategy
- Workspace is the top-level data owner (not User)
- Default Workspace (id: 1) seeded on first launch, transparent in Phase 1
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

## Open Questions (from PRODUCT.md)
- Free, freemium, paid, or ad-supported for public launch?
- Receipt scanning for public launch?
- Spanish language support?
- Product analytics strategy?

## Phase Roadmap
- Phase 1: Android offline, no backend, no auth
- Phase 2: FastAPI backend, PostgreSQL, auth, sync, recurring transactions
- Phase 3: React web dashboard + reports
- Phase 4: Flutter or KMP cross-platform mobile, multi-workspace UI
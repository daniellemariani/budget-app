# Capital — CONTEXT.md

Session continuity and handoff document. Read this first at the start of every session. Update it at the end of every session before closing.

---

## Project Overview

**App:** Capital (`com.dmariani.capital`) — personal finance Android app
**Slogan:** Every expense. Every goal. One place.
**Positioning:** Calm, premium financial management (comparable to Copilot Money, Monarch Money)
**Owner:** Danielle Mariani — sole developer and product owner
**Methodology:** Spec-Driven Development (SDD) — all architectural and product decisions are documented before implementation begins

---

## Tech Stack by Phase

| Phase | Platform | Stack |
|---|---|---|
| 1 (active) | Android | Kotlin, Jetpack Compose, Hilt, Room, Coroutines/Flow |
| 2 | Backend | FastAPI + Supabase (PostgreSQL, auth) |
| 3 | Web | React, TypeScript, TanStack Query, shadcn/ui, Tailwind |
| 4 | Cross-platform | Flutter or KMP (deferred) |

---

## Spec Document Versions

### Root / Cross-Cutting

| File | Version | Notes |
|---|---|---|
| `SPEC.md` | 1.0.6 | Canonical app definition, glossary, global business rules |
| `ARCHITECTURE.md` | 1.0.3 | Package structure, navigation architecture, DI patterns |
| `design.md` | 0.4.0 | Design system — tokens, typography, components incl. CategoryPill |
| `navigation.md` | 0.7.0 | App structure, launch logic, nav diagram, sub-screen pattern |

### Technical Specs (`specs/technical/`)

| File | Version | Notes |
|---|---|---|
| `data-model.md` | 0.8.0 | Category now has `color TEXT NOT NULL` |
| `api-contract.md` | 0.6.0 | Category payloads carry `color`; dashboard summary carries `category_color` |
| `offline-sync.md` | 0.4.0 | Stable — no changes this session |

### Feature Specs

| Feature | requirements.md | design.md | tasks.md | Status |
|---|---|---|---|---|
| Onboarding | 0.6.0 | 0.7.0 | 0.8.0 | Spec complete |
| Home | 0.4.0 | 0.3.0 | 0.2.0 | Spec complete |
| Settings | — | — | — | Phase 1 scope confirmed; spec not yet written |
| Accounts | — | — | — | Not started |
| Categories | — | — | — | Not started |
| Merchants | — | — | — | Not started |
| Transactions | — | — | — | Not started |
| Transfers | — | — | — | Not started |
| Budgets | — | — | — | Not started |
| Goals | — | — | — | Not started |
| Dashboard | — | — | — | Not started |

---

## Android Architecture Summary

**Pattern:** MVVM + Repository with Clean Architecture use cases.

```
UI (Composables)
    └── ViewModel              # UI state, user events, business logic coordination
        └── Use Case           # Single business operation
            └── Repository     # Data access interface, abstracts Room
                └── DAO        # Room DAOs, raw SQL queries
                    └── Room Database
```

- ViewModels scoped to composable destinations via `hiltViewModel()`
- Use cases injected into ViewModels; repositories injected into use cases — all via Hilt
- UI observes `StateFlow` from ViewModel; never accesses Repository directly
- Errors surfaced via sealed `UiState` (Loading / Success / Error) — no silent failures

**Package structure:** Feature-based at the top level, layer-based inside each feature.

```
com.dmariani.capital/
├── core/
│   ├── ui/          # shared Composables, theme, design tokens
│   ├── domain/      # pure Kotlin domain entities (no framework deps)
│   └── data/        # AppDatabase.kt, DAOs, PreferencesDataSource
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── accounts/
│   ├── transactions/
│   ├── categories/
│   ├── merchants/
│   ├── transfers/
│   ├── budgets/
│   ├── goals/
│   ├── dashboard/
│   └── settings/
└── app/
    ├── MainActivity.kt
    ├── AppNavGraph.kt           # composes all feature nested nav graphs
    ├── TopLevelDestination.kt   # sealed class for the 5 tab routes
    └── ui/
        ├── BottomNavigationBar.kt
        └── AppHeader.kt
```

Each feature folder: `ui/` (Screen + ViewModel) · `domain/` (Repository interface + use cases) · `data/` (local/ + RepositoryImpl)

**DI modules:** `DatabaseModule` (Room DB + DAOs) · `RepositoryModule` (binds interfaces to impls) · feature-level modules where needed · `PreferencesDataSource` centralized in `core/data/`

**Navigation:** Single `NavHost` in `AppNavGraph.kt`. Each feature defines its own `NavGraphBuilder` extension function. Features never import each other. Tab back stacks preserved via `saveState` / `restoreState`. Settings registered as a non-tab destination reachable from the gear icon.

**Local DB:** Room is the sole source of truth in Phase 1. DB name: `capital.db`. Amounts as INTEGER (cents). Dates as INTEGER (Unix UTC). `workspace_id` FK on all financial entities from Phase 1.

---

## Workspace Strategy

- Every financial entity carries `workspace_id` (FK) from Phase 1 (BR-WS-01)
- A default Workspace (name: `"Personal"`, UUID v4 generated client-side) is created on first launch — not visible in UI in Phase 1 (BR-WS-02)
- Single-workspace in Phase 1; multi-workspace introduced in Phase 4 — schema is already compatible, no migration needed

---

## Currency Strategy

- Level 1 multi-currency: multiple currencies per workspace, no conversion performed (BR-CU-04)
- `base_currency` on Workspace (ISO 4217, defaults to `USD`) · `currency_code` on Account (defaults to base, overridable) · `currency_code` on Transaction (inherited from Account at creation, immutable after — BR-CU-03)
- Aggregated totals group by currency; cross-currency conversion is out of scope for all phases
- Currency picker introduced in Phase 2 onboarding (Phase 1 hardcodes USD)

---

## Data Model Principles

| Principle | Rule |
|---|---|
| Soft delete | All entities use `deleted_at` (nullable timestamp). Hard deletes never performed. (BR-DI-01) |
| Amounts as integers | All monetary values stored in cents as `INTEGER`. Never `REAL` or `FLOAT`. (BR-DI-03) |
| Dates as integers | All timestamps stored as Unix UTC integers. |
| Primary keys | UUID v4 (TEXT), generated client-side. Ensures global uniqueness without server coordination. |
| Derived values | Never persisted: account balance, goal progress, budget spent, net worth, pinned sort order. All computed at query time. |
| Workspace scoping | `workspace_id` FK on all financial entities from Phase 1. |

---

## Budget Philosophy

A Budget is a **flexible monthly spending plan**, never a hard cap. Transactions are never blocked by budget status. UI language is always: **Planned / Spent / Remaining** — never "limit" or "cap". Spending status (variance) is calculated at query time as `planned − spent`; positive = underspent, negative = overspent.

---

## Sync Strategy Summary

> Full detail: `specs/technical/offline-sync.md`

**Triggers:** App foreground (connectivity check on resume) · manual pull-to-refresh · periodic WorkManager (Phase 2)

**Flow:** Push local pending records first → pull server changes by watermark → advance watermark on full success

**Conflict resolution:** Server is authoritative. Before flagging a conflict on a locally modified record, a **semantic equality check** compares all business fields (excludes `id`, `sync_status`, `last_synced_at`, `updated_at`, `deleted_at`). If fields are identical, no conflict — local record is marked synced.

**Watermark approach:** Client tracks `last_synced_at` per entity. Records where `updated_at > last_synced_at` have pending changes. Server returns all records modified after the watermark. Watermark only advances on full successful pull completion.

**Batch processing:** Records batched at 100 per request. Processing is record-level, not batch-level — one record failure does not affect others in the same batch. Server returns `accepted / conflicts / rejected` per entity type.

**Sync Orchestrator:** `SyncOrchestrator` (Kotlin class) coordinates push → pull sequence. Exactly one sync cycle active at a time, enforced via a `Mutex`. `Mutex` released in `finally` block.

**Cancellation safety:** Sync is safe to interrupt at any point. Partial record-level progress is preserved on cancellation (app close, process kill, WorkManager cancellation). Watermark only advances on full success — interrupted pull is retried in full on next trigger. `Mutex` released in `finally`. Room WAL ensures committed writes survive process kill.

---

## Auth Flow Summary (Phase 2+)

```
User → Android (Supabase SDK, gets JWT)
     → FastAPI (validates JWT locally using SUPABASE_JWT_SECRET)
     → PostgreSQL on Supabase (via direct connection string)
```

- JWT validated locally on every request — no per-request Supabase call
- Android stores tokens in `EncryptedSharedPreferences`
- Phase 1 data migrated to default Workspace on first authenticated sync
- Same JWT pattern applies in Phase 3 (web)

---

## Navigation Decisions

| Decision | Rule |
|---|---|
| Header — Dashboard | Displays "Capital" as `Text` composable using `BorelFontFamily`, 26sp, centered |
| Header — non-Dashboard tabs | Displays tab name using Inter (`MaterialTheme.typography.titleLarge`), centered |
| Gear icon | Dashboard only — navigates to Settings |
| Scroll behavior | `enterAlwaysScrollBehavior` on all tabs — header hides on scroll down, reappears on scroll up |
| Bottom nav | Icon-only, no labels (reference: Instagram, X) |
| Sub-screens | Full-screen; shell header and bottom nav hidden; feature-owned top bar with back arrow, title, optional actions |

---

## Design Decisions (Session History)

| Decision | Detail |
|---|---|
| CategoryPill component | Icon + name pill; full-opacity color for text/icon; 15% alpha same color for background; derived at render time from single stored hex; `radius.sm`; `Label` scale; display-only |
| Category color field | Single `color TEXT NOT NULL` (`#RRGGBB`) on Category entity; no second color stored |
| Date-grouped transaction list | `TransactionDateHeader` + `TransactionItem` composables; sealed list built in ViewModel from `ORDER BY date DESC`; pure UI change, no data model impact |
| "Need it?" flag | Deferred. Potential future replacement: generic post-hoc `flag` field. Revisit after personal app usage. |

---

## App Assets

- App icon and adaptive icon assets (SVG + XML) — produced and ready for placement
- Borel font (`borel_regular.ttf`) — registered in `core/ui/theme/Type.kt` as `BorelFontFamily`; Dashboard AppHeader only, 26sp minimum 20sp

---

## Open Technical Decisions

| Decision | Notes |
|---|---|
| Backend hosting | Render vs Railway — undecided |
| Feature flags / remote config | DB table vs Firebase Remote Config — undecided |
| Web hosting | Vercel is the leading candidate |
| Long-term sync scope | Phase 1–2: full local mirror (offline-first). Consider windowed sync (e.g. last 12 months on device) for Phase 2+ based on data volume and target device specs. Revisit at Phase 2 kickoff. |

---

## SDD Rules & Working Principles

- **One document, one change, one session** — prevents runaway edits and keeps changes reviewable
- **Question before generating** — flag decisions needing confirmation before writing large outputs
- **IDs are never renumbered** — BR/RQ/AC/TSK IDs are permanent once assigned; format `[TYPE]-[FEAT]-NN` with two-character feature abbreviation
- **Feature specs never copy global rule text** — reference by ID only
- **Three files per feature spec, no exceptions:** `requirements.md` · `design.md` · `tasks.md`
- **Tasks files include:** exact file paths, effort estimates (S/M/L), status tracking, dependency links
- **Present changes before writing** — Dani reviews a to-do list or change summary before confirming large drafts
- **Numbered feedback** — observations addressed one by one before proceeding
- **Cross-document consistency is first-class** — changes to shared concepts must propagate across all affected files
- **One new chat per major step** — avoid context degradation; CONTEXT.md bridges sessions

## File Editing Workflow

- `/mnt/project/` is read-only — never attempt direct edits
- Reliable pattern: `cp /mnt/project/[file] /home/claude/[file]` → `str_replace` edits on `/home/claude/[file]` → `cp` to `/mnt/user-data/outputs/[file]` → `present_files`
- `grep -n` with multiple file paths is effective for cross-file auditing before and after edits
- Python3 `str.replace()` inline scripts are more reliable than `sed` for multi-line or special-character replacements

## Tooling Evaluation

Implementation is being evaluated in parallel on feature-namespaced branches:

| Tool | Branch pattern | Rules |
|---|---|---|
| Cursor | `eval/cursor/[feature]` | `.cursor/rules/*.mdc` — `alwaysApply: true` on core; glob-scoped for Android, design system, FastAPI, React |
| Claude Code | `eval/claude-code/[feature]` | `CLAUDE.md` + `.claude/rules/*.md` — full parity with Cursor rules |

Metrics: spec adherence · architectural compliance · corrections needed · back-and-forth required

---

## Next Steps

1. Begin Accounts feature spec (`requirements.md` → `design.md` → `tasks.md`)
2. Continue in recommended order: Accounts → Categories → Merchants → Transactions → Transfers → Budgets → Goals → Dashboard → Settings
3. Evaluate Cursor vs Claude Code on first implementation feature (onboarding or accounts)
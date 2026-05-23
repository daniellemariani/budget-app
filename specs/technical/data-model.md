# Data Model — Budget App

**Version:** 0.6.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-04-30
**Last Updated:** 2026-05-23

## Overview

This document is the canonical database schema reference for the Budget App. It defines every entity, field, constraint, and relationship across all phases. All other documents (feature specs, API contract) reference this file — they do not duplicate schema definitions.

Phase 1 entities are fully defined. Phase 2 entities (User, WorkspaceMember, RecurringTransaction) are stubbed with enough detail to inform Phase 1 field decisions (e.g. `workspace_id` FK, `recurring_id` on Transaction).

## Related Documents

| Document | Purpose |
|---|---|
| SPEC.md | Global business rules and feature index |
| ARCHITECTURE.md | Stack decisions and data layer constraints |
| specs/technical/api-contract.md | API endpoint definitions |
| specs/technical/offline-sync.md | Sync strategy and conflict resolution |

---

## Design Principles

These principles are enforced at the database layer across all platforms and phases.

| Principle | Rule |
|---|---|
| Soft delete | All entities use `deleted_at` (nullable timestamp). Hard deletes are never performed. (BR-DI-01) |
| Audit timestamps | All entities include `created_at` and `updated_at`. (BR-DI-02) |
| Amounts as integers | All monetary values stored in cents as `INTEGER`. Never `REAL` or `FLOAT`. (BR-DI-03) |
| UTC dates | All timestamps stored in UTC. Converted to device local time in the UI layer. (BR-DI-04) |
| Workspace isolation | All direct financial entities carry a `workspace_id` FK from Phase 1 for query performance and security boundary enforcement. (BR-WS-01) |
| Currency codes | ISO 4217 three-letter codes used throughout (e.g. `USD`, `EUR`, `MXN`). |
| Primary keys | All entities use UUID v4 (TEXT) as primary key, generated client-side at creation time. Ensures global uniqueness across devices without server coordination. Supports offline-first sync introduced in Phase 2. SQLite stores UUIDs as TEXT. |
| Sync | All entities include `last_synced_at` (nullable INTEGER timestamp) and `sync_status` (nullable ENUM: PENDING, SYNCED, FAILED, CONFLICT), present from Phase 1 to avoid future migrations. Unused until Phase 2. |
| Derived Values | The following fields are calculated (not persisted): Account current balance, Goal progress, Budget spent amount, Spending variance, and Net worth |
---

## Entity Reference

### Workspace

Top-level container for all financial data. A default Workspace is created on first launch with a newly generated UUID. It is used transparently in Phase 1 and is not visible in the UI.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| name | TEXT | No | — | Workspace display name |
| base_currency | TEXT | No | `USD` | ISO 4217 code. Default currency for all accounts in this workspace. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `name` must be non-empty
- `base_currency` must be a valid ISO 4217 code
- The default Workspace (created on first launch) cannot be deleted (BR-WS-04)
- A Workspace cannot be deleted if it is the last remaining one (BR-WS-03)

---

### Account

A financial source belonging to a Workspace (e.g. Checking, Savings, Credit Card, Cash). Each Account has its own currency.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| name | TEXT | No | — | User-defined account name (e.g. "Chase Checking") |
| type | TEXT | No | — | ENUM: `CHECKING`, `SAVINGS`, `CREDIT_CARD`, `CASH` |
| currency_code | TEXT | No | workspace base_currency | ISO 4217 code. Independent per account. (BR-CU-02) |
| initial_balance | INTEGER | No | 0 | Opening balance in cents. Positive for assets, positive for credit card debt outstanding. |
| credit_limit | INTEGER | Yes | null | Credit limit in cents. Applicable to `CREDIT_CARD` accounts only. Used to calculate available credit in the UI. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `type` must be one of the defined ENUM values
- `currency_code` must be a valid ISO 4217 code
- `credit_limit` is only meaningful when `type = CREDIT_CARD`. Ignored for all other account types.
- `credit_limit` must be a positive integer when set (> 0)
- Cannot be soft-deleted if it has associated transactions or transfers. User must reassign first. (BR-AC-03)
- Credit card balance represents debt outstanding, not available funds. (BR-AC-01)
- Unique on `(workspace_id, name)` where deleted_at IS NULL

---

### Category

A user-defined label for grouping transactions. System-seeded defaults are provided on first launch and cannot be deleted, only hidden.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| name | TEXT | No | — | Display name (e.g. "Groceries", "Utilities") |
| icon | TEXT | Yes | null | Emoji character used as the category icon (e.g. `🛒`, `💡`). Optional. |
| is_default | INTEGER | No | 0 | Boolean flag. 1 = system-seeded default category. |
| is_hidden | INTEGER | No | 0 | Boolean flag. 1 = hidden from selection UI. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- Cannot be soft-deleted if it has associated transactions. User must reassign transactions first. (BR-CA-01)
- Default categories (`is_default = 1`) cannot be soft-deleted, only hidden. (BR-CA-02)
- `name` must be non-empty
- Unique on `(workspace_id, name)` where deleted_at IS NULL

---

### Merchant

A user-defined entity representing a seller or service provider. Linked optionally to transactions.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| name | TEXT | No | — | Merchant display name (e.g. "Trader Joe's", "Netflix") |
| logo_url | TEXT | Yes | null | Optional URL to merchant logo image. Manually entered in Phase 1. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- Can be soft-deleted regardless of associated transactions. Existing transactions retain the merchant reference, but the merchant is hidden from selection. (BR-ME-01)
- `name` must be non-empty
- Unique on `(workspace_id, name)` where deleted_at IS NULL

---

### Transaction

A single income or expense entry. Always belongs to one Account and one Category. Optionally linked to a Merchant.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| account_id | TEXT | No | — | FK → Account.id |
| category_id | TEXT | No | — | FK → Category.id |
| merchant_id | TEXT | Yes | null | FK → Merchant.id. Optional. |
| recurring_id | TEXT | Yes | null | FK → RecurringTransaction.id. Null in Phase 1. (Phase 2) |
| type | TEXT | No | — | ENUM: `INCOME`, `EXPENSE` |
| amount | INTEGER | No | — | Amount in cents. Always stored as positive integer. (BR-TX-01) |
| currency_code | TEXT | No | — | ISO 4217 code. Inherited from Account at creation time. Immutable after creation. (BR-CU-03) |
| date | INTEGER | No | — | Transaction date. Unix timestamp, UTC. |
| notes | TEXT | Yes | null | Optional free-text note |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `type` must be one of the defined ENUM values
- `amount` must be a positive integer (> 0)
- `currency_code` is set at creation time from the linked Account and cannot be modified after (BR-CU-03)
- Transfers are separate entities and are never stored as Transactions (BR-TX-02)

---

### Transfer

A movement of money between two accounts. Not counted as income or expense. Both accounts must share the same currency in Phase 1.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| from_account_id | TEXT | No | — | FK → Account.id. Source account. |
| to_account_id | TEXT | No | — | FK → Account.id. Destination account. |
| amount | INTEGER | No | — | Amount in cents. Always positive. |
| currency_code | TEXT | No | — | ISO 4217 code. Inherited from source account. |
| date | INTEGER | No | — | Transfer date. Unix timestamp, UTC. |
| notes | TEXT | Yes | null | Optional free-text note |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `from_account_id` and `to_account_id` must both be present and must be different accounts (BR-TR-01)
- `from_account_id` and `to_account_id` must share the same `currency_code` in Phase 1 (BR-TR-02)
- `amount` must be a positive integer (> 0)
- Transfers must not have a category (BR-TR-01)

---

### Budget

An optional monthly spending plan for a Category. Non-blocking — transactions are never prevented by budget status. Budget rows are per calendar month. A `carry_forward` flag causes the app to auto-generate next month's budget with the same amount when a new period begins.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| category_id | TEXT | No | — | FK → Category.id |
| amount | INTEGER | No | — | Planned monthly spending in cents. |
| currency_code | TEXT | No | workspace base_currency | ISO 4217 code. Denominated in workspace base_currency in Phase 1. (BR-CU-05) |
| period_year | INTEGER | No | — | Calendar year of the budget period (e.g. 2026) |
| period_month | INTEGER | No | — | Calendar month of the budget period (1–12) |
| carry_forward | INTEGER | No | 1 | Boolean flag. 1 = auto-generate this budget for the next month when a new period begins. 0 = one-off budget for this month only. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- One budget per category per month per workspace. Unique on `(workspace_id, category_id, period_year, period_month)` where `deleted_at IS NULL`
- `amount` must be a positive integer (> 0)
- `period_month` must be between 1 and 12
- Budget period is always a calendar month (BR-BU-01)
- Budgets are optional — transactions can exist without a corresponding budget (BR-BU-02)
- Budgets are non-blocking (BR-BU-03, BR-BU-04)
- Unused budget does not roll over (BR-BU-05)
- When `carry_forward = 1`, the app auto-generates next month's budget row on first access of a new period. Auto-generated rows are independent — editing or deleting one does not affect other months. If a month is skipped, all missing months are generated in sequence up to the current period.

---

### Goal

A specific, actionable financial target (e.g. Emergency Fund, Trip to Hawaii).

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| name | TEXT | No | — | Goal display name (e.g. "Hawaii Trip") |
| target_amount | INTEGER | No | — | Target amount in cents. |
| currency_code | TEXT | No | workspace base_currency | ISO 4217 code. |
| target_date | INTEGER | Yes | null | Optional target completion date. Unix timestamp, UTC. |
| notes | TEXT | Yes | null | Optional free-text note |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `target_amount` must be a positive integer (> 0)
- `currency_code` must be a valid ISO 4217 code
- Goal progress is calculated at query time as `SUM(GoalContribution.amount) / target_amount` (BR-GL-01)

---

### GoalContribution

A manual payment toward a Goal. Tracked independently from monthly category budgets.

**Phase:** 1

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| goal_id | TEXT | No | — | FK → Goal.id |
| amount | INTEGER | No | — | Contribution amount in cents. |
| currency_code | TEXT | No | — | ISO 4217 code. Inherited from parent Goal. |
| date | INTEGER | No | — | Contribution date. Unix timestamp, UTC. |
| notes | TEXT | Yes | null | Optional free-text note |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. Null in Phase 1. (Phase 2) |
| sync_status | TEXT | Yes | null | ENUM: PENDING, SYNCED, FAILED, CONFLICT. Null in Phase 1. (Phase 2) |

**Constraints:**
- `amount` must be a positive integer (> 0)
- `currency_code` is inherited from the parent Goal and cannot differ
- Goal contributions are tracked independently from monthly budgets (BR-GL-02)

---

## Phase 2 Entities

The following entities are stubbed for planning purposes. Full schema is defined at Phase 2 kickoff. Fields marked here reflect what Phase 1 already accounts for (e.g. `recurring_id` on Transaction).

---

### User *(Phase 2)*

Represents an authenticated user. Introduced when Supabase Auth is added. In Phase 1, a single anonymous owner is assumed.

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| supabase_uid | TEXT | No | — | Supabase Auth user ID. Unique. |
| email | TEXT | No | — | User email address. Unique. |
| display_name | TEXT | Yes | null | Optional display name |
| avatar_url | TEXT | Yes | null | Optional avatar URL (Supabase Storage) |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. (Phase 2) |
| sync_status | TEXT | Yes | PENDING | ENUM: PENDING, SYNCED, FAILED, CONFLICT. |

---

### WorkspaceMember *(Phase 2)*

Junction entity linking a User to a Workspace with an assigned role. Manages multi-user access and permissions, including the invite lifecycle.

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| user_id | TEXT | Yes | null | FK → User.id. Null for PENDING members — populated when the invite is accepted. |
| role | TEXT | No | — | ENUM: `OWNER`, `ADMIN`, `MEMBER`, `VIEWER` |
| status | TEXT | No | `PENDING` | ENUM: `PENDING`, `ACTIVE`, `REVOKED`. PENDING = invite sent, not yet accepted. ACTIVE = member has joined. REVOKED = access removed. |
| display_name | TEXT | Yes | null | Denormalized from User.display_name for query convenience. Null for PENDING members (no User record yet). Kept in sync when the User updates their profile. |
| email | TEXT | Yes | null | Invite email for PENDING members (set at invite time). Updated to User.email on invite acceptance. Used to display who a pending invite was sent to. |
| invite_token | TEXT | Yes | null | Signed backend JWT used to accept the invite. Stored for revocation support. Cleared after acceptance or expiry. Server-only — never returned in API responses or sync payloads. |
| invite_expires_at | INTEGER | Yes | null | Unix timestamp, UTC. Expiry of the invite token (7 days from invite creation by default, configurable). Null for ACTIVE and REVOKED members. Server-only — never returned in API responses or sync payloads. |
| invited_at | INTEGER | Yes | null | Timestamp when the invitation was sent. Unix timestamp, UTC. |
| joined_at | INTEGER | Yes | null | Timestamp when the user accepted the invite. Unix timestamp, UTC. |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete; used to revoke access without destroying the record. |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. (Phase 2) |
| sync_status | TEXT | Yes | PENDING | ENUM: PENDING, SYNCED, FAILED, CONFLICT. |

**Constraints:**
- Unique on `(workspace_id, user_id)` where `deleted_at IS NULL` and `user_id IS NOT NULL`
- Each Workspace must have exactly one active `OWNER` (BR-WS-05)
- `role` must be one of the defined ENUM values
- `status` must be one of the defined ENUM values
- `user_id` is null only when `status = PENDING`. Must be non-null for `ACTIVE` and `REVOKED` records.
- `invite_token` and `invite_expires_at` are only meaningful when `status = PENDING`. Cleared (set to null) on acceptance or revocation.
- `invite_token` and `invite_expires_at` are server-only fields. They are stored for internal validation and revocation but are never included in API responses or sync payloads. The raw token value is never exposed to any client.
- `display_name` is denormalized — it must be updated whenever the linked User's `display_name` changes.
- `email` is set to the invite email at creation time. Updated to match User.email on invite acceptance.

---

### RecurringTransaction *(Phase 2)*

A template that generates scheduled Transaction entries automatically. Transactions generated from a template carry a `recurring_id` FK back to this entity.

| Field | Type | Nullable | Default | Description |
|---|---|---|---|---|
| id | TEXT | No | UUID v4 | Primary key. Generated client-side at creation time. |
| workspace_id | TEXT | No | — | FK → Workspace.id |
| account_id | TEXT | No | — | FK → Account.id |
| category_id | TEXT | No | — | FK → Category.id |
| merchant_id | TEXT | Yes | null | FK → Merchant.id. Optional. |
| type | TEXT | No | — | ENUM: `INCOME`, `EXPENSE` |
| amount | INTEGER | No | — | Amount in cents. Always positive. |
| currency_code | TEXT | No | — | ISO 4217 code. Inherited from Account. |
| frequency | TEXT | No | — | ENUM: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| start_date | INTEGER | No | — | First occurrence date. Unix timestamp, UTC. |
| end_date | INTEGER | Yes | null | Optional end date. Unix timestamp, UTC. |
| total_installments | INTEGER | Yes | null | Optional. Total number of installments if installment-based. |
| remaining_installments | INTEGER | Yes | null | Optional. Remaining installments. Decremented on each generation. |
| notes | TEXT | Yes | null | Optional free-text note |
| created_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| updated_at | INTEGER | No | now (UTC) | Unix timestamp, UTC |
| deleted_at | INTEGER | Yes | null | Soft delete timestamp, UTC |
| last_synced_at | INTEGER | Yes | null | Unix timestamp, UTC. Tracks last successful sync with backend. (Phase 2) |
| sync_status | TEXT | Yes | PENDING | ENUM: PENDING, SYNCED, FAILED, CONFLICT. |

---

## Entity Relationship Summary

```
Workspace
├── Account (workspace_id)
├── Category (workspace_id)
├── Merchant (workspace_id)
├── Transaction (workspace_id)
│   ├── → Account (account_id)
│   ├── → Category (category_id)
│   ├── → Merchant (merchant_id, optional)
│   └── → RecurringTransaction (recurring_id, optional — Phase 2)
├── Transfer (workspace_id)
│   ├── → Account (from_account_id)
│   └── → Account (to_account_id)
├── Budget (workspace_id)
│   └── → Category (category_id)
├── Goal (workspace_id)
│   └── GoalContribution (workspace_id, goal_id)
└── WorkspaceMember (workspace_id — Phase 2)
    └── → User (user_id — Phase 2)
```

---

## Indexes

Indexes are listed per entity for fields that appear frequently in queries, filters, or joins.

| Entity | Index Fields | Rationale |
|---|---|---|
| Transaction | `(workspace_id, date)` | Date-range queries for period reporting |
| Transaction | `(workspace_id, account_id)` | Account transaction history |
| Transaction | `(workspace_id, category_id)` | Budget spending calculations |
| Transaction | `(workspace_id, deleted_at)` | Soft delete filter on all list queries |
| Budget | `(workspace_id, category_id, period_year, period_month)` | Budget lookup per category per period |
| Transfer | `(workspace_id, date)` | Date-range transfer queries |
| Transfer | `(workspace_id, from_account_id)` | Account transfer history |
| Transfer | `(workspace_id, to_account_id)` | Account transfer history |
| GoalContribution | `(workspace_id, goal_id)` | Progress calculation per goal |
| WorkspaceMember | `(workspace_id, user_id)` | Member lookup and deduplication (Phase 2) |
| WorkspaceMember | `(workspace_id, status)` | Member list queries filtered by status — PENDING invite management, ACTIVE member access checks (Phase 2) |

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-08 | Danielle Mariani | Initial entity draft |
| 0.2.0 | 2026-05-08 | Danielle Mariani | Add workspace_id to GoalContribution. Add last_synced_at to all entities. |
| 0.3.0 | 2026-05-11 | Danielle Mariani | Switch all primary keys and foreign keys to UUID v4 (TEXT). Default Workspace now uses a generated UUID instead of a hardcoded id. Tighten Design Principles wording for Sync and Primary keys. |
| 0.4.0 | 2026-05-11 | Danielle Mariani | Add sync_status to all entities |
| 0.5.0 | 2026-05-11 | Danielle Mariani | Add workspace_id and name constraints to Account, Category and Merchant. Add Derived Values to Design Principles. |
| 0.6.0 | 2026-05-23 | Danielle Mariani | Expand WorkspaceMember stub with five new fields: status (PENDING/ACTIVE/REVOKED), display_name (denormalized from User), email (invite email → account email), invite_token (server-only, stored for revocation), invite_expires_at (server-only, 7-day TTL). Make user_id nullable for PENDING members. Update WorkspaceMember constraints. Add WorkspaceMember(workspace_id, status) index. |
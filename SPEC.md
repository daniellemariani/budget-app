# Budget App

**Version:** 0.1.0
**Status:** In Progress
**Owner:** Danielle Mariani
**Last Updated:** 2026-04-21

## Overview
Budget App is a personal finance management tool that allows users to track expenses, manage monthly budgets per category, and visualize spending patterns. Built offline-first for Android, with a web dashboard and backend sync in later phases.

## Glossary

| Term | Definition |
|---|---|
| Transaction | A single income or expense entry with amount, date, category, account, merchant, and optional notes. Does not include transfers — see Transfer |
| Category | A user-defined label for grouping transactions (e.g. Groceries, Utilities) |
| Budget | An optional monthly spending plan for a category. Represents intended spending, not a hard cap. Transactions are never blocked by budget status. |
| Spending Status | The current state of a budget category, showing planned amount, amount spent, and variance (over or under). Calculated at query time as: Budget - Spent (positive = underspent, negative = overspent).|
| Period | A calendar month used as the unit of budget tracking |
| Account | A financial source (e.g. Checking, Savings, Credit Card, Cash) |
| Merchant | An entity (person or company) that sells goods or services (e.g. Trader Joe's, Netflix, Amazon) |
| Goal | A specific, actionable financial target that provides purpose for your spending plan (e.g. Trip, Emergency Fund, Buy a House) |
| Transfer | A movement of money between two financial accounts. Not counted as income or expense. (e.g. Credit Card payment, savings deposit) |
| GoalContribution | A manual payment toward a Goal, tracked independently from monthly budgets |
| RecurringTransaction | A transaction template that generates scheduled payments automatically (e.g. Netflix, Mortgage, Gym membership) |
| Workspace | Top-level container for all financial data representing a household or individual. Users belong to one or more workspaces. All financial data belongs to a Workspace, not a User directly. A default workspace is created transparently on first launch in Phase 1. |

## User Roles

### Phase 1
- **Owner** — single user, full access to all data, no authentication required

### Phase 2+
- **Owner** — created the workspace, full access, 
  manages members and billing
- **Admin** — full access to data, can manage members
- **Member** — full read/write access to transactions and budgets
- **Viewer** — read-only access (future consideration)

### Workspace model
Each household or individual is represented as a Workspace. Users can belong to multiple workspaces (e.g. family + personal). All financial data belongs to a Workspace, not a User directly.

## Feature Index

### Phase 1 — Android Offline
| Feature | Spec | Description | Status |
|---|---|---|---|
| Onboarding | specs/features/onboarding/spec.md | Getting started, initial setup | Not Started |
| Transactions | specs/features/transactions/spec.md | Add, edit, delete income and expenses | Not Started |
| Categories | specs/features/categories/spec.md | Organize transactions by type | Not Started |
| Accounts | specs/features/accounts/spec.md | Financial accounts (checking, savings, credit card, cash) | Not Started |
| Merchants | specs/features/merchants/spec.md | Add, edit, delete merchants | Not Started |
| Transfers | specs/features/transfers/spec.md | Move money between accounts | Not Started |
| Budgets | specs/features/budgets/spec.md | Monthly spending limits per category | Not Started |
| Goals | specs/features/goals/spec.md | Savings targets with progress tracking | Not Started |
| Dashboard | specs/features/dashboard/spec.md | Summary view of financial health | Not Started |

### Phase 2 — Backend + Sync
| Feature | Spec | Description | Status |
|---|---|---|---|
| Recurring Transactions | specs/features/recurring-transactions/spec.md | Scheduled and installment-based transactions | Not Started |
| Auth | specs/features/auth/spec.md | User registration, login, session management | Not Started |
| Sync | specs/features/sync/spec.md | Data sync between device and backend | Not Started |

### Phase 3 — Web

Scope: Full web interface connecting to the Phase 2 backend. All Phase 1 features are supported unless noted otherwise.
Web-specific behavior is documented in a "Web" section within each existing feature spec.

| Feature | Spec | Description | Status |
|---|---|---|---|
| Web Dashboard | specs/features/web-dashboard/spec.md | Full-featured web interface for budget management and reporting, connects to Phase 2 backend | Not Started |
| Reports | specs/features/reports/spec.md | Monthly income vs expense charts and spending analysis | Not Started |

### Phase 4 — Cross-Platform Mobile
| Feature | Spec | Description | Status |
|---|---|---|---|
| Flutter/KMP Migration | TBD | Cross-platform mobile rewrite. Decision between Flutter and KMP deferred to Phase 4.  | Not Started |

## Data Model Summary
Core entities and relationships:

- **Category** — belongs to a Workspace, user-defined, has many Transactions and Budgets
- **Transaction** — belongs to a Workspace, has one Category and one Account, optionally linked to a Merchant, has amount/date/type/notes
- **Budget** — monthly limit per Category, has planned amount.
- **Account** — belongs to a Workspace, financial source per Transaction
- **Merchant** - user-defined, has many Transactions
- **Transfer** - has an origin Account and a destination Account, has amount/date/notes
- **Goal** - has target amount/target date
- **GoalContribution** - belongs to one Goal, has amount/date/notes
- **User** — owns all data, introduced in Phase 2
- **RecurringTransaction** — generates scheduled Transactions, linked back via recurring_id on Transaction, has amount/frequency/start date/end date (optional)/ total installments (optional)/remaining installments (optional), introduced in Phase 2
- **Workspace** — top-level container for all financial data. A default workspace is created on first launch and used transparently in Phase 1. Multi-workspace support introduced in Phase 4.

Full schema: specs/technical/data-model.md

## Global Business Rules

### Data Integrity
- BR-G01: No hard deletes — all entities use soft delete (deleted_at timestamp)
- BR-G02: All entities include created_at and updated_at timestamps
- BR-G03: Amounts are always stored in cents (integer) to avoid float precision issues
- BR-G04: All dates are stored in UTC, displayed in device local time

### Budgets
- BR-G05: Budget period is always a calendar month
- BR-G06: Budgets are optional — transactions can exist without a corresponding budget
- BR-G07: Budgets are non-blocking — transactions are never prevented or flagged as errors due to budget status
- BR-G08: Overspending is allowed and visible — spending status shows variance without restricting entry
- BR-G09: Unused budget does not roll over to the next month (configurable in a future phase)

### Categories
- BR-G10: A category cannot be soft-deleted if it has associated transactions. User must reassign transactions first, or hide the category instead.
- BR-G11: System provides a default set of categories on first launch; user can add custom categories, edit names, or hide defaults. Default categories cannot be deleted, only hidden.

### Transactions
- BR-G12: Transaction type (INCOME/EXPENSE) determines sign in balance calculations — amounts always stored as positive integers
- BR-G13: Transfers are not counted as income or expenses in period balance calculations

### Accounts
- BR-G14: Credit card account balance represents debt outstanding, not available funds
- BR-G15: Net worth = SUM(asset account balances) - SUM(credit card balances)
- BR-G16: An Account cannot be soft-deleted if it has associated transactions or transfers. User must reassign first. Goal contributions are not directly linked to accounts and are unaffected by account deletion.

### Merchants
- BR-G17: A Merchant can be soft-deleted regardless of associated transactions. Existing transactions retain the merchant reference but it is hidden from selection.

### Goals
- BR-G18: Goal progress = SUM(contributions) / target_amount, calculated at query time
- BR-G19: Goal contributions are tracked independently from monthly category budgets

### Transfers
- BR-G20: A transfer must always include both a source and destination account and must not have a category.

### Workspace
- BR-G21: All financial entities belong to exactly one Workspace via workspace_id
- BR-G22: A default Workspace (id: 1, name: "default") is created on first launch. It is not visible in the UI in Phase 1.
- BR-G23: A Workspace cannot be deleted if it is the last remaining Workspace. At least one Workspace must always exist to ensure all financial data has a valid owner.
- BR-G24: The default Workspace (id: 1) cannot be deleted under any circumstances.

## Global Non-Functional Requirements

### Performance
- NFR-G01: All screens must render initial state in under 300ms
- NFR-G02: Database queries must complete in under 100ms for up to 5 years of data

### Offline
- NFR-G03: All Phase 1 features must work 100% offline
- NFR-G04: No feature may depend on network availability in Phase 1

### Accessibility
- NFR-G05: Minimum touch target size of 48x48dp on all interactive elements
- NFR-G06: All UI must support system font size scaling

### Data Safety
- NFR-G07: No financial data is transmitted to any external service in Phase 1
- NFR-G08: Local database must be included in Android Auto Backup

## Out of Scope
- Automatic bank or card transaction import
- Investment portfolio tracking
- Tax reporting or export
- Financial advice or AI-driven recommendations
- Cryptocurrency tracking
- Multi-currency support (Phase 1 — revisit for public launch)
- Weekly period view (Phase 1)
- Receipt or attachment scanning (post Phase 1, phase TBD)
- Recurring transactions (Phase 2)
- Credit card cashback and rewards tracking (Phase 1 — revisit Phase 2)
- Per-account transaction history and statement view (Phase 1)
- Running balance per account after each transaction (Phase 1)

## Related Documents

| Document | Purpose |
|---|---|
| PRODUCT.md | Vision, users, success criteria |
| ARCHITECTURE.md | Technical decisions and stack |
| ROADMAP.md | Phase timeline and progress |
| specs/technical/data-model.md | Full database schema |
| specs/technical/api-contract.md | API endpoint definitions |
| specs/technical/offline-sync.md | Sync strategy and conflict resolution |

# Budget App

**Version:** 0.1.0
**Status:** In Progress
**Owner:** Danielle Mariani
**Created at:** 2026-04-21
**Last Updated:** 2026-04-27

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
| Currency | A system of money (banknotes and coins) issued by a government and used as a legal tender and medium of exchange within a specific country |
| BaseCurrency | The primary currency of a Workspace, set during onboarding and used as the default for all accounts and aggregated display totals. Stored as an ISO 4217 code (e.g. USD, EUR, MXN) |
| CurrencyCode | An ISO 4217 three-letter currency identifier assigned to each Account and inherited by its Transactions. Determines how amounts are displayed and grouped in reports. (e.g. USD, EUR, MXN, GBP) |
| ISO 4217 | International standard defining three-letter currency codes (e.g. USD for US Dollar, EUR for Euro, MXN for Mexican Peso) |

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
- **Budget** — monthly limit per Category, has planned amount, has currency_code (USD by default).
- **Account** — belongs to a Workspace, financial source per Transaction, has currency_code (USD by default)
- **Merchant** - belongs to a Workspace, user-defined, has many Transactions
- **Transfer** — has origin Account and destination Account, has amount/date/notes. currency_code follows the source Account's currency_code.
- **Goal** - has target amount/target date/currency_code (USD by default)
- **GoalContribution** — belongs to one Goal, has amount/date/notes. Currency inherited from parent Goal.
- **User** — owns all data, introduced in Phase 2
- **RecurringTransaction** — generates scheduled Transactions, linked back via recurring_id on Transaction, has amount/frequency/start date/end date (optional)/ total installments (optional)/remaining installments (optional), introduced in Phase 2
- **Workspace** — top-level container for all financial data. A default workspace is created on first launch and used transparently in Phase 1. Has base_currency (USD by default). Multi-workspace support introduced in Phase 4.


Full schema: specs/technical/data-model.md

## Global Business Rules

### Data Integrity
- BR-DI-01: No hard deletes — all entities use soft delete (deleted_at timestamp)
- BR-DI-02: All entities include `created_at` and `updated_at` timestamps
- BR-DI-03: Amounts are always stored in cents (integer) to avoid float precision issues
- BR-DI-04: All dates are stored in UTC, displayed in device local time

### Budgets
- BR-BU-01: Budget period is always a calendar month
- BR-BU-02: Budgets are optional — transactions can exist without a corresponding budget
- BR-BU-03: Budgets are non-blocking — transactions are never prevented or flagged as errors due to budget status
- BR-BU-04: Overspending is allowed and visible — spending status shows variance without restricting entry
- BR-BU-05: Unused budget does not roll over to the next month (configurable in a future phase)

### Categories
- BR-CA-01: A category cannot be soft-deleted if it has associated transactions. User must reassign transactions first, or hide the category instead.
- BR-CA-02: System provides a default set of categories on first launch; user can add custom categories, edit names, or hide defaults. Default categories cannot be deleted, only hidden.

### Transactions
- BR-TX-01: Transaction type (INCOME/EXPENSE) determines sign in balance calculations — amounts always stored as positive integers
- BR-TX-02: Transfers are not counted as income or expenses in period balance calculations

### Accounts
- BR-AC-01: Credit card account balance represents debt outstanding, not available funds
- BR-AC-02: Net worth = SUM(asset account balances) - SUM(credit card balances)
- BR-AC-03: An Account cannot be soft-deleted if it has associated transactions or transfers. User must reassign first. Goal contributions are not directly linked to accounts and are unaffected by account deletion.

### Merchants
- BR-ME-01: A Merchant can be soft-deleted regardless of associated transactions. Existing transactions retain the merchant reference but it is hidden from selection.

### Goals
- BR-GL-01: Goal progress = SUM(contributions) / target_amount, calculated at query time
- BR-GL-02: Goal contributions are tracked independently from monthly category budgets

### Transfers
- BR-TR-01: A transfer must always include both a source and destination account and must not have a category.
- BR-TR-02: Transfers between accounts of different currencies are not supported in Phase 1. Both accounts must share the same currency_code.

### Workspace
- BR-WS-01: All financial entities belong to exactly one Workspace via workspace_id
- BR-WS-02: A default Workspace (id: 1, name: "default") is created on first launch. It is not visible in the UI in Phase 1.
- BR-WS-03: A Workspace cannot be deleted if it is the last remaining Workspace. At least one Workspace must always exist to ensure all financial data has a valid owner.
- BR-WS-04: The default Workspace (id: 1) cannot be deleted under any circumstances.

### Currency
- BR-CU-01: Workspace base_currency defaults to USD in Phase 1. A currency picker is introduced in the onboarding flow in Phase 2 when multi-user and international support is added. base_currency is detected from device locale in Phase 2+.
- BR-CU-02: Each Account has an independent currency_code that defaults to workspace base_currency but can be overridden per account during setup.
- BR-CU-03: Transaction currency_code is inherited from its Account at creation time and stored explicitly. It cannot be changed after creation.
- BR-CU-04: Aggregated totals group amounts by currency in Phase 1. Cross-currency conversion is not performed automatically.
- BR-CU-05: Budgets are denominated in workspace base_currency in Phase 1.

## Global Non-Functional Requirements

### Performance
- NFR-PE-01: All screens must render initial state in under 300ms
- NFR-PE-02: Database queries must complete in under 100ms for up to 5 years of data

### Offline
- NFR-OF-01: All Phase 1 features must work 100% offline
- NFR-OF-02: No feature may depend on network availability in Phase 1

### Accessibility
- NFR-AC-01: Minimum touch target size of 48x48dp on all interactive elements
- NFR-AC-02: All UI must support system font size scaling

### Data Safety
- NFR-DS-01: No financial data is transmitted to any external service in Phase 1
- NFR-DS-02: Local database must be included in Android Auto Backup

## Out of Scope
- Automatic bank or card transaction import
- Investment portfolio tracking
- Tax reporting or export
- Financial advice or AI-driven recommendations
- Cryptocurrency tracking
- Automatic multi-currency conversion and exchange rates (all phases). Note: currency_code and base_currency fields are included in the data model from Phase 1 to support Level 1 multi-currency (multiple currencies per workspace, no conversion) and to avoid future migration cost if Level 2 is pursued.
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

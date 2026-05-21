# API Contract — Budget App

**Version:** 0.2.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-05-19
**Last Updated:** 2026-05-20

---

## Overview

This document is the canonical API contract for the Budget App backend (FastAPI). It defines all endpoints, request/response schemas, validation rules, and sync behavior for Phase 2 and beyond.

This document owns the **transport layer** — the wire format exchanged between clients and the server. Transport schemas defined here are distinct from the Room entities and domain models used internally by Android clients, and from the canonical database schema in `specs/technical/data-model.md`. They may look similar initially but are independently versioned and may diverge as the API evolves.

All clients — Android (Phase 2), Web (Phase 3), and Flutter/KMP (Phase 4) — consume this API. Any change to this contract must be evaluated for impact across all current and future clients.

The sync strategy driving the push/pull endpoints is fully specified in `specs/technical/offline-sync.md`. This document translates those requirements into concrete endpoint definitions.

---

## Related Documents

| Document | Purpose |
|---|---|
| SPEC.md | Global business rules and feature index |
| ARCHITECTURE.md | Stack decisions, auth flow, API design conventions |
| specs/technical/data-model.md | Canonical database schema — source of truth for field definitions |
| specs/technical/offline-sync.md | Sync strategy, conflict resolution, watermark approach |
| specs/features/auth/spec.md | Full auth feature spec (Phase 2) |
| specs/features/sync/spec.md | Sync UI spec — conflict resolution UI, sync status indicators |
| specs/features/dashboard/spec.md | Dashboard feature spec |

---

## Design Conventions

These conventions apply globally to every endpoint. Individual endpoint definitions do not repeat them.

### Base URL and Versioning

All endpoints are versioned under `/api/v1/`. See the **Versioning Strategy** section for compatibility guarantees.

```
https://<host>/api/v1/
```

### Naming

- Endpoint paths are lowercase hyphenated (e.g. `/goal-contributions`, not `/goalContributions`)
- Resource names are plural nouns (e.g. `/accounts`, `/transactions`)
- Sub-resources are nested only when the parent ID is required for context

### Required Headers

Every authenticated request must include both of the following headers:

| Header | Format | Description |
|---|---|---|
| `Authorization` | `Bearer <supabase_access_token>` | Supabase-issued JWT identifying the user |
| `X-Workspace-ID` | UUID v4 string | The active workspace for this request |

**`X-Workspace-ID` behavior:**
- The server validates that the authenticated user is an `ACTIVE` member of the specified workspace via `WorkspaceMember` before processing any request.
- In Phase 2 (single workspace per user), this header will always carry the user's default workspace ID. It is required from Phase 2 to avoid a breaking change when multi-workspace is introduced in Phase 4.
- In Phase 4, the client switches workspaces by changing this header value — no token exchange required.
- Requests with a missing or invalid `X-Workspace-ID` return `403 Forbidden`.

### Request Format

- All request bodies are JSON (`Content-Type: application/json`)
- All requests must include both required headers (see above)

### Response Format

- All responses are JSON
- HTTP status codes are used semantically (see Error Reference)
- All timestamps are Unix timestamps (integers, UTC)
- All monetary amounts are integers in cents

### Response Envelope

All single-resource and list responses share a consistent envelope structure. Clients should always read from `data` and never assume the root of a response is the resource itself.

**Single resource:**
```json
{
  "data": { "...resource object..." },
  "meta": {
    "request_id": "string (UUID — for tracing and support)",
    "timestamp": "integer (Unix UTC — when the server processed the request)"
  }
}
```

**List resource:**
```json
{
  "data": ["array of resource objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": {
    "request_id": "string (UUID)",
    "timestamp": "integer (Unix UTC)"
  }
}
```

`meta` is present on every response. It is never null. `request_id` can be used by clients to report issues and by the server for log correlation.

### Workspace Scoping

Every endpoint that returns or mutates financial data is scoped to the workspace identified by `X-Workspace-ID`. The server derives `workspace_id` from this header — validated against the authenticated user's `WorkspaceMember` record — not from JWT claims directly. No `workspace_id` query parameter is accepted on standard feature endpoints.

### Soft Deletes

`DELETE` on any resource endpoint triggers a **soft delete** only — `deleted_at` is set to the current UTC timestamp. No record is ever hard-deleted via the API. Soft-deleted records are excluded from standard list responses (`WHERE deleted_at IS NULL`) but are included in sync pull payloads.

### Pagination

Standard list endpoints use offset-based pagination. The sync pull endpoint uses cursor-based pagination. See **Shared Schemas → Pagination Schema** for full shapes.

---

## Versioning Strategy

### Current Version

The API is currently at `v1`. All endpoints live under `/api/v1/`. This is the first and only active version.

### Backward Compatibility

Within `v1`, all changes must be **additive and non-breaking**:

- New optional fields may be added to request or response schemas
- New endpoints may be added
- Existing required fields must not be removed or renamed
- Existing response field types must not change
- Enum values must not be removed; new values may be added and clients must handle unknown enum values gracefully

Clients are expected to ignore unknown fields in responses (tolerant reader pattern). Android Kotlin Serialization and web JSON parsers must be configured accordingly.

### Breaking Changes

A breaking change requires a new API version (`v2`). Breaking changes include:

- Removing or renaming an existing field
- Changing the type of an existing field
- Removing an endpoint
- Changing the meaning of an existing status code or error response

### Deprecation Strategy

When an endpoint or field is deprecated:

1. The deprecated item is marked in this document with a `[DEPRECATED]` label and a pointer to the replacement
2. A `Deprecation` response header is added to affected responses with the planned sunset date
3. The deprecated item remains functional for a minimum of one full release cycle before removal
4. Removal is only performed in a new major version (`v2`)

As of v0.2.0, no items are deprecated.

### Multi-Client Considerations

Phase 2 introduces Android as the first API client. Phase 3 adds the web client. Phase 4 adds Flutter/KMP. All clients consume the same versioned API. Version negotiation is not supported — all active clients must target the current version.

---

## Authentication & Authorization

### Identity vs. Authorization Boundary

**Supabase owns identity.** Registration, login, token refresh, and logout are handled entirely by the Supabase Auth SDK on the client side (Android SDK, Supabase JS). FastAPI does not implement these flows and does not expose auth endpoints.

**FastAPI owns workspace authorization.** After the Supabase JWT is validated, FastAPI resolves the user's workspace membership via `WorkspaceMember` and enforces role-based access control on every request.

Full auth SDK integration details are specified in `specs/features/auth/spec.md`.

### Supabase JWT Flow

```
Client (Android / Web)
  │
  ├─► Supabase Auth SDK — signs in with email/password
  │        └─► Returns: access_token (JWT), refresh_token
  │
  └─► FastAPI — all subsequent requests include:
           ├─► Authorization: Bearer <access_token>
           └─► X-Workspace-ID: <workspace_uuid>
                    │
                    └─► Validates JWT locally using SUPABASE_JWT_SECRET
                             ├─► Invalid / Expired → 401 Unauthorized
                             └─► Valid → resolve WorkspaceMember → enforce role
```

### Token Validation

The backend validates the following on every JWT:

- **Signature** — verified using `SUPABASE_JWT_SECRET` (no Supabase network call per request)
- **Expiry** (`exp` claim) — rejected if expired
- **Issuer** (`iss` claim) — must match the configured Supabase project URL
- **User membership** — `user_id` from JWT must have an `ACTIVE` WorkspaceMember record for the requested workspace

Token refresh is handled by the Supabase SDK on the client. When the backend returns `401`, the client refreshes and retries. Full lifecycle in `specs/features/auth/spec.md`.

### Role Permission Matrix

Roles are defined on the `WorkspaceMember` entity. Every endpoint specifies the minimum role required.

| Action | OWNER | ADMIN | MEMBER | VIEWER |
|---|---|---|---|---|
| View transactions, transfers, budgets, goals, contributions | ✓ | ✓ | ✓ | ✓ |
| View accounts (name, type, currency only) | ✓ | ✓ | ✓ | ✓ |
| View account balances, credit limits, net worth | ✓ | ✓ | ✓ | ✗ |
| Create / edit / delete transactions, transfers | ✓ | ✓ | ✓ | ✗ |
| Create / edit / delete budgets, goals, contributions | ✓ | ✓ | ✓ | ✗ |
| Create / edit / delete recurring transactions | ✓ | ✓ | ✓ | ✗ |
| Create / edit / delete categories, merchants | ✓ | ✓ | ✗ | ✗ |
| Create / edit / delete accounts | ✓ | ✓ | ✗ | ✗ |
| Edit workspace settings | ✓ | ✓ | ✗ | ✗ |
| Invite / manage / remove members | ✓ | ✓ | ✗ | ✗ |
| Transfer workspace ownership | ✓ | ✗ | ✗ | ✗ |

**VIEWER balance restriction:** VIEWER responses for Account objects omit `initial_balance` and `credit_limit`. Net worth aggregates in the Dashboard are omitted entirely for VIEWERs.

### Workspace Member Invite Flow

Inviting a user to a workspace is a backend-orchestrated flow, not a Supabase Auth flow. Supabase handles identity (ensuring the invitee has or creates an account); FastAPI handles workspace membership.

```
OWNER/ADMIN calls POST /api/v1/workspace-members/invite
  │
  └─► FastAPI creates WorkspaceMember (status: PENDING)
           └─► Generates signed invite token (backend JWT, 7-day expiry)
                    └─► Sends invite email via transactional email provider
                             └─► Invitee receives deep link containing token
                                      │
                                      ├─► Has account → POST /api/v1/workspace-members/accept
                                      └─► No account → Supabase registration → POST /api/v1/workspace-members/accept
                                                   └─► FastAPI sets joined_at, status: ACTIVE
```

**Invite token:** Backend-issued JWT signed with a separate secret, distinct from Supabase tokens. Contains `workspace_id`, `invitee_email`, and `exp` (7 days from issue). Stored in the `WorkspaceMember` record for revocation support.

**Expiry:** 7 days. Configurable via backend environment variable. Expired invites can be resent by OWNER/ADMIN.

**Transactional email provider:** Not yet selected. Resend and SendGrid are candidates. Decision deferred to Phase 2 kickoff.

---

## Shared Schemas

These schemas are reused across multiple endpoints. Individual endpoint definitions reference them by name.

### Metadata

Present on every response in the `meta` field.

```json
{
  "request_id": "string (UUID — for tracing and support correlation)",
  "timestamp": "integer (Unix UTC — server processing time)"
}
```

### Sync Metadata

Present on every entity in sync push and pull payloads. Reflects the sync fields defined in `data-model.md`. Omitted from standard feature endpoint responses.

```json
{
  "sync_status": "PENDING | SYNCED | FAILED | CONFLICT | null",
  "last_synced_at": "integer (Unix UTC) | null",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

**Notes:**
- `updated_at` is always client-provided on push. The server stores it as-is and never overwrites it with the server clock.
- `deleted_at` non-null indicates a soft-deleted record. Included in pull payloads so deletions propagate to all clients.
- `sync_status` and `last_synced_at` in a pull response reflect the server's view. Clients update local values after applying the record.

### Pagination Schema

**Offset-based (standard list endpoints) — request parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Page number. Default: 1 |
| `page_size` | integer | No | Records per page. Default: 20, max: 100 |

**Offset-based — response:**

```json
{
  "pagination": {
    "page": "integer",
    "page_size": "integer",
    "total": "integer",
    "has_next": "boolean"
  }
}
```

**Cursor-based (sync pull only) — request parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `since` | integer (Unix UTC) | No | Watermark from last successful pull. Omit for first full sync. |
| `cursor` | string | No | Opaque cursor from previous page. Omit for first page. |
| `page_size` | integer | No | Records per page. Default: 500, max: 500 |

**Cursor-based — response:**

```json
{
  "pagination": {
    "next_cursor": "string | null",
    "has_next": "boolean",
    "page_size": "integer"
  }
}
```

`next_cursor` is null when all pages are consumed. Clients must exhaust all pages before advancing the local watermark.

### Error Schema

All error responses share this structure:

```json
{
  "error": {
    "code": "string (machine-readable)",
    "message": "string (human-readable)",
    "details": "object | null (field-level errors or additional context)"
  },
  "meta": {
    "request_id": "string (UUID)",
    "timestamp": "integer (Unix UTC)"
  }
}
```

**Standard error codes:**

| Code | HTTP Status | Description |
|---|---|---|
| `UNAUTHORIZED` | 401 | Missing or invalid JWT, or expired token |
| `FORBIDDEN` | 403 | Authenticated but insufficient role, or invalid `X-Workspace-ID` |
| `NOT_FOUND` | 404 | Resource does not exist or is soft-deleted |
| `VALIDATION_ERROR` | 400 | Request body failed schema or business rule validation |
| `CONFLICT` | 409 | Sync conflict detected (see Conflict Schema) |
| `RATE_LIMITED` | 429 | Too many requests; respect `Retry-After` response header |
| `SERVER_ERROR` | 500 | Unexpected server error |

### Conflict Schema

Returned per-record in sync push responses when the server detects a conflict.

```json
{
  "id": "string (UUID)",
  "entity_type": "string (e.g. transaction, account)",
  "conflict_type": "UPDATED_VS_UPDATED | DELETED_VS_UPDATED | UPDATED_VS_DELETED",
  "server_version": { "...full entity object as stored on server..." },
  "client_updated_at": "integer (Unix UTC)",
  "server_updated_at": "integer (Unix UTC)"
}
```

**Conflict types:**
- `UPDATED_VS_UPDATED` — both client and server modified the same record; business fields differ
- `DELETED_VS_UPDATED` — client soft-deleted the record; server has a newer update
- `UPDATED_VS_DELETED` — client has local changes; server soft-deleted the record

`server_version` contains the full entity object so the conflict resolution UI can present both versions. Full resolution flow: `specs/features/sync/spec.md`.

### Batch Result Schema

Returned by `POST /api/v1/sync/push`. One result entry per submitted record.

```json
{
  "results": [
    {
      "id": "string (UUID)",
      "entity_type": "string",
      "status": "success | conflict | error",
      "conflict": "ConflictSchema object | null",
      "error": "ErrorSchema object | null",
      "server_updated_at": "integer (Unix UTC) | null"
    }
  ],
  "summary": {
    "total": "integer",
    "success_count": "integer",
    "conflict_count": "integer",
    "error_count": "integer"
  }
}
```

`conflict` is populated only when `status = conflict`. `error` is populated only when `status = error`. `server_updated_at` is the server's stored `updated_at` after processing — used by the client to reconcile timestamps on successful push.

### Budget Spending Schema

Included in Budget responses as a server-computed `spending` object. Never stored — calculated at query time.

```json
{
  "spending": {
    "planned_amount": "integer (cents) — the budget amount",
    "spent_amount": "integer (cents) — SUM of EXPENSE transactions for this category and period",
    "remaining_amount": "integer (cents) — planned_amount minus spent_amount; negative means overspent"
  }
}
```

Spending status semantics: positive `remaining_amount` = underspent; negative = overspent. Consistent with BR-BU-04 and the Spending Status definition in SPEC.md glossary.

### Goal Progress Schema

Included in Goal responses as a server-computed `progress` object. Never stored — calculated at query time.

```json
{
  "progress": {
    "contributed_amount": "integer (cents) — SUM of active GoalContributions",
    "progress_percentage": "float (0.0–1.0+) — contributed_amount / target_amount; may exceed 1.0 if over-contributed"
  }
}
```

---

## Entity Schemas (Transport Layer)

These schemas define the wire format for each entity as exchanged between client and server. They are distinct from Room entities and domain models used internally by clients. Fields map closely to `data-model.md` but the transport schema is independently versioned.

All entity objects include Sync Metadata fields in sync payloads. Standard feature endpoint responses omit sync metadata fields unless noted.

### Workspace

```json
{
  "id": "string (UUID v4)",
  "name": "string",
  "base_currency": "string (ISO 4217, e.g. USD)",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### WorkspaceMember

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "user_id": "string (UUID v4)",
  "role": "OWNER | ADMIN | MEMBER | VIEWER",
  "status": "PENDING | ACTIVE | REVOKED",
  "invited_at": "integer (Unix UTC) | null",
  "joined_at": "integer (Unix UTC) | null",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

**Note:** The `status` field is a pending delta to `data-model.md` v0.5.0. `data-model.md` must be updated before Phase 2 implementation to add `status`, `invite_token`, and `invite_expires_at` to the WorkspaceMember entity.

**VIEWER visibility:** VIEWER responses omit `user_id` to protect member privacy. Name and role are included.

### Account

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "name": "string",
  "type": "CHECKING | SAVINGS | CREDIT_CARD | CASH",
  "currency_code": "string (ISO 4217)",
  "initial_balance": "integer (cents) — omitted for VIEWER role",
  "credit_limit": "integer (cents) | null — omitted for VIEWER role",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### Category

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "name": "string",
  "icon": "string (emoji) | null",
  "is_default": "boolean",
  "is_hidden": "boolean",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### Merchant

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "name": "string",
  "logo_url": "string | null",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### Transaction

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "account_id": "string (UUID v4)",
  "category_id": "string (UUID v4)",
  "merchant_id": "string (UUID v4) | null",
  "recurring_id": "string (UUID v4) | null",
  "type": "INCOME | EXPENSE",
  "amount": "integer (cents, always positive)",
  "currency_code": "string (ISO 4217)",
  "date": "integer (Unix UTC)",
  "notes": "string | null",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### Transfer

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "from_account_id": "string (UUID v4)",
  "to_account_id": "string (UUID v4)",
  "amount": "integer (cents, always positive)",
  "currency_code": "string (ISO 4217)",
  "date": "integer (Unix UTC)",
  "notes": "string | null",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

### Budget

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "category_id": "string (UUID v4)",
  "amount": "integer (cents)",
  "currency_code": "string (ISO 4217)",
  "period_year": "integer (e.g. 2026)",
  "period_month": "integer (1–12)",
  "carry_forward": "boolean",
  "spending": { "...BudgetSpendingSchema..." },
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

`spending` is always included in feature endpoint responses. It is omitted from sync payloads (derived values are never synced).

### Goal

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "name": "string",
  "target_amount": "integer (cents)",
  "currency_code": "string (ISO 4217)",
  "target_date": "integer (Unix UTC) | null",
  "notes": "string | null",
  "progress": { "...GoalProgressSchema..." },
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

`progress` is always included in feature endpoint responses. It is omitted from sync payloads.

### GoalContribution

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "goal_id": "string (UUID v4)",
  "amount": "integer (cents)",
  "currency_code": "string (ISO 4217)",
  "date": "integer (Unix UTC)",
  "notes": "string | null",
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

---

## Ordering Guarantees

### Canonical Entity Order

The following dependency order governs both sync push payloads and sync pull responses. It ensures that when a record is applied, all records it references as foreign keys already exist in the target database.

| Order | Entity | Depends On |
|---|---|---|
| 1 | Workspace | — |
| 2 | WorkspaceMember | Workspace |
| 3 | Account | Workspace |
| 4 | Category | Workspace |
| 5 | Merchant | Workspace |
| 6 | Goal | Workspace |
| 7 | Transaction | Account, Category, Merchant (optional) |
| 8 | Transfer | Account (×2) |
| 9 | Budget | Category |
| 10 | GoalContribution | Goal |
| 11 | RecurringTransaction | Account, Category, Merchant (optional) — Phase 2 |

**Push:** The client groups records into entity-keyed arrays. The server processes them in this order regardless of array order in the request body — ordering is enforced server-side.

**Pull:** The server returns records grouped by entity type in this dependency order. The client applies them in the order received.

**WorkspaceMember sync visibility:** Only `ACTIVE` WorkspaceMember records are included in pull payloads delivered to MEMBER and VIEWER roles. `PENDING` and `REVOKED` records are visible only to OWNER and ADMIN via the WorkspaceMember feature API.

**Within an entity type:** No business-field sort order (e.g. by date) is applied. Ordering within a type is arbitrary. UI sort order is a query-time concern handled by the client's local database.

---

## Sync API

The sync API provides bidirectional, offline-first data synchronization. Full strategy and conflict resolution are in `specs/technical/offline-sync.md`. This section defines the concrete endpoint contracts.

### POST /api/v1/sync/push

Pushes locally modified records from the client to the server. Accepts a batch of records grouped by entity type. Each record is processed independently — a failure on one record does not affect others.

**Auth requirements:** MEMBER or above (all roles except VIEWER)

**Request schema:**

```json
{
  "workspaces":             ["array of Workspace objects with Sync Metadata | omit key if empty"],
  "workspace_members":      ["array of WorkspaceMember objects with Sync Metadata | omit key if empty"],
  "accounts":               ["array of Account objects with Sync Metadata | omit key if empty"],
  "categories":             ["array of Category objects with Sync Metadata | omit key if empty"],
  "merchants":              ["array of Merchant objects with Sync Metadata | omit key if empty"],
  "goals":                  ["array of Goal objects with Sync Metadata | omit key if empty"],
  "transactions":           ["array of Transaction objects with Sync Metadata | omit key if empty"],
  "transfers":              ["array of Transfer objects with Sync Metadata | omit key if empty"],
  "budgets":                ["array of Budget objects with Sync Metadata | omit key if empty"],
  "goal_contributions":     ["array of GoalContribution objects with Sync Metadata | omit key if empty"],
  "recurring_transactions": ["array of RecurringTransaction objects with Sync Metadata | omit key if empty — Phase 2"]
}
```

Each record includes the full Sync Metadata block. `updated_at` is always client-provided; the server stores it as-is. Computed fields (`spending`, `progress`) must be omitted from push payloads.

**Batch size:** Default 100 records per request across all entity types combined. Larger pending sets are split into sequential requests by the client.

**Force flag:** Include `"force": true` on an individual entity object to instruct the server to accept that record unconditionally, bypassing conflict detection. Used when the user chooses to keep their local version after conflict resolution.

**Response schema:** See Batch Result Schema.

**Sample response:**
```json
{
  "results": [
    {
      "id": "a1b2c3d4-...",
      "entity_type": "transaction",
      "status": "success",
      "conflict": null,
      "error": null,
      "server_updated_at": 1716000000
    },
    {
      "id": "e5f6g7h8-...",
      "entity_type": "account",
      "status": "conflict",
      "conflict": {
        "id": "e5f6g7h8-...",
        "entity_type": "account",
        "conflict_type": "UPDATED_VS_UPDATED",
        "server_version": { "...full Account object..." },
        "client_updated_at": 1715999000,
        "server_updated_at": 1716000500
      },
      "error": null,
      "server_updated_at": null
    }
  ],
  "summary": {
    "total": 2,
    "success_count": 1,
    "conflict_count": 1,
    "error_count": 0
  }
}
```

**Server behavior:**
- Upserts on `id` — creates if not found, updates if found
- Conflict detection: if the server's stored `updated_at` is newer than the incoming record's `updated_at` and `force` is not set, returns `status: conflict` for that record
- `force: true` bypasses conflict detection and applies the client version unconditionally
- Records the client-provided `updated_at` as-is; also writes an internal `server_received_at` (not returned to clients) for audit and telemetry
- Validates all records against schema constraints; invalid records return `status: error` without affecting other records
- Enforces the canonical entity ordering server-side regardless of array order in the request

**HTTP status codes:**

| Code | Meaning |
|---|---|
| 200 | Request processed. Inspect per-record `status` for individual outcomes. |
| 400 | Malformed request body (structural, not per-record validation failures) |
| 401 | Unauthorized |
| 403 | `X-Workspace-ID` mismatch or insufficient role |

**Sync considerations:**
- Client marks successfully pushed records `SYNCED` and updates `last_synced_at`
- `conflict` records are marked `CONFLICT` locally; not overwritten until user resolves
- `error` records are marked `FAILED` locally and retried with exponential backoff

---

### GET /api/v1/sync/pull

Pulls records modified on the server since the client's last watermark. Returns records in canonical entity dependency order using cursor-based pagination. Includes soft-deleted records.

**Auth requirements:** VIEWER or above (all roles)

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `since` | integer (Unix UTC) | No | Watermark from last successful pull. Omit for first full sync — server returns all workspace records. |
| `cursor` | string | No | Opaque pagination cursor from previous page. Omit for first page. |
| `page_size` | integer | No | Records per page. Default: 500, max: 500. |

**Response schema:**

```json
{
  "data": {
    "workspaces":             ["array of Workspace objects with Sync Metadata"],
    "workspace_members":      ["array of WorkspaceMember objects with Sync Metadata"],
    "accounts":               ["array of Account objects with Sync Metadata"],
    "categories":             ["array of Category objects with Sync Metadata"],
    "merchants":              ["array of Merchant objects with Sync Metadata"],
    "goals":                  ["array of Goal objects with Sync Metadata"],
    "transactions":           ["array of Transaction objects with Sync Metadata"],
    "transfers":              ["array of Transfer objects with Sync Metadata"],
    "budgets":                ["array of Budget objects with Sync Metadata"],
    "goal_contributions":     ["array of GoalContribution objects with Sync Metadata"],
    "recurring_transactions": ["array of RecurringTransaction objects with Sync Metadata — Phase 2"]
  },
  "pagination": {
    "next_cursor": "string | null",
    "has_next": "boolean",
    "page_size": "integer"
  },
  "meta": { "...Metadata..." }
}
```

**Server behavior:**
- Returns all records where `updated_at > since`, scoped to the authenticated workspace
- Includes soft-deleted records (`deleted_at IS NOT NULL`) so deletions propagate
- Filters WorkspaceMember records by role: MEMBER/VIEWER receive only `ACTIVE` members; OWNER/ADMIN receive all statuses
- Returns records grouped by entity type in canonical dependency order
- Cursor is a composite of `updated_at` + `id` of the last record on the current page — stable under concurrent server-side inserts
- If `since` is omitted, all workspace records are returned (first full sync)

**Client behavior:**
- Must follow all pages until `has_next = false` before advancing the local watermark
- For each received record: if local `sync_status = SYNCED`, overwrite with server version; if `sync_status IN (PENDING, FAILED, NULL)`, perform semantic equality check before flagging conflict (see `offline-sync.md`)
- Advance watermark only after all pages are successfully applied

**HTTP status codes:**

| Code | Meaning |
|---|---|
| 200 | Success |
| 400 | Invalid `since` or `cursor` parameter |
| 401 | Unauthorized |
| 403 | Invalid `X-Workspace-ID` |

---

## Feature APIs

Feature APIs provide CRUD access for UI-driven flows. In Phase 2, the Android client reads primarily from Room (offline-first) and uses sync for server propagation. Feature endpoints are used for specific read scenarios and are the primary data source for the web client in Phase 3.

### Date Filtering

The following filter parameters are standardized across all list endpoints that return date-scoped records (Transactions, Transfers, GoalContributions, and Budget). Two approaches are supported:

**Period presets** — convenience shortcuts the server resolves to a date range:

| Value | Resolves To |
|---|---|
| `today` | Current calendar day (UTC) |
| `this_week` | Monday–Sunday of the current week (UTC) |
| `this_month` | First to last day of the current calendar month |
| `last_month` | First to last day of the previous calendar month |
| `ytd` | January 1 to today of the current year |
| `this_year` | Full current calendar year |

**Explicit date range** — `date_from` and `date_to` as Unix timestamps (inclusive). Takes precedence over `period` if both are provided.

**Note:** All period presets are resolved server-side in UTC. Timezone handling is deferred — see Open Questions.

---

### Workspace API

#### GET /api/v1/workspaces

**Purpose:** List all workspaces the authenticated user belongs to. In Phase 2 this returns exactly one workspace. In Phase 4 it returns all workspaces the user is an active member of.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |

**Response schema:**
```json
{
  "data": ["array of Workspace objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-WS-01, BR-WS-03

**Sync considerations:** Phase 4 multi-workspace — client uses this list to populate the workspace switcher and determine valid `X-Workspace-ID` values.

---

#### GET /api/v1/workspaces/{workspace_id}

**Purpose:** Retrieve details for a specific workspace.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Workspace object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `workspace_id` must match the `X-Workspace-ID` header value. Requesting a workspace the user is not a member of returns `403`.

**Business rule references:** BR-WS-01, BR-WS-02

---

#### PATCH /api/v1/workspaces/{workspace_id}

**Purpose:** Update workspace settings.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "name": "string | omit if unchanged",
  "base_currency": "string (ISO 4217) | omit if unchanged"
}
```

**Response schema:**
```json
{
  "data": { "...updated Workspace object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `name` must be non-empty if provided
- `base_currency` must be a valid ISO 4217 code if provided

**Business rule references:** BR-WS-01, BR-CU-01

**Sync considerations:** Change propagates to all clients via pull on next sync cycle.

---

### WorkspaceMember API

#### GET /api/v1/workspace-members

**Purpose:** List workspace members. OWNER/ADMIN see all statuses (PENDING, ACTIVE, REVOKED). MEMBER and VIEWER see only ACTIVE members.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `status` | string | No | Filter by status: `PENDING`, `ACTIVE`, `REVOKED`. MEMBER/VIEWER may only use `ACTIVE`. |

**Response schema:**
```json
{
  "data": ["array of WorkspaceMember objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-WS-05

---

#### POST /api/v1/workspace-members/invite

**Purpose:** Invite a user to the workspace by email. Creates a PENDING WorkspaceMember record and sends an invite email.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "email": "string (valid email address)",
  "role": "ADMIN | MEMBER | VIEWER"
}
```

**Response schema:**
```json
{
  "data": { "...WorkspaceMember object (status: PENDING)..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `email` must be a valid email address
- `role` must be one of `ADMIN`, `MEMBER`, `VIEWER` — OWNER cannot be assigned via invite
- If a PENDING invite already exists for this email in this workspace, the existing invite is resent rather than creating a duplicate
- If the email already belongs to an ACTIVE member of this workspace, returns `400 VALIDATION_ERROR`

**Business rule references:** BR-WS-05

**Sync considerations:** PENDING WorkspaceMember records are not included in pull payloads for MEMBER/VIEWER clients. OWNER/ADMIN clients receive them.

---

#### POST /api/v1/workspace-members/accept

**Purpose:** Accept a workspace invite using the token from the invite email. Activates the WorkspaceMember record.

**Auth requirements:** Valid Supabase JWT (invitee must be authenticated). No workspace membership required — this endpoint is exempt from `X-Workspace-ID` validation since the user is not yet a member.

**Request schema:**
```json
{
  "invite_token": "string (signed backend JWT from invite email)"
}
```

**Response schema:**
```json
{
  "data": { "...WorkspaceMember object (status: ACTIVE)..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `invite_token` must be a valid, unexpired backend-issued JWT
- Token `invitee_email` must match the authenticated user's email
- If the token is expired, returns `400 VALIDATION_ERROR` with a prompt to request a new invite

**Business rule references:** BR-WS-05

**Sync considerations:** On activation, the new member's first sync pull returns all ACTIVE workspace data.

---

#### POST /api/v1/workspace-members/{member_id}/resend-invite

**Purpose:** Resend the invite email for a PENDING member. Generates a new token and resets the expiry.

**Auth requirements:** ADMIN or above

**Request schema:** None

**Response schema:**
```json
{
  "data": { "...WorkspaceMember object (status: PENDING, updated invited_at)..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Target member must have `status: PENDING`
- Returns `400 VALIDATION_ERROR` if member is already ACTIVE or REVOKED

---

#### PATCH /api/v1/workspace-members/{member_id}

**Purpose:** Change a member's role.

**Auth requirements:** ADMIN or above. OWNER role can only be assigned or transferred by the current OWNER.

**Request schema:**
```json
{
  "role": "OWNER | ADMIN | MEMBER | VIEWER"
}
```

**Response schema:**
```json
{
  "data": { "...updated WorkspaceMember object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Assigning `OWNER` transfers ownership — the current OWNER is downgraded to `ADMIN` atomically. Only the current OWNER may perform this action.
- A workspace must always have exactly one OWNER (BR-WS-05)
- An ADMIN cannot change the OWNER's role

**Business rule references:** BR-WS-05

**Sync considerations:** Role changes propagate to all clients via pull. Clients must re-evaluate permission-gated UI immediately on sync.

---

#### DELETE /api/v1/workspace-members/{member_id}

**Purpose:** Remove a member from the workspace. Sets `status: REVOKED` and `deleted_at`. The member loses access immediately.

**Auth requirements:** ADMIN or above. Members may delete their own membership (self-removal) regardless of role.

**Response schema:** HTTP 204 No Content

**Validation rules:**
- OWNER cannot be removed. The OWNER role must be transferred before removal.
- Removing the last OWNER returns `400 VALIDATION_ERROR` (BR-WS-05)

**Business rule references:** BR-WS-03, BR-WS-05, BR-DI-01

**Sync considerations:** REVOKED members lose access immediately. Their subsequent requests return `403`. Their local data is not automatically purged — this is a client-side responsibility on logout.

---

### Account API

#### GET /api/v1/accounts

**Purpose:** List all active accounts in the workspace.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |

**Response schema:**
```json
{
  "data": ["array of Account objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-AC-01, BR-AC-02, BR-CU-02

**Sync considerations:** `initial_balance` and `credit_limit` are omitted for VIEWER role responses.

---

#### GET /api/v1/accounts/{account_id}

**Purpose:** Retrieve a single account.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Account object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

**Business rule references:** BR-AC-01, BR-CU-02

---

#### POST /api/v1/accounts

**Purpose:** Create a new account.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "type": "CHECKING | SAVINGS | CREDIT_CARD | CASH",
  "currency_code": "string (ISO 4217)",
  "initial_balance": "integer (cents)",
  "credit_limit": "integer (cents) | null"
}
```

**Response schema:**
```json
{
  "data": { "...created Account object..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `id` must be a valid UUID v4 (client-generated for offline-first idempotency)
- `name` must be non-empty and unique within the workspace (case-insensitive, excluding soft-deleted)
- `type` must be one of the defined ENUM values
- `currency_code` must be a valid ISO 4217 code
- `credit_limit` must be a positive integer if provided, and is only meaningful for `CREDIT_CARD` type

**Business rule references:** BR-AC-01, BR-AC-03, BR-CU-02

---

#### PATCH /api/v1/accounts/{account_id}

**Purpose:** Update an existing account.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "name": "string | omit if unchanged",
  "credit_limit": "integer (cents) | null | omit if unchanged"
}
```

Note: `type` and `currency_code` are immutable after creation.

**Response schema:**
```json
{
  "data": { "...updated Account object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `name` must be non-empty and remain unique within the workspace if provided
- `credit_limit` only meaningful for `CREDIT_CARD` accounts

**Business rule references:** BR-AC-01, BR-CU-02

---

#### DELETE /api/v1/accounts/{account_id}

**Purpose:** Soft-delete an account.

**Auth requirements:** ADMIN or above

**Response schema:** HTTP 204 No Content

**Validation rules:**
- Account must have no associated active transactions or transfers. Returns `400 VALIDATION_ERROR` with detail if violated.

**Business rule references:** BR-AC-03, BR-DI-01

**Sync considerations:** Sets `deleted_at`. Propagates to all clients via pull.

---

### Category API

#### GET /api/v1/categories

**Purpose:** List categories in the workspace.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `include_hidden` | boolean | No | Include hidden categories. Default: false |

**Response schema:**
```json
{
  "data": ["array of Category objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-CA-01, BR-CA-02

---

#### GET /api/v1/categories/{category_id}

**Purpose:** Retrieve a single category.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Category object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/categories

**Purpose:** Create a custom category.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "icon": "string (emoji) | null"
}
```

**Response schema:**
```json
{
  "data": { "...created Category object..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `name` must be non-empty and unique within the workspace
- `is_default` is always `false` for user-created categories

**Business rule references:** BR-CA-02

---

#### PATCH /api/v1/categories/{category_id}

**Purpose:** Update a category's name, icon, or hidden state.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "name": "string | omit if unchanged",
  "icon": "string (emoji) | null | omit if unchanged",
  "is_hidden": "boolean | omit if unchanged"
}
```

**Response schema:**
```json
{
  "data": { "...updated Category object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `name` must be non-empty and remain unique within the workspace if provided

**Business rule references:** BR-CA-01, BR-CA-02

---

#### DELETE /api/v1/categories/{category_id}

**Purpose:** Soft-delete a custom category.

**Auth requirements:** ADMIN or above

**Response schema:** HTTP 204 No Content

**Validation rules:**
- Category must have no associated active transactions. Returns `400 VALIDATION_ERROR` if violated.
- Default categories (`is_default = true`) cannot be soft-deleted. Returns `403 FORBIDDEN`.

**Business rule references:** BR-CA-01, BR-CA-02, BR-DI-01

---

### Merchant API

#### GET /api/v1/merchants

**Purpose:** List active merchants in the workspace.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `q` | string | No | Search by merchant name (partial match) |

**Response schema:**
```json
{
  "data": ["array of Merchant objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-ME-01

---

#### GET /api/v1/merchants/{merchant_id}

**Purpose:** Retrieve a single merchant.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Merchant object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/merchants

**Purpose:** Create a new merchant.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "logo_url": "string | null"
}
```

**Response schema:**
```json
{
  "data": { "...created Merchant object..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `name` must be non-empty and unique within the workspace

**Business rule references:** BR-ME-01

---

#### PATCH /api/v1/merchants/{merchant_id}

**Purpose:** Update a merchant.

**Auth requirements:** ADMIN or above

**Request schema:**
```json
{
  "name": "string | omit if unchanged",
  "logo_url": "string | null | omit if unchanged"
}
```

**Response schema:**
```json
{
  "data": { "...updated Merchant object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `name` must be non-empty and remain unique within the workspace if provided

**Business rule references:** BR-ME-01

---

#### DELETE /api/v1/merchants/{merchant_id}

**Purpose:** Soft-delete a merchant.

**Auth requirements:** ADMIN or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-ME-01, BR-DI-01

**Sync considerations:** Existing transactions retain `merchant_id` after soft delete. The merchant is hidden from selection UI but the reference is preserved in historical records.

---

### Transaction API

#### GET /api/v1/transactions

**Purpose:** List transactions in the workspace with optional filtering.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `account_id` | UUID | No | Filter by account |
| `category_id` | UUID | No | Filter by category |
| `merchant_id` | UUID | No | Filter by merchant |
| `type` | string | No | Filter by type: `INCOME` or `EXPENSE` |
| `period` | string | No | Period preset: `today`, `this_week`, `this_month`, `last_month`, `ytd`, `this_year` |
| `date_from` | integer (Unix UTC) | No | Start of explicit date range (inclusive). Overrides `period`. |
| `date_to` | integer (Unix UTC) | No | End of explicit date range (inclusive). Overrides `period`. |

**Response schema:**
```json
{
  "data": ["array of Transaction objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-TX-01, BR-TX-02

---

#### GET /api/v1/transactions/{transaction_id}

**Purpose:** Retrieve a single transaction.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Transaction object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/transactions

**Purpose:** Create a new transaction.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "account_id": "string (UUID v4)",
  "category_id": "string (UUID v4)",
  "merchant_id": "string (UUID v4) | null",
  "type": "INCOME | EXPENSE",
  "amount": "integer (cents, positive)",
  "date": "integer (Unix UTC)",
  "notes": "string | null"
}
```

Note: `currency_code` is not accepted in the request — it is inherited from the linked Account at creation time (BR-CU-03).

**Response schema:**
```json
{
  "data": { "...created Transaction object (includes currency_code)..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `account_id` must reference an active account in the workspace
- `category_id` must reference an active, non-hidden category in the workspace
- `merchant_id`, if provided, must reference an existing (active or soft-deleted) merchant in the workspace
- `amount` must be a positive integer (> 0)
- `type` must be `INCOME` or `EXPENSE`

**Business rule references:** BR-TX-01, BR-TX-02, BR-CU-03

**Sync considerations:** `currency_code` is set server-side from the linked Account and returned in the response. The client must update the locally cached `currency_code` on the record after a successful push.

---

#### PATCH /api/v1/transactions/{transaction_id}

**Purpose:** Update an existing transaction.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "category_id": "string (UUID v4) | omit if unchanged",
  "merchant_id": "string (UUID v4) | null | omit if unchanged",
  "amount": "integer (cents, positive) | omit if unchanged",
  "date": "integer (Unix UTC) | omit if unchanged",
  "notes": "string | null | omit if unchanged"
}
```

Note: `account_id`, `type`, and `currency_code` are immutable after creation.

**Response schema:**
```json
{
  "data": { "...updated Transaction object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `category_id` must reference an active, non-hidden category if provided
- `amount` must be a positive integer if provided

**Business rule references:** BR-TX-01, BR-CU-03

---

#### DELETE /api/v1/transactions/{transaction_id}

**Purpose:** Soft-delete a transaction.

**Auth requirements:** MEMBER or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-DI-01

**Sync considerations:** Sets `deleted_at`. Budget spending calculations exclude soft-deleted transactions automatically.

---

### Transfer API

#### GET /api/v1/transfers

**Purpose:** List transfers in the workspace with optional filtering.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `account_id` | UUID | No | Matches either `from_account_id` or `to_account_id` |
| `period` | string | No | Period preset: `today`, `this_week`, `this_month`, `last_month`, `ytd`, `this_year` |
| `date_from` | integer (Unix UTC) | No | Start of explicit date range (inclusive). Overrides `period`. |
| `date_to` | integer (Unix UTC) | No | End of explicit date range (inclusive). Overrides `period`. |

**Response schema:**
```json
{
  "data": ["array of Transfer objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-TX-02, BR-TR-01, BR-TR-02

---

#### GET /api/v1/transfers/{transfer_id}

**Purpose:** Retrieve a single transfer.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Transfer object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/transfers

**Purpose:** Create a new transfer between two accounts.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "from_account_id": "string (UUID v4)",
  "to_account_id": "string (UUID v4)",
  "amount": "integer (cents, positive)",
  "date": "integer (Unix UTC)",
  "notes": "string | null"
}
```

Note: `currency_code` is inherited from the source account by the server.

**Response schema:**
```json
{
  "data": { "...created Transfer object..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `from_account_id` and `to_account_id` must both be active accounts in the workspace
- `from_account_id` and `to_account_id` must be different accounts
- Both accounts must share the same `currency_code` in Phase 2 (BR-TR-02)
- `amount` must be a positive integer

**Business rule references:** BR-TR-01, BR-TR-02, BR-TX-02

**Sync considerations:** Transfers are immutable after creation — no PATCH endpoint. To correct a transfer, soft-delete and re-create.

---

#### DELETE /api/v1/transfers/{transfer_id}

**Purpose:** Soft-delete a transfer.

**Auth requirements:** MEMBER or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-TR-01, BR-DI-01

---

### Budget API

#### GET /api/v1/budgets

**Purpose:** List budgets for the workspace with optional period filtering. Includes server-computed spending totals.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `period_year` | integer | No | Filter by year (e.g. 2026). Used with `period_month`. |
| `period_month` | integer | No | Filter by month (1–12). Used with `period_year`. |
| `period` | string | No | Period preset as a shortcut. `this_month` maps to current `period_year` + `period_month`. Overridden by explicit `period_year`/`period_month`. |
| `category_id` | UUID | No | Filter by category |

**Response schema:**
```json
{
  "data": ["array of Budget objects (each includes spending object)"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-BU-01 through BR-BU-05

---

#### GET /api/v1/budgets/{budget_id}

**Purpose:** Retrieve a single budget with computed spending totals.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Budget object with spending..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/budgets

**Purpose:** Create a budget for a category and period.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "category_id": "string (UUID v4)",
  "amount": "integer (cents, positive)",
  "period_year": "integer",
  "period_month": "integer (1–12)",
  "carry_forward": "boolean (default: true)"
}
```

Note: `currency_code` defaults to workspace `base_currency` (BR-CU-05) and is not accepted in the request.

**Response schema:**
```json
{
  "data": { "...created Budget object with spending..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `category_id` must reference an active category
- `amount` must be a positive integer
- `period_month` must be 1–12
- Unique constraint: one budget per category per period per workspace. Returns `400 VALIDATION_ERROR` if a budget already exists for that combination.

**Business rule references:** BR-BU-01, BR-BU-02, BR-BU-03, BR-BU-04, BR-CU-05

---

#### PATCH /api/v1/budgets/{budget_id}

**Purpose:** Update a budget's amount or carry-forward flag.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "amount": "integer (cents, positive) | omit if unchanged",
  "carry_forward": "boolean | omit if unchanged"
}
```

**Response schema:**
```json
{
  "data": { "...updated Budget object with spending..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `amount` must be a positive integer if provided

**Business rule references:** BR-BU-01, BR-BU-05

---

#### DELETE /api/v1/budgets/{budget_id}

**Purpose:** Soft-delete a budget.

**Auth requirements:** MEMBER or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-BU-02, BR-DI-01

**Sync considerations:** Deleting a budget does not affect existing transactions. Auto-generated `carry_forward` budgets for future months are independent records and are unaffected.

---

### Goal API

#### GET /api/v1/goals

**Purpose:** List all active goals in the workspace. Includes server-computed progress.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |

**Response schema:**
```json
{
  "data": ["array of Goal objects (each includes progress object)"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-GL-01, BR-GL-02

---

#### GET /api/v1/goals/{goal_id}

**Purpose:** Retrieve a single goal with computed progress.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...Goal object with progress..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/goals

**Purpose:** Create a new goal.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "target_amount": "integer (cents, positive)",
  "currency_code": "string (ISO 4217)",
  "target_date": "integer (Unix UTC) | null",
  "notes": "string | null"
}
```

**Response schema:**
```json
{
  "data": { "...created Goal object with progress..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `name` must be non-empty
- `target_amount` must be a positive integer
- `currency_code` must be a valid ISO 4217 code

**Business rule references:** BR-GL-01, BR-GL-02

---

#### PATCH /api/v1/goals/{goal_id}

**Purpose:** Update a goal.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "name": "string | omit if unchanged",
  "target_amount": "integer (cents, positive) | omit if unchanged",
  "target_date": "integer (Unix UTC) | null | omit if unchanged",
  "notes": "string | null | omit if unchanged"
}
```

Note: `currency_code` is immutable after creation.

**Response schema:**
```json
{
  "data": { "...updated Goal object with progress..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `target_amount` must be a positive integer if provided

**Business rule references:** BR-GL-01

---

#### DELETE /api/v1/goals/{goal_id}

**Purpose:** Soft-delete a goal.

**Auth requirements:** MEMBER or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-DI-01

**Sync considerations:** Soft-deleting a goal does not cascade to its contributions. Contributions remain for historical accuracy but are excluded from progress calculations. See Open Questions for the cascade decision.

---

### Goal Contribution API

#### GET /api/v1/goal-contributions

**Purpose:** List goal contributions with optional filtering.

**Auth requirements:** VIEWER or above

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `page` | integer | No | Default: 1 |
| `page_size` | integer | No | Default: 20, max: 100 |
| `goal_id` | UUID | No | Filter by goal |
| `period` | string | No | Period preset: `today`, `this_week`, `this_month`, `last_month`, `ytd`, `this_year` |
| `date_from` | integer (Unix UTC) | No | Start of explicit date range (inclusive). Overrides `period`. |
| `date_to` | integer (Unix UTC) | No | End of explicit date range (inclusive). Overrides `period`. |

**Response schema:**
```json
{
  "data": ["array of GoalContribution objects"],
  "pagination": { "...PaginationSchema..." },
  "meta": { "...Metadata..." }
}
```

**Business rule references:** BR-GL-01, BR-GL-02

---

#### GET /api/v1/goal-contributions/{contribution_id}

**Purpose:** Retrieve a single goal contribution.

**Auth requirements:** VIEWER or above

**Response schema:**
```json
{
  "data": { "...GoalContribution object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the workspace
- Returns `404` if soft-deleted

---

#### POST /api/v1/goal-contributions

**Purpose:** Record a contribution toward a goal.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "id": "string (UUID v4, client-generated)",
  "goal_id": "string (UUID v4)",
  "amount": "integer (cents, positive)",
  "date": "integer (Unix UTC)",
  "notes": "string | null"
}
```

Note: `currency_code` is inherited from the parent Goal by the server.

**Response schema:**
```json
{
  "data": { "...created GoalContribution object (includes currency_code)..." },
  "meta": { "...Metadata..." }
}
```

HTTP 201.

**Validation rules:**
- `goal_id` must reference an active goal in the workspace
- `amount` must be a positive integer

**Business rule references:** BR-GL-01, BR-GL-02

**Sync considerations:** `currency_code` is set server-side from the parent Goal. Client must update locally cached `currency_code` after successful push.

---

#### PATCH /api/v1/goal-contributions/{contribution_id}

**Purpose:** Update a goal contribution.

**Auth requirements:** MEMBER or above

**Request schema:**
```json
{
  "amount": "integer (cents, positive) | omit if unchanged",
  "date": "integer (Unix UTC) | omit if unchanged",
  "notes": "string | null | omit if unchanged"
}
```

Note: `goal_id` and `currency_code` are immutable after creation.

**Response schema:**
```json
{
  "data": { "...updated GoalContribution object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `amount` must be a positive integer if provided

**Business rule references:** BR-GL-02

---

#### DELETE /api/v1/goal-contributions/{contribution_id}

**Purpose:** Soft-delete a goal contribution.

**Auth requirements:** MEMBER or above

**Response schema:** HTTP 204 No Content

**Business rule references:** BR-GL-01, BR-DI-01

**Sync considerations:** Goal progress is recalculated at query time. Soft-deleted contributions are excluded from `contributed_amount` automatically.

---

### User API *(Phase 2 stub)*

User management endpoints are introduced in Phase 2 alongside Supabase Auth. Full request/response schemas are defined in `specs/features/auth/spec.md`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/users/me` | Retrieve the authenticated user's profile |
| `PATCH` | `/api/v1/users/me` | Update display name or avatar |
| `DELETE` | `/api/v1/users/me` | Deactivate account (soft delete) |

---

### Recurring Transaction API *(Phase 2 stub)*

Recurring transactions are introduced in Phase 2. Full spec: `specs/features/recurring-transactions/spec.md`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/recurring-transactions` | List all active recurring transaction templates |
| `GET` | `/api/v1/recurring-transactions/{id}` | Retrieve a single template |
| `POST` | `/api/v1/recurring-transactions` | Create a recurring transaction template |
| `PATCH` | `/api/v1/recurring-transactions/{id}` | Update a template |
| `DELETE` | `/api/v1/recurring-transactions/{id}` | Soft-delete a template |

---

### Dashboard API

#### GET /api/v1/dashboard/summary

**Purpose:** Return a pre-aggregated snapshot of the workspace's financial health for display on the dashboard. Designed as a Backend For Frontend (BFF) endpoint — a single request that assembles the data the dashboard screen needs, avoiding multiple round-trips from the web client. The Android client generates equivalent data locally from Room queries and does not call this endpoint in Phase 2.

**Auth requirements:** VIEWER or above. Net worth block is omitted entirely for VIEWER role.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `period_year` | integer | No | Year for budget and spending blocks. Default: current year. |
| `period_month` | integer | No | Month for budget and spending blocks. Default: current month. |

**Response schema:**

```json
{
  "data": {
    "net_worth": {
      "total_assets": "integer (cents) — sum of CHECKING + SAVINGS account balances",
      "total_liabilities": "integer (cents) — sum of CREDIT_CARD balances",
      "net_worth": "integer (cents) — assets minus liabilities",
      "currency_code": "string (ISO 4217) — workspace base_currency",
      "note": "string | null — present if multi-currency accounts exist and totals are partial"
    },
    "current_period": {
      "period_year": "integer",
      "period_month": "integer",
      "total_income": "integer (cents)",
      "total_expenses": "integer (cents)",
      "net": "integer (cents) — income minus expenses"
    },
    "previous_period": {
      "period_year": "integer",
      "period_month": "integer",
      "total_income": "integer (cents)",
      "total_expenses": "integer (cents)",
      "net": "integer (cents)"
    },
    "budget_status": [
      {
        "category_id": "string (UUID v4)",
        "category_name": "string",
        "category_icon": "string | null",
        "planned_amount": "integer (cents)",
        "spent_amount": "integer (cents)",
        "remaining_amount": "integer (cents)"
      }
    ],
    "top_spending_categories": [
      {
        "category_id": "string (UUID v4)",
        "category_name": "string",
        "category_icon": "string | null",
        "spent_amount": "integer (cents)",
        "percentage_of_total": "float (0.0–1.0)"
      }
    ],
    "goal_progress": [
      {
        "goal_id": "string (UUID v4)",
        "goal_name": "string",
        "target_amount": "integer (cents)",
        "contributed_amount": "integer (cents)",
        "progress_percentage": "float (0.0–1.0+)",
        "target_date": "integer (Unix UTC) | null"
      }
    ],
    "recent_transactions": [
      { "...Transaction object (last 10, sorted by date desc)..." }
    ]
  },
  "meta": { "...Metadata..." }
}
```

**Notes:**
- `budget_status` covers all budgets for the requested period, sorted by `remaining_amount` ascending (most overspent first)
- `top_spending_categories` is the top 5 categories by `spent_amount` for the requested period
- `net_worth` is omitted for VIEWER role
- `note` in `net_worth` is included when the workspace has accounts in multiple currencies — aggregation is in `base_currency` only; cross-currency accounts are excluded from the total with an explanatory note
- This endpoint is primarily for the Phase 3 web client. The Android client computes equivalent data locally from Room.

**Business rule references:** BR-AC-01, BR-AC-02, BR-BU-01, BR-BU-04, BR-GL-01

**Sync considerations:** Read-only. Not part of the sync push/pull cycle.

---

## Security Considerations

### HTTPS Enforcement

All API traffic must occur over HTTPS. The backend rejects plain HTTP connections. TLS termination is handled at the hosting layer (Render or Railway — decision deferred to Phase 2 kickoff).

### JWT Validation

The backend validates every incoming JWT locally using `SUPABASE_JWT_SECRET`. No network call to Supabase is made per request. Validation checks: signature, expiry (`exp`), and issuer (`iss`). Expired or malformed tokens return `401 Unauthorized`.

### Workspace Isolation

All queries are filtered by `workspace_id` derived from the validated `X-Workspace-ID` header, cross-referenced against the authenticated user's `WorkspaceMember` record. This is enforced at the service and query layer — not just at the route level — and applies to sync endpoints as well as feature endpoints. A user cannot read or mutate records belonging to a workspace they are not an `ACTIVE` member of.

### Financial Data Protection

- Financial data is never included in server logs in plain text
- No financial data is transmitted to third-party analytics, crash reporting, or monitoring services
- This policy applies in all phases (NFR-DS-01 and its Phase 2 extension)

### Client-Side Security (Android)

- JWTs are stored in `EncryptedSharedPreferences` — never in plain SharedPreferences, files, or logcat
- Network requests are made over HTTPS only; certificate pinning via OkHttp is an open decision (see Open Questions)

### Rate Limiting

The API enforces rate limits per authenticated user. When the limit is exceeded, the server returns `429 Too Many Requests` with a `Retry-After` header. The Android sync client respects this header and delays the retry accordingly (see `offline-sync.md` — Retry Strategy).

### Input Validation

All incoming request bodies are validated against the schemas defined in this document before any database operation is performed. Validation failures return `400 VALIDATION_ERROR` with field-level detail in `error.details`. The server never trusts client-supplied `workspace_id` on feature endpoint bodies — it is always derived from the validated `X-Workspace-ID` header.

---

## Open Questions

- **Timezone handling for period presets** — period presets (`today`, `this_week`, etc.) are currently resolved in UTC. A `timezone` field on the Workspace entity (IANA format, e.g. `America/New_York`) would allow server-side resolution in the user's local time. Deferred — revisit before Phase 3 web implementation.
- **Text search on transactions** — should `GET /api/v1/transactions` support a `q` parameter for searching merchant name or notes? Useful for the web dashboard. Deferred to Phase 3 feature spec.
- **Goal cascade on delete** — should soft-deleting a goal cascade a soft-delete to its GoalContributions, or leave them as orphaned records? Current behavior leaves them independent. Decision required before Phase 2 implementation.
- **WorkspaceMember invite token storage** — the invite token should be stored in the `WorkspaceMember` record for revocation support. `data-model.md` must be updated to add `invite_token` (TEXT, nullable) and `invite_expires_at` (INTEGER, nullable) fields before Phase 2 implementation.
- **WorkspaceMember.status field** — `status` (`PENDING`, `ACTIVE`, `REVOKED`) is defined in this document but not yet in `data-model.md` v0.5.0. Must be added before Phase 2 implementation.
- **Transactional email provider** — invite emails require a provider (Resend and SendGrid are candidates). Decision at Phase 2 kickoff.
- **Certificate pinning on Android** — implement from Phase 2 start via OkHttp, or defer? Risk: pinning requires maintenance when certs rotate. Decision at Phase 2 kickoff.
- **Pull page size configurability** — hardcode at 500 for Phase 2 or make configurable via remote config from the start? Carried from `offline-sync.md`. Decision at Phase 2 kickoff.
- **Dashboard endpoint iteration** — content of `GET /api/v1/dashboard/summary` should be validated against actual UI mockups once `specs/features/dashboard/spec.md` is written. The response schema may need adjustment.
- **X-Workspace-ID in Phase 4** — confirm that passing the workspace UUID as a header is the right multi-workspace switching mechanism, or evaluate alternatives (e.g. URL prefix `/workspaces/{id}/`) at Phase 4 kickoff.
- **Budget spending on list vs detail** — `spending` is currently included on both `GET /api/v1/budgets` (list) and `GET /api/v1/budgets/{id}` (detail). For large workspaces with many budget rows, computing spending totals for every row on a list call may be expensive. Revisit with performance benchmarks at Phase 2 implementation.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-19 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-05-20 | Danielle Mariani | Add X-Workspace-ID header and multi-workspace strategy. Clarify Supabase owns identity, FastAPI owns authorization. Remove auth endpoint stubs. Add Role Permission Matrix with VIEWER balance restriction. Add WorkspaceMember entity schema and full API (invite flow, accept, resend, role change, remove). Add WorkspaceMember to canonical sync entity order at position 2. Add Budget spending and Goal progress computed schemas. Add Dashboard BFF endpoint. Normalize all query parameters to table format. Add standardized date filtering (period presets + date range) across Transactions, Transfers, GoalContributions, Budgets. Add response envelope clarification with data/meta example. Update Security Considerations to reflect X-Workspace-ID workspace isolation. Expand Open Questions. |
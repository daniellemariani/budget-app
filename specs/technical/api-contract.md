# API Contract — Budget App

**Version:** 0.1.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-05-19
**Last Updated:** 2026-05-19

---

## Overview

This document is the canonical API contract for the Budget App backend (FastAPI). It defines all endpoints, request/response schemas, validation rules, and sync behavior for Phase 2 and beyond.

This document owns the **transport layer** — the wire format exchanged between clients and the server. Transport schemas defined here are distinct from domain entities (defined in `specs/technical/data-model.md`) and Room entities used internally by the Android client. They may look similar at the start but diverge as the API evolves independently of the data model.

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
| specs/features/auth/spec.md | Full auth feature spec (expanded in Phase 2) |
| specs/features/sync/spec.md | Sync UI spec (conflict resolution UI, sync status indicators) |

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
- Sub-resources are nested only when the parent ID is required for context (e.g. `/goals/{goal_id}/contributions` is acceptable but avoided in sync payloads — see Entity Schemas)

### Request Format

- All request bodies are JSON (`Content-Type: application/json`)
- All requests must include `Authorization: Bearer <jwt>` (see Authentication & Authorization)

### Response Format

- All responses are JSON
- HTTP status codes are used semantically (see Error Reference)
- All timestamps are Unix timestamps (integers, UTC)
- All monetary amounts are integers in cents

### Workspace Scoping

Every endpoint that returns or mutates financial data is implicitly scoped to the authenticated user's workspace. The `workspace_id` is derived from the JWT claims — it is never a user-supplied query parameter on standard feature endpoints. The sync endpoints accept entity payloads that include `workspace_id` per record; the server validates that each record's `workspace_id` matches the authenticated workspace.

### Soft Deletes

`DELETE` on any resource endpoint triggers a **soft delete** only — `deleted_at` is set to the current UTC timestamp. No record is ever hard-deleted via the API. Soft-deleted records are excluded from standard list responses (`WHERE deleted_at IS NULL`) but are included in sync pull payloads.

### Pagination

Standard list endpoints use offset-based pagination. The sync pull endpoint uses cursor-based pagination. See **Shared Schemas → Pagination Schema** for request/response shapes.

---

## Versioning Strategy

### Current Version

The API is currently at `v1`. All endpoints live under `/api/v1/`. This is the first and only active version.

### Backward Compatibility

Within `v1`, all changes must be **additive and non-breaking**:

- New optional fields may be added to request or response schemas.
- New endpoints may be added.
- Existing required fields must not be removed or renamed.
- Existing response field types must not change.
- Enum values must not be removed (new values may be added; clients must handle unknown enum values gracefully).

Clients are expected to ignore unknown fields in responses (tolerant reader pattern). Android Kotlin Serialization and web JSON parsers must be configured accordingly.

### Breaking Changes

A breaking change requires a new API version (`v2`). Breaking changes include:

- Removing or renaming an existing field
- Changing the type of an existing field
- Removing an endpoint
- Changing the meaning of an existing status code or error response

### Deprecation Strategy

When an endpoint or field is deprecated in favor of a newer version:

1. The deprecated item is marked in this document with a `[DEPRECATED]` label and a pointer to the replacement.
2. A `Deprecation` response header is added to affected responses with the planned sunset date.
3. The deprecated item remains functional for a minimum of one full release cycle before removal.
4. Removal is only performed in a new major version (`v2`).

As of v0.1.0 of this document, no items are deprecated.

### Multi-Client Considerations

Phase 2 introduces Android as the first API client. Phase 3 adds the web client. Phase 4 adds Flutter/KMP. All clients consume the same versioned API. Version negotiation is not supported — all active clients must target the current version. Older clients that cannot be updated must be sunset before breaking changes are released.

---

## Authentication & Authorization

### Overview

Authentication is delegated to Supabase Auth. The FastAPI backend does **not** manage user credentials directly. Instead, it validates Supabase-issued JWTs locally on every request using the `SUPABASE_JWT_SECRET` environment variable — no network call to Supabase is required per request.

### Supabase JWT Flow

```
Client (Android / Web)
  │
  ├─► Supabase Auth SDK — signs in with email/password
  │        │
  │        └─► Returns: access_token (JWT), refresh_token
  │
  └─► FastAPI — all subsequent requests include access_token in Authorization header
           │
           └─► Validates JWT locally using SUPABASE_JWT_SECRET
                    │
                    ├─► Valid → extract user_id and workspace_id from claims → proceed
                    └─► Invalid / Expired → 401 Unauthorized
```

### Authorization Header Format

Every authenticated request must include:

```
Authorization: Bearer <supabase_access_token>
```

No other auth schemes are supported. Requests without a valid `Authorization` header are rejected with `401 Unauthorized`.

### Token Validation

The backend validates the following on every JWT:

- Signature — verified using `SUPABASE_JWT_SECRET`
- Expiry (`exp` claim) — rejected if expired
- Issuer (`iss` claim) — must match the Supabase project URL
- `user_id` claim — must correspond to an existing `WorkspaceMember` record

Token refresh is handled entirely by the client using the Supabase SDK. When the backend returns `401`, the client is responsible for refreshing the token and retrying the request. Full token lifecycle details are in `specs/features/auth/spec.md`.

### Workspace Authorization

Every authenticated request is scoped to the user's active workspace. The `workspace_id` is resolved from the JWT's `user_id` claim by looking up the user's `WorkspaceMember` record on the backend. The resolved `workspace_id` is applied as a filter on all queries — users cannot access or mutate data belonging to a workspace they are not a member of.

In Phase 2, each user belongs to exactly one workspace. Multi-workspace switching is introduced in Phase 4. At that point, the client will pass the active `workspace_id` explicitly (mechanism TBD at Phase 4 kickoff).

### Auth Endpoints (Stub)

Auth endpoints live under `/api/v1/auth/`. Full request/response schemas are defined in `specs/features/auth/spec.md`.

| Method | Path | Description | Phase |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Create a new user account via Supabase Auth | 2 |
| `POST` | `/api/v1/auth/login` | Sign in and receive access + refresh tokens | 2 |
| `POST` | `/api/v1/auth/refresh` | Exchange a refresh token for a new access token | 2 |
| `POST` | `/api/v1/auth/logout` | Invalidate the current session | 2 |

---

## Shared Schemas

These schemas are reused across multiple endpoints. Individual endpoint definitions reference them by name.

### Metadata

Included in all single-resource responses. Provides envelope context.

```json
{
  "request_id": "string (UUID)",
  "timestamp": "integer (Unix UTC)"
}
```

### Sync Metadata

Present on every entity in sync push and pull payloads. Reflects the sync fields defined in `data-model.md`.

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
- `deleted_at` being non-null indicates a soft-deleted record. Included in pull payloads so deletions propagate.
- `sync_status` and `last_synced_at` in a pull response reflect the server's view. Clients update their local values after applying the record.

### Pagination Schema

**Request parameters (offset-based — standard list endpoints):**

```json
{
  "page": "integer, default: 1",
  "page_size": "integer, default: 20, max: 100"
}
```

**Request parameters (cursor-based — sync pull only):**

```json
{
  "since": "integer (Unix UTC) | omitted for first full sync",
  "cursor": "string (opaque cursor) | omitted for first page"
}
```

**Response envelope (offset-based):**

```json
{
  "data": ["array of resource objects"],
  "pagination": {
    "page": "integer",
    "page_size": "integer",
    "total": "integer",
    "has_next": "boolean"
  }
}
```

**Response envelope (cursor-based — sync pull):**

```json
{
  "data": ["array of entity objects"],
  "pagination": {
    "next_cursor": "string | null",
    "has_next": "boolean",
    "page_size": "integer"
  }
}
```

`next_cursor` is null when all pages have been consumed. Clients must follow pagination until `has_next = false` before advancing the local watermark.

### Error Schema

All error responses share this shape:

```json
{
  "error": {
    "code": "string (machine-readable, e.g. VALIDATION_ERROR)",
    "message": "string (human-readable)",
    "details": "object | null (field-level errors or additional context)"
  },
  "request_id": "string (UUID)"
}
```

**Standard error codes:**

| Code | HTTP Status | Description |
|---|---|---|
| `UNAUTHORIZED` | 401 | Missing or invalid JWT |
| `FORBIDDEN` | 403 | Authenticated but not authorized for this resource |
| `NOT_FOUND` | 404 | Resource does not exist or is soft-deleted |
| `VALIDATION_ERROR` | 400 | Request body failed schema or business rule validation |
| `CONFLICT` | 409 | Sync conflict detected (see Conflict Schema) |
| `RATE_LIMITED` | 429 | Too many requests; respect `Retry-After` header |
| `SERVER_ERROR` | 500 | Unexpected server error |

### Conflict Schema

Returned per-record in sync push responses when the server detects a conflict.

```json
{
  "id": "string (UUID)",
  "entity_type": "string (e.g. transaction, account)",
  "conflict_type": "UPDATED_VS_UPDATED | DELETED_VS_UPDATED | UPDATED_VS_DELETED",
  "server_version": "object (full entity as stored on server)",
  "client_updated_at": "integer (Unix UTC)",
  "server_updated_at": "integer (Unix UTC)"
}
```

**Conflict types:**
- `UPDATED_VS_UPDATED` — both client and server modified the same record; business fields differ
- `DELETED_VS_UPDATED` — client soft-deleted the record; server has a newer update
- `UPDATED_VS_DELETED` — client has local changes; server soft-deleted the record

The `server_version` field contains the full entity object so the conflict resolution UI can present both versions to the user. Full resolution flow: `specs/features/sync/spec.md`.

### Batch Result Schema

Returned by `POST /api/v1/sync/push`. One result entry per record submitted.

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

`conflict` is populated only when `status = conflict`. `error` is populated only when `status = error`. `server_updated_at` is the server's stored `updated_at` for the record after processing — used by the client to reconcile timestamps on successful push.

---

## Entity Schemas (Transport Layer)

These schemas define the wire format for each entity as exchanged between client and server. They are distinct from the Room entities and domain models used internally by clients. Fields map to `data-model.md` but the transport schema is independently versioned and may diverge over time.

All entity objects include the Sync Metadata fields (`sync_status`, `last_synced_at`, `updated_at`, `deleted_at`) in sync payloads. Standard feature endpoint responses omit sync fields unless noted.

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

### Account

```json
{
  "id": "string (UUID v4)",
  "workspace_id": "string (UUID v4)",
  "name": "string",
  "type": "CHECKING | SAVINGS | CREDIT_CARD | CASH",
  "currency_code": "string (ISO 4217)",
  "initial_balance": "integer (cents)",
  "credit_limit": "integer (cents) | null",
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
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

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
  "created_at": "integer (Unix UTC)",
  "updated_at": "integer (Unix UTC)",
  "deleted_at": "integer (Unix UTC) | null"
}
```

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

The following canonical dependency order governs both sync push payloads and sync pull responses. It ensures that when a record is applied, all records it references as foreign keys already exist in the target database.

| Order | Entity | Depends On |
|---|---|---|
| 1 | Workspace | — |
| 2 | Account | Workspace |
| 3 | Category | Workspace |
| 4 | Merchant | Workspace |
| 5 | Goal | Workspace |
| 6 | Transaction | Account, Category, Merchant (optional) |
| 7 | Transfer | Account (×2) |
| 8 | Budget | Category |
| 9 | GoalContribution | Goal |
| 10 | RecurringTransaction | Account, Category, Merchant (optional) — Phase 2 |

**Push:** The client groups records into entity-keyed arrays and the server processes them in this order, regardless of how they arrive in the request body. The server never relies on the client to send entities in dependency order within a single request — ordering is enforced server-side.

**Pull:** The server returns records grouped by entity type in this dependency order. The client applies them in the order received.

**Within an entity type:** No business-field sort order (e.g. by date) is applied. Ordering within a type is arbitrary. UI sort order is a query-time concern handled by the client's local database, not by sync payloads.

---

## Sync API

The sync API is the core of the Phase 2 data layer. It provides two endpoints — push and pull — that together implement bidirectional, offline-first sync. Full strategy and conflict resolution are specified in `specs/technical/offline-sync.md`. This section translates those requirements into concrete endpoint definitions.

### POST /api/v1/sync/push

Pushes locally modified records from the client to the server. Accepts a batch of records grouped by entity type. Each record is processed independently — a failure on one record does not affect others.

**Auth required:** Yes

**Request Schema:**

```json
{
  "workspaces":          ["array of Workspace objects with Sync Metadata | omit if empty"],
  "accounts":            ["array of Account objects with Sync Metadata | omit if empty"],
  "categories":          ["array of Category objects with Sync Metadata | omit if empty"],
  "merchants":           ["array of Merchant objects with Sync Metadata | omit if empty"],
  "goals":               ["array of Goal objects with Sync Metadata | omit if empty"],
  "transactions":        ["array of Transaction objects with Sync Metadata | omit if empty"],
  "transfers":           ["array of Transfer objects with Sync Metadata | omit if empty"],
  "budgets":             ["array of Budget objects with Sync Metadata | omit if empty"],
  "goal_contributions":  ["array of GoalContribution objects with Sync Metadata | omit if empty"],
  "recurring_transactions": ["array of RecurringTransaction objects with Sync Metadata | omit if empty — Phase 2"]
}
```

Each record in the payload must include the full Sync Metadata block (see Shared Schemas). `updated_at` is always client-provided; the server stores it as-is.

Default batch size: 100 records per request across all entity types combined. The client splits larger pending sets into sequential requests.

**Force flag:** A record may include `"force": true` at the top level of the entity object to instruct the server to accept the client version unconditionally, bypassing conflict detection. Used when the user chooses to keep their local version after conflict resolution.

**Response Schema:** `BatchResultSchema` (see Shared Schemas)

```json
{
  "results": [
    {
      "id": "uuid",
      "entity_type": "transaction",
      "status": "success",
      "conflict": null,
      "error": null,
      "server_updated_at": 1716000000
    },
    {
      "id": "uuid",
      "entity_type": "account",
      "status": "conflict",
      "conflict": {
        "id": "uuid",
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

**Server Behavior:**

- Performs upsert on `id` for each record — creates if not found, updates if found.
- Conflict detection: if the server's stored `updated_at` for a record is newer than the incoming record's `updated_at`, and `force` is not set, a conflict is returned for that record.
- The `force: true` flag bypasses conflict detection and applies the client version unconditionally.
- Server stores the client-provided `updated_at` as-is. It also records an internal `server_received_at` timestamp (not returned to clients) for audit and telemetry.
- Validates all records against schema constraints. Invalid records return `status: error` with an `ErrorSchema` payload; they do not affect other records.
- Processing follows the canonical entity order regardless of the order arrays appear in the request body.

**HTTP Status Codes:**

| Code | Meaning |
|---|---|
| 200 | Request processed. Inspect per-record `status` for individual outcomes. |
| 400 | Malformed request body (not per-record validation failures — those are in results). |
| 401 | Unauthorized. |
| 403 | Workspace mismatch — records do not belong to authenticated workspace. |

**Sync Considerations:**

- The client marks each successfully pushed record `SYNCED` locally and sets `last_synced_at` to the current UTC time.
- Records returned as `conflict` are marked `CONFLICT` locally and not overwritten until user resolves.
- Records returned as `error` are marked `FAILED` locally and retried with exponential backoff.

---

### GET /api/v1/sync/pull

Pulls records modified on the server since the client's last sync watermark. Returns records in canonical entity dependency order using cursor-based pagination. Includes soft-deleted records.

**Auth required:** Yes

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `since` | integer (Unix UTC) | No | Watermark from last successful pull. Omit for first full sync — server returns all workspace records. |
| `cursor` | string | No | Opaque pagination cursor from previous page response. Omit for first page. |
| `page_size` | integer | No | Records per page. Default: 500. Maximum: 500. |

**Response Schema:**

```json
{
  "data": {
    "workspaces":          ["array of Workspace objects with Sync Metadata"],
    "accounts":            ["array of Account objects with Sync Metadata"],
    "categories":          ["array of Category objects with Sync Metadata"],
    "merchants":           ["array of Merchant objects with Sync Metadata"],
    "goals":               ["array of Goal objects with Sync Metadata"],
    "transactions":        ["array of Transaction objects with Sync Metadata"],
    "transfers":           ["array of Transfer objects with Sync Metadata"],
    "budgets":             ["array of Budget objects with Sync Metadata"],
    "goal_contributions":  ["array of GoalContribution objects with Sync Metadata"],
    "recurring_transactions": ["array of RecurringTransaction objects with Sync Metadata — Phase 2"]
  },
  "pagination": {
    "next_cursor": "string | null",
    "has_next": "boolean",
    "page_size": "integer"
  }
}
```

**Server Behavior:**

- Returns all records where `updated_at > since`, scoped to the authenticated workspace.
- Includes soft-deleted records (`deleted_at IS NOT NULL`) so deletions propagate to all clients.
- Records are returned grouped by entity type in canonical dependency order (see Ordering Guarantees).
- Cursor is a composite of `updated_at` + `id` of the last record on the current page — stable under concurrent server-side inserts (no skipped or duplicated records across pages).
- If `since` is omitted, all records for the workspace are returned (first full sync).

**Client Behavior:**

- The client must follow all pages until `has_next = false` before advancing the local watermark.
- For each received record: if the local record exists and has `sync_status = SYNCED`, overwrite with server version. If `sync_status IN (PENDING, FAILED, NULL)`, perform a semantic equality check before flagging a conflict (see `offline-sync.md`).
- After all pages are applied, advance the local watermark to the current UTC time.

**HTTP Status Codes:**

| Code | Meaning |
|---|---|
| 200 | Success. |
| 400 | Invalid `since` or `cursor` parameter. |
| 401 | Unauthorized. |

---

## Feature APIs

Each feature API provides CRUD access for use by UI-driven flows (list screens, detail screens, forms). These endpoints complement the sync API — they are not a replacement for it. In Phase 2, the Android client reads primarily from Room (offline-first) and writes through the Repository layer, which sets `sync_status = PENDING` locally and defers to the sync API for server propagation. Feature endpoints are used for specific read scenarios (e.g. server-side filtered lists, dashboards on the web) and may be used more heavily by the web client in Phase 3.

---

### Workspace API

#### GET /api/v1/workspaces/{workspace_id}

**Purpose:** Retrieve the authenticated user's workspace details.

**Auth requirements:** User must be a member of the requested workspace.

**Request schema:** None (workspace_id in path).

**Response schema:**

```json
{
  "data": { "...Workspace object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `workspace_id` must be a valid UUID and match the authenticated user's workspace.

**Business rule references:** BR-WS-01, BR-WS-02, BR-WS-03

**Sync considerations:** Workspace is the top-level sync entity. Changes to workspace settings (e.g. `base_currency`) propagate via the push/pull sync cycle.

---

#### PATCH /api/v1/workspaces/{workspace_id}

**Purpose:** Update workspace settings (e.g. name, base_currency).

**Auth requirements:** OWNER or ADMIN role.

**Request schema:**

```json
{
  "name": "string | omit if unchanged",
  "base_currency": "string (ISO 4217) | omit if unchanged"
}
```

**Response schema:** Updated Workspace object.

**Validation rules:**
- `name` must be non-empty if provided.
- `base_currency` must be a valid ISO 4217 code if provided.

**Business rule references:** BR-WS-01, BR-CU-01

**Sync considerations:** `updated_at` is set to current UTC on the server. Change propagates to all clients via pull on next sync.

---

### Account API

#### GET /api/v1/accounts

**Purpose:** List all active accounts in the workspace.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size` (offset pagination).

**Response schema:**

```json
{
  "data": ["array of Account objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Validation rules:** None beyond auth.

**Business rule references:** BR-AC-01, BR-AC-02, BR-CU-02

**Sync considerations:** Account list is authoritative from Room in Phase 2 Android client. This endpoint is primarily used by the web client (Phase 3).

---

#### GET /api/v1/accounts/{account_id}

**Purpose:** Retrieve a single account.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Account object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- `account_id` must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

**Business rule references:** BR-AC-01, BR-CU-02

**Sync considerations:** None specific.

---

#### POST /api/v1/accounts

**Purpose:** Create a new account.

**Auth requirements:** OWNER or ADMIN role.

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

**Response schema:** Created Account object. HTTP 201.

**Validation rules:**
- `id` must be a valid UUID v4 (client-generated for offline-first idempotency).
- `name` must be non-empty.
- `type` must be one of the defined ENUM values.
- `currency_code` must be a valid ISO 4217 code.
- `initial_balance` must be an integer (may be 0).
- `credit_limit` must be a positive integer if provided, and only meaningful for `CREDIT_CARD` type.
- `name` must be unique within the workspace (case-insensitive, excluding soft-deleted).

**Business rule references:** BR-AC-01, BR-AC-03, BR-CU-02

**Sync considerations:** Client generates the UUID client-side for offline-first consistency. Push via sync API is the standard path; this endpoint supports direct creation (e.g. web client, onboarding).

---

#### PATCH /api/v1/accounts/{account_id}

**Purpose:** Update an existing account.

**Auth requirements:** OWNER or ADMIN role.

**Request schema:**

```json
{
  "name": "string | omit if unchanged",
  "credit_limit": "integer (cents) | null | omit if unchanged"
}
```

Note: `type` and `currency_code` are immutable after creation.

**Response schema:** Updated Account object.

**Validation rules:**
- `name` must be non-empty if provided.
- `name` must remain unique within the workspace.
- `credit_limit` only valid for `CREDIT_CARD` accounts.

**Business rule references:** BR-AC-01, BR-CU-02

**Sync considerations:** `updated_at` set to current UTC. Propagates via pull.

---

#### DELETE /api/v1/accounts/{account_id}

**Purpose:** Soft-delete an account.

**Auth requirements:** OWNER or ADMIN role.

**Response schema:** HTTP 204 No Content.

**Validation rules:**
- Account must not have associated active transactions or transfers. Returns `400 VALIDATION_ERROR` with detail if violated.

**Business rule references:** BR-AC-03, BR-DI-01

**Sync considerations:** Sets `deleted_at`. Propagates to all clients via pull. Clients must reassign transactions before deletion is permitted.

---

### Category API

#### GET /api/v1/categories

**Purpose:** List all active (non-hidden) categories in the workspace.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size`, `include_hidden` (boolean, default false).

**Response schema:**

```json
{
  "data": ["array of Category objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-CA-01, BR-CA-02

**Sync considerations:** Default categories are seeded on first launch (Android) or on workspace creation (backend). They carry `is_default: true` and are included in all pull payloads.

---

#### GET /api/v1/categories/{category_id}

**Purpose:** Retrieve a single category.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Category object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

**Business rule references:** BR-CA-01, BR-CA-02

---

#### POST /api/v1/categories

**Purpose:** Create a custom category.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "icon": "string (emoji) | null"
}
```

**Response schema:** Created Category object. HTTP 201.

**Validation rules:**
- `name` must be non-empty.
- `name` must be unique within the workspace.
- `is_default` is always `false` for user-created categories.

**Business rule references:** BR-CA-02

**Sync considerations:** Standard offline-first path; client creates locally first.

---

#### PATCH /api/v1/categories/{category_id}

**Purpose:** Update a category's name, icon, or hidden state.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "name": "string | omit if unchanged",
  "icon": "string (emoji) | null | omit if unchanged",
  "is_hidden": "boolean | omit if unchanged"
}
```

**Response schema:** Updated Category object.

**Validation rules:**
- `name` must be non-empty if provided.
- `name` must remain unique within the workspace.

**Business rule references:** BR-CA-01, BR-CA-02

**Sync considerations:** `updated_at` set to current UTC. Propagates via pull.

---

#### DELETE /api/v1/categories/{category_id}

**Purpose:** Soft-delete a custom category.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Validation rules:**
- Category must have no associated active transactions. Returns `400 VALIDATION_ERROR` if violated.
- Default categories (`is_default = true`) cannot be soft-deleted — returns `403 FORBIDDEN`.

**Business rule references:** BR-CA-01, BR-CA-02, BR-DI-01

**Sync considerations:** Sets `deleted_at`. Propagates via pull. Default categories are never deleted via API.

---

### Merchant API

#### GET /api/v1/merchants

**Purpose:** List all active merchants in the workspace.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size`, `q` (search string, optional — matches on `name`).

**Response schema:**

```json
{
  "data": ["array of Merchant objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-ME-01

---

#### GET /api/v1/merchants/{merchant_id}

**Purpose:** Retrieve a single merchant.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Merchant object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/merchants

**Purpose:** Create a new merchant.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "id": "string (UUID v4, client-generated)",
  "name": "string",
  "logo_url": "string | null"
}
```

**Response schema:** Created Merchant object. HTTP 201.

**Validation rules:**
- `name` must be non-empty.
- `name` must be unique within the workspace.

**Business rule references:** BR-ME-01

---

#### PATCH /api/v1/merchants/{merchant_id}

**Purpose:** Update a merchant.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "name": "string | omit if unchanged",
  "logo_url": "string | null | omit if unchanged"
}
```

**Response schema:** Updated Merchant object.

**Validation rules:**
- `name` must be non-empty if provided.
- `name` must remain unique within the workspace.

**Business rule references:** BR-ME-01

---

#### DELETE /api/v1/merchants/{merchant_id}

**Purpose:** Soft-delete a merchant.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Validation rules:** None beyond auth — merchants can be soft-deleted regardless of associated transactions.

**Business rule references:** BR-ME-01, BR-DI-01

**Sync considerations:** Existing transactions retain `merchant_id` after soft delete. The merchant is hidden from selection UI but the reference is preserved in historical records.

---

### Transaction API

#### GET /api/v1/transactions

**Purpose:** List transactions in the workspace with optional filtering.

**Auth requirements:** Any workspace member.

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `page` | integer | Default: 1 |
| `page_size` | integer | Default: 20, max: 100 |
| `account_id` | UUID | Filter by account |
| `category_id` | UUID | Filter by category |
| `merchant_id` | UUID | Filter by merchant |
| `type` | INCOME \| EXPENSE | Filter by transaction type |
| `date_from` | integer (Unix UTC) | Start of date range (inclusive) |
| `date_to` | integer (Unix UTC) | End of date range (inclusive) |

**Response schema:**

```json
{
  "data": ["array of Transaction objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-TX-01, BR-TX-02

---

#### GET /api/v1/transactions/{transaction_id}

**Purpose:** Retrieve a single transaction.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Transaction object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/transactions

**Purpose:** Create a new transaction.

**Auth requirements:** Any workspace member.

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

Note: `currency_code` is not accepted in the request — it is inherited from the linked Account at creation time by the server (BR-CU-03).

**Response schema:** Created Transaction object (including `currency_code`). HTTP 201.

**Validation rules:**
- `account_id` must reference an active account in the workspace.
- `category_id` must reference an active category in the workspace.
- `merchant_id`, if provided, must reference an active or soft-deleted merchant in the workspace.
- `amount` must be a positive integer (> 0).
- `type` must be one of `INCOME`, `EXPENSE`.
- `date` must be a valid Unix timestamp.

**Business rule references:** BR-TX-01, BR-TX-02, BR-CU-03

**Sync considerations:** `currency_code` is set server-side from the Account at creation and returned in the response. Client must update the locally cached `currency_code` on the record after a successful push.

---

#### PATCH /api/v1/transactions/{transaction_id}

**Purpose:** Update an existing transaction.

**Auth requirements:** Any workspace member.

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

**Response schema:** Updated Transaction object.

**Validation rules:**
- `category_id` must reference an active category if provided.
- `merchant_id`, if provided, must reference a valid merchant.
- `amount` must be a positive integer.

**Business rule references:** BR-TX-01, BR-CU-03

---

#### DELETE /api/v1/transactions/{transaction_id}

**Purpose:** Soft-delete a transaction.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Business rule references:** BR-DI-01

**Sync considerations:** Sets `deleted_at`. Propagates via pull. Budget spending calculations exclude soft-deleted transactions automatically.

---

### Transfer API

#### GET /api/v1/transfers

**Purpose:** List transfers in the workspace with optional filtering.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size`, `account_id` (matches either from or to), `date_from`, `date_to`.

**Response schema:**

```json
{
  "data": ["array of Transfer objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-TX-02, BR-TR-01, BR-TR-02

---

#### GET /api/v1/transfers/{transfer_id}

**Purpose:** Retrieve a single transfer.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Transfer object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/transfers

**Purpose:** Create a new transfer.

**Auth requirements:** Any workspace member.

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

**Response schema:** Created Transfer object. HTTP 201.

**Validation rules:**
- `from_account_id` and `to_account_id` must both be active accounts in the workspace.
- `from_account_id` and `to_account_id` must be different accounts.
- Both accounts must share the same `currency_code` in Phase 2 (BR-TR-02).
- `amount` must be a positive integer.

**Business rule references:** BR-TR-01, BR-TR-02, BR-TX-02

**Sync considerations:** Transfers are treated as immutable after creation — no PATCH endpoint. If a transfer must be corrected, soft-delete and re-create.

---

#### DELETE /api/v1/transfers/{transfer_id}

**Purpose:** Soft-delete a transfer.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Business rule references:** BR-TR-01, BR-DI-01

**Sync considerations:** Sets `deleted_at`. Propagates via pull.

---

### Budget API

#### GET /api/v1/budgets

**Purpose:** List budgets for the workspace, optionally filtered by period.

**Auth requirements:** Any workspace member.

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `page` | integer | Default: 1 |
| `page_size` | integer | Default: 20, max: 100 |
| `period_year` | integer | Filter by year (e.g. 2026) |
| `period_month` | integer | Filter by month (1–12) |
| `category_id` | UUID | Filter by category |

**Response schema:**

```json
{
  "data": ["array of Budget objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-BU-01 through BR-BU-05

---

#### GET /api/v1/budgets/{budget_id}

**Purpose:** Retrieve a single budget.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Budget object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/budgets

**Purpose:** Create a budget for a category and period.

**Auth requirements:** Any workspace member.

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

**Response schema:** Created Budget object. HTTP 201.

**Validation rules:**
- `category_id` must reference an active category.
- `amount` must be a positive integer.
- `period_month` must be 1–12.
- One budget per category per period per workspace. Returns `400 VALIDATION_ERROR` if a budget already exists for that combination.

**Business rule references:** BR-BU-01, BR-BU-02, BR-BU-03, BR-BU-04, BR-CU-05

---

#### PATCH /api/v1/budgets/{budget_id}

**Purpose:** Update a budget's amount or carry-forward setting.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "amount": "integer (cents, positive) | omit if unchanged",
  "carry_forward": "boolean | omit if unchanged"
}
```

**Response schema:** Updated Budget object.

**Validation rules:**
- `amount` must be a positive integer if provided.

**Business rule references:** BR-BU-01, BR-BU-05

---

#### DELETE /api/v1/budgets/{budget_id}

**Purpose:** Soft-delete a budget.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Business rule references:** BR-BU-02, BR-DI-01

**Sync considerations:** Deleting a budget does not affect existing transactions. Auto-generated `carry_forward` budgets for future months are independent and unaffected.

---

### Goal API

#### GET /api/v1/goals

**Purpose:** List all active goals in the workspace.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size`.

**Response schema:**

```json
{
  "data": ["array of Goal objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-GL-01, BR-GL-02

---

#### GET /api/v1/goals/{goal_id}

**Purpose:** Retrieve a single goal.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...Goal object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/goals

**Purpose:** Create a new goal.

**Auth requirements:** Any workspace member.

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

**Response schema:** Created Goal object. HTTP 201.

**Validation rules:**
- `name` must be non-empty.
- `target_amount` must be a positive integer.
- `currency_code` must be a valid ISO 4217 code.

**Business rule references:** BR-GL-01, BR-GL-02

---

#### PATCH /api/v1/goals/{goal_id}

**Purpose:** Update a goal.

**Auth requirements:** Any workspace member.

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

**Response schema:** Updated Goal object.

**Validation rules:**
- `target_amount` must be a positive integer if provided.

**Business rule references:** BR-GL-01

---

#### DELETE /api/v1/goals/{goal_id}

**Purpose:** Soft-delete a goal.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Business rule references:** BR-DI-01

**Sync considerations:** Soft-deleting a goal does not soft-delete its contributions. Contributions remain in storage for historical accuracy but are excluded from progress calculations for the deleted goal.

---

### Goal Contribution API

#### GET /api/v1/goal-contributions

**Purpose:** List goal contributions, optionally filtered by goal.

**Auth requirements:** Any workspace member.

**Query parameters:** `page`, `page_size`, `goal_id` (UUID, optional), `date_from`, `date_to`.

**Response schema:**

```json
{
  "data": ["array of GoalContribution objects"],
  "pagination": { "...PaginationSchema..." }
}
```

**Business rule references:** BR-GL-01, BR-GL-02

---

#### GET /api/v1/goal-contributions/{contribution_id}

**Purpose:** Retrieve a single goal contribution.

**Auth requirements:** Any workspace member.

**Response schema:**

```json
{
  "data": { "...GoalContribution object..." },
  "meta": { "...Metadata..." }
}
```

**Validation rules:**
- Must exist and belong to the authenticated workspace.
- Returns `404` if soft-deleted.

---

#### POST /api/v1/goal-contributions

**Purpose:** Record a contribution toward a goal.

**Auth requirements:** Any workspace member.

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

**Response schema:** Created GoalContribution object. HTTP 201.

**Validation rules:**
- `goal_id` must reference an active goal in the workspace.
- `amount` must be a positive integer.

**Business rule references:** BR-GL-01, BR-GL-02

**Sync considerations:** `currency_code` is set server-side from the parent Goal. Client must update the locally cached `currency_code` on the record after successful push.

---

#### PATCH /api/v1/goal-contributions/{contribution_id}

**Purpose:** Update a goal contribution.

**Auth requirements:** Any workspace member.

**Request schema:**

```json
{
  "amount": "integer (cents, positive) | omit if unchanged",
  "date": "integer (Unix UTC) | omit if unchanged",
  "notes": "string | null | omit if unchanged"
}
```

Note: `goal_id` and `currency_code` are immutable after creation.

**Response schema:** Updated GoalContribution object.

**Validation rules:**
- `amount` must be a positive integer if provided.

**Business rule references:** BR-GL-02

---

#### DELETE /api/v1/goal-contributions/{contribution_id}

**Purpose:** Soft-delete a goal contribution.

**Auth requirements:** Any workspace member.

**Response schema:** HTTP 204 No Content.

**Business rule references:** BR-GL-01, BR-DI-01

**Sync considerations:** Goal progress (SUM of contributions / target_amount) is recalculated at query time. Soft-deleted contributions are excluded from the sum automatically.

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
| `POST` | `/api/v1/recurring-transactions` | Create a new recurring transaction template |
| `PATCH` | `/api/v1/recurring-transactions/{id}` | Update a template |
| `DELETE` | `/api/v1/recurring-transactions/{id}` | Soft-delete a template |

---

## Security Considerations

### HTTPS Enforcement

All API traffic must occur over HTTPS. The backend rejects plain HTTP connections. TLS termination is handled at the hosting layer (Render or Railway — decision deferred to Phase 2 kickoff).

### JWT Validation

The backend validates every incoming JWT locally using `SUPABASE_JWT_SECRET`. No network call to Supabase is made per request. Validation checks: signature, expiry (`exp`), and issuer (`iss`). Expired or malformed tokens return `401 Unauthorized`.

### Workspace Isolation

All queries are filtered by `workspace_id` derived from the authenticated user's JWT claims. No user can read or mutate records belonging to a workspace they are not a member of. This is enforced at the service/query layer — not just at the route level — so it applies to sync endpoints as well as feature endpoints.

### Financial Data Protection

- Financial data is never included in server logs in plain text.
- No financial data is transmitted to third-party analytics, crash reporting, or monitoring services.
- This policy applies in all phases.

### Client-Side Security (Android)

- JWTs are stored in `EncryptedSharedPreferences` — never in plain SharedPreferences, files, or logcat.
- Network requests are made over HTTPS only; OkHttp enforces this via certificate pinning (TBD at Phase 2 kickoff).

### Rate Limiting

The API enforces rate limits per authenticated user. When the limit is exceeded, the server returns `429 Too Many Requests` with a `Retry-After` header. The Android sync client must respect this header and delay the retry accordingly (see `offline-sync.md` — Retry Strategy).

### Input Validation

All incoming request bodies are validated against the schemas defined in this document before any database operation is performed. Validation failures return `400 VALIDATION_ERROR` with field-level detail. The server never trusts client-provided `workspace_id` on standard feature endpoints — it is always derived from the JWT.

---

## Open Questions

- Certificate pinning on Android (OkHttp) — implement from Phase 2 start or defer? Risk of breaking updates if cert rotates.
- Should `GET /api/v1/transactions` support a `q` (text search) parameter for merchant name or notes? Useful for the web dashboard (Phase 3).
- Pull `page_size` — configurable via remote config from Phase 2, or hardcoded and revisited later? (Carried from `offline-sync.md`.)
- Should the Budget API return computed spending totals (planned / spent / remaining) inline on `GET` responses, or leave aggregation to the client? Inline aggregation is convenient for the web client but adds server-side query complexity.
- `DELETE /api/v1/goals/{goal_id}` — should soft-deleting a goal cascade a soft-delete to its contributions, or leave them orphaned? Current spec leaves them as independent records. Decision should be revisited before Phase 2 implementation.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-19 | Danielle Mariani | Initial draft |
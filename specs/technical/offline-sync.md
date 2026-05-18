# Offline Sync Strategy — Budget App

**Version:** 0.3.0
**Status:** Draft
**Owner:** Danielle Mariani
**Created at:** 2026-05-15
**Last Updated:** 2026-05-18

---

## Overview

This document defines the bidirectional sync strategy between the Android client and the FastAPI backend, introduced in Phase 2. In Phase 1, the app operates entirely offline with Room as the sole source of truth. This document has no impact on Phase 1 behavior — all sync fields (`last_synced_at`, `sync_status`) are present in the Phase 1 schema but remain null and inactive until Phase 2.

The sync strategy is designed to support an offline-first experience where the device always remains operational without network access, and data is synchronized with the server opportunistically when connectivity is available.

---

## Goals

- Allow the Android app to operate fully offline at all times, with sync occurring in the background when connectivity is available.
- Propagate local changes (creates, updates, soft deletes) to the backend reliably and in the correct entity dependency order.
- Pull server-side changes incrementally using a watermark approach, avoiding full re-downloads on every sync.
- Detect and surface conflicts to the user when the same record has diverged on both client and server, while avoiding false conflicts caused by semantically identical records.
- Ensure all sync operations are idempotent — retrying a failed sync produces no duplicate or corrupted data.
- Ensure exactly one sync cycle is active at any given time — concurrent sync operations are not permitted.
- Guarantee safe recovery from mid-sync interruptions (app close, process kill, worker cancellation) with no data loss or corruption.
- Avoid blocking the UI at any point during sync; sync runs asynchronously and status is communicated passively.

---

## Non-Goals

- **Real-time sync**: No WebSocket or push-notification-based live sync in Phase 2. Sync is trigger-based (see Sync Triggers).
- **Attachment sync**: No file or receipt attachment sync in any current phase.
- **Selective sync**: All entities are synced in full; windowed or partial sync is a future consideration (see Performance Considerations).
- **Automatic conflict resolution beyond server-wins**: Complex merge strategies are out of scope. CONFLICT state requires explicit user action.
- **Cross-workspace sync**: Each workspace is synced independently. Cross-workspace data access is not supported.
- **Exchange rate or currency conversion sync**: No external currency data is fetched or synced.
- **Device clock correction**: Phase 2 does not validate or correct device clock skew. This is a known limitation (see Sync Metadata).

---

## Related Documents

| Document | Purpose |
|---|---|
| SPEC.md | Global business rules and feature index |
| ARCHITECTURE.md | Stack decisions, sync strategy overview, security model |
| specs/technical/data-model.md | Full schema including `sync_status`, `last_synced_at`, `updated_at`, `deleted_at` |
| specs/technical/api-contract.md | Pull and push endpoint definitions (depends on this document) |
| specs/features/auth/spec.md | JWT requirements and token lifecycle |
| specs/features/sync/spec.md | Feature-level sync UI spec (sync status indicators, conflict resolution UI) |

---

## Core Principles

### Offline First

The app must function without network access at all times. Every feature works entirely against the local Room database. Sync is always a background operation — never a prerequisite for reading or writing data.

### Local Database as Source of Truth

Room is the single source of truth on the device. All reads and writes go through Room first. The backend reflects the device's state after a successful sync, not the other way around.

### Eventual Consistency

The app guarantees that, given enough connectivity, all devices sharing a workspace will converge to the same state. It does not guarantee immediate consistency across devices.

### Server Authority

When a conflict is detected — meaning the same record has been modified on both the client and the server since the last sync — the server version wins by default. The exception is records flagged as `CONFLICT`, which require explicit user resolution before the server version is applied (see Conflict Resolution).

### Soft Delete Propagation

Deletions are always soft — a `deleted_at` timestamp is set, never a hard delete. Soft-deleted records are included in sync payloads so that deletions propagate to all devices and the backend. The server applies received `deleted_at` values and returns its own soft-deleted records in pull responses.

### Idempotency

All sync operations must be safe to retry. Sending the same record multiple times must not produce duplicates or corrupt state. The backend uses the entity's UUID as the idempotency key — an upsert (create or update based on `id`) is performed on every received record.

### Single Sync at a Time

Exactly one sync cycle must be active at any given time per workspace. Concurrent sync operations are not permitted. This is enforced via a `Mutex` in the `SyncOrchestrator` (see Sync Concurrency Policy).

### Interruption Safety

Sync must be safe to interrupt at any point — whether by app close, process kill, or worker cancellation. Partial progress is preserved at the record level. The watermark is only advanced on full successful completion, so interrupted pulls are always retried in full on the next trigger (see Cancellation Behavior).

---

## Sync Lifecycle

This section describes the full end-to-end journey of data, from a local write through to a completed sync round-trip.

### Local Write Flow

1. User creates, updates, or soft-deletes a record in the UI.
2. The ViewModel calls the relevant use case, which writes to Room via the Repository.
3. Room persists the record immediately. `updated_at` is set to the current UTC timestamp on the device.
4. `sync_status` is set to `PENDING`. `last_synced_at` remains unchanged.
5. The UI reflects the change immediately from Room — no waiting for network.

### Sync Trigger Flow

1. A sync trigger fires (see Sync Triggers).
2. The Sync Orchestrator attempts to acquire the sync `Mutex` (see Sync Concurrency Policy).
3. If another sync is already in flight: the trigger is handled per concurrency policy (dropped or queued).
4. If the `Mutex` is acquired: the Sync Orchestrator checks for network connectivity.
5. If offline: the `Mutex` is released; sync is deferred until connectivity is restored.
6. If online: the Sync Orchestrator begins the push flow, followed by the pull flow.

### Push Flow

1. The Sync Orchestrator queries Room for all records where `sync_status IN ('PENDING', 'FAILED') OR sync_status IS NULL`, across all entities, ordered by dependency (see Ordering Guarantees). Records with `sync_status IS NULL` are Phase 1 records being synced for the first time in Phase 2.
2. No business-field sort order (e.g. by date) is applied to the push payload — ordering is by entity dependency only. UI sort order is a query-time concern handled by Room DAOs, not by sync.
3. Records are batched and sent to the push endpoint (`POST /api/v1/sync/push`). Default batch size: 100 records per request.
4. The backend processes each record independently and returns per-record results (success, conflict, error). **Batch processing is record-level, not batch-level — a failure on one record does not affect the processing of other records in the same batch.**
5. On success: `sync_status` → `SYNCED`, `last_synced_at` → current UTC timestamp.
6. On conflict: `sync_status` → `CONFLICT`. Record is not overwritten locally until the user resolves the conflict.
7. On failure: `sync_status` → `FAILED`. Record is queued for retry.

### Pull Flow

1. The Sync Orchestrator reads the local watermark — the minimum `last_synced_at` across all synced entities, or `null` if this is the first sync.
2. A pull request is sent to the backend (`GET /api/v1/sync/pull?since={watermark}`).
3. The backend returns all records modified after the watermark, including soft-deleted records, in dependency order.
4. For each received record, the Sync Orchestrator applies it to Room:
   - If the record does not exist locally: insert it, mark `SYNCED`.
   - If the record exists locally and `sync_status = SYNCED`: overwrite with server version, mark `SYNCED`.
   - If the record exists locally and `sync_status IN ('PENDING', 'FAILED', NULL)`: perform a **semantic equality check** before flagging a conflict (see Semantic Equality Check below).
   - If the record exists locally and `sync_status = CONFLICT`: do not overwrite. User must resolve first.
5. Inserted or updated records from the server are marked `SYNCED` with the current `last_synced_at`.

#### Semantic Equality Check

Before flagging a conflict on a locally modified record (`sync_status IN ('PENDING', 'FAILED', NULL)`), the Sync Orchestrator compares the local and server versions field by field across all **business fields** — defined as all fields except `id`, `sync_status`, `last_synced_at`, `updated_at`, and `deleted_at`.

- If all business fields are identical: the records are semantically equal. Mark the local record `SYNCED` and reconcile timestamps by adopting the server's `updated_at` and `last_synced_at`. No conflict is surfaced to the user.
- If any business field differs: a genuine conflict exists. Set `sync_status = CONFLICT`. Do not overwrite local data. Surface to the user for resolution.

This prevents false conflicts caused by the same change being made independently on two devices (e.g. two devices both auto-generating an identical carry-forward budget row, or correcting a merchant name to the same value).

### Sync Completion Flow

1. After push and pull complete successfully, the Sync Orchestrator updates the global watermark and releases the `Mutex`.
2. Any foreground sync status indicator (e.g. last synced timestamp, spinner) is updated via StateFlow to the UI.
3. If any records remain in `FAILED` or `CONFLICT` state, the UI surfaces this passively (e.g. a sync warning icon). It does not block the user.

### Cancellation Behavior

Sync must be safe to interrupt at any point without data loss or corruption. The following scenarios are handled explicitly:

**App closed gracefully while a foreground sync is running:**
The coroutine running the `SyncOrchestrator` is cancelled via structured concurrency when the lifecycle scope is destroyed. Because sync writes to Room record-by-record (not as a single atomic transaction), all records successfully written before cancellation retain their updated `sync_status`. Records not yet processed remain `PENDING` or `FAILED`. The `Mutex` is released via a `finally` block. The watermark is only advanced at the end of a complete, successful pull cycle — an interrupted pull does not advance the watermark. On next launch, the sync resumes cleanly from the last valid watermark.

**Process killed (OOM, user force-stop):**
The `Mutex` is in-memory and is gone with the process — this is safe because no sync is in flight by definition on next launch. Room's write-ahead logging (WAL) ensures all committed record-level writes survive the kill. The outcome is identical to graceful cancellation: partial progress is preserved, incomplete work is picked up on the next sync trigger.

**WorkManager worker cancelled mid-sync:**
WorkManager may cancel a `SyncWorker` if its constraints are no longer met (e.g. network drops mid-sync). Cancellation is cooperative via Kotlin coroutine cancellation. Record-level partial progress is preserved. WorkManager reschedules the worker when constraints are satisfied again, and the `SyncOrchestrator` resumes from the last valid watermark.

**Key safety invariant across all three scenarios:** the watermark only advances on full successful completion of a pull cycle. An interrupted sync always retries the full pull from the last valid watermark on the next trigger. No data is lost; no records are silently skipped.

---

## Sync Triggers

### App Foreground

When the app returns to the foreground, the Sync Orchestrator performs a connectivity check. If the device is online, a sync is initiated immediately. This is the primary sync mechanism for active users.

Implementation: `DefaultLifecycleObserver.onStart()` in the Android app triggers the Sync Orchestrator.

### Manual Sync

The user can trigger a sync manually via a pull-to-refresh gesture on the main screen. If the device is offline, the UI surfaces an appropriate message. A manual sync always performs a full push + pull cycle. If a sync is already in flight when the user triggers a manual refresh, the manual sync is queued as a single pending follow-up (see Sync Concurrency Policy).

### Background Sync (WorkManager)

A periodic background sync is scheduled via WorkManager to keep data fresh even when the app is not in the foreground. Recommended interval: every 15–30 minutes, subject to device battery and connectivity constraints.

- Uses `PeriodicWorkRequest` with a `NetworkType.CONNECTED` constraint.
- Runs only when the device has network connectivity.
- Uses `ExistingPeriodicWorkPolicy.KEEP` to prevent duplicate WorkManager workers from being enqueued.
- Does not run if the app is actively in the foreground (foreground sync takes precedence).

### Connectivity Restoration

When the device regains network connectivity after being offline, a sync is triggered automatically. This ensures that writes made while offline are pushed to the server as soon as possible, without requiring user action.

Implementation: A `ConnectivityManager.NetworkCallback` registered in the app monitors connectivity state. On `onAvailable()`, a sync is enqueued via the Sync Orchestrator.

---

## Sync Concurrency Policy

Exactly one sync cycle must be active at any given time per workspace. Without this constraint, concurrent syncs could query the same `PENDING` records simultaneously, produce duplicate push payloads, and race to update `sync_status`, leading to corrupted sync state.

### Enforcement Mechanism

The `SyncOrchestrator` holds a Kotlin `Mutex`. Any sync operation — regardless of trigger source — must acquire the `Mutex` before starting and release it on completion, failure, or cancellation via a `finally` block.

### Behavior per Trigger

| Trigger | Sync already in flight | Behavior |
|---|---|---|
| App foreground | Yes | Drop — the in-flight sync covers it |
| Background (WorkManager) | Yes | Drop — `ExistingPeriodicWorkPolicy.KEEP` prevents duplicate workers; `Mutex` guards at runtime |
| Connectivity restored | Yes | Drop — the in-flight sync covers it |
| Manual user refresh | Yes | Queue a single follow-up sync to run immediately after the current one completes |
| Manual user refresh | No | Acquire `Mutex`, begin sync immediately |

A queued manual follow-up is represented as a single boolean flag — not a queue of multiple requests. If the user triggers manual refresh multiple times while a sync is in flight, only one follow-up sync is queued.

---

## Sync Metadata

These four fields are present on every entity from Phase 1. They are the foundation of the sync mechanism.

### sync_status

Tracks the current sync state of each individual record. A nullable TEXT field stored as a string ENUM in Room.

- Null in Phase 1 (field exists but is inactive).
- In Phase 2, set to `PENDING` on every new record creation or local update.
- Valid values: `PENDING`, `SYNCED`, `FAILED`, `CONFLICT`.

### updated_at

A non-nullable INTEGER (Unix timestamp, UTC) recording the last time this record was modified. Set by the device clock on every local create or update. This value is stored as-is by the server on push — the server never overwrites it with its own clock. It represents when the user made the change, not when the server received it.

Conflict detection compares the incoming client `updated_at` against the server's stored `updated_at` for the same record. Because both values are client-originated, they are on the same scale.

**Known limitation:** Phase 2 does not validate or correct device clock skew. A device with a significantly incorrect clock may produce unreliable `updated_at` values, which could affect conflict detection. This is accepted as a known limitation and revisited post-Phase 2 if it proves to be a real problem in practice.

### last_synced_at

A nullable INTEGER (Unix timestamp, UTC) recording the last time this record was successfully synced with the backend. Set to null until the first successful sync of the record. Updated to the current UTC time on each successful push or pull of the record.

### deleted_at

A nullable INTEGER (Unix timestamp, UTC). Set when a record is soft-deleted. Included in sync payloads to propagate deletions to the server and other devices. Never null for a deleted record; never set for an active record.

---

## Entity Sync States

### PENDING

The record has been created or modified locally and has not yet been successfully synced to the backend. This is the default state for all new or edited records in Phase 2. Phase 1 records with `sync_status IS NULL` are treated as `PENDING` during the first Phase 2 sync.

### SYNCED

The record has been successfully pushed to the backend and matches the server's version. No local changes are pending.

### FAILED

A sync attempt for this record failed due to a network error, server error, or invalid payload. The record will be retried on the next sync cycle using exponential backoff (see Retry Strategy). The local data is preserved.

### CONFLICT

The record was modified both locally and on the server since the last sync, and the business fields differ between the two versions. The record requires explicit user resolution before sync can proceed for this record. Records are only flagged `CONFLICT` after a semantic equality check confirms a genuine difference (see Semantic Equality Check).

### State Machine

```
[Created / Updated locally]
          |
          v
       PENDING ◄─────────────────────────────┐
       /      \                               │
  push ok   push fails                        │
      |           |                           │
      v           v                           │
   SYNCED       FAILED ──► retry ────────► PENDING
      |
  local edit
      |
      v
   PENDING
      |
  pull arrives, business fields differ
      |
      v
   CONFLICT
      |
   user resolves
      |
      v
   PENDING ──► (next sync) ──► SYNCED
```

**Special case — semantic equality on pull:**
```
PENDING / FAILED / NULL + pull arrives, business fields identical
          |
          v
       SYNCED (timestamps reconciled to server values, no conflict surfaced)
```

**Transitions per operation:**

| Operation | Resulting sync_status |
|---|---|
| Create record locally | `PENDING` |
| Update record locally | `PENDING` |
| Soft delete locally | `PENDING` |
| Successful push | `SYNCED` |
| Failed push (network/server error) | `FAILED` |
| Conflict detected on push or pull (business fields differ) | `CONFLICT` |
| Semantic equality confirmed on pull (business fields identical) | `SYNCED` |
| User resolves conflict | `PENDING` (re-queued for push) |
| Retry after FAILED | `PENDING` (re-queued for push) |

---

## Watermark Strategy

### Client Watermark

The watermark is the timestamp representing the last point in time at which the client successfully pulled data from the server. It is stored locally as a single value per workspace (not per entity) and is advanced after each successful, complete pull cycle.

Storage: a dedicated `SyncMetadata` table or shared preferences key, keyed by `workspace_id`.

### Incremental Pulls

On every pull after the first, the client sends its watermark as a query parameter (`since`). The server returns only records where `updated_at > since`. This ensures that only the delta — records that changed since the last pull — is transmitted, keeping sync payloads small and fast.

### Delta Synchronization

When the server returns a delta payload, the Sync Orchestrator applies each record to the local Room database in dependency order. The semantic equality check is applied before any conflict is flagged (see Pull Flow). After applying the full delta successfully, the local watermark is advanced to the current UTC time.

### First Full Sync

On a new device install, or on the first sync after a user authenticates in Phase 2, the client has no watermark. The client sends no `since` parameter. The server returns all records for the workspace. This is the only full-download sync; all subsequent syncs are incremental.

A first full sync also occurs when the Phase 1 local database is migrated to Phase 2 — the client pushes all existing local records (with `sync_status IS NULL`, treated as `PENDING`) to the server, then pulls the full server state to reconcile.

---

## Conflict Resolution

### Conflict Definition

A conflict occurs when the same record (identified by `id`) has been modified both locally (`sync_status IN ('PENDING', 'FAILED', NULL)`) and on the server (its `updated_at` on the server is newer than the client's `last_synced_at`) since the last successful sync, **and** the two versions differ in at least one business field. Records that are semantically equal are reconciled silently and never reach conflict state (see Semantic Equality Check).

### Server Authoritative Rules

The server is the authoritative source for conflict resolution. When a conflict is detected during a push, the server rejects the client's version and returns its own. The Sync Orchestrator marks the record as `CONFLICT` without overwriting the local version.

### Last Write Wins vs Explicit Resolution

For records where the local `sync_status = SYNCED` at the time a server version arrives via pull, the server version is applied silently (last-write-wins). This covers the common case of two devices where one synced before the other made changes.

Explicit resolution is only required when the local record is `PENDING`, `FAILED`, or `NULL` at the time a conflicting server version arrives — meaning the user has unsaved local changes that differ from the server version and would be lost by applying it silently.

### User-Visible Conflict Resolution

When a record enters `CONFLICT` state, the UI surfaces it as a conflict requiring resolution. The user is presented with:

- The local (device) version of the record.
- The server version of the record.
- A choice: keep local version or accept server version.

Selecting "keep local version" re-queues the record as `PENDING` and attempts to push it with a `force: true` flag, instructing the server to accept the client version unconditionally. Selecting "accept server version" overwrites the local record with the server's data and marks it `SYNCED`.

Full UI details are specified in `specs/features/sync/spec.md`.

### Deleted vs Updated Conflicts

A specific conflict case arises when a record is soft-deleted on one device while it was updated on another:

- **Locally deleted, server updated**: The local record is marked `CONFLICT`. The user is informed the record was deleted locally but modified elsewhere. The user chooses to restore or confirm deletion.
- **Locally updated, server deleted**: The local record is marked `CONFLICT`. The user is informed the record was deleted on another device. The user chooses to keep their local edits (which restores the record) or accept the deletion.

### Multi-Device Scenarios

When more than two devices share a workspace (Phase 4+), the same conflict detection and resolution logic applies. The server timestamp is always the reference point. Device A's changes synced to the server before Device B's are treated as the server's authoritative state when Device B attempts to sync.

---

## Sync Queue Architecture

### Queue Processing

The Sync Orchestrator maintains an implicit queue of records to push, queried from Room by `sync_status IN ('PENDING', 'FAILED') OR sync_status IS NULL`. Records are processed in batches, in entity dependency order (see Ordering Guarantees). There is no persistent in-memory queue; the Room database itself is the queue.

### Retry Strategy

Failed records (`sync_status = FAILED`) are retried on the next sync trigger. The Sync Orchestrator does not immediately retry failed records inline — it completes the current sync cycle and defers retries to the next trigger, which is subject to exponential backoff.

A record is considered permanently failed (and surfaced to the user) after a configurable number of consecutive failures (suggested default: 5). Permanently failed records remain in `FAILED` state and are not retried automatically until the user manually triggers a sync or the app restarts.

### Exponential Backoff

Background sync retries follow exponential backoff to avoid hammering the server during outages:

- Initial retry delay: 30 seconds
- Multiplier: 2x per failure
- Maximum delay: 1 hour
- Jitter: ±10% to prevent thundering herd

WorkManager's built-in backoff policy is used for background sync. Foreground sync (app foreground, manual) bypasses backoff and always attempts immediately.

### Batch Processing

Records are grouped into batches per sync cycle and sent to the server in chunks. Each record in a batch is processed independently by the server — **batch processing is record-level, not batch-level. A failure on one record does not affect the processing of other records in the same batch.** The server returns a per-record result for every record in the batch.

Default push batch size: 100 records per request. If the total set of pending records exceeds 100, it is split into multiple sequential requests. Batch size is configurable via remote config in a future phase.

Pull responses from the server are also paginated (see Pagination under API Expectations). Client batch size (push) and server page size (pull) are independent settings serving the same purpose from opposite directions: limiting how much data travels in a single request.

### Ordering Guarantees

Entities must be pushed in dependency order to avoid foreign key violations on the server. No business-field sort order (e.g. by date) is applied within an entity type — ordering within a type is arbitrary.

Dependency push order:

1. Workspace
2. Account
3. Category
4. Merchant
5. Goal
6. Transaction (depends on Account, Category, Merchant)
7. Transfer (depends on Account)
8. Budget (depends on Category)
9. GoalContribution (depends on Goal)
10. RecurringTransaction (depends on Account, Category, Merchant — Phase 2)

Pull responses from the server are returned in the same dependency order, and the Sync Orchestrator applies them in that order to avoid referencing a not-yet-applied parent record.

---

## Deletion Strategy

### Soft Deletes

All deletions set `deleted_at` to the current UTC timestamp. No record is ever hard-deleted from the local database or the server. This guarantees that deletion events can always be propagated, even if one device was offline when the deletion occurred.

### Tombstone Propagation

When a soft-deleted record is pushed to the server, the server sets its own `deleted_at` and includes the record in subsequent pull responses (with `deleted_at` set) so that all other devices receive the deletion and apply it locally.

A record with `deleted_at` set is excluded from all UI queries via `WHERE deleted_at IS NULL` filters but remains in the database for sync purposes.

### Restore Behavior

A soft-deleted record can be restored (in the conflict resolution flow or, in future, via an undo feature) by clearing `deleted_at`, setting `updated_at` to now, and setting `sync_status = PENDING`. The restored record is pushed to the server on the next sync.

### Permanent Cleanup Policy (Future)

Hard deletion of old soft-deleted records is not implemented in Phase 2. A future cleanup policy may purge records where `deleted_at` is older than a configurable retention period (e.g. 90 days), once all devices in a workspace have confirmed receipt of the deletion via watermark advancement. This is out of scope for Phase 2.

---

## Error Handling

### Network Failures

If a sync attempt fails due to no connectivity or a timeout, the Sync Orchestrator records the failure, releases the `Mutex`, and defers to the next trigger. Records pushed during a failed attempt remain `PENDING` or `FAILED`. No partial writes are committed to Room as a result of a network failure.

### Auth Failures

If the backend returns `401 Unauthorized`, the Sync Orchestrator suspends the sync cycle, releases the `Mutex`, and surfaces an auth error to the user. Token refresh and re-authentication are handled by the Auth feature. Full details in `specs/features/auth/spec.md`.

### Partial Sync Failures

If a batch push partially succeeds (some records accepted, some rejected), the Sync Orchestrator processes each record's result individually: successful records are marked `SYNCED`, failed records are marked `FAILED`. **Batch processing is record-level — a failure on one record does not roll back or affect other records in the same batch.**

### Corrupted Payloads

If the server returns a malformed or unparseable response, the Sync Orchestrator logs the error, releases the `Mutex`, and does not advance the watermark. No local data is modified. The next sync trigger will retry the full cycle.

### Retryable vs Non-Retryable Errors

| Error | Retryable | Behavior |
|---|---|---|
| Network timeout | Yes | Mark FAILED, retry with backoff |
| HTTP 5xx (server error) | Yes | Mark FAILED, retry with backoff |
| HTTP 429 (rate limited) | Yes | Retry after `Retry-After` header delay |
| HTTP 401 (unauthorized) | No | Suspend sync, surface auth error |
| HTTP 400 (bad request) | No | Mark FAILED permanently, surface to user |
| HTTP 409 (conflict) | No | Mark CONFLICT, surface to user |
| Corrupted response payload | Yes (full cycle) | Retry entire sync cycle |

---

## API Expectations

This section defines what `offline-sync.md` requires from the API contract. Full endpoint definitions are in `specs/technical/api-contract.md`.

### Pull Endpoint Expectations

- `GET /api/v1/sync/pull`
- Query parameter: `since` (Unix timestamp, UTC). Optional. If omitted, returns all records for the workspace (first full sync).
- Returns all records where `updated_at > since`, including soft-deleted records (`deleted_at IS NOT NULL`).
- Response is scoped to the authenticated user's workspace. No cross-workspace data is returned.
- Response is paginated using cursor-based pagination. Default page size: 500 records. Client must follow pagination until exhausted before advancing the watermark.
- Records are returned in dependency order (same as push order) to support in-order application.

### Push Endpoint Expectations

- `POST /api/v1/sync/push`
- Accepts a batch of records across all entity types in a single request.
- Performs upsert on `id` — creates if not found, updates if found.
- Each record is processed independently. Returns per-record results: `success`, `conflict`, or `error`. A failure on one record does not affect others.
- `conflict` responses include the server's current version of the record for display in the conflict resolution UI.
- Conflict override: a `force: true` flag on a record instructs the server to accept the client version unconditionally (used when the user chooses to keep their local version after conflict resolution).
- The server stores the client-provided `updated_at` as-is. It does not overwrite it with the server's own clock.

### Idempotent Operations

All push operations must be idempotent. Sending the same record twice must result in the same server state. The server uses `id` as the upsert key. `updated_at` is used to determine whether an incoming record is newer than the stored version.

### Pagination

Pull responses use cursor-based pagination. The cursor is the `id` (or `updated_at` + `id` composite) of the last record on the current page. The client sends the cursor as a query parameter to retrieve the next page. This approach is stable under concurrent inserts — new records arriving on the server while the client is mid-pull do not shift page boundaries or cause records to be skipped or duplicated, as would happen with offset-based pagination.

The watermark is only advanced after all pages of a pull cycle are successfully applied.

---

## Android Client Responsibilities

### Repository Layer

Each feature's `RepositoryImpl` is responsible for writing `sync_status = PENDING` and updating `updated_at` on every local mutation. Repositories never call the network directly.

### Local Data Sources

`LocalDataSource` classes wrap DAO calls. They are the only layer that reads from and writes to Room. Sync state fields are updated here.

### Sync Orchestrator

A dedicated `SyncOrchestrator` class coordinates the full push + pull cycle. It is injected via Hilt and called from WorkManager workers and lifecycle observers. It is responsible for:

- Acquiring and releasing the sync `Mutex` (via `finally`) to enforce single-sync-at-a-time and safe cancellation.
- Querying `PENDING`, `FAILED`, and `NULL` sync_status records from Room.
- Performing the semantic equality check during pull application.
- Calling the push and pull remote data sources.
- Applying pull results to Room via local data sources.
- Updating `sync_status` and `last_synced_at` per record.
- Advancing the watermark only after a complete, successful pull cycle.
- Managing the manual sync follow-up flag.
- Exposing a `StateFlow<SyncState>` for the UI to observe.

### WorkManager

WorkManager manages periodic and connectivity-triggered background sync. A `SyncWorker` class implements `CoroutineWorker` and delegates to the `SyncOrchestrator`. WorkManager uses `ExistingPeriodicWorkPolicy.KEEP` to prevent duplicate workers. The `SyncOrchestrator` `Mutex` provides a second layer of protection at runtime. WorkManager cancellation is handled cooperatively via Kotlin coroutine cancellation — partial record-level progress is preserved on cancellation.

### Connectivity Monitoring

A `ConnectivityObserver` (wrapping `ConnectivityManager.NetworkCallback`) monitors network availability and notifies the `SyncOrchestrator` on connectivity restoration. This is a foreground-only observer; WorkManager handles background connectivity constraints independently.

---

## Backend Responsibilities

### Conflict Detection

On receiving a push record, the backend compares the incoming `updated_at` against the stored `updated_at` for the same `id`. If the stored record is newer (or was deleted after the client's `last_synced_at`), a conflict is returned for that record. The `force: true` flag bypasses conflict detection and instructs the server to accept the client version unconditionally.

### Validation

The backend validates all incoming records against the schema defined in `specs/technical/data-model.md`. Invalid records (missing required fields, invalid enum values, constraint violations) are rejected with `HTTP 400` and a per-record error message. Validation failures are non-retryable and are surfaced to the user.

### Server Timestamps

The server stores the client-provided `updated_at` value as-is on every push. It does not overwrite it with the server's own clock. This preserves the user's mutation time and ensures conflict detection compares values on the same scale — both client-originated.

In addition to `updated_at`, the server records a `server_received_at` timestamp on every incoming record. This is a backend-only field — it is not synced to clients and has no equivalent in the Room schema. Its purpose is operational:

- **Debugging**: when a conflict is reported and client `updated_at` values look suspicious (e.g. device clock was wrong), `server_received_at` provides the ground truth of when the record actually arrived.
- **Telemetry**: sync latency can be derived as `server_received_at - updated_at` per record, giving visibility into real-world sync lag.
- **Audit**: for financial data, knowing when a record was received by the server — independent of when it was created on-device — has compliance value as the app scales toward multi-user use.

### Multi-Workspace Isolation

All pull and push operations are scoped to the authenticated user's workspace (`workspace_id` derived from the JWT claims). The backend rejects any request that attempts to read or write records belonging to a workspace they are not a member of.

---

## Performance Considerations

### Batch Sizes

Default push batch size: 100 records per request. Default pull page size: 500 records per response. Both are independent and configurable via remote config in a future phase.

### Payload Size

Amounts are stored as integers; timestamps as integers. No large blob fields exist on sync entities in Phase 2 (no attachments). Payload sizes should be small per record. A batch of 100 records is expected to remain well under 1MB in all cases.

### Large Dataset Scaling

Phase 1 and 2 use full local mirroring of all workspace data. For long-term scaling with large transaction histories, a windowed sync strategy (e.g. last 12 months on device, older data fetched on demand) is a future option. This is deferred to Phase 2 kickoff review based on data volume projections and target device specs.

### Windowed Sync (Future)

A windowed sync strategy would keep only a rolling window of records on-device (e.g. last 12 months of transactions) and fetch older records on demand via the API. This reduces initial sync time and device storage requirements for long-lived accounts. Not in scope for Phase 2.

---

## Security Considerations

### HTTPS Only

All sync traffic between the Android client and the FastAPI backend is transmitted over HTTPS. Plain HTTP is not accepted by the backend.

### Encrypted Token Storage

JWTs used to authenticate sync requests are stored in `EncryptedSharedPreferences` on Android. Tokens are never stored in plain SharedPreferences, files, or logs.

### Workspace Isolation

The backend enforces workspace isolation at the query layer — all sync operations are scoped to the `workspace_id` derived from the authenticated user's JWT claims. A user cannot read or write records belonging to a workspace they are not a member of.

### Sensitive Financial Data

Financial data (transactions, balances, budgets) is never logged in plain text on the client or server. No financial data is transmitted to any third-party analytics or crash reporting service. This applies in all phases (NFR-DS-01 and its Phase 2 extension).

---

## Future Enhancements

- **Real-Time Sync**: WebSocket or server-sent events for live sync across devices. Not in scope for Phase 2.
- **Push Notifications**: Trigger a sync on the receiving device when another device pushes a change. Depends on Firebase Cloud Messaging (FCM) integration — a Google service that delivers silent push notifications to Android devices, waking the app to pull fresh data without user action.
- **Selective Sync**: Allow the user to choose which accounts or date ranges are synced to a given device (e.g. keep only the last 6 months on a secondary device).
- **Attachment Sync**: Sync receipt images or other attachments via Supabase Storage. Depends on the receipt scanning feature (post Phase 1, phase TBD).
- **Clock Skew Correction**: Detect and compensate for device clock drift to improve conflict detection reliability. Not in scope for Phase 2.

---

## Open Questions

- What is the exact pull page size — should it be configurable from the start or hardcoded in Phase 2?
- Should the `SyncOrchestrator` expose a per-entity sync status (e.g. "Accounts synced, Transactions pending") or a single workspace-level sync state? A pragmatic middle ground is workspace-level status in the UI with per-entity detail accessible in a sync log or debug screen — deferring granular UI complexity to when multi-device conflict scenarios are real (Phase 4+).
- Should permanently failed records (after max retries) require a manual user action to reset, or should they auto-reset on app restart?
- At Phase 2 kickoff: define the maximum age threshold for purging soft-deleted records (Permanent Cleanup Policy).
- Should conflict resolution UI be presented inline (on the record's detail screen) or in a dedicated conflict queue screen? To be resolved in `specs/features/sync/spec.md`.

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-15 | Danielle Mariani | Initial draft |
| 0.2.0 | 2026-05-17 | Danielle Mariani | Add NULL sync_status handling for Phase 1 records. Add semantic equality check to Pull Flow and Conflict Resolution. Clarify record-level batch isolation in Push Flow, Batch Processing, and Partial Sync Failures. Clarify updated_at as client-originated; server does not overwrite. Add device clock skew as known limitation. Add Sync Concurrency Policy section. Clarify payload sort order. Update SyncOrchestrator open question with pragmatic middle-ground option. Expand FCM description in Future Enhancements. |
| 0.3.0 | 2026-05-18 | Danielle Mariani | Add Cancellation Behavior subsection covering graceful close, process kill, and WorkManager cancellation. Add Interruption Safety core principle. Add server_received_at to Backend Responsibilities with audit, telemetry, and debugging rationale. Simplify updated_at rule to client-authoritative only. Fix Single Sync at a Time wording from "may" to "must". Update Goals and Android Client Responsibilities to reflect cancellation safety. |
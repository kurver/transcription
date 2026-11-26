# SPEC-009 — HTTP API

**Status:** Implemented. JSON over HTTP under `/api`; identity via `X-User-Id` (SPEC-005). Errors: `{"error": "<message>"}` with 400 (validation), 401 (identity), 403 (role/ownership), 404 (not found / not visible).

Legacy naming: `documents` / `documentId` = a recording ([ADR-0010](../adr/0010-audio-first-scope.md)).

## Requests (USER)

| Method & path | Role | Purpose |
|---|---|---|
| `GET /users` | none | Seeded users for the dev switcher |
| `GET /me` | any | Current user |
| `GET /requests` | USER | Own requests |
| `POST /requests` | USER | Create. Body: `{title, caseId?, caseOwner?, dueDate?, priority?, customFields?}` |
| `GET /requests/{id}` | visible | `RequestDetail`: summary, customFields, documents, tasks, reviews |
| `PUT /requests/{id}/metadata` | owner | Replace metadata (audited) |
| `POST /requests/{id}/documents` | owner | Multipart `file`; audio only |
| `POST /requests/{id}/submit` | owner | Queue for processing |
| `POST /requests/{id}/retry` | owner | Re-queue failed recordings |
| `GET /queue/completed` | USER | Own completed requests |
| `POST /requests/{id}/send-to-specialists` | owner | Optional body `{reason}`; request must be `COMPLETED` |

## Transcriptions

| Method & path | Role | Purpose |
|---|---|---|
| `GET /transcriptions/{id}` | visible | Summary + current content + current version |
| `GET /transcriptions/{id}/audio` | visible | Original recording bytes (`audio/*`) |
| `GET /transcriptions/{id}/versions` | visible | Full ordered history |
| `POST /transcriptions/{id}/versions` | claiming TRANSCRIBER | `{mode: EDIT\|REPLACEMENT, content, comment}` |
| `POST /transcriptions/{id}/qa-request` | owner / claiming transcriber | Create `OPEN` review |

## Transcriber queue

| Method & path | Role | Purpose |
|---|---|---|
| `GET /specialist/queue` | TRANSCRIBER | Shared open + own claimed requests |
| `POST /specialist/tasks/{taskId}/claim` | TRANSCRIBER | Claim |
| `POST /specialist/tasks/{taskId}/complete` | claimant | Complete (queues `RECHECK` reviews) |

## QA

| Method & path | Role | Purpose |
|---|---|---|
| `GET /qa/queue` | QA | Shared open + own claimed requests |
| `POST /qa/requests/{requestId}/claim` | QA | Claim all open reviews on the request |
| `POST /qa/reviews/{reviewId}/verdict` | claimant | `{verdict: PASS\|FAIL, comment}` |
| `GET /qa/requests` | QA | All requests, read-only |
| `GET /qa/audit?limit=200` | QA | Latest audit events (limit 1–1000) |

## Demo / mock (non-production)

| Method & path | Purpose |
|---|---|
| `POST /api/demo/requests?count&submit` | SPEC-008 (USER, `app.demo.enabled`) |
| `POST /mock-api/transcribe` | Mock transcription API (multipart `file` → `{text}`) |

## Request summary fields

`id, title, status, ownerId, caseId, caseOwner, dueDate, priority, createdAt, submittedAt, documentCount, transcribedCount`, plus `specialist` (`taskId, status, assignedTo, source, reason`) and `qa` counts (`open, claimed, passed, failed`).

## Gaps

- No OpenAPI document is generated; this table is the contract for now.
- No pagination except `limit` on the audit log.

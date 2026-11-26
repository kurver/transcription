# SPEC-005 — Access & visibility

**Status:** Implemented (identity is a POC stub — [ADR-0006](../adr/0006-poc-identity-stub.md)). Tests: `WorkflowTest` (isolation tests).

## Identity

- **REQ-005-1** Every `/api` call except `GET /api/users` MUST carry `X-User-Id` naming a seeded user; missing → 401 "Missing X-User-Id header", unknown/invalid → 401.
- **REQ-005-2** The role is taken from the user record, never from the client.
- **REQ-005-3** `GET /api/users` (unauthenticated) feeds the dev role switcher. **This MUST be removed when real authentication is introduced.**

## Read visibility (single choke point: `AccessService`)

| Role | Requests visible | Transcriptions visible |
|---|---|---|
| USER | Only own | Those of own requests |
| TRANSCRIBER | Requests that have (or had) a specialist task | Those of visible requests |
| QA | All (read-only) | All |

- **REQ-005-4** A request/transcription the caller may not see MUST yield **404**, not 403, so existence is not disclosed.
- **REQ-005-5** All reads of requests, transcriptions, versions and **audio** MUST go through `AccessService`; no repository is exposed directly to controllers for reads.
- **REQ-005-6** Per-user queues (`/requests`, `/queue/completed`) MUST return only the caller's own requests.

## Mutation rules

| Action | Rule |
|---|---|
| Create/edit/upload/submit/retry | Owner (role USER) |
| Send to specialists | Owner |
| Claim/complete specialist task | TRANSCRIBER; complete only by claimant |
| Add version | TRANSCRIBER holding the claimed task |
| Request QA | Owner or transcriber with claimed task; never QA |
| Claim QA / verdict | QA; verdict only by claimant |

- **REQ-005-7** A role/ownership violation on a mutation MUST yield 403 (or 404 where REQ-005-4 applies).

## Acceptance criteria

- *Given* Alice's request, *when* Bob GETs it, *then* 404; his list does not include it.
- *Given* no header, *then* 401.
- *Given* Bob GETs `/transcriptions/{alice's}/audio`, *then* 404.

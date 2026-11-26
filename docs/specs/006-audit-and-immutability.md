# SPEC-006 — Audit & immutability

**Status:** Implemented. See [ADR-0003](../adr/0003-append-only-history.md).

- **REQ-006-1** Nothing is ever deleted: no API deletes requests, recordings, transcriptions, versions, tasks, reviews or audit events.
- **REQ-006-2** `TranscriptionVersion` and `AuditEvent` are immutable (`@Immutable` entities) and their repositories MUST NOT expose update/delete operations (they extend `Repository`, not `JpaRepository`).
- **REQ-006-3** Editing or replacing a transcript MUST append a new version (SPEC-003).
- **REQ-006-4** Metadata edits MUST be audited with previous values (SPEC-001).
- **REQ-006-5** Audit events record: actor (null = system), entity type, entity id, action, optional detail, timestamp.
- **REQ-006-6** The following MUST be audited: request created, metadata updated, recording uploaded, request status changes, send to specialists (incl. QA-failure reopen), task claim/complete, version added, QA requested/claimed/verdict.
- **REQ-006-7** QA users MAY read the audit log (`GET /api/qa/audit`); other roles MUST NOT.
- **REQ-006-8** Mutable operational rows (`Document.status`, `TranscriptionRequest.status`, task/review status) MAY change, but every such change that matters to the workflow MUST also be audited or derivable from retained history.

## Acceptance criteria

- *Given* any sequence of edits, *then* the version count only increases and earlier version content is byte-identical.
- *Given* a USER calls the audit endpoint, *then* 403.
- *Static check:* no `delete`/`remove` methods on version/audit repositories.

## Gaps

- No DB-level enforcement (triggers/revokes) of append-only; protection is at the application layer only.
- Operational status columns are overwritten in place; full state history relies on audit events.

# ADR-0003: Nothing is ever deleted; immutable history

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
Specialist transcribers can edit or entirely replace a transcript. QA needs to see what was replaced and why. Transcripts may be evidence in legal/claims contexts.

## Decision
Transcript content is stored as **append-only versions**. `TranscriptionVersion` and `AuditEvent` are `@Immutable` JPA entities, and their repositories extend Spring Data's bare `Repository` so no delete/update methods exist. No API deletes any entity. Replacement is "a new version with `source=REPLACEMENT`".

## Consequences
+ Full QA/audit trail; trivially diffable history; protection visible in the type system. − Storage grows monotonically; any future privacy erasure/retention requirement needs a new ADR (likely a redaction mechanism, not deletion). − Enforcement is application-level only (no DB triggers/permissions).

## Alternatives
Soft-delete flags on mutable rows (rejected: easy to forget filters, still permits edits); DB-level temporal tables (deferred: H2/Postgres portability).

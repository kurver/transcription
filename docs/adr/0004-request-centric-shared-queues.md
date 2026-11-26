# ADR-0004: Requests are the unit of work; queues are shared and request-level

- **Status:** Accepted (supersedes the initial per-document, per-user design)
- **Date:** 2026-11-26

## Context
The first cut surfaced individual documents as work items with per-user queues. Users described their work as *requests* with case metadata (case ID, case owner, due dates), and wanted one shared transcriber queue and one shared QA queue that people *take from*.

## Decision
- All lists and queues contain **requests** (`RequestSummary`); recordings are only ever shown inside a request.
- Transcriber queue = one `SpecialistTask` per request (`OPEN → CLAIMED → DONE`), claimed by a single transcriber.
- QA queue = `QaReview` rows per **document** (needed because sampling/verdicts are per transcript) but queued and claimed per request.
- The owner's **completed queue** remains private per user.
- Queue order: priority, due date (nulls last), age.
- Request metadata: fixed fields (title, case ID, case owner, due date, priority) plus bounded custom key/value fields.

## Consequences
+ Matches how work is described and prioritised; claiming prevents double work. − QA needs two granularities (document review rows, request presentation). − Fixed fields are columns; custom fields are an element collection (not queryable/indexed yet).

## Alternatives
Per-document queues (rejected by users); fully dynamic metadata schema (rejected: over-engineered for POC).

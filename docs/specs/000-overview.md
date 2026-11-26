# SPEC-000 — Overview

**Status:** Implemented (POC)

## Purpose

An audio transcription service. Users submit **requests** containing audio recordings; recordings are transcribed automatically by a transcription API (mocked in the POC; Azure AI Foundry models expected later); completed work sits in the owner's private queue and can be handed to a shared team of specialist transcribers who correct or replace the machine transcript; a separate QA role samples and reviews the results. Every change is retained for QA and audit.

## Scope

In scope (POC): request management with case metadata, audio upload, asynchronous transcription via a pluggable API client, owner's completed queue, shared transcriber queue with edit/replace and full version history, shared QA queue with random sampling and on-demand review, audit trail, role-switching UI, demo data generator.

Out of scope (POC): real authentication/authorisation, multi-tenancy, real speech-to-text, speaker diarisation or timestamps editing tools, notifications/email, SLA reporting, retention/archival policy, file virus scanning, horizontal scaling of the worker.

## Roles

| Role | Seeded users | Can |
|---|---|---|
| `USER` | Alice (1), Bob (2) | Create/edit own requests, upload recordings, submit, retry, view own completed queue, send completed requests to specialists, request QA on own transcripts |
| `TRANSCRIBER` | Tina (3), Terry (4) | See the shared transcriber queue, claim a request, edit/replace transcripts of claimed requests, complete the task, request QA on documents of claimed requests |
| `QA` | Quinn (5) | See the shared QA queue and all requests (read-only), claim a request's reviews, record PASS/FAIL verdicts with comments, view the audit log |

## Glossary

- **Request** — the unit of work: case metadata plus one or more recordings. Never the recording alone.
- **Recording** — an uploaded audio file. Stored in code/API as `Document` (see [ADR-0010](../adr/0010-audio-first-scope.md)).
- **Transcription** — the transcript of one recording; has an append-only list of **versions** (v1 from the API, later edits/replacements).
- **Specialist task** — a request-level work item in the transcriber queue (`OPEN → CLAIMED → DONE`).
- **QA review** — a document-level review (`OPEN → CLAIMED → PASSED/FAILED`), presented grouped by request.
- **Completed queue** — the owner's private list of requests whose recordings are all transcribed.

## End-to-end flow

1. USER creates a request (metadata) → uploads recordings → submits. (SPEC-001, 002)
2. Worker transcribes each recording via the transcription API; request becomes `COMPLETED` when all succeed. At that moment each transcription is randomly sampled for QA. (SPEC-002, 004)
3. Owner sees the request in their completed queue; optionally sends it to the specialists. (SPEC-003)
4. A TRANSCRIBER claims the request, edits or replaces transcripts (new versions), requests QA where wanted, marks the task done. (SPEC-003)
5. QA claims a request's pending reviews, listens/reads, records PASS/FAIL. A FAIL reopens a transcriber task; completing it re-queues failed recordings for QA (`RECHECK`). (SPEC-004)

## Configuration

| Property (env) | Default | Meaning |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | H2 file `./data/db` | Datasource |
| `STORAGE_ROOT` | `./data/uploads` | Recording storage root |
| `app.transcription.base-url` (`TRANSCRIPTION_BASE_URL`) | `http://localhost:8080/mock-api` | Transcription API |
| `app.processing.enabled` / `poll-ms` | true / 2000 | Worker on/off and poll interval |
| `app.qa.sample-rate` (`QA_SAMPLE_RATE`) | 0.25 | Probability a transcription is sampled for QA |
| `app.mock.delay-min-ms` / `delay-max-ms` / `slow-chance` | 1000 / 8000 / 0.15 | Mock API random latency (slow calls up to 3× max) |
| `app.mock.failure-rate` | 0.0 | Mock API failure probability |
| `app.demo.enabled` | true | Demo generator endpoint |

## Known gaps (cross-cutting)

- No real authentication (ADR-0006). Any caller can claim any seeded identity.
- Migration V3 verified on H2 only, not on PostgreSQL.
- Worker is single-threaded and not safe to run as multiple instances.
- Recording size is capped only by the multipart limit (50 MB file / 200 MB request); no duration limits.

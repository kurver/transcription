# Documentation

This project is developed **spec first**: the specs describe what the system must do, the ADRs record why it is built the way it is. Code and tests follow; when they disagree with a spec, one of them is wrong and the disagreement is resolved explicitly (see [ADR-0011](adr/0011-spec-driven-development.md)).

## Specs (`specs/`) — what the system does

| Spec | Covers |
|---|---|
| [SPEC-000 Overview](specs/000-overview.md) | Purpose, scope, roles, glossary, end-to-end flow, configuration |
| [SPEC-001 Requests & metadata](specs/001-requests-and-metadata.md) | Request lifecycle, case metadata, submission |
| [SPEC-002 Recordings & processing](specs/002-recordings-and-processing.md) | Audio upload, async processing, transcription API contract, status roll-up |
| [SPEC-003 Transcriber queue & versioning](specs/003-transcriber-queue-and-versioning.md) | Hand-off to specialists, claiming, edit/replace, version history |
| [SPEC-004 QA review](specs/004-qa-review.md) | Sampling, on-demand QA, verdicts, failure loop |
| [SPEC-005 Access & visibility](specs/005-access-and-visibility.md) | Identity stub, per-role visibility, mutation rules |
| [SPEC-006 Audit & immutability](specs/006-audit-and-immutability.md) | What is recorded, what can never change or be deleted |
| [SPEC-007 User interface](specs/007-user-interface.md) | Screens, navigation, theming, accessibility |
| [SPEC-008 Demo tooling](specs/008-demo-tooling.md) | Demo request generator, mock API behaviour |
| [SPEC-009 HTTP API](specs/009-http-api.md) | Endpoint reference |

## ADRs (`adr/`) — why

| ADR | Decision | Status |
|---|---|---|
| [0001](adr/0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](adr/0002-technology-stack.md) | Spring Boot + React, Maven, Vite | Accepted |
| [0003](adr/0003-append-only-history.md) | Nothing is ever deleted; immutable history | Accepted |
| [0004](adr/0004-request-centric-shared-queues.md) | Requests are the unit of work; shared queues | Accepted |
| [0005](adr/0005-async-processing-and-transcription-client.md) | DB-polling worker behind a `TranscriptionClient` port; mocked API | Accepted |
| [0006](adr/0006-poc-identity-stub.md) | Header-based identity stub for the POC | Accepted (POC only) |
| [0007](adr/0007-persistence-and-storage.md) | H2 + Flyway (Postgres-ready); local-disk file storage | Accepted |
| [0008](adr/0008-qa-sampling-and-verdicts.md) | Random + on-demand QA; verdict-only; failures reopen work | Accepted |
| [0009](adr/0009-design-tokens-and-theming.md) | CSS custom-property design tokens, `light-dark()` theming | Accepted |
| [0010](adr/0010-audio-first-scope.md) | Audio recordings only; legacy `Document` naming kept | Accepted |
| [0011](adr/0011-spec-driven-development.md) | Spec-driven development workflow | Accepted |

## Conventions

- **Requirement IDs** (`REQ-<spec>-<n>`, e.g. `REQ-004-3`) are stable. Never renumber; retire a requirement by marking it *(removed)*.
- Requirements use **MUST / SHOULD / MAY** in the RFC 2119 sense.
- Each spec has a **Status** per requirement area: *Implemented* (code + test), *Implemented, untested*, or *Gap* (specified, not built). Gaps are listed explicitly rather than hidden.
- Each spec ends with **Acceptance criteria** written as Given/When/Then and, where one exists, the automated test that proves it.
- ADRs are immutable once accepted: to change a decision, add a new ADR that supersedes it and update the old one's status line only.

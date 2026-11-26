# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

POC **audio transcription** service. Everything revolves around **requests** (with metadata: case ID, case owner, due date, priority, custom fields), not individual recordings: users submit requests of audio recordings (the `Document` entity/API naming is legacy and means a recording), a (mocked) transcription API processes them, completed requests sit in the owner's private queue, and can be handed to a shared transcriber queue (edit/replace with full history). A shared QA queue holds document-level reviews, grouped by request.

## Spec-driven workflow

Specs in `docs/specs/` (requirement IDs `REQ-<spec>-<n>`) are the source of truth for behaviour; `docs/adr/` records decisions. Read the relevant spec before changing behaviour. For any behaviour change: update the spec (and add an ADR if a decision is made or reversed) **first**, then the test, then the code, then the spec's Status/Gaps. Never renumber requirement IDs. See `docs/README.md`.

## Layout & commands

- `backend/` — Java 21, Spring Boot 4, Maven wrapper. Run from `backend/`:
  - `./mvnw spring-boot:run` (port 8080; H2 file DB in `backend/data/`, override with `DB_URL`/`DB_USER`/`DB_PASSWORD`, uploads with `STORAGE_ROOT`)
  - `./mvnw test` (`WorkflowTest` is the end-to-end flow + isolation test)
- `frontend/` — React + Vite + TypeScript. Run from `frontend/`: `npm run dev` (proxies `/api` to :8080), `npm run build`.
  - Requires Node >= 22.12 (Vite 8). Node 26 is installed via nvm but the shell default is still 22.2: run `nvm use 26` (or put `~/.nvm/versions/node/v26.13.1/bin` on PATH) first, or `npm install`/`build` will fail on rolldown's native binding.
  - npm must use the public registry (no private CodeArtifact config).

## Architecture rules that are easy to break

- **Nothing is ever deleted.** `TranscriptionVersion` and `AuditEvent` are `@Immutable`, and their repositories extend `Repository` (not `JpaRepository`) so there are no delete methods. Edits/replacements append a new version; never add delete/update paths for these.
- **Queues are request-level, work items are shared.** `RequestViewService` builds every list (`RequestSummary`); never expose documents as queue items. Transcriber queue = `SpecialistTask` per request (OPEN → CLAIMED → DONE, claimed by one transcriber). QA queue = `QaReview` per document, claimed per request via `QaService.claimRequest`. Queue order: priority, then due date, then age.
- **Visibility is enforced in `AccessService`** (all reads go through it): users see only their own requests (others get 404), transcribers only requests that have a specialist task, QA everything read-only. Mutations check roles/claims in the services (e.g. only the claiming transcriber can add versions or request QA).
- **QA**: reviews come from random sampling when a request completes (`app.qa.sample-rate`, default 0.25, once per transcription via `Transcription.qaSampled`) or from an explicit request by the owner / working transcriber. QA records PASS/FAIL only and never edits content. FAIL re-opens a specialist task (`QA_FAILURE`) if none is active; completing a task re-queues failed documents for QA (`RECHECK`).
- **Identity is a POC stub**: the `X-User-Id` header (frontend role switcher) resolved by `CurrentUserResolver`; users are seeded in `V2__seed_users.sql`. Roles: USER, TRANSCRIBER, QA.
- **Transcription API is mocked**: `MockTranscriptionController` (`/mock-api/transcribe`; random per-document delay `app.mock.delay-min-ms`/`delay-max-ms`/`slow-chance`, failure rate `app.mock.failure-rate`) called by `HttpTranscriptionClient` through the `TranscriptionClient` interface. Point `app.transcription.base-url` at the real service later.
- **Audio only**: `RequestService.addDocument` rejects non-audio files (`audio/*` content type or known audio extension). `GET /api/transcriptions/{id}/audio` streams the original recording (same visibility as the transcript) for the player on `TranscriptionPage`; the frontend fetches it with the identity header and plays an object URL. The mock API returns a canned speaker-labelled, timestamped transcript.
- **Demo generator**: `DemoController` (`POST /api/demo/requests?count=N`, gated by `app.demo.enabled`) creates and submits sample requests with silent placeholder WAV recordings; surfaced as the "Generate 3 demo requests" button on My requests. Not for production.
- `ProcessingWorker` (scheduled poll) drives document status; request status is derived from its documents, and completion triggers QA sampling. Tests set `app.processing.enabled=false` and call `processPending()` directly; they share one DB per test class, so scope assertions to the request ids they create (see `TestSupport`).
- Schema is managed by Flyway (`ddl-auto=none`); add a new `V<n>__*.sql` migration for any entity change.
- Boot 4 package moves: e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.

## Frontend styling

- Design tokens are CSS custom properties in `frontend/src/styles/`: `tokens.css` (raw primitives — never reference from components) → `theme.css` (semantic tokens, each defined once as `light-dark(light, dark)`; `<html data-theme>` set by `ThemeToggle`/`useTheme` overrides the OS setting, and an inline script in `index.html` applies the saved choice before first paint). Components and pages use **only** semantic `var(--…)` tokens: no hex/rgb values or raw px colours outside `src/styles/`. Add new colours to `theme.css`.
- Use the shared components in `frontend/src/components/` (Button, Badge, Card, DataTable, Field/Input/Select/Textarea, Alert, PageHeader, AppShell) instead of ad-hoc markup. Each has a CSS Module; page-level styles live in `pages/pages.module.css`.
- Workflow status → colour lives in one place: `components/statusTone.ts`. Add new statuses there.
- Keep text contrast ≥ 4.5:1 and control borders ≥ 3:1 in both themes when changing token values.

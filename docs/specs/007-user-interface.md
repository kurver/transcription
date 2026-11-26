# SPEC-007 — User interface

**Status:** Implemented. React 19 + Vite SPA, `frontend/`. See [ADR-0009](../adr/0009-design-tokens-and-theming.md).

## Navigation (by role, via the dev role switcher "Acting as")

| Role | Links |
|---|---|
| USER | My requests (`/requests`), Completed queue (`/completed`) |
| TRANSCRIBER | Transcriber queue (`/specialist`) |
| QA | QA queue (`/qa`), All requests (`/qa/all`), Audit log (`/qa/audit`) |

Other routes: `/requests/:id` (request detail), `/transcriptions/:id` (transcript).

## Requirements

- **REQ-007-1** Every list is a list of **requests** (shared `RequestTable`), showing case ID, title, case owner, priority, due date (overdue highlighted where relevant), status, and transcribed/total recordings. Individual recordings MUST NOT appear as queue items.
- **REQ-007-2** Request detail shows metadata (incl. custom fields), a Recordings table (name, size, status, version, QA state), specialist task state, QA reviews, and role-appropriate actions (Add recordings, Submit, Retry, Send to specialists, Claim, Complete, Request QA, Claim for QA).
- **REQ-007-3** The transcript page MUST show an audio player for the original recording, the current transcript, version history with diffs, and role-appropriate actions: Edit/replace (claiming transcriber), Request QA, PASS/FAIL form with required comment on FAIL (claiming QA user).
- **REQ-007-4** The file picker MUST be filtered to audio and accept multiple files.
- **REQ-007-5** Lists refresh by polling (3 s) so processing progress is visible without reload.
- **REQ-007-6** The server's error message MUST be shown to the user on failed actions.
- **REQ-007-7** Styling MUST use only semantic design tokens (`theme.css`) and shared components; no raw colours in components/pages. Status → colour mapping lives in `statusTone.ts` only.
- **REQ-007-8** Theme: Auto (OS) / Light / Dark toggle, persisted in `localStorage` (`transcription.theme`) and applied before first paint.
- **REQ-007-9** Text contrast MUST be ≥ 4.5:1 and control borders ≥ 3:1 in both themes.
- **REQ-007-10** Layout MUST remain usable at ~375 px width; focus rings visible; reduced-motion respected.

## Acceptance criteria

- Walk-through as Alice → Tina → Quinn → Terry (create, upload, submit, send, claim, replace, complete, QA fail, task reappears) completes in the UI (verified manually with Playwright script, not committed).
- Contrast script reports all token pairs ≥ thresholds (scratch script, not committed).

## Gaps

- No automated frontend tests; no committed e2e or contrast checks.
- No pagination, search or filtering on lists.
- Audio player is a plain `<audio>` (no waveform, speed control, or transcript time-sync).

# ADR-0010: Audio recordings only; legacy `Document` naming retained

- **Status:** Accepted (supersedes the earlier "any document type" scope)
- **Date:** 2026-11-26

## Context
The POC began as a generic document transcription service (Word, PDF, Excel). The actual product is an **audio transcription** service.

## Decision
- Only audio uploads are accepted (`audio/*` content type or known audio extension); everything else is rejected with 400.
- The original recording is retrievable (`GET /transcriptions/{id}/audio`) with the same visibility as the transcript, for playback during editing and QA.
- The mock API returns timestamped, speaker-labelled text; demo data uses short silent WAVs.
- UI copy says "recordings". The internal/API names `Document`, `/documents`, `documentId` are **kept** to avoid an invasive schema/API rename during the pivot.

## Consequences
+ Pivot delivered with a small, low-risk change set. − Naming mismatch between domain language and code; a rename (table `document` → `recording`, API paths) is deferred and should be its own migration + ADR once the real transcription API integration shape is known. − No validation of actual audio content/duration yet.

# SPEC-002 — Recordings & processing

**Status:** Implemented. Tests: `WorkflowTest`, `QaSamplingTest` (worker driven synchronously via `processPending()`).

## Upload

- **REQ-002-1** A recording is accepted only if its content type starts with `audio/` or its filename extension is one of `mp3 wav m4a aac ogg oga opus flac wma webm amr`. Otherwise HTTP 400 "Only audio recordings are supported".
- **REQ-002-2** Empty files MUST be rejected (400).
- **REQ-002-3** Only the owner may upload. Bytes are stored through the `FileStorage` port under an opaque key; the original filename, content type and size are kept on the `Document` row.
- **REQ-002-4** A recording in a `DRAFT` request starts `PENDING`; in a submitted request it starts `QUEUED`.
- **REQ-002-5** Recordings MUST NOT be deleted or replaced. (A new upload is a new recording.)

## Processing

Recording status: `PENDING → QUEUED → PROCESSING → TRANSCRIBED | FAILED` (`FAILED → QUEUED` on retry).

- **REQ-002-6** Request status is derived after every recording change (not for `DRAFT`): any `PROCESSING` → `PROCESSING`; else any `QUEUED` → `QUEUED`; else any `FAILED` → `FAILED`; else `COMPLETED`.
- **REQ-002-7** A background worker MUST poll for `QUEUED` recordings every `app.processing.poll-ms`, mark them `PROCESSING`, and process them one at a time, in id order.
- **REQ-002-8** On success the worker MUST create the `Transcription` and its version 1 (`source=MOCK_API`) atomically and mark the recording `TRANSCRIBED`.
- **REQ-002-9** On any failure the recording MUST be marked `FAILED` with the error message retained; other recordings in the request continue.
- **REQ-002-10** When a request first becomes `COMPLETED` the worker MUST trigger QA sampling (SPEC-004 REQ-004-1).
- **REQ-002-11** Status transitions MUST be written to the audit trail (`STATUS_<status>`, actor = system).

## Transcription API contract (`TranscriptionClient` port)

- `transcribe(filename, contentType, bytes) → text` — blocking; throws on failure.
- HTTP implementation: `POST {base-url}/transcribe`, multipart field `file`; response JSON `{"text": "..."}`; non-2xx is a failure.
- **REQ-002-12** The base URL MUST be configurable so the mock can be replaced by Azure AI Foundry without code changes outside the client adapter.
- **REQ-002-13** The recording audio MUST remain retrievable (SPEC-005) for playback alongside the transcript.

## Acceptance criteria

- *Given* `a.mp3`, `b.wav` submitted and the worker run, *then* both `TRANSCRIBED`, request `COMPLETED`, each transcript has exactly one version.
- *Given* a client that fails for one recording, *then* that recording is `FAILED`, request `FAILED`; after retry and re-run it completes (`WorkflowTest` failure test).
- *Given* a PDF upload, *then* 400 (`WorkflowTest.onlyAudioIsAccepted…`).

## Gaps

- No audio-content validation (a mislabelled file passes), duration limit, or transcoding.
- Worker has no concurrency, back-off or per-recording retry limit.

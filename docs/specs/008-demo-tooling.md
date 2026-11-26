# SPEC-008 — Demo tooling & mock API

**Status:** Implemented. Not for production.

## Demo request generator

- **REQ-008-1** `POST /api/demo/requests?count=N&submit=true|false` (N clamped to 1–10, default 3; submit default true) creates N requests for the calling USER. Gated by `app.demo.enabled`; absent when false. Other roles → 403.
- **REQ-008-2** Each request is drawn from realistic scenarios (witness interviews, board meetings, claimant calls, dictations, hearings, toolbox talks) with sensible metadata: case ID `CASE-<year>-<nnnn>`, a random case owner, a due date 1–14 days ahead, priority weighted to NORMAL (≈55%), HIGH (≈30%), URGENT (≈15%), and `Department` / `Jurisdiction` custom fields.
- **REQ-008-3** Each request gets 2–4 recordings with descriptive names (e.g. `Interview - J Morgan.wav`), each a valid silent 8 kHz mono 8-bit WAV of 8–24 s so playback works.
- **REQ-008-4** The UI exposes this as "Generate 3 demo requests" on My requests.

## Mock transcription API (`/mock-api/transcribe`)

- **REQ-008-5** Returns a canned, timestamped, speaker-labelled transcript naming the file, content type and an estimated duration derived from file size.
- **REQ-008-6** Latency is random per call so queue processing is visible: uniform in `[delay-min-ms, delay-max-ms]` (1–8 s), and with probability `slow-chance` (0.15) up to 3× max. Setting min = max gives a fixed delay.
- **REQ-008-7** Fails with 503 with probability `app.mock.failure-rate` (default 0).
- **REQ-008-8** Because the worker is sequential, slow calls delay later recordings; this is intended for demonstration.

## Gaps

- Mock ignores audio content and always returns the same text.
- Demo endpoint has no rate limit beyond the count clamp.

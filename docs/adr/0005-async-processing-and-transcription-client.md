# ADR-0005: DB-polling worker behind a `TranscriptionClient` port; mocked API

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
Transcription will be offloaded to an external API (likely Azure AI Foundry models) and can be slow or fail. The POC must demonstrate queueing without that dependency.

## Decision
- Submission only changes status to `QUEUED`. A Spring `@Scheduled` worker polls `QUEUED` recordings, runs them sequentially through the `TranscriptionClient` interface, and updates status in small transactions.
- `HttpTranscriptionClient` posts multipart audio to `app.transcription.base-url`; the default points at an in-app `MockTranscriptionController` with configurable random latency and failure rate.
- Request status is **derived** from recording statuses; completion triggers QA sampling.
- Tests disable the scheduler (`app.processing.enabled=false`) and call `processPending()` directly.

## Consequences
+ No message broker needed; trivially testable; swapping to the real API is a base-URL/adapter change. − Single-threaded and single-instance only; slow items block later ones; no retry/back-off. Scaling needs row-level claiming (`SELECT … FOR UPDATE SKIP LOCKED`) or a real queue — a future ADR.

## Alternatives
Spring events/`@Async` (lost on restart); RabbitMQ/Azure Service Bus (premature for POC).

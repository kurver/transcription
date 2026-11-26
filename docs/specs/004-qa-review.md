# SPEC-004 — QA review

**Status:** Implemented. Tests: `QaSamplingTest` (sample-rate 1.0), `WorkflowTest` (sample-rate 0).

QA reviews are **document-level** records, but are *presented and claimed grouped by request* (see [ADR-0004](../adr/0004-request-centric-shared-queues.md)). QA reviewers never edit transcripts; they record a verdict.

## Sources of reviews

| Source | Trigger |
|---|---|
| `SAMPLED` | Random sampling when a request completes |
| `USER_REQUEST` | Owner asks for QA on one recording |
| `TRANSCRIBER_REQUEST` | Transcriber with a claimed task asks for QA on one recording |
| `RECHECK` | A transcriber completes a task after a QA failure |

- **REQ-004-1** When a request becomes `COMPLETED`, each of its transcriptions that has not yet been sampled MUST be sampled independently with probability `app.qa.sample-rate` (default 0.25). A sampled transcription gets an `OPEN` review (`SAMPLED`). Every transcription MUST be marked as sampled (`qaSampled`) whether or not it was selected, so it is never sampled again (e.g. when a request re-completes after another upload).
- **REQ-004-2** An owner or a transcriber MAY request QA for one document. QA users cannot (403). A transcriber MUST hold a claimed task on that request (403 otherwise). Rejected with 400 if a review is already `OPEN`/`CLAIMED` for that transcription.
- **REQ-004-3** Review states: `OPEN → CLAIMED → PASSED | FAILED`. Stored reviews are never deleted; a new review creates a new row.

## Shared queue and claiming

- **REQ-004-4** All QA users see one shared queue of requests with `OPEN` reviews, plus requests where they hold `CLAIMED` reviews, ordered per REQ-001-10.
- **REQ-004-5** A QA user claims a **request**: all its `OPEN` reviews become `CLAIMED` by them. Nothing to claim → 400.
- **REQ-004-6** QA users MAY view every request read-only ("All requests") and the full audit log.

## Verdicts

- **REQ-004-7** Only the claiming QA user MAY record a verdict on a `CLAIMED` review (403 otherwise). Verdict is `PASS` or `FAIL`.
- **REQ-004-8** A `FAIL` MUST include a comment (400 otherwise). A `PASS` comment is optional. Verdict, comment, reviewer and time are stored on the review.
- **REQ-004-9** On `FAIL`, if the request has no active specialist task, the system MUST create one (`OPEN`, source `QA_FAILURE`, reason naming the document and comment) so it re-enters the shared transcriber queue. If a task is already active, no second task is created.
- **REQ-004-10** When a transcriber completes a task, every document whose latest review is `FAILED` MUST receive a new `OPEN` review with source `RECHECK`.
- **REQ-004-11** QA MUST NOT be able to create versions or otherwise alter transcripts.

## Acceptance criteria

- *Given* sample-rate 1.0 and a completed 2-document request, *then* 2 `OPEN` reviews exist and the request is in Quinn's queue; re-running completion does not create more (`QaSamplingTest`).
- *Given* Quinn fails a document without a comment, *then* 400; with a comment, *then* the request reappears in the transcriber queue with source `QA_FAILURE` (`QaSamplingTest`).
- *Given* Terry has not claimed the request, *when* he requests QA, *then* 403.
- *Given* the transcriber completes the QA-failure task, *then* a `RECHECK` review is `OPEN` for the failed document.

## Gaps / open questions

- Sampling is per-transcription Bernoulli, so a small request may get zero or all documents sampled; stratified or minimum-per-request sampling is undecided.
- No SLA/ageing for QA; no reviewer-conflict rule (a QA user could review work they cannot influence, but there is no restriction by transcriber).

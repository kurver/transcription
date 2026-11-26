# ADR-0008: Random + on-demand QA; verdict-only; failures reopen work

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
QA must see a representative sample, but users and transcribers also need to flag specific recordings. QA is a separate role and should not become a second editing layer.

## Decision
- When a request completes, each not-yet-sampled transcription is sampled with probability `app.qa.sample-rate` (default **0.25**), tracked by `Transcription.qaSampled` so it happens once.
- Users (owner) and transcribers (with a claimed task) can request QA per document.
- QA reviews are per document, claimed per request; QA records **PASS/FAIL + comment only** (comment mandatory on FAIL).
- FAIL reopens a transcriber task (`QA_FAILURE`) if none is active; task completion queues `RECHECK` reviews for failed documents.

## Consequences
+ Simple, auditable loop; QA independence preserved. − Independent Bernoulli sampling gives no per-request guarantees (a 1-recording request is reviewed 25% of the time). Rate is global, not per user/priority. Stratified sampling is a possible future ADR.

## Alternatives
QA editing transcripts directly (rejected by users); fixed "every Nth" sampling (predictable, gameable).

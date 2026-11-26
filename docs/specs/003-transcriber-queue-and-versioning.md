# SPEC-003 — Transcriber queue & versioning

**Status:** Implemented. Tests: `WorkflowTest`.

## Hand-off

- **REQ-003-1** The owner MAY send a `COMPLETED` request to the specialist team with an optional reason. This creates a `SpecialistTask` (`OPEN`, source `USER_REQUEST`) for the **request**, not for individual recordings.
- **REQ-003-2** A request MUST NOT have more than one active (`OPEN`/`CLAIMED`) task; a second send yields 400 "Already with the specialist team".
- **REQ-003-3** Requests that are not `COMPLETED` cannot be sent (400).

## Shared queue and claiming

- **REQ-003-4** All TRANSCRIBERs see one shared queue of requests with an `OPEN` task, plus the requests whose task they themselves have claimed, ordered per REQ-001-10.
- **REQ-003-5** A TRANSCRIBER MAY claim an `OPEN` task (`→ CLAIMED`, `assignedTo` = them). Once claimed it MUST leave the shared queue for everyone else. Claiming a non-open task yields 400.
- **REQ-003-6** Only the claiming transcriber MAY complete a task (`→ DONE`); others get 403.
- **REQ-003-7** Task creation, claim and completion MUST be audited.

## Editing and versions

- **REQ-003-8** Only the transcriber holding a `CLAIMED` task on the request MAY add versions to its transcriptions (403 otherwise).
- **REQ-003-9** A new version has mode `EDIT` (modified text) or `REPLACEMENT` (entirely new text) and MUST include non-blank content and a non-blank comment.
- **REQ-003-10** Adding a version MUST append: `versionNo = previous count + 1`; earlier versions are never modified or removed. The transcription's current version pointer moves to the new version.
- **REQ-003-11** Each version records author, source, comment and timestamp. Version 1 has author null and source `MOCK_API`.
- **REQ-003-12** Any role with visibility of the transcription MAY read its full version history (diffable in the UI).
- **REQ-003-13** A transcriber working a claimed request MAY request QA on any of its documents (SPEC-004).

## Acceptance criteria

- *Given* a completed request sent to specialists, *when* Tina claims it, *then* Terry's queue no longer lists it.
- *Given* Terry has not claimed it, *when* he POSTs a version, *then* 403.
- *Given* Tina replaces a transcript, *then* history has v1 `MOCK_API` and v2 `REPLACEMENT` with her comment, and v1 content is unchanged.

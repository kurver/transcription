# SPEC-001 — Requests & metadata

**Status:** Implemented. Tests: `WorkflowTest`.

## Request lifecycle

`DRAFT → QUEUED → PROCESSING → COMPLETED | FAILED`

- **REQ-001-1** A USER MUST be able to create a request with only a title; all other metadata is optional.
- **REQ-001-2** A request MUST belong to exactly one owner (the creating USER). Only roles `USER` can create requests.
- **REQ-001-3** Request status after submission MUST be derived from its recordings (SPEC-002 REQ-002-6), never set directly by clients.
- **REQ-001-4** Submitting MUST fail unless the request is `DRAFT` and has at least one recording. Submitting moves the request and its recordings to `QUEUED`.
- **REQ-001-5** Adding a recording to an already-submitted request MUST put that recording and the request back in `QUEUED`.
- **REQ-001-6** The owner MUST be able to retry a `FAILED` request; failed recordings return to `QUEUED`.

## Metadata

| Field | Type | Rules |
|---|---|---|
| `title` | string | Required, ≤ 255 chars |
| `caseId` | string | Optional, ≤ 100 chars |
| `caseOwner` | string | Optional, ≤ 200 chars (free text; the person responsible for the case, not the system owner) |
| `dueDate` | date | Optional |
| `priority` | `LOW \| NORMAL \| HIGH \| URGENT` | Default `NORMAL` |
| `customFields` | map<string,string> | ≤ 20 entries; name ≤ 100, value ≤ 1000 chars; names/values trimmed |

- **REQ-001-7** Violating a limit MUST yield HTTP 400 with a message naming the field.
- **REQ-001-8** The owner MAY edit metadata at any time (including after submission). Every edit MUST write an audit event recording the **previous** values.
- **REQ-001-9** Blank optional strings MUST be stored as null.

## Ordering

- **REQ-001-10** Every queue list MUST be ordered by priority (urgent first), then due date ascending (no due date last), then creation time ascending.

## Acceptance criteria

- *Given* Alice with no requests, *when* she POSTs `{"title":"x"}`, *then* a `DRAFT` request with priority `NORMAL` is returned. 
- *Given* a draft with no recordings, *when* submitted, *then* 400.
- *Given* low/urgent/normal-with-due-date requests sent to specialists, *when* a transcriber lists the queue, *then* order is urgent, normal(due), low (`WorkflowTest.queuesAreOrderedByPriorityThenDueDate`).
- *Given* metadata is edited, *then* an audit event `METADATA_UPDATED` holds the previous values.

# ADR-0011: Spec-driven development

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
Requirements arrived conversationally and the product shape changed three times. Behaviour was documented only in code, tests and `CLAUDE.md`.

## Decision
- `docs/specs/` is the source of truth for **what** the system does; `docs/adr/` for **why**. See `docs/README.md`.
- Workflow for any behaviour change: (1) update/add the spec requirement (stable `REQ-…` IDs) and acceptance criteria; (2) add an ADR if an architectural decision is made or reversed; (3) write/adjust the test that expresses the acceptance criterion; (4) implement; (5) update the spec's Status/Gaps.
- Tests reference the requirement they prove (name or comment) where practical.
- `CLAUDE.md` stays short and points to the specs; it holds only guidance for working in the repo.
- The initial specs were reverse-documented from the working POC; their **Gaps** sections are the backlog.

## Consequences
+ Shared, reviewable intent; easier to hand to new people or AI agents; gaps made explicit. − Specs can drift from code unless review enforces the workflow; no automated traceability yet (a future improvement: a check that every `REQ-` has a test reference).

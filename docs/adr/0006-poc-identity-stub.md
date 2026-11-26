# ADR-0006: Header-based identity stub for the POC

- **Status:** Accepted for the POC only; **must be superseded before any real use**
- **Date:** 2026-11-26

## Context
Authentication was explicitly out of scope; the demo needs to switch between USER, TRANSCRIBER and QA quickly.

## Decision
Five users are seeded by migration. The front end's role switcher stores a user id and sends `X-User-Id`; `CurrentUserResolver` loads the user (and role) from the database. `GET /api/users` is unauthenticated to feed the switcher. Authorisation is nonetheless enforced server-side on role, ownership and claims (SPEC-005).

## Consequences
+ Zero setup; authorisation logic is real and testable. − Anyone can impersonate anyone. A future ADR will replace the resolver with OIDC (likely Entra ID given Azure) and remove `/api/users`; the `CurrentUser` abstraction is the seam.

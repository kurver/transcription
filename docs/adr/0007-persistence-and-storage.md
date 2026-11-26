# ADR-0007: H2 + Flyway (PostgreSQL-ready); local-disk file storage

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
POC needs zero-install persistence but a credible path to PostgreSQL. Recordings are binary and potentially large.

## Decision
- H2 (file DB for dev, in-memory for tests/demos) selected via `DB_URL/DB_USER/DB_PASSWORD`; schema owned by **Flyway** migrations (`V1` schema, `V2` seed users, `V3` request-centric queues); Hibernate `ddl-auto=none`. SQL kept portable to PostgreSQL.
- Recording bytes are stored through a `FileStorage` port (`store(InputStream) → key`, `load(key) → bytes`) implemented by `LocalDiskFileStorage` under `STORAGE_ROOT`; the DB holds only the opaque key and metadata.
- Every entity change requires a new `V<n>__*.sql`.

## Consequences
+ Easy local runs; swapping to Postgres/Azure Blob is configuration plus one adapter. − V3 has been verified on H2 only; `load` reads whole files into memory (fine for POC sizes, not for large audio); local disk is not shared across instances.

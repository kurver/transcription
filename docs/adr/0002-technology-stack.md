# ADR-0002: Technology stack

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
Team direction: React front end, Java back end, Maven, Spring Boot.

## Decision
- **Backend:** Java 21, Spring Boot 4, Spring Data JPA (Hibernate 7), Flyway, Jackson 3, Maven wrapper (`./mvnw`). Tests: JUnit + MockMvc against an in-memory H2.
- **Frontend:** React 19, TypeScript, Vite 8, TanStack Query, react-router-dom, CSS Modules. Dev server proxies `/api` to `:8080`.
- Monorepo: `backend/`, `frontend/`, `docs/`.

## Consequences
+ Familiar, mainstream stack with strong tooling. − Spring Boot 4 / Hibernate 7 are new: package moves (e.g. `org.springframework.boot.webmvc.test.autoconfigure`) trip up older examples. − Vite 8 needs Node ≥ 22.12.

## Alternatives
Gradle (rejected: Maven requested); Next.js (rejected: separate Java back end required).

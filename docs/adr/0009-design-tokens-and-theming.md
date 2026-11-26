# ADR-0009: CSS custom-property design tokens with `light-dark()` theming

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
The UI looked unstyled and inconsistent. Direction: clean enterprise look, fresh palette, tokens plus shared components, with light and dark support.

## Decision
- Two token layers in `frontend/src/styles/`: `tokens.css` (raw primitives, never used by components) and `theme.css` (semantic tokens, each defined once with `light-dark(light, dark)`).
- `<html data-theme="light|dark">` (set by `ThemeToggle`/`useTheme`, persisted in `localStorage`, applied by an inline script before first paint) overrides the OS `prefers-color-scheme`; absent = Auto.
- Components/pages use only semantic `var(--…)`; shared components (Button, Badge, Card, DataTable, Field, Alert, PageHeader, AppShell) own the markup/CSS Modules. Status→tone mapping only in `statusTone.ts`.
- Contrast: text ≥ 4.5:1, control borders ≥ 3:1 in both themes (checked by a script).

## Consequences
+ Re-theme from one file; consistent UI; no runtime CSS-in-JS dependency. − `light-dark()` requires modern browsers; contrast check is not yet part of CI.

## Alternatives
Tailwind or a component library (rejected: want token ownership, no new dependency); CSS-in-JS (runtime cost).

# KI-03: Token Audit & Pruning

**Scope:** `conductor/` docs (index, rules, KI index).
**Last verified:** 2026-06-20

## Purpose

Keep the AI **working set** lean — the docs an agent loads to generate code: `conductor/index.md` + `conductor/rules/*` + `conductor/knowledge/INDEX.md`. KIs are lazy-loaded, so individual `KI-NNN_*.md` files do not count against the per-task budget.

## Budget targets

- `conductor/index.md` dense Project Context block: **~1,000–1,500 tokens**.
- Each KI: **split when it passes ~400 lines** along sub-package boundaries (`CORE_RULES.md §6`).
- `INDEX.md`: **one line per KI**, no prose.

## Index pruning rules

- Every KI create / rename / delete updates `knowledge/INDEX.md` in the **same turn** — no stale rows, no dangling links.
- Remove dead links the moment a target file is deleted.
- Don't duplicate a rule across files — keep one canonical home and link to it.

## Running an audit (`/token_audit`)

1. Measure bytes/tokens of `index.md`, `rules/*`, and `knowledge/INDEX.md`.
2. Flag anything over budget; move detail into a lazy-loaded KI or trim redundancy.
3. Report before/after token counts. Stop before `git add` (`CORE_RULES.md §2`).

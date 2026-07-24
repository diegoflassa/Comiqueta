---
name: bootstrap-ai
description: AI model initialization sequence — load order (AGENTS → index → ai_behavior → CORE_RULES → architecture → task-specific KI), key file pointers, escalation matrix, and token discipline rules. Invoked by /initialize.
metadata:
  last-updated: '2026-07-21'
  keywords:
  - bootstrap
  - initialization
  - AI session
  - escalation matrix
  - token discipline
---

# AI Bootstrap (Agnostic)

Any model. Load in order, stop when you have enough for the task.

## Init Sequence

1. `../AGENTS.md` (repo root) — project identity, stack, critical rules
2. `conductor/index.md` — documentation map + project context
3. `conductor/rules/ai_behavior.md` — behavior rules (think-first, correctness-first, simplicity)
4. `conductor/rules/CORE_RULES.md` — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, logging, strings, DB migration safety)
5. `conductor/rules/architecture.md` — **only when the task touches structure** (module graph, layers, MVI, Hilt, Room/SAF)
6. Task-specific KI from `conductor/knowledge/INDEX.md` (lazy-load; do not pre-load)

## Key Files (SOT)

| File | Owns |
|---|---|
| `rules/ai_behavior.md` | How to behave |
| `rules/CORE_RULES.md` | Cross-cutting project rules (Timber logging, strings, tests, DB migrations, changelog) |
| `rules/architecture.md` | Module graph, layers, MVI, Hilt, Room/SAF, build |
| `rules/COMPOSE_RULES.md` | Compose rules (stability, recomposition, memory, theme fidelity) |
| `rules/GRADLE_RULES.md` | Gradle build-tooling standards (DSL, catalog, convention plugins, caching, CI wrapper validation) |
| `rules/PREVIEW_STANDARD.md` | `@Preview` spec |
| `rules/INSTRUMENTED_TEST_STANDARD.md` | Compose UI test pattern |
| `rules/CI.md` | Manual pre-PR checklist |
| `knowledge/INDEX.md` | KI index — fetch individual KIs only on task match |
| `knowledge/TEST_COVERAGE.md` | Per-module test inventory + known gaps |
| `knowledge/KI-TBD.md` | Everything deferred / not yet implemented |
| `plannings/INDEX.md` | Active + archived plans |

## Escalation Matrix

| Situation | Capability needed |
|---|---|
| Architecture decision / "impossible" bug | Highest reasoning tier |
| Room schema / migration, SAF permission lifecycle | Mid-tier or above (mandatory) |
| Feature implementation | Mid-tier or above |
| Review / cross-check / pattern analysis | Any model |

## Token Discipline

- Never read an entire KI unless the task demands it. Use `knowledge/INDEX.md` first.
- Never re-read a file already in context.
- Prefer `grep`/targeted line-range reads over dumping whole files.
- `architecture.md` is step 5, not step 1 — skip it for a one-file change.

# AI Bootstrap (Agnostic)

Any model. Load in order, stop when you have enough for the task.

## Init Sequence
1. `conductor/rules/ai_behavior.md` — behavior rules (think-before-code, surgical, simplicity)
2. `conductor/rules/CORE_RULES.md` — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, Hilt, Room/SAF, Timber, build)
3. `conductor/index.md` — documentation map + project context (arch, rules, workflows)
4. Task-specific KI from `conductor/ki/KI_INDEX.md` (lazy-load; do not pre-load)

## Key Files (SOT)
- `conductor/rules/ai_behavior.md` — core AI behavior rules
- `conductor/rules/CORE_RULES.md` — operational + project rules
- `conductor/index.md` — project architecture, rules, workflows
- `conductor/ki/KI_INDEX.md` — KI index
- `conductor/rules/COMPOSE_RULES.md` — Compose rules (stability, recomposition, memory)
- `conductor/rules/INSTRUMENTED_TEST_STANDARD.md` — Compose UI test pattern

## Escalation Matrix
| Situation | Capability needed |
|-----------|-------------------|
| Architectural / impossible bug | Highest reasoning tier |
| Core module changes | Mid-tier or above (recommended) |
| Review / pattern analysis | Any model |
| Feature deep-dive / bug hunt | Mid-tier or above |

## Token Discipline
- Never read an entire KI unless the task demands it. Use `KI_INDEX.md` first.
- Never re-read a file already in context.
- Prefer `grep`/search over dumping whole files.

# AGENTS.md — Comiqueta Entry Point

Single bootstrap file for all AI agents working on this repo.

**Project:** Comiqueta — premium Android comic reader (CBZ, CBR, CB7, CBT, PDF). Jetpack Compose + Hilt + Room + Nav3.

## Bootstrap (read in order, stop if any fails)

1. **Behavior rules** → [conductor/rules/ai_behavior.md](conductor/rules/ai_behavior.md) — think-before-code, simplicity, surgical.
2. **Operational + project rules** → [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) — git safety, token economy, no-inline-FQN, KI/planning discipline, Hilt/Room/SAF/Timber/build. Self-contained.
3. **Project context** → [conductor/index.md](conductor/index.md) — documentation map + architecture, rules, workflows.
4. **Knowledge index** → [conductor/ki/KI_INDEX.md](conductor/ki/KI_INDEX.md) — one-line KI summaries. Fetch individual `KI-NNN_*.md` files **only** when the current task matches an index entry.

## Knowledge Items

- [KI-001: Viewer Pinch-to-Zoom & Pan](conductor/ki/KI-001_Viewer-Pinch-Zoom-Fix.md)
- [KI-002: Viewer Pinch-to-Zoom NaN State Corruption Fix](conductor/ki/KI-002_Viewer-Pinch-Zoom-NaN-Fix.md)
- [KI-003: Token Audit & Pruning](conductor/ki/KI-003_Token-Audit-and-Pruning.md)

## Rules & Workflows

On-demand — load only when the task matches:

| File | Load when |
|------|-----------|
| [conductor/rules/ai_behavior.md](conductor/rules/ai_behavior.md) | All tasks — universal AI behavior rules |
| [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) | All non-trivial tasks — operational + project rules |
| [conductor/rules/COMPOSE_RULES.md](conductor/rules/COMPOSE_RULES.md) | Touching any `@Composable` / screen |
| [conductor/rules/INSTRUMENTED_TEST_STANDARD.md](conductor/rules/INSTRUMENTED_TEST_STANDARD.md) | Writing or changing Compose UI tests |
| [conductor/workflows/INDEX.md](conductor/workflows/INDEX.md) | Running a workflow (`/clean`, `/remove_filter`, `/update_kis`) |

## Templates

- [conductor/templates/COMMIT_TEMPLATE.md](conductor/templates/COMMIT_TEMPLATE.md) — conventional commit format
- [conductor/templates/TEST_SCOPE_TEMPLATE.md](conductor/templates/TEST_SCOPE_TEMPLATE.md) — deciding test scope before committing

## Invariants

- `conductor/rules/ai_behavior.md` — behavioral rules (think-before-code, surgical, simplicity).
- `conductor/rules/CORE_RULES.md` — operational + project rules (git safety, token economy, KI/planning discipline). **Self-contained — no external/global rules file.**
- Project-specific context in `conductor/index.md`.
- Layering: `UI → VM → Domain → Data`.
- `NO commit without explicit approval.`

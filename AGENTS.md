# AGENTS.md — Comiqueta Entry Point

Single bootstrap file for all AI agents working on this repo.

**Project:** Comiqueta — premium Android comic reader (CBZ, CBR, CB7, CBT, PDF). Jetpack Compose + Hilt + Room + Nav3.

## Bootstrap (read in order, stop if any fails)

1. **Behavior rules** → [conductor/rules/ai_behavior.md](conductor/rules/ai_behavior.md) — think-before-code, correctness-first, simplicity.
2. **Operational + project rules** → [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) — git safety, token economy, code style, KI discipline, regression tests. The rule set spans sibling files under `conductor/rules/` — CORE_RULES holds the master index mapping every `§N` to its file. Self-contained: this repository needs no other checkout.
3. **Rules index** → [conductor/rules/INDEX.md](conductor/rules/INDEX.md) — which rules file owns which topic, and a topic→section lookup. Load a rules file on match; jump to the `§N` you need rather than reading whole.
4. **Project context** → [conductor/index.md](conductor/index.md) — documentation map + architecture, rules, workflows.
5. **Knowledge index** → [conductor/knowledge/INDEX.md](conductor/knowledge/INDEX.md) — one-line KI summaries. Fetch individual `KI-NN-*.md` files **only** when the current task matches an index entry.

## Knowledge Items

- [KI-01: Viewer Pinch-to-Zoom & Pan](conductor/knowledge/KI-01-VIEWER-PINCH-ZOOM-FIX.md)
- [KI-02: Viewer Pinch-to-Zoom NaN State Corruption Fix](conductor/knowledge/KI-02-VIEWER-PINCH-ZOOM-NAN-FIX.md)
- [KI-03: Token Audit & Pruning](conductor/knowledge/KI-03-TOKEN-AUDIT-AND-PRUNING.md)
- [KI-04: Log Filters Catalogue](conductor/knowledge/KI-04-LOG-FILTERS.md)
- [KI-TBD: Future Work Index](conductor/knowledge/KI-TBD.md)

Full index (read this first, not the list above): [conductor/knowledge/INDEX.md](conductor/knowledge/INDEX.md)

## Rules & Workflows

On-demand — load only when the task matches:

| File | Load when |
|------|-----------|
| [conductor/rules/ai_behavior.md](conductor/rules/ai_behavior.md) | All tasks — universal AI behavior rules |
| [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) | All non-trivial tasks — operational + project rules |
| [conductor/rules/architecture.md](conductor/rules/architecture.md) | Touching module structure, layers, DI, or persistence |
| [conductor/rules/COMPOSE_RULES.md](conductor/rules/COMPOSE_RULES.md) | Touching any `@Composable` / screen |
| [conductor/rules/PREVIEW_STANDARD.md](conductor/rules/PREVIEW_STANDARD.md) | Adding or auditing any `@Preview` |
| [conductor/rules/CI.md](conductor/rules/CI.md) | Before opening a PR / merging |
| [conductor/rules/INSTRUMENTED_TEST_STANDARD.md](conductor/rules/INSTRUMENTED_TEST_STANDARD.md) | Writing or changing Compose UI tests |
| [conductor/workflows/INDEX.md](conductor/workflows/INDEX.md) | Running a workflow (`/clean`, `/remove_filter`, `/update_kis`) |

## Templates

- [conductor/templates/COMMIT_TEMPLATE.md](conductor/templates/COMMIT_TEMPLATE.md) — conventional commit format
- [conductor/templates/TEST_SCOPE_TEMPLATE.md](conductor/templates/TEST_SCOPE_TEMPLATE.md) — deciding test scope before committing

## Invariants

- `conductor/rules/ai_behavior.md` — behavioral rules (think-before-code, correctness-first, simplicity).
- `conductor/rules/CORE_RULES.md` — operational + project rules (git safety, token economy, KI/planning discipline). **Self-contained — no external/global rules file.**
- Project-specific context in `conductor/index.md`.
- Layering: `UI → VM → Domain → Data`.
- **All app code must be logged** so a failure can be root-caused from a log capture alone. `debug` may log sensitive values in full; **`release` is the only variant that must redact** — and redacted never means silent (`LOGGING_RULES.md` §8.2 + §8.3).
- **Never `fallbackToDestructiveMigration()`** — every schema change ships a version bump + `Migration` + exported schema + passing migration test in the same turn (`CORE_RULES.md` §13).
- Every bug fix ships a pinning regression test in the same turn (`CORE_RULES.md` §12).
- `NO commit without explicit approval.`

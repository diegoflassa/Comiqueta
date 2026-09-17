# AGENTS.md — Comiqueta AI Entrypoint

Canonical AI entrypoint for this repository, read automatically by every agent. Single bootstrap file
for every AI agent working on this repository - Claude, Gemini, Copilot and the rest. Documentation map, bootstrap
sequence, skills catalogue and KI index → [conductor/index.md](conductor/index.md). Load knowledge lazily; never
pre-load KIs.

**Before acting on the first prompt of a session:** confirm this repository's skills are registered - every folder
under `.agents/skills/` should appear among the available skills - in Claude Code through its `.claude/skills/`
pointer. A missing one means a missing pointer or frontmatter that failed to parse
([DOC_GOVERNANCE.md](conductor/rules/DOC_GOVERNANCE.md) §18.1). The `initialize` skill runs this check.

---

## Bootstrap (read in order, stop if any fails)

1. **Behavior rules** → [conductor/rules/AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) — think-before-code, correctness-first, simplicity.
2. **Operational + project rules** → [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) — git safety, token economy, code style, KI discipline, regression tests. The rule set spans sibling files under `conductor/rules/` — CORE_RULES holds the master index mapping every `§N` to its file. Self-contained: this repository needs no other checkout.
3. **Rules index** → [conductor/rules/INDEX.md](conductor/rules/INDEX.md) — which rules file owns which topic, and a topic→section lookup. Load a rules file on match; jump to the `§N` you need rather than reading whole.
4. **Project context** → [conductor/index.md](conductor/index.md) — documentation map + architecture, rules, workflows.
5. **Knowledge index** → [conductor/knowledge/INDEX.md](conductor/knowledge/INDEX.md) — one-line KI summaries. Fetch individual `KI-NN-*.md` files **only** when the current task matches an index entry.

---

## Critical Rules (must hold before any action)

Rules 1-4 and 22 always apply. The rest are **triggers**: when a trigger fires, load the named skill **before** acting -
the full spec lives there and in the rules section it links, not here. File-anchored rules, and how an agent
that does not discover `.agents/` reaches them, follow the numbered rules.

1. **Git safety (ABSOLUTE):** no `git add`, `commit`, `push`, `mv` or `rm` unless the user asks in the current turn - past approval never carries over. Moves are filesystem moves. → [CORE_RULES.md](conductor/rules/CORE_RULES.md) §2.

2. **Authorisation:** no build and no commit without explicit confirmation in the current turn. → CORE_RULES §1.

3. **Stability first:** preserving existing behaviour outranks every other rule; do an impact analysis before writing logic. → CORE_RULES §0.

4. **Correctness, then economy:** the most-correct end state regardless of effort ([AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) §3); read only what the task needs (CORE_RULES §3, [AGENT_IO_RULES.md](conductor/rules/AGENT_IO_RULES.md) §20–§22). Correctness beats economy, always.

5. **Logging:** use the `logging` skill whenever adding or reviewing a log line, a `catch`, a `runCatching` failure branch or any error path.

6. **Strings:** use the `strings` skill whenever adding or editing user-visible text, `contentDescription` included.

7. **Single activation:** use the `tapguard` skill whenever adding or touching a tappable control - button, navigation trigger, list item, anything that commits.

8. **KI sync:** use the `ki-sync` skill after any change to behaviour, structure or a public contract - the affected KI is updated in the same turn.

9. **Planning:** use the `planning` skill whenever the user asks for a plan or planning in any form, before writing into `conductor/plannings/`, and whenever a META_PLANNING - file or pasted text - is provided.

10. **Deferred work:** use the `models` skill whenever work is written down for a later turn - a plan task, a backlog row, a deferred item, a handoff.

11. **Regression tests:** use the `regression-test` skill before calling any bug fix done.

12. **Changelog:** use the `changelog` skill when a fix, feature or planning task lands.

13. **Secrets:** use the `secrets` skill when touching config or properties files, example scripts, captured evidence, or any document that could carry a credential.

14. **Reference documents:** use the `reference-docs` skill when a task depends on a document from outside the repository, or a path is about to be written into a tracked file.

15. **Rule placement:** use the `rule-placement` skill whenever a new rule or convention is agreed, or a section is added to a rules file.

16. **Agent I/O:** use the `agent-io` skill when editing a document with non-ASCII text, writing a long file, or about to treat console output as a fact.

17. **Enum vs sealed:** use the `enum-vs-sealed` skill when declaring or reviewing a sealed hierarchy in domain or data code.

18. **Interfaces:** use the `interfaces` skill when declaring, extracting or reviewing any interface - one is used only when more than one implementation is already used, or another is highly likely.

19. **Handoff:** use the `handoff` skill when the user asks for a continuation prompt, when ending a session with work open, or before a compaction.

20. **Cross-project rule sync (MANDATORY):** a shared AI-workflow rule added or changed here is written into the two sibling projects in the same turn - in each one's own words, stack and numbering, never as a pointer to another repository. → CORE_RULES §15.

21. **Agent surface (MANDATORY):** Antigravity reads `.agents/` - rules only when always-on or tied to a kind of file, skills at `.agents/skills/<name>/SKILL.md` (never under `conductor/`), slash commands in `.agents/workflows/`. Claude Code reaches each skill and workflow through a pointer at `.claude/skills/<name>/SKILL.md` or `.claude/commands/<name>.md`, added, renamed or deleted with its source. File names are identifiers: a rename fixes every inbound link in the same turn. → [DOC_GOVERNANCE.md](conductor/rules/DOC_GOVERNANCE.md) §18–§18.2.

22. **File size:** a hand-maintained implementation file - production or test code, a script, an executable config - targets 200-400 physical lines and never ends a task above 600; a new, changed or edited legacy file past 600 is decomposed by responsibility in the same task, with no artificial split. → [AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) §9.

**How an agent reaches `.agents/`.** Antigravity registers `.agents/skills/` and loads `.agents/rules/` on its own.
Claude Code registers every skill and workflow through its pointer in `.claude/skills/` or `.claude/commands/`
(`/handoff`, `/clean`), so a skill named by a trigger above loads by name; it loads no `.agents/rules/`, so before
editing a file that matches a row below, open that rule. An agent with neither surface opens
`.agents/skills/<name>/SKILL.md` itself when a trigger names it. `.agents/rules/00-always.md` is always on in
Antigravity, and its content is rules 1-3 above.

| Files being edited | Rule to open |
|---|---|
| `.agents/**/*.md` | [`.agents/rules/agent-surface-parity.md`](.agents/rules/agent-surface-parity.md) |
| `**/ui/**/*.kt`, `**/*Screen.kt`, `**/components/**/*.kt` | [`.agents/rules/compose-widgets.md`](.agents/rules/compose-widgets.md) |
| `**/*Database.kt`, `**/*Dao.kt`, `**/*Entity.kt`, `**/schemas/**` | [`.agents/rules/db-migration.md`](.agents/rules/db-migration.md) |
| `**/*.gradle.kts`, `**/libs.versions.toml`, `build-logic/**` | [`.agents/rules/gradle.md`](.agents/rules/gradle.md) |

---

## Execution Discipline (every turn)

Operative block - obey it without opening anything else. Spec and exceptions:
[AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) §7-§8, [CORE_RULES.md](conductor/rules/CORE_RULES.md) §3 and
[AGENT_IO_RULES.md](conductor/rules/AGENT_IO_RULES.md) §20–§22. **Correctness beats economy** - nothing here licenses
reading, testing or verifying less.

- **Start every task with its list** - done, current (exactly one), pending - before the first tool call of that unit.
- **Mark a task done the moment it is done**, and only when code, tests, KI and verification are all done. Partial
  work stays in progress with what is missing written beside it.
- **Do not re-read what is already in context** unless the file may have changed outside you - a failed edit, a
  build, a git operation, a compaction.
- **Targeted search before whole-file reads**, and independent reads in one parallel batch.
- **Never guess to save a read.** An uncertain signature, field, path or constant is read.
- **Deliverable text comes in one fenced block** - a commit message, a prompt, anything meant to be copied.
- **Do not edit `AGENTS.md`, `CLAUDE.md`, `conductor/rules/**` or a `SKILL.md` mid-session** unless that is the
  task: they sit in the cached prefix, and every turn after the edit pays for it.

---

## Project Context

**Project:** Comiqueta — premium Android comic reader (CBZ, CBR, CB7, CBT, PDF). Jetpack Compose + Hilt + Room + Nav3.

## Invariants

- `conductor/rules/AI_BEHAVIOR.md` — behavioral rules (think-before-code, correctness-first, simplicity).
- `conductor/rules/CORE_RULES.md` — operational + project rules (git safety, token economy, KI/planning discipline). **Self-contained — no external/global rules file.**
- Project-specific context in `conductor/index.md`.
- Layering: `UI → VM → Domain → Data`.
- **All app code must be logged** so a failure can be root-caused from a log capture alone. `debug` may log sensitive values in full; **`release` is the only variant that must redact** — and redacted never means silent (`LOGGING_RULES.md` §8.2 + §8.3).
- **Never `fallbackToDestructiveMigration()`** — every schema change ships a version bump + `Migration` + exported schema + passing migration test in the same turn (`CORE_RULES.md` §13).
- Every bug fix ships a pinning regression test in the same turn (`CORE_RULES.md` §12).
- `NO commit without explicit approval.`

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
| [conductor/rules/AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) | All tasks — universal AI behavior rules |
| [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) | All non-trivial tasks — operational + project rules |
| [conductor/rules/ARCHITECTURE.md](conductor/rules/ARCHITECTURE.md) | Touching module structure, layers, DI, or persistence |
| [conductor/rules/COMPOSE_RULES.md](conductor/rules/COMPOSE_RULES.md) | Touching any `@Composable` / screen |
| [conductor/rules/PREVIEW_STANDARD.md](conductor/rules/PREVIEW_STANDARD.md) | Adding or auditing any `@Preview` |
| [conductor/rules/CI.md](conductor/rules/CI.md) | Before opening a PR / merging |
| [conductor/rules/INSTRUMENTED_TEST_STANDARD.md](conductor/rules/INSTRUMENTED_TEST_STANDARD.md) | Writing or changing Compose UI tests |
| [conductor/workflows/INDEX.md](conductor/workflows/INDEX.md) | Running a workflow (`/clean`, `/remove-filter`, `/update-kis`) |

## Templates

- [conductor/templates/COMMIT_TEMPLATE.md](conductor/templates/COMMIT_TEMPLATE.md) — conventional commit format
- [conductor/templates/TEST_SCOPE_TEMPLATE.md](conductor/templates/TEST_SCOPE_TEMPLATE.md) — deciding test scope before committing

## Where Things Are

- **Documentation map, rules list, skills catalogue, KI index** → [conductor/index.md](conductor/index.md).
- **Rules:** [conductor/rules/INDEX.md](conductor/rules/INDEX.md) says which file owns which topic, and the master
  index at the top of `CORE_RULES.md` maps every `§N` to its file. Load `AI_BEHAVIOR.md` + `CORE_RULES.md` for any
  non-trivial change, `ARCHITECTURE.md` when the task touches structure. Jump to the `§N` you need.
- **Knowledge:** [conductor/knowledge/INDEX.md](conductor/knowledge/INDEX.md) - lazy-load a KI only when the task
  matches its row.
- **Agent surface:** `.agents/rules/` (always-on and file-anchored rules), `.agents/skills/` (procedures and
  moment-bound rules), `.agents/workflows/` (slash commands), and their Claude Code pointers in `.claude/skills/`
  and `.claude/commands/` - [DOC_GOVERNANCE.md](conductor/rules/DOC_GOVERNANCE.md) §18–§18.2.

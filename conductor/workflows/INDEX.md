# Workflows Index — Comiqueta

> Procedural runbooks invoked by slash commands. Each describes a safe, scoped operation an AI runs end-to-end. **Read the workflow file before executing** — it is the spec.

## Available

- **[clean.md](clean.md)** — `/clean`. Pre-commit hygiene: remove debug logs/commented code/orphaned imports, validate `@Preview`s, surface hardcoded UI literals. Never touches logic. Stops before `git add`.
- **[remove-filter.md](remove-filter.md)** — `/remove-filter <FILTER>`. Targeted removal of debug-level `TimberLogger.logD`/`logV` calls tagged with a specific `[TAG]` token. Never touches `logI`/`logW`/`logE`.
- **[update-kis.md](update-kis.md)** — `/update-kis`. Create or update KIs from knowledge accumulated during the session. Present-tense, self-sufficient, no planning markers.

- **[rules-status.md](rules-status.md)** — `/rules-status`. Audit the rule set for contradictions, dead index rows, stale `.agents/` pointers and machine-absolute paths. Read-only.
- **[token-audit.md](token-audit.md)** — `/token-audit`. Measure every context file against its budget and report what should be split, including the always-loaded floor.
- Session handoff is a skill, not a workflow - [handoff](../../.agents/skills/handoff/SKILL.md), loaded when its description matches.

## Execution rules

1. Read the workflow file first — don't improvise.
2. Honor `CORE_RULES.md §2` (Git Safety): no `git add`/`commit`/`mv`/`rm` unless the user asks in the current turn.
3. Stay inside the workflow's declared scope — a runbook defines exactly what it may touch. Correctness still governs the edits you do make (`AI_BEHAVIOR.md §3`).
4. If a workflow would run a build (`./gradlew …`), stop and ask (`CORE_RULES.md §1`).
5. Report files touched, lines changed, and items surfaced but not auto-fixed.

## How to add a workflow

1. Author `<name>.md` - lowercase-hyphen, the slash command's own name - with YAML frontmatter (`name`, `description`), then: scope, procedure, what it does **not** do, verification, related workflows.
2. Add `.agents/workflows/<name>.md` with the same name and a `description` - that file is the Antigravity slash command. A procedure the model should pick up on its own is a skill instead ([DOC_GOVERNANCE.md](../rules/DOC_GOVERNANCE.md) §18). For a tool that maps slash commands through its own config (`slashCmds[]`), add the mapping there as well.
3. List it above with one bullet: bold link + one-sentence purpose.


# Workflows Index — Comiqueta

> Procedural runbooks invoked by slash commands. Each describes a safe, scoped operation an AI runs end-to-end. **Read the workflow file before executing** — it is the spec.

## Available

- **[clean.md](clean.md)** — `/clean`. Pre-commit hygiene: remove debug logs/commented code/orphaned imports, validate `@Preview`s, surface hardcoded UI literals. Never touches logic. Stops before `git add`.
- **[remove_filter.md](remove_filter.md)** — `/remove_filter <FILTER>`. Targeted removal of debug-level `TimberLogger.logD`/`logV` calls tagged with a specific `[TAG]` token. Never touches `logI`/`logW`/`logE`.
- **[update_kis.md](update_kis.md)** — `/update_kis`. Create or update KIs from knowledge accumulated during the session. Present-tense, self-sufficient, no planning markers.

- **[rules_status.md](rules_status.md)** — `/rules_status`. Audit the rule set for contradictions, dead index rows, stale `.agent/` pointers and machine-absolute paths. Read-only.
- **[token_audit.md](token_audit.md)** — `/token_audit`. Measure every context file against its budget and report what should be split, including the always-loaded floor.
- **[continuation_prompt.md](continuation_prompt.md)** — `/continuation_prompt`. Produce a one-block handoff prompt carrying all state needed to resume in another session.

## Execution rules

1. Read the workflow file first — don't improvise.
2. Honor `CORE_RULES.md §2` (Git Safety): no `git add`/`commit`/`mv`/`rm` unless the user asks in the current turn.
3. Stay inside the workflow's declared scope — a runbook defines exactly what it may touch. Correctness still governs the edits you do make (`ai_behavior.md §3`).
4. If a workflow would run a build (`./gradlew …`), stop and ask (`CORE_RULES.md §1`).
5. Report files touched, lines changed, and items surfaced but not auto-fixed.

## How to add a workflow

1. Author `<name>.md` with YAML frontmatter (`name`, `description`), then: scope, procedure, what it does **not** do, verification, related workflows.
2. Add the slash-command mapping in the AI tool's own config (`slashCmds[]`).
3. List it above with one bullet: bold link + one-sentence purpose.


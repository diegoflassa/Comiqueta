# Workflows Index — Comiqueta

> Procedural runbooks invoked by slash commands. Each describes a safe, scoped operation an AI runs end-to-end. **Read the workflow file before executing** — it is the spec.

## Available

- **[clean.md](clean.md)** — `/clean`. Pre-commit hygiene: remove debug logs/commented code/orphaned imports, validate `@Preview`s, surface hardcoded UI literals. Never touches logic. Stops before `git add`.
- **[remove_filter.md](remove_filter.md)** — `/remove_filter <FILTER>`. Surgical removal of debug-level `TimberLogger.logD`/`logV` calls tagged with a specific `[TAG]` token. Never touches `logI`/`logW`/`logE`.
- **[update_kis.md](update_kis.md)** — `/update_kis`. Create or update KIs from knowledge accumulated during the session. Present-tense, self-sufficient, no planning markers.

## Execution rules

1. Read the workflow file first — don't improvise.
2. Honor `CORE_RULES.md §2` (Git Safety): no `git add`/`commit`/`mv`/`rm` unless the user asks in the current turn.
3. Honor `CORE_RULES.md §0` + `ai_behavior.md §3` (surgical): touch only what scope dictates.
4. If a workflow would run a build (`./gradlew …`), stop and ask (`CORE_RULES.md §1`).
5. Report files touched, lines changed, and items surfaced but not auto-fixed.

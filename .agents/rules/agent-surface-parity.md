---
description: The .agents surface Antigravity discovers - rules only when always-on or tied to a kind of file, skills for everything that binds at a recognisable moment, workflows as slash commands - and the Claude Code pointer each skill and workflow gets under .claude, with the names and frontmatter each one needs. Use when creating, renaming or editing any file under .agents, .claude/skills or .claude/commands.
trigger: glob
globs: ".agents/**/*.md,.claude/skills/**/*.md,.claude/commands/*.md"
---

# Agent surface parity

Full spec (source of truth) - [DOC_GOVERNANCE.md](../../conductor/rules/DOC_GOVERNANCE.md) §18 to §18.2.

- **`conductor/rules/` is the source of truth.** A rule or skill here is a checklist plus a link - never a second
  copy of the rule text.
- **Rules** (`.agents/rules/*.md`) only for what loads without the model choosing it - `trigger: always_on`, or
  `trigger: glob` with `globs`. The keys are exactly `description`, `trigger` and `globs`; omitting `trigger`
  means `always_on`.
- **Skills** (`.agents/skills/<name>/SKILL.md`) for every rule that binds at a recognisable moment and every
  procedure. The keys are `name` and `description`; the folder name equals `name`, lowercase-hyphen. A skill never
  lives under `conductor/` - Antigravity would never find it.
- **Workflows** (`.agents/workflows/*.md`) are slash commands, named exactly like the runbook they follow in
  `conductor/workflows/`.
- **`description` is the whole interface** - what the file covers and when it applies. A changed trigger
  condition means a changed `description`, in the same turn.
- **Frontmatter fails silently.** Quote a `description` that contains a colon followed by a space, and confirm
  the file registered rather than assuming it did.
- **Names are identifiers.** Rename and fix every inbound link in the same turn, then run the link checker.
- **Never add `.agents/` to `.gitignore`.** Adding, renaming or deleting anything here updates the catalogue in
  `conductor/index.md`.
- **Antigravity ignores any other key silently**, and `description` is the only thing it matches on.
- **`trigger` values:** `always_on` only for what must hold every turn; `glob` for a rule tied to a kind of file,
  with `globs` quoted. `model_decision` and `manual` are valid but unused here - a rule that binds at a
  recognisable moment is a skill, and a procedure invoked by name is a workflow.
- **A stale `description` is worse than a missing one** - it fires on the wrong tasks and stays quiet on the right
  ones.
- **Discovery honours `.gitignore` with no error and no warning.** Ignore individual files inside `.agents/` if
  needed, never the directory.
- **Every skill and workflow has one Claude Code pointer** - `.claude/skills/<name>/SKILL.md` or
  `.claude/commands/<name>.md`: the same name, the source's `description`, a body that only links to the source,
  never a copy or a symlink. Adding, renaming or deleting one, or changing its `description`, does the same to the
  pointer in the same turn.
- **Claude Code loads no `.agents/rules/`.** Every skill trigger and file-anchored rule stays named in `AGENTS.md`,
  which `CLAUDE.md` imports.

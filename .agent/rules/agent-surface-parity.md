---
description: Rules under .agent/rules are pointers into conductor/rules, never copies, and their frontmatter is the whole interface. Use when creating or editing any file under .agent, or when a rule changes the moment at which it applies.
trigger: glob
globs: .agent/**/*.md
---

# Agent surface parity

Full spec (source of truth) - [DOC_GOVERNANCE.md](../../conductor/rules/DOC_GOVERNANCE.md) §18.

- **`conductor/rules/` is the source of truth. A file here is a pointer** - a short checklist plus a link.
  Never a second copy of the rule text.
- **Antigravity parses exactly three keys** - `description`, `trigger`, `globs`. Anything else is silently
  ignored. `description` is the only thing matched on, so it says what the rule covers and when it applies.
- **Omitting `trigger` means `always_on`.** Choose deliberately - `always_on` only for what must hold every
  turn, `model_decision` for a rule that binds at a recognisable moment, `glob` for a rule tied to a file
  kind, `manual` for a named procedure.
- **A changed trigger condition means a changed `description`, in the same turn.** A stale description fires
  on the wrong tasks and stays quiet on the right ones.
- **Never add `.agent/` to `.gitignore`** - discovery honours gitignore with no error and no warning.
- Adding, renaming or deleting a pointer updates the catalogue in `conductor/index.md`.

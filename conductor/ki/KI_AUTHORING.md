# KI Authoring

Read only when creating or revising a Knowledge Item. Not needed for normal tasks.

## Creating a KI

File: `conductor/ki/KI-NNN_Human-Readable-Name.md` (NNN = zero-padded, name uses Title-Case-With-Hyphens)

Then add a row to the index table in `KI_INDEX.md`.

**Template:**

```markdown
# KI-NNN: Human Readable Title

**Scope:** <modules / files affected>
**Last verified:** YYYY-MM-DD

## Problem
<what knowledge gap or bug>

## Root Cause / Rules
<numbered or prose explanation>

## Fix / Implementation
<code snippet or pattern>

## Validation
- [ ] <testable criteria>
```

## Writing Standards

- Write in **present tense** — a KI is a canonical spec, not a changelog. No "Phase X added…", "Task Y changed…"; state what the code IS.
- Keep a single `**Last verified:** YYYY-MM-DD` line — no dated history chains.
- **Self-sufficient** — no links to `conductor/plannings/*` (those get deleted); inline what the KI needs.
- If a KI grows past ~400 lines or spans multiple sub-packages, **split it** along sub-package boundaries (overview parent + parts). Full discipline: `CORE_RULES.md §6`.

## Keeping the index up to date

**Every create, rename, or revision of a KI must be immediately followed by a matching update to `KI_INDEX.md`.** Never leave the index stale. Specifically:

- **New KI** — add a row with all five columns: ID, filename, Topic, When to read, Key files.
- **Renamed KI** — update the filename cell in the index row.
- **Revised KI** — update Topic, When to read, and Key files if any of them changed.
- **Deleted KI** — remove the row; do not leave a dangling reference.

The index is the only file an AI reads before deciding which KI to load. A stale index wastes tokens on wrong KIs or misses the right one entirely.

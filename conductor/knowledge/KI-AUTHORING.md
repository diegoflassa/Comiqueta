# KI Authoring

Read only when creating or revising a Knowledge Item. Not needed for normal tasks.

## Creating a KI

File: `conductor/knowledge/KI-NN-UPPER-KEBAB-NAME.md` — `NN` is the zero-padded number, the name is UPPERCASE-WITH-HYPHENS (e.g. `KI-04-LOG-FILTERS.md`). Non-numbered KIs use a bare name: `KI-TBD.md`, `KI-AUTHORING.md`.

Then add a row to the index table in `INDEX.md`.

**Template:**

```markdown
# KI-NN: Human Readable Title

**Scope:** <modules / files affected>
**Last verified:** YYYY-MM-DD
**Reflects code:** `<short-sha>`

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
- **Stamp the commit under it:** `**Reflects code:** `<short-sha>``, naming the commit the KI was
  checked against. A date says when someone touched the document; the commit says which code it was
  checked against, and only the second answers "is this wrong, or just old?". Refresh it in the same
  turn you edit the KI, and **only for what you actually re-read** — a bulk re-stamp is a false
  statement. Never invent one: `git log -1 -- <file>` is the answer. Full discipline:
  `CORE_RULES.md §6.4`.
- **Self-sufficient** — no links to `conductor/plannings/*` (those get deleted); inline what the KI needs.
- If a KI grows past ~400 lines or spans multiple sub-packages, **split it** along sub-package boundaries (overview parent + parts). Full discipline: `CORE_RULES.md §6`.

## Keeping the index up to date

**Every create, rename, or revision of a KI must be immediately followed by a matching update to `INDEX.md`.** Never leave the index stale. Specifically:

- **New KI** — add a row with all five columns: ID, filename, Topic, When to read, Key files.
- **Renamed KI** — update the filename cell in the index row.
- **Revised KI** — update Topic, When to read, and Key files if any of them changed.
- **Deleted KI** — remove the row; do not leave a dangling reference.

The index is the only file an AI reads before deciding which KI to load. A stale index wastes tokens on wrong KIs or misses the right one entirely.

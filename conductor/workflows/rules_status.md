---
name: rules_status
description: Audit this repository's rule set for internal contradictions, dead index rows, stale pointers and machine-absolute paths. Reports only - never edits without approval.
---

# /rules_status — Rule Set Audit

> Invoked by `/rules_status`. Read-only. Produces a findings report; **fixes require approval**.

---

## Scope

Everything under `conductor/rules/`, the numeric index inside each rules file, `AGENTS.md`, and every
pointer under `.agent/rules/`.

## Procedure

1. **Index integrity.** For every row in the numeric index at the top of [`CORE_RULES.md`](../rules/CORE_RULES.md):
   a heading with that number exists, and the row's anchor matches the heading text. Report rows with no
   heading, and headings with no row — §17.1 makes both a defect.
2. **Heading levels.** One dot is `###`, two dots is `####`. Report any mismatch.
3. **Duplicate numbers.** No two sections share a number. A duplicate silently breaks every citation of it.
4. **Cross-reference resolution.** Every `§N` cited from a rules file, a KI or a workflow resolves to a
   section that exists **in this repository**. Report dangling citations.
5. **Contradictions.** Compare sections against each other and against `AGENTS.md`. Two rules that both
   claim to be mandatory and cannot both be obeyed is the highest-severity finding — name both.
6. **Pointer freshness.** For each `.agent/rules/*.md`: the section it cites exists, and its `description`
   still describes when the rule actually binds. Report `trigger` omitted (which silently means
   `always_on`) and any second copy of rule text that should be a pointer.
7. **Machine-absolute paths.** Grep tracked documents for `D:\`, `C:\`, `/Users/`, another checkout name,
   or a chat permalink. §16.2 allows none.
8. **Size budget.** Report `CORE_RULES.md` over ~30 KB and any KI over ~400 lines.

## What it does NOT do

- Edit anything. It reports; the user decides.
- Touch another repository. This audit is scoped to this project alone.

## Verification

Report as a table — finding, location, severity, suggested fix. State explicitly when a check passed with
nothing to report; a silent check is indistinguishable from one that was skipped.

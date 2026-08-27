---
name: token_audit
description: Measure the size of every file an AI loads as context and flag what exceeds its budget. Read-only; reports what should be split.
---

# /token_audit — Context Budget Audit

> Invoked by `/token_audit`. Read-only.

---

## Scope

`AGENTS.md`, everything under `conductor/rules/` and `conductor/knowledge/`, and the `.agent/` tree.

## Procedure

1. Measure each file in bytes and lines. Estimate tokens at roughly bytes ÷ 4.
2. **Flag `CORE_RULES.md` over ~30 KB.** It is loaded for any non-trivial change, so its size is paid on
   almost every task — §17.2 requires a split by topic past that point.
3. **Flag any KI over ~400 lines** — §6.3 requires a split along sub-package boundaries.
4. **Flag any `.agent/rules/*.md` over ~40 lines.** A pointer that long has stopped being a pointer and has
   become a second copy of the rule, which §18 forbids.
5. **Flag `always_on` count.** Every `always_on` rule is paid on every turn; more than one or two means the
   budget is being spent on rules that only bind sometimes.
6. **Report the always-loaded total** — `AGENTS.md` plus every `always_on` pointer. That number is the floor
   cost of every single task in this repository.

## What it does NOT do

- Delete or split anything. It measures and recommends.

## Verification

Report a table sorted largest-first, with a column for the budget each file is measured against, and end
with the always-loaded floor in tokens.

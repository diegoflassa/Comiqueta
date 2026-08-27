---
description: Knowledge Items are present-tense specs that must be updated in the same turn as the code they describe. Use after any change to behaviour, structure or a public contract, and when writing or splitting a KI.
trigger: model_decision
---

# KI sync

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §6 to §6.3.

- **Identify the affected KI from [knowledge/INDEX.md](../../conductor/knowledge/INDEX.md) and update it
  before the turn ends.** Do not defer. Sync file tables, business rules, layer boundaries, public contracts
  and test targets - only what changed.
- **Write present tense, as if the change was always the intent.** No "Phase X added", no dated history
  chains. One `**Last verified:** YYYY-MM-DD` line and nothing more.
- **A KI must be readable on its own.** No links to `conductor/plannings/*` - plannings are temporary and a
  KI that links to one breaks when the plan is archived. Inline what the KI needs.
- **Past ~400 lines or covering multiple sub-packages, split it** along sub-package boundaries.
- Adding, renaming or deleting a KI updates `knowledge/INDEX.md` in the same turn.

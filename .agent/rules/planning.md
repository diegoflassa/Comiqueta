---
description: When work needs a written plan, where plans live, how they are archived, and the META_PLANNING synthesis protocol. Use before starting multi-file work, and whenever a META_PLANNING file appears.
trigger: model_decision
---

# Planning

Full spec (source of truth) - [PLANNING_RULES.md](../../conductor/rules/PLANNING_RULES.md) §7 to §7.5.

- **Write a plan when** work spans 3+ files, crosses layers, fixes a blocking bug, gates behind a flag, or
  the approach is uncertain. Plans live in `conductor/plannings/`.
- **A `META_PLANNING_*.md` is a consolidation prompt, never an execution target.** Read it whole, synthesise
  one canonical plan, then **move** the META_PLANNING to `plannings/archived/` and update the index. Do not
  start implementing during the synthesis turn.
- **Never delete a plan.** Completed or obsolete plans move to `plannings/archived/`, which is the one part
  of `plannings/` under version control.
- **A deferred item and the plan that owns it link both ways** - the plan cites the `KI-TBD.md` entry number,
  the entry cites the plan and the task ID. Close both ends in the same turn.
- **`KI-TBD.md` holds only work still to be done.** Evidence goes to `conductor/analysis/`; a standing check
  is a rule, not a backlog item; a closed entry takes its number to history permanently.
- **An agent IDE's own plan documents are not plannings.** Antigravity writes `implementation_plan.md`,
  `task.md` and `walkthrough.md` outside the repo. Transcribe into `conductor/plannings/` before executing.

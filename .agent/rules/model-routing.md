---
description: Every task written down for a later turn must name the model that will run it, with thinking mode, effort and a one-line justification, plus a fallback routing. Use when writing a plan, adding a backlog row, deferring an item to KI-TBD, or drafting a handover prompt.
trigger: model_decision
---

# Model routing for deferred work

Full spec (source of truth) - [PLANNING_RULES.md](../../conductor/rules/PLANNING_RULES.md) §24.

- **The trigger is deferral, not size.** The moment work is recorded for a future turn it becomes a
  routing decision, made now by whoever still has the context.
- **One model per task, decided per task.** A plan of eight tasks makes eight choices.
- **Write it as Model · Think · Effort**, in two places - the plan's routing table and the head of the
  task body. A reader who must scroll back to a table will not scroll back.
- **Two routings per task** - A preferred, B fallback for when A is rate-limited or gone. Each carries
  its own one-line justification. Neither may be blank or "same as above".
- **Declare the pool at the head of the plan, with the date.** Antigravity's picker changes.
- **Correctness first.** Between two tiers, go up. Cheaper wins only between genuine equals - and a
  downgrade justified by "better instructions" requires those instructions to be written first.

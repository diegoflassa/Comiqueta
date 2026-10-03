---
name: models
description: "Every task written down for a later turn names one recommended model from the pool, with its fixed configuration and a one-line justification, plus a fallback from a different provider only when it produces code of the same quality. Use when writing a plan, adding a backlog row, deferring an item to KI-TBD, or drafting a handover prompt."
---

# Model routing for deferred work

Full spec (source of truth) - [PLANNING_RULES.md](../../../conductor/rules/PLANNING_RULES.md) §24. Decision guide -
[KI-05](../../../conductor/knowledge/KI-05-MODEL-SELECTION.md).

- **The trigger is deferral, not size.** The moment work is recorded for a future turn it becomes a
  routing decision, made now by whoever still has the context.
- **One model per task, decided per task.** A plan of eight tasks makes eight choices.
- **Only the pool, only its configuration** (§24.6): Claude 5.0 Opus, Claude 5.0 Sonnet, Claude 5.0 Haiku,
  Grok 4.6, Grok 4.2, Gemini 3.2 Pro High, Gemini 3.8 Flash High, GPT-5.6 Sol, Grok 4.20 (Reasoning), Grok 4.20 (Non-Reasoning), GPT-5.6 Sol Fast, Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1, Claude Haiku 4.5, Claude Opus 4.8, Claude Opus 4.7, Claude Opus 4.6, Claude Sonnet 4.6, GPT-6 Astra, GPT-6 Luna, GPT-6 Sol - each written with the routing string the pool
  table gives it. Never tune an effort or a thinking switch per task; pick another pool model instead.
- **Recommended, always** - the model most likely to execute the task correctly, with a one-line justification.
- **Fallback only when it matches** (§24.4) - from a **different provider** than the recommended model, and only
  when it produces code of the same quality; otherwise `Fallback: none - <why>`. **Fallback notes** carry any
  extra guidance it needs, written into the task body before the fallback is recorded.
- **GPT-6 Astra is T0 - reserved for very difficult problems** (the most capable model) and always carries a T1 fallback from another provider; `Fallback: none` is never written for it, and it is never the fallback of another model. Say in one line why no T1 model is enough.
- **Write them in two places** - the plan's routing table and the head of the task body, each routing with its
  own justification. A reader who must scroll back to a table will not scroll back.
- **Declare the pool at the head of the plan, with the date.** The picker changes.
- **One plan file, written by `Claude 5.0 Opus · Think ON · Effort Extra High`.** A routing from before the current pool is re-routed when its task is
  next touched, before it runs - never in a sweep.
- **Correctness first.** Between two tiers, go up. Cheaper wins only between genuine equals - and a
  downgrade justified by "better instructions" requires those instructions to be written first.

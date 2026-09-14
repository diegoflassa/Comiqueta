# KI-05: Choosing the Model for a Task

**Scope:** every task written down for a later turn - plan tasks, `KI-TBD.md` rows, handoff prompts. No source file.
**Last verified:** 2026-09-13
**Reflects code:** `6dbb14a`

## Problem

PLANNING_RULES §24 makes every deferred task name one recommended model from a fixed pool of eleven, and a
fallback from a different provider only when it produces code of the same quality. The rule says *what* is
recorded. This KI is the judgement behind it: which model a task needs, which models from other providers count
as the same quality for that kind of task, and how that judgement is corrected when a model surprises. Source of truth: [PLANNING_RULES.md](../rules/PLANNING_RULES.md) §24.2 (format), §24.4 (recommended and fallback), §24.5 (cheapest that still guarantees the outcome) and §24.6 (the pool).

## Root Cause / Rules

### 1. Three tiers

Every pool model sits in one tier. **The placement is this repository's working judgement as of 2026-09-13, not a
vendor benchmark**, and only the evidence log (§5) moves a model. It follows each provider's own line-up -
frontier, mid, fast - and where a placement was uncertain the model went to the **lower** tier: over-trusting a
model is the failure that ships bugs, while under-trusting one only costs a stronger model than the task needed.

| Tier | What the task risks | Models |
|---|---|---|
| **T1 - judgement** | A decision still open, where a wrong answer compiles and passes tests: money, auth, crypto, concurrency, a schema migration, a cross-layer design, a root cause not yet found, synthesising competing plans | Claude 5.0 Opus · Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |
| **T2 - specified execution** | A slip inside decisions already made and written down: files named, contract quoted, invariants and acceptance checks in the task body, an exemplar to copy | Claude 5.0 Sonnet · Grok 4.2 · Gemini 3.2 Pro High · GPT-5.6 Sol Fast · Grok 4.20 (Non-Reasoning) |
| **T3 - mechanical** | A slip in an edit with no decision in it: a rename, a string or resource sweep, a documentation sync, a formatting pass | Claude 5.0 Haiku · Gemini 3.8 Flash High |

The routing string of each model - its configuration included - is in PLANNING_RULES §24.6 and is not repeated
here.

### 2. The recommended model

1. **Tier the task by its hardest step.** A task with an easy phase and a hard phase is two tasks
   (PLANNING_RULES §24.3), never an average.
2. **Tighten before you tier.** When the risk is underspecification, write the missing files, contract,
   invariants and acceptance checks into the task body - that can move a task from T1 to T2 (§24.5). When the
   risk is judgement, it stays T1 whatever is written.
3. **Pick within the tier.** The model the evidence log shows succeeding on this kind of task; with no evidence,
   the first model in its tier row. Between genuine equals the cheaper one wins (§24.5), and the justification
   says so.

### 3. The fallback

- **Another provider** (§24.4) - never negotiable.
- **The same tier or higher.** A higher tier always matches. The same tier matches unless the evidence log records
  that model failing this kind of task.
- **Never a lower tier.** If fallback notes would make a lower-tier model good enough, the risk was
  underspecification - re-tier the task itself (§2 step 2) and route it again.
- **No candidate left: `Fallback: none - <why>`.** T1 has exactly one candidate from another provider for each
  recommended model, so a single disqualifying row in the log leaves a T1 task without a fallback.

| Recommended | Fallback candidates - same tier first, then higher |
|---|---|
| Claude 5.0 Opus | Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |
| Grok 4.6 | Claude 5.0 Opus · GPT-5.6 Sol |
| GPT-5.6 Sol | Claude 5.0 Opus · Grok 4.6 · Grok 4.20 (Reasoning) |
| Grok 4.20 (Reasoning) | Claude 5.0 Opus · GPT-5.6 Sol |
| Claude 5.0 Sonnet | Grok 4.2 · Gemini 3.2 Pro High · GPT-5.6 Sol Fast · Grok 4.20 (Non-Reasoning) · then Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |
| Grok 4.2 | Claude 5.0 Sonnet · Gemini 3.2 Pro High · GPT-5.6 Sol Fast · then Claude 5.0 Opus · GPT-5.6 Sol |
| Gemini 3.2 Pro High | Claude 5.0 Sonnet · Grok 4.2 · GPT-5.6 Sol Fast · Grok 4.20 (Non-Reasoning) · then Claude 5.0 Opus · Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |
| GPT-5.6 Sol Fast | Claude 5.0 Sonnet · Grok 4.2 · Gemini 3.2 Pro High · Grok 4.20 (Non-Reasoning) · then Claude 5.0 Opus · Grok 4.6 · Grok 4.20 (Reasoning) |
| Grok 4.20 (Non-Reasoning) | Claude 5.0 Sonnet · Gemini 3.2 Pro High · GPT-5.6 Sol Fast · then Claude 5.0 Opus · GPT-5.6 Sol |
| Claude 5.0 Haiku | Gemini 3.8 Flash High · then Grok 4.2 · Gemini 3.2 Pro High · GPT-5.6 Sol Fast · Grok 4.20 (Non-Reasoning) · Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |
| Gemini 3.8 Flash High | Claude 5.0 Haiku · then Claude 5.0 Sonnet · Grok 4.2 · GPT-5.6 Sol Fast · Grok 4.20 (Non-Reasoning) · Claude 5.0 Opus · Grok 4.6 · GPT-5.6 Sol · Grok 4.20 (Reasoning) |

### 4. Fallback notes

Written only when the fallback needs something the recommended model would not, in the task body next to the
`Fallback:` line, and in the same edit that records the fallback. They carry:

- **What to read first** - the rule section, KI or exemplar file the recommended model would find on its own.
- **An invariant the evidence log shows this model missing** - stated as the rule, with its section.
- **A narrower scope** - the only files it may edit, or the exact output expected.
- **A check before reporting done** - the command or test that proves the result.

Never generic advice ("be careful", "think step by step"), and never a repeat of the task body.

### 5. Evidence log

A row is added when a routed model fails a task, or clearly succeeds where its placement was in doubt. **Two
failures of the same kind** move that model down a tier for that kind of task, or disqualify it as a fallback for
it - the tier table and the candidates table change in the same turn, and the row says so. A success moves a
model up only once it has repeated on the same kind of task.

| Date | Task (plan and task ID, in prose) | Model | Outcome | What it shows |
|---|---|---|---|---|
| - | No entry yet. | - | - | - |

## Fix / Implementation

**An open migration decision (T1).** A Room schema change whose data mapping is not yet decided
([CORE_RULES.md](../rules/CORE_RULES.md) §13).

- `Recommended: Claude 5.0 Opus · Think ON · Effort Extra High` - a wrong mapping loses the user's library and
  still passes a fresh-install test.
- `Fallback: Grok 4.6 · Effort High` - the other T1 model, from another provider.
- `Fallback notes:` read CORE_RULES §13 before editing - a version bump, a real `Migration`, an exported schema and
  a passing migration test in the same turn; never `fallbackToDestructiveMigration()`.

**A screen change with its decisions made (T2).** The files are named and the structure copies a named
exemplar screen.

- `Recommended: Claude 5.0 Sonnet · Think ON · Effort Extra High` - every decision is written; the risk is a slip.
- `Fallback: Grok 4.2 · Think ON` - same tier, another provider.
- `Fallback notes:` copy the exemplar's structure; user-visible text goes through the `strings` skill.

**A string sweep (T3).** One new key in every locale folder (the `strings` skill).

- `Recommended: Claude 5.0 Haiku · Think ON` - no decision in the edit.
- `Fallback: Gemini 3.8 Flash High` - same tier, another provider. No notes needed.

## Validation

- [ ] Every routing names a pool model with its PLANNING_RULES §24.6 routing string, character for character.
- [ ] The recommended model's tier matches the task's hardest step.
- [ ] The fallback's provider differs from the recommended model's.
- [ ] The fallback is the same tier or higher and not disqualified by the evidence log - or the task says
      `Fallback: none - <why>`.
- [ ] Fallback notes, when present, are concrete: something to read, an invariant, a scope or a check.
- [ ] Each routing carries its own one-line justification, in the routing table and at the head of the task body.

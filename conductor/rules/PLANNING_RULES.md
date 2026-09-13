# Planning & Backlog — Comiqueta

Plans, META_PLANNING synthesis, plan lifecycle, the deferred-item backlog, and agent-generated planning artefacts.

> **Part of this project's rule set.** Section numbers are one shared space across
> `conductor/rules/` — `§7` is `§7` no matter which file holds it, and the master index in
> [CORE_RULES.md](CORE_RULES.md) says where each one lives. Numbers are never reused or renumbered.
> Cite as `§N`, never by line.

---

## 7. Planning Protocol

- **Storage:** all plans in `conductor/plannings/`. Completed/obsolete → `conductor/plannings/archived/` (**never delete**). Keep `conductor/plannings/INDEX.md` current.
- **Naming:** `[CODE]_[desc]_plan.md` if a ticket exists, else `[feature]_[desc]_plan.md`.
- **Create a plan when** work spans 3+ files, crosses layers (UI+VM+data), fixes a blocking bug, gates behind a flag, or the approach is uncertain. Content: scope, steps, testing checklist, blockers, dependencies.
- **META_PLANNING** (`META_PLANNING_*.md`) is consolidation scaffolding: synthesise the multi-model proposals into one canonical plan, write it, then archive the META_PLANNING and update `INDEX.md`. Never execute a META_PLANNING as-is.
- **One planning request, one plan file (MANDATORY).** Whatever carried the request — a chat message, a pasted
  template, a META_PLANNING — exactly **one** plan file is created in this repository: never an edition per model,
  a file per proposal, or a split into several plans. If a live plan for the same task already exists, the new
  one replaces it and the old file moves to `archived/` as obsolete in the same turn (§7.2), so one live plan
  remains.

### 7.1 META_PLANNING Protocol (GLOBAL RULE)

A `META_PLANNING_*.md` file is a **consolidation prompt**: it collects planning proposals produced by multiple AI models and instructs one AI to synthesise them into a single canonical execution plan.

**Purpose:** META_PLANNINGs are never executed as-is. They exist only to produce a real plan.

**Execution protocol (mandatory):**

1. **Read the META_PLANNING file** in full.
2. **Synthesise** the proposals into a single canonical `[feature_name]_plan.md` inside `conductor/plannings/` following naming rules.
3. **The synthesised plan is the output** — write it with full scope, implementation steps, testing checklist, blockers, and dependencies.
4. **MOVE the META_PLANNING file to `conductor/plannings/archived/`** after the synthesised plan is written and confirmed. It is scaffolding for the synthesis, never an execution target — but it is the only record of which proposals produced the canonical plan, so it is archived like any other plan (§7.2) and never deleted. Use a filesystem `mv`, never `git mv` (§2).
5. **Update `conductor/plannings/INDEX.md`**: move the META_PLANNING row to the archived table, add the new canonical plan row with status `🔵 Backlog`.
6. **Do NOT start implementing** during the META_PLANNING synthesis turn unless the user explicitly asks. Synthesis = planning only.
  7. **One file.** A synthesis produces **one** plan file (§7). Do not write a sibling edition for
    another model or a file per proposal. Route each task inside it per §24: **one recommended model**
    from the pool with its fixed configuration (§24.6), and a **fallback from a different provider only
    when it produces code of the same quality** (§24.4). **The plan is written by `Claude 5.0 Opus · Think ON · Effort Extra High`.**
     - If sibling editions already exist from before this rule, they stay in sync (§7.1a) until the
       work finishes; then archive the edition that was executed and delete the other, porting any
       notes that exist only in the doomed file. That carve-out from §7.2 applies only to those
       leftover siblings, never to distinct plans.

> **Rationale:** META_PLANNINGs accumulate noise. The synthesis step produces a clean, deduplicated, actionable plan that any AI can execute without re-reading the original proposals.

### 7.1a Multi-Edition Plan Sync (GLOBAL RULE)

**New plans are one file.** Do not create a second edition. This section applies only when sibling
files already exist.

When leftover sibling files of one task set are still live, those files are **two views of ONE backlog,
not two backlogs**. They must never disagree.

- **Same task IDs, same numbering, forever.** Task IDs are authoritative — never renumber them in one
  edition without renumbering every sibling identically.
- **Mirror every advance in the same turn.** When a task is completed or advanced in one edition,
  update *all* sibling editions in that same turn: the task-index row, the task body/header, and a
  mirrored revision-history note.
- **Each edition must carry this rule in its own text**, so an executor that opens only one edition
  still learns it has siblings to update.
- **One source of truth.** Where the task set is also tracked elsewhere (KI-TBD, a plan's own
  tracker), that tracker wins. If editions drift from it or from each other, reconcile *every*
  edition to the tracker.
- **At completion**, collapse the editions down to one survivor per §7.1 step 7.

### 7.2 Plan Lifecycle

- ❌ **NEVER DELETE** completed or obsolete plans from `conductor/plannings/` — **META_PLANNINGs
  included** (§7.1 step 4). **Sole exception:** redundant *editions* of one plan collapse to a
  single survivor at completion — see §7.1 step 7.
- ✅ **MOVE** completed or expired plans to `conductor/plannings/archived/` for permanent record.
- 🔒 **`archived/` is version-controlled; the rest of `conductor/plannings/` is not.** `.gitignore`
  ignores `conductor/plannings` and re-includes `archived/`, so the permanent record has history and
  survives a lost machine, while live plans and `KI-TBD.md` stay untracked. Two consequences: archiving
  a plan is a real commit-worthy change, and §6.2 still binds — a tracked document may never link to a
  live plan or to `KI-TBD.md`, only to an archived one.
- **Status Transitions:**
  - **Ready → Active**: Update plan with start date, update `INDEX.md` status.
  - **Active → Completed**: Create/update corresponding KI, link bidirectionally, move to archived.
  - **Active → Obsolete**: Archive immediately if plan is superseded or ticket closed without implementation.

### 7.3 Deferred items and plans link BOTH ways (MANDATORY)

`conductor/knowledge/KI-TBD.md` is the single source of truth for every deferred item; plans are where
those items get executed. **The moment a plan takes ownership of a TBD entry, the two gain a link to
each other** — the plan's task cites the TBD entry number, and the TBD entry cites the plan file and
the task ID inside it.

Without the link, both failure modes are silent. The same entry gets planned twice and the overlap is
not knowable from reading either plan. Worse in the other direction: the work ships, the plan is
archived, and the TBD entry still lists it as open — so the next session re-investigates something that
is already in production.

- **Cite the entry number and the task ID** (`#12`, `T4`). Never cite line numbers — both documents are
  edited constantly and line numbers drift within the day.
- **Link when the entry is picked up, not retroactively in a sweep.** A bulk back-link pass done from
  titles alone produces confident-looking links nobody ever verified against the entry body.
- **On completion, close both ends in the same turn:** the TBD entry moves to the closed-item history
  with its evidence trail, and the plan is archived (§7.2). An entry that merely *looks* done stays open
  until someone proves it.
- **A blocked entry still gets a task**, created with the blocked status and the blocker named. A blocker
  that lives only in someone's memory is indistinguishable from an item nobody looked at.

`KI-TBD.md` and live plans are both untracked (§7.2), so they may link to each other freely. The
prohibition binds in the other direction only: no KI, rules file or other **tracked** document may link
either of them (§6.2) — state the fact in prose instead.

### 7.4 The deferred-item backlog holds ONLY work still to be done (MANDATORY)

`KI-TBD.md` is a **worklist, not an archive**. One test decides any line in it: **would an executor
picking this up tomorrow need it?** If not, it does not belong there. Finished work belongs in the
closed-item history, bulky evidence in `conductor/analysis/`, and a standing check in a rules file (§17).

**Tidy it in the same turn you touch an entry, never in a periodic sweep.** A backlog only reaches the
state where it needs a dedicated cleanup pass because every prior session deferred the cleanup — and by
then each verdict can silently delete the only record that some work is still outstanding.

**What must not accumulate there:**

- **Closed work.** The moment an item is done end to end it moves to the history document with its
  evidence trail, in the same turn, and its index row goes with it (§7.3).
- **Raw evidence.** Logcat dumps, crash captures and long narrative analysis go to `conductor/analysis/`
  and are cited from the entry. An entry states the *conclusion* and what remains; it is not the place
  to re-read a capture.
- **Commented-out or superseded bodies.** Move it or delete it — an invisible 30-line block is pure cost.
- **Standing checks and policies.** A recurring check is a **rule**, not deferred work; route it to the
  rules file that owns the topic (§17). Deferred work is finishable; a standing check never is, so it can
  never legitimately leave the backlog and will distort every count forever.

**Leanness is never bought by closing live work.** An entry does **not** close when evidence is still
outstanding, when the decision or investigation is done but the implementation is deferred, or when it is
partially done. The last two are the common cases, so expect **few** closures and never inflate the count
to shorten the file. Leanness comes from cutting dead weight out of surviving entries, not from closing
them. And **numbers never come back**: a closed entry takes its number to history permanently — never
renumber survivors to close gaps, and never hand a freed number to a new item.

### 7.5 Agent-generated planning artefacts are NOT plannings (MANDATORY)

Agent IDEs write their own planning documents outside the repository. Antigravity stores
`implementation_plan.md`, `task.md` and `walkthrough.md` under `~/.gemini/antigravity*/brain/<id>/`, keyed
by conversation — invisible to git, to every other tool, and to the next session.

Treat them exactly as a META_PLANNING (§7.1): **scaffolding, never the plan.** Work meeting the §7
creation criteria — 3+ files, crosses layers, fixes a blocking bug, uncertain approach — is transcribed
into `conductor/plannings/` **before execution starts**, and that file is what every later turn cites.
Never cite a brain-directory path from a plan, a KI or a commit message: it resolves for one machine and
one conversation and for nothing else (§16.2).

---

## 24. Model Assignment for Deferred Tasks (GLOBAL - MANDATORY)

**Any task written down to be executed later must name the model that will execute it.** The trigger
is deferral, not size: the moment work is recorded for a future turn — a plan step, a backlog row, a
`KI-TBD.md` item, a handover prompt — it stops being "whatever model picks it up" and becomes a
routing decision, made now by whoever still has the context.

A task with no model named is not routed. It gets taken by whichever model opens the file next, which
in practice is whichever one happened to open the session — and that is how a migration touching
money or auth ends up attempted by the model chosen that morning for lint fixes.

### 24.1 One model per task, decided per task (MANDATORY)

A plan of eight tasks makes eight independent choices, not one choice applied eight times. Plans mix
trivial mechanical edits with exhaustive cross-layer work, and a single plan-wide model is wrong at
both ends: it burns the top tier on renames and under-serves the one task that actually needed it.

Where a plan genuinely does route uniformly, that is still eight recorded choices that happened to
agree — say so at the head of the plan and keep the per-task annotation anyway, so a row stays
correct when it is later moved, split, or copied into another plan.

### 24.2 Annotation format (MANDATORY)

**A routing is one model from the pool (§24.6), written with its configuration exactly as the pool table
gives it.** The configuration belongs to the model, not to the task: never raise or lower an effort, and never
switch thinking on or off, to suit a task — pick a different pool model instead.

| Line | Written as | Required |
|---|---|---|
| Recommended | `Recommended: <routing string from §24.6>` | always |
| Fallback | `Fallback: <routing string from §24.6>`, or `Fallback: none — <why>` | always — `none` is an answer |
| Fallback notes | `Fallback notes: <what the fallback needs that the recommended model does not>` | only when needed |

For example: `Recommended: Claude 5.0 Opus · Think ON · Effort Extra High` and `Fallback: Grok 4.6 · Effort High`.

It appears in **two** places per task: in the plan's routing table, and again at the head of the task
body. The duplication is deliberate — a task body is read on its own, far from the table, and a
reader who must scroll back to a table to learn how to run the task will not scroll back.

**The pool is declared, never assumed.** Antigravity's model picker changes without notice, so a plan
names the models it was routed against at its head, with the date. A stale pool is visible; an
implicit one is not.

### 24.3 Correctness first (MANDATORY)

**Pick the model most likely to execute that specific task correctly** — that is the recommended model. Cost and speed are not the
criterion. They enter only as a tiebreaker between two candidates genuinely equally likely to
succeed — and when they do, name both and say why the cheaper one still suffices.

- When a task sits between two tiers, go **up**, not down.
- When a task has an easy phase and a hard phase, split it into two tasks with two models rather than
  averaging them into one.
- **State a one-line justification beside every choice** — why this model is enough, or why a stronger
  one is required. A bare model name records the conclusion and discards the reasoning, so the next
  person to touch the row cannot tell whether it was judged or copied.

### 24.4 A recommended model, and a fallback only when it matches (MANDATORY)

**Every deferred task names one recommended model and, only when one exists, a fallback from a different
provider that produces code of the same quality.** §24.1 still holds — both choices are made per task.

| Routing | Meaning |
|---|---|
| **Recommended** | the model most likely to execute this task correctly (§24.3) |
| **Fallback** | what runs when the recommended model is rate-limited, withdrawn, or missing from the picker |
| **Fallback notes** | what the fallback needs to reach the same quality — a file to read first, an invariant it is known to miss, a narrower edit scope |

Both routings appear in both places §24.2 requires. In the table the routings alone suffice; at the head of
the task body **each one carries its own one-line justification**. Two model names sharing one sentence is one
recommendation written twice, not two.

- **A different provider, always.** Recommended Claude → fallback Grok or Gemini; recommended Grok → Claude or
  Gemini; recommended Gemini → Claude or Grok. A second model from the same provider is not a fallback: the
  quota, outage or account problem that stops one stops both.
- **The same code quality, or no fallback.** When no model from another provider will produce code as good as
  the recommended one on this task, write `Fallback: none — <why>`, and the task waits for the recommended model
  instead of running on a weaker one. The explicit `none` records that the question was judged; a missing line
  looks forgotten.
- **Fallback notes are written before the fallback is recorded.** A fallback that matches only with extra
  guidance matches only once that guidance is in the task body (§24.5) — a fallback justified by notes nobody
  wrote is just a downgrade.
- **Say which capability drove a split** when the two routings differ in kind: large-context sweep, deep
  non-deterministic reasoning, long mechanical edit.
- **The decision guide is [KI-05](../knowledge/KI-05-MODEL-SELECTION.md)** — the tier of every pool model,
  which models count as the same quality, and the evidence that corrects both.

Why a fallback at all: the available pool is an operational constraint that changes without warning — a quota
is hit mid-plan, a preview model is withdrawn, an account loses a provider. A task carrying only the routing
that was legal the day it was written gets silently substituted by whoever opens it next, and the recorded
reasoning is lost. Recording the fallback — or recording that none is good enough — means the substitution
**was judged in advance by whoever had the context**.

### 24.5 Prefer the cheapest model that still guarantees the outcome (MANDATORY)

§24.3 says correctness decides, and it still does. This is about what happens *after* two
candidates are both judged able to deliver the same correct result: **route to the cheaper one.**

- **Reach for the top tier because the task needs it, not by default.** Reflexively routing everything
  to the strongest model is not caution — it stops the routing decision from carrying any information.
- **A better-specified task can legitimately lower the tier.** Where the risk that justified the top
  tier is *underspecification* — an ambiguous contract, an unstated invariant, an unnamed file — remove
  that risk in the plan instead of buying a bigger model: name the files, quote the contract, state
  the invariant, list the acceptance checks, point at the exemplar to copy. **The instructions must be
  written into the task body before the downgrade is recorded** — a downgrade justified by guidance
  nobody wrote is just a downgrade.
- **The bound, and it is absolute: never trade quality for tier.** If the risk is *judgement* rather
  than specification — cross-cutting synthesis, non-deterministic concurrency, a decision that must
  not be improvised, a money path — no amount of extra instruction substitutes for capability, and the
  task stays at the tier §24.3 chose.
- **Record the reasoning, not just the outcome.** A tier that dropped with no recorded justification
  reads as an unreviewed cost cut, and the next executor cannot tell whether it was judged or guessed.
- **Applies to the recommended model and the fallback of §24.4, independently**, and **is revisited when the task changes**:
  if scope grows or the contract turns out ambiguous, the tier goes back up before execution, not
  after a failed attempt.

### 24.6 One file, one pool (MANDATORY)

**One plan file** (§7). Do not write a second edition for another model. Every task inside it is routed to
**models from this pool only, each written with exactly the routing string below.** **The plan is written by
`Claude 5.0 Opus · Think ON · Effort Extra High`.**

**Pool (2026-09-13):** seven models from three providers. No other model is named.

| Model | Provider | Routing string — configuration included |
|---|---|---|
| Claude 5.0 Opus | Anthropic | `Claude 5.0 Opus · Think ON · Effort Extra High` |
| Claude 5.0 Sonnet | Anthropic | `Claude 5.0 Sonnet · Think ON · Effort Extra High` |
| Claude 5.0 Haiku | Anthropic | `Claude 5.0 Haiku · Think ON` |
| Grok 4.6 | xAI | `Grok 4.6 · Effort High` |
| Grok 4.2 | xAI | `Grok 4.2 · Think ON` |
| Gemini 3.2 Pro High | Google | `Gemini 3.2 Pro High` |
| Gemini 3.8 Flash High | Google | `Gemini 3.8 Flash High` |

Which of them a task needs, and which count as the same quality from another provider, is
[KI-05](../knowledge/KI-05-MODEL-SELECTION.md). **When the pool changes, this table and KI-05 change in
the same turn.**

**Routings written before this pool** — `A:` / `B:` lines, `Grok 4.20 Reasoning`, a fallback from the same
provider, an effort chosen per task — are re-routed under §24.2–§24.4 **when their task is next planned,
edited or picked up for execution, and before it runs**. Never in a bulk sweep: a routing rewritten
mechanically is a routing nobody judged.

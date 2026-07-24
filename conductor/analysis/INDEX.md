# Analysis Index — Comiqueta

> One-off investigations: root-cause write-ups, cross-project comparisons, audit findings, flow verifications. **Lazy-load** — read only when the current task references one by name.

An analysis document is a **snapshot of an investigation**, not a spec. It may go stale the moment the code changes; that is expected and acceptable. The moment a finding becomes a durable rule or contract, promote it:

| Finding type | Promote to |
|---|---|
| A durable contract or business rule | the relevant KI in `knowledge/` |
| A project-wide rule | `rules/CORE_RULES.md` |
| Work to be executed | a plan in `plannings/` |

The analysis file then stays as provenance — it is never the source of truth.

---

## Available Analyses

_None yet._

---

## How to add an entry

1. Name the file for the question it answers, in `SCREAMING_SNAKE_CASE.md` (e.g. `SYNC_FLOW_AUDIT.md`).
2. Open with the date, the question, and the verdict — an AI reading it should get the conclusion in the first five lines.
3. Add a bullet here: **bolded link** + one sentence + the date it was written.
4. If the analysis produced a durable rule, promote it per the table above and say so in the bullet.

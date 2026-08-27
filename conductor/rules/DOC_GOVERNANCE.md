# Documentation Governance — Comiqueta

External reference documents, where a new rule is written, and parity between the agent surfaces.

> **Part of this project's rule set.** Section numbers are one shared space across
> `conductor/rules/` — `§16` is `§16` no matter which file holds it, and the master index in
> [CORE_RULES.md](CORE_RULES.md) says where each one lives. Numbers are never reused or renumbered.
> Cite as `§N`, never by line.

---

## 16. Reference Document Ownership (GLOBAL - MANDATORY)

**Every externally-provided document a task depends on is copied into `conductor/references/` before it is
used, and cited from there - never from where it arrived.** A contract, spec, mockup, PDF, screenshot or
exported chat that exists only in `Downloads/`, an e-mail, a chat paste or another checkout is **not**
available to this project.

| Material | Where it goes |
|---|---|
| Someone else's contract or spec - API docs, wire formats, SDK or hardware docs, vendor PDFs | `conductor/references/` |
| Design mockups, screenshots, reference images | `conductor/references/assets/<topic>/` |
| A Q&A thread, or answers received to our own questions | `conductor/references/`, beside the contract it answers |
| A one-off investigation of our own code, or a capture | `conductor/analysis/` - durable findings get promoted into a KI |

- **Copy before use, in the same turn.** The moment a document justifies a decision, shapes a DTO or becomes
  a plan step, it must already be in `conductor/`. *"I read it from `Downloads/`"* is the exact failure this
  rule prevents.
- **Register it.** `conductor/references/INDEX.md` gets a row: link, plus one sentence on what is in it and
  when to read it. An unindexed reference is one nobody lazy-loads.
- **Record provenance and status inside the file** - who provided it, when, and whether it is **ratified or
  a draft**. A draft that reads as a contract is worse than no document: work gets built against it with
  full confidence.
- **Transcribe verbatim; never tidy someone else's contract.** Corrections go in a banner at the top or in
  the consuming KI, so the mirror stays a mirror. Where two references conflict, the newer one wins and the
  older gains a banner saying so.
- **Keep binary originals** beside any text extraction - an extraction is lossy, and tables are exactly
  where that loss hides.

### 16.1 A new version never overwrites the old one (MANDATORY)

**When a document arrives that would replace a file in `conductor/references/`, BOTH versions are kept.**
Contracts get re-sent under the same filename, and each drop silently destroys the record of what the
contract said when the code was written against it. When this app and its source of truth disagree, the
first question is always *which version was this built against* - unanswerable once the old file is gone.
The diff between two drops is often the most valuable document of all: it is the only place a *silent*
contract change is visible.

1. **The canonical filename never changes and always holds the newest version.**
2. **Move the outgoing version** to `conductor/references/superseded/<basename>_<YYYY-MM-DD>.<ext>` - a
   filesystem `mv`, never `git mv` (§2).
3. **The date is the document's own date**, not the date it arrived - that is the date the sender will cite.
   If it carries none, use the receipt date and say so in the index row.
4. **Write the new version at the canonical path** and stamp its date if the sender did not.
5. **Add a row to `conductor/references/superseded/INDEX.md`**: file, date, and one line on *what changed*.
   A superseded file with no note forces a full diff to answer any question.

Dating every version is unambiguous but breaks every inbound link on every drop. Dating only the new file
is worse: the newest document ends up being the one *with* a date while the stale one keeps the plain name,
which is backwards from how anyone reads it. A date sorts, is self-describing, and matches what the sender
remembers - a `-v2` counter does none of those.

### 16.2 No machine-absolute paths in a tracked document (MANDATORY)

**This project is self-contained.** No rule, KI, plan or `AGENTS.md` may point at an absolute path
(`D:\users\...`), another checkout on this machine, a chat permalink, or any location outside this
repository as the place to read something. Such a path resolves for exactly one person on exactly one
machine, and silently resolves to nothing - or to a *different* version - for everyone and everything else,
including a future session on the same machine.

If material owned by another project matters here, **copy the part that matters into
`conductor/references/`** per §16 and cite the copy. Naming another system in prose as context is fine;
citing it as a path that has to be opened to do the work is not. A clone of this repository alone must be
enough to do any task in it.

---

## 17. Rule Placement (GLOBAL - MANDATORY)

**Every rule agreed with the user is written into this project's `conductor/rules/` in the same turn it is
agreed.** A rule that lives only in a chat thread, a commit message or a source comment does not exist: the
next session cannot see it, and neither can any other tool reading the repo.

- **Write it into the file that already owns the topic.** Build tooling -> [`GRADLE_RULES.md`](GRADLE_RULES.md).
  Standards, logging, KI discipline, planning -> this file. Compose -> [`COMPOSE_RULES.md`](COMPOSE_RULES.md).
  Previews -> [`PREVIEW_STANDARD.md`](PREVIEW_STANDARD.md). Do not create a new rules file when an existing
  one owns the subject.
- **`AGENTS.md` gets a one-line pointer only when the rule must be honoured before any action.** Otherwise
  the rules file carries the full spec and `AGENTS.md` stays short enough to read every session.
- **Never park a rule in a user-level or tool-level config file.** Anything specific to this codebase belongs
  here, version-controlled next to the code it governs and visible to every contributor and every model.
- **Only record what was actually agreed.** Do not fold in adjacent rules that seemed like a good idea at the
  time: an unrequested rule is indistinguishable from an agreed one once written down, and it will be
  enforced as if it had been.

### 17.1 Heading level, and the index row (MANDATORY)

A rules file is navigated by number, not by scrolling, and other documents cite subsections directly
(`§8.3`, `§7.1`). Two mechanical mistakes break that:

- **The heading level matches the number's depth.** One dot is `###`, two dots is `####`. A `§1.1` written
  as `##` renders as a sibling of `§1` rather than a child of it. The anchor derives from the heading *text*,
  not its level, so fixing a level never breaks an inbound link.
- **Every new heading gets its index row in the same edit that adds the heading.** Add both or neither - a
  table of contents that omits a MANDATORY rule twenty lines below it is worse than no table at all.

**Never renumber an existing section.** Its number is a public identifier that KIs, workflows and other
rules already cite by hand. New sections append at the end, even where a lower number would read better.

### 17.2 A rules file has a token budget (MANDATORY)

`CORE_RULES.md` is loaded for any non-trivial change, so its size is paid on almost every task - which puts
it in direct tension with §3. **Past roughly 30 KB, split it by topic** into a sibling file under
`conductor/rules/`, leave the section numbers untouched, and point the numeric index at the new home. This
is §6.3 applied to the one file nobody thinks to measure.

The failure it prevents is concrete. A rules file that grows to 90 KB is roughly 24k tokens, and a header
telling the agent to read it *for any non-trivial change* then contradicts the token-economy rule inside it.
Both cannot be obeyed, so one is silently ignored - and it will be whichever the agent notices last.

---

## 18. Agent Surface Parity (GLOBAL - MANDATORY)

More than one agent surface reads this repository: `AGENTS.md` + `conductor/` (Claude Code, Gemini CLI,
Copilot) and `.agent/rules/` + `.agent/workflows/` (Antigravity). Two surfaces holding the same rule
diverge by default, and nothing detects it.

- **`conductor/rules/` is the single source of truth.** Every file under `.agent/rules/` is a **pointer**: a
  checklist of at most ~25 lines plus a link to the section that owns the spec. Never a second copy of the
  rule text.
- **Frontmatter is the whole interface.** Antigravity parses exactly three keys - `description`, `trigger`,
  `globs`. Anything else is ignored silently. `description` is the only thing the model matches on, so it
  states **what the rule covers and when it applies**, never just a title.
- **`trigger` is chosen by when the rule binds**, never by how important it feels:

  | `trigger` | Use for |
  |---|---|
  | `always_on` | Only what must hold on every turn - git safety, layering, stability. It is paid on every turn. |
  | `model_decision` | The default. A rule binding at a recognisable moment: adding a log, closing a bug fix. |
  | `glob` | A rule anchored to a kind of file; set `globs` alongside it. |
  | `manual` | A procedure invoked by name. |

  **Omitting `trigger` defaults to `always_on`** - an omission silently makes the rule permanent context.
- **When a rule's trigger condition changes, its pointer's `description` is updated in the same turn.** A
  pointer with a stale description is worse than a missing one: it fires on the wrong tasks and stays quiet
  on the right ones.
- **Never add `.agent/` to `.gitignore`.** Rule discovery honours gitignore with no error and no warning, so
  an ignored directory produces a repo whose rules simply never load and an agent that never knows. Ignore
  individual files inside it if needed; never the directory.
- **Workflows are procedures, not rules.** A repeatable sequence the user invokes by name belongs in
  `.agent/workflows/`, mirroring `conductor/workflows/`. Adding, renaming or deleting either updates the
  catalogue in [`../index.md`](../index.md) in the same turn.

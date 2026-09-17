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
Copilot) and `.agents/` (Antigravity, which also accepts `.agent/`; this repository uses `.agents/`, the name
Antigravity lists first and the one Codex and harness-score read), plus `.claude/` (Claude Code's own pointers into
`.agents/`). Two surfaces holding the same rule diverge by default, and nothing detects it.

Antigravity discovers three kinds of file under `.agents/`, and each has exactly one job:

| Path | Holds | Frontmatter |
|---|---|---|
| `.agents/rules/*.md` | Only what must load without the model choosing it - `always_on`, or `glob` for a kind of file | `description`, `trigger`, `globs` |
| `.agents/skills/<name>/SKILL.md` | Every rule that binds at a recognisable moment, and every procedure - loaded when its `description` matches the task | `name`, `description` |
| `.agents/workflows/*.md` | Slash commands the user invokes by name, each following a runbook in `conductor/workflows/` or a template in `conductor/templates/` | `description` |

Claude Code registers skills and slash commands only under `.claude/`, so every skill and workflow above has
exactly one **pointer** there. Invoking it in Claude Code loads the pointer, which sends the agent to the source:

| Path | Points at | Frontmatter |
|---|---|---|
| `.claude/skills/<name>/SKILL.md` | `.agents/skills/<name>/SKILL.md` - `/<name>`, and matched on its `description` like any skill | `name`, `description` - both the source's |
| `.claude/commands/<name>.md` | `.agents/workflows/<name>.md` - `/<name>`, typed by the user only | `description` - the source's; `disable-model-invocation: true` |

- **`conductor/rules/` is the single source of truth.** Every rule or skill under `.agents/` is a **pointer**: a
  checklist of at most ~25 lines plus a link to the section that owns the spec. Never a second copy of the
  rule text. A skill carries a full procedure only where no rules file owns one (`handoff`, `initialize`,
  `planning`).
- **A skill never lives under `conductor/`.** Antigravity discovers skills only at
  `.agents/skills/<name>/SKILL.md`; anywhere else a skill is inert prose that no agent loads.
- **Frontmatter is the whole interface.** Antigravity parses exactly the keys in the table above - anything
  else is ignored silently. `description` is the only thing the model matches on, so it states **what the file
  covers and when it applies**, never just a title.
- **`trigger` is chosen by when the rule binds**, never by how important it feels:

  | `trigger` | Use for |
  |---|---|
  | `always_on` | Only what must hold on every turn - git safety, layering, stability. It is paid on every turn. |
  | `glob` | A rule anchored to a kind of file; set `globs` alongside it, quoted. |
  | `model_decision` | Valid, and not used here: a rule binding at a recognisable moment - adding a log, closing a bug fix - is a skill. |
  | `manual` | Valid, and not used here: a procedure invoked by name is a workflow. |

  **Omitting `trigger` defaults to `always_on`** - an omission silently makes the rule permanent context.
- **When a rule's or skill's trigger condition changes, its `description` is updated in the same turn.** A
  stale description is worse than a missing one: it fires on the wrong tasks and stays quiet on the right ones.
- **Only the surfaces this repository is worked with.** Antigravity reads `.agents/`, and Claude Code reads
  `CLAUDE.md` and `.claude/`. No folder, file or frontmatter key for any other agent runtime is added. Claude
  Code does not discover `.agents/`: it reaches skills and workflows through their pointers and loads no
  `.agents/rules/`, so `AGENTS.md` still names every skill trigger and file-anchored rule, and `CLAUDE.md`
  imports `AGENTS.md`.
- **A pointer is a link, never a copy.** Its body only sends the agent to its source, and its `description` is
  the source's, so both match the same tasks. Adding, renaming or deleting a skill or workflow, or changing its
  `description`, does the same to its pointer in the same turn. No symlink either: Git on Windows checks one out
  as a plain text file. The slash command is the folder or file name, never `name`. `check_agent_docs.py`
  reports a missing pointer, an orphan, a wrong link or a differing `description` from either side, and
  `test_hooks.py` fails when the two sets differ.
- **Antigravity does not read `.claude/`** (checked 2026-09-17 in the installed IDE): its agent reads only
  `.agents/`, and the VS Code Chat panel it ships reads `.claude/skills/` only with the experimental
  `chat.useClaudeSkills` setting, off by default. Check again after an IDE update before relying on it.
- **Hooks enforce what the rules only say.** Claude Code runs committed scripts from `.claude/settings.json`:
  `tools/hooks/guard_git.py` (PreToolUse) asks before a git write or a recursive forced delete (CORE_RULES §2), and before a Gradle build (CORE_RULES §1);
  `tools/hooks/check_agent_docs.py` (PostToolUse) reports frontmatter and link problems in an edited AI document, and a pointer out of step with its source.
  A hook never denies and never blocks silently - it asks, or it reports - and `tools/hooks/test_hooks.py` pins it.
  Antigravity's counterpart is `.agents/hooks.json`, shipped with `"enabled": false` until its tool names
  are confirmed in the IDE (`run_command` / `write_to_file` are guesses). A matcher naming the wrong tool
  fails open, so it stays off.
- **A subagent only when a fresh context is the point.** Claude Code reads subagents from `.claude/agents/<name>.md`
  (`name`, `description`, and `tools` narrowed to what the job needs); Antigravity has no subagent format, so a
  subagent is never the only home of a procedure or a rule. Its body points at `AGENTS.md`, the skills and the
  rules sections and never copies them - a copy drifts the first time a rule changes. One is added only for work
  that is better done without the author's context, such as reviewing a diff someone else wrote; never to re-run
  a checklist the main agent already loads as a skill, and never for a score. Each one is listed in the Agent
  Surface table of [`../index.md`](../index.md).
- **Never add `.agents/` to `.gitignore`.** Discovery honours gitignore with no error and no warning, so an
  ignored directory produces a repository whose rules and skills simply never load, and an agent that never
  knows. Ignore individual files inside it if needed; never the directory.
- **Workflows are procedures, not rules.** A repeatable sequence the user invokes by name belongs in
  `.agents/workflows/`, mirroring `conductor/workflows/`. Adding, renaming or deleting a rule, skill or
  workflow updates the catalogue in [`../index.md`](../index.md) in the same turn.

### 18.1 Frontmatter fails silently, so verify it (MANDATORY)

`description` is a YAML **plain scalar**, so **a colon followed by a space anywhere inside it ends the
scalar and breaks the parse** — `e.g.: `, `note: `, `Applies: when…`. The file then does not load,
with no error and no warning: it simply never appears. Use an em dash instead, or wrap the whole
description in double quotes.

The same silence covers every other authoring mistake here — an unknown key, a mistyped `trigger`, a
malformed `globs`. So **verify, never assume**: after adding or editing anything under `.agents/` or
`.claude/`, confirm it actually registered instead of trusting that it did. A rule nobody can see is
indistinguishable from a rule nobody wrote, and it fails in the direction that looks like success.

In Claude Code, typing `/` offers every registered skill and command. A `.claude/skills/` or `.claude/commands/`
folder that did not exist when the session started registers only after a restart. A listed skill is still not
proof: Claude Code registers one whose frontmatter failed to parse, with no `description` to match on, so run
`check_agent_docs.py` on it as well.

**Relative links resolve from the file's own directory** — `.agents/rules/<name>.md` is two levels
below the repo root and `.agents/skills/<name>/SKILL.md` three, so a link into the docs is
`../../conductor/<…>` from a rule and `../../../conductor/<…>` from a skill. A pointer under
`.claude/skills/` sits at a skill's depth and one under `.claude/commands/` at a rule's. Verify the depth; a wrong
one still renders as a link and fails only when someone follows it.

### 18.2 File names are identifiers (MANDATORY)

A file name under `conductor/` or `.agents/` is resolved by something - a link, a slash command, the skill
registry - so renaming one is never cosmetic, and there is one convention per location:

| Location | Convention | Example |
|---|---|---|
| `conductor/rules/` | `SCREAMING_SNAKE.md` | `CORE_RULES.md`, `AI_BEHAVIOR.md`, `ARCHITECTURE.md` |
| `conductor/workflows/` and `.agents/workflows/` | lowercase-hyphen, identical in both, and equal to the slash command | `remove-filter.md` is `/remove-filter` |
| `.agents/skills/<name>/` | lowercase-hyphen folder equal to the frontmatter `name`; the file is always `SKILL.md` | `logging/SKILL.md` |
| `.agents/rules/` | lowercase-hyphen | `agent-surface-parity.md` |
| `.claude/skills/<name>/` and `.claude/commands/` | identical to the skill folder or workflow file it points at, and equal to the slash command | `.claude/skills/logging/SKILL.md` is `/logging` |
| `conductor/templates/` | `<NAME>_TEMPLATE.md` | `COMMIT_TEMPLATE.md` |
| `conductor/guides/` | lowercase-hyphen | `bootstrap-ai.md` |
| `conductor/knowledge/` | `KI-<nn>-NAME-IN-CAPS.md` | `KI-04-LOG-FILTERS.md` |
| A folder's index, and the root map | `INDEX.md`, and `conductor/index.md` | - |
| Repository root | `AGENTS.md`, `CLAUDE.md`, `README.md`, `CHANGELOG.md` | - |

- **Rename and fix every inbound reference in the same turn**, then run a link checker. Zero broken relative
  links is the definition of done.
- **A case-only rename takes two steps on a case-insensitive filesystem** (`x.md` to `x.tmp` to `X.md`); a
  direct one is a silent no-op.
- **Anchor the search** when the old name is a suffix of another name - `(?<![\w-])architecture\.md` - or the
  replacement breaks the neighbour.

# CORE RULES — Comiqueta

**Self-contained operational + project rules.** Load alongside `ai_behavior.md` (behavioral) for any non-trivial change. Architecture (module graph, layers, MVI, DI, persistence, build) lives in [`architecture.md`](architecture.md) — not restated here.

---

---

## Index

Jump by number. The rule set spans several files under `conductor/rules/`; this table is the
single lookup for all of them. Load only the file the row points at.

| § | Rule | Lives in |
|---|---|---|
| **0** | [Stability & Non-Regression](#0-stability--non-regression-absolute) **(ABSOLUTE)** | — |
| **1** | [Standards](#1-standards) | — |
| **2** | [Git Safety](#2-git-safety-absolute) **(ABSOLUTE)** | — |
| **3** | [Token Economy](#3-token-economy) | — |
| **4** | [Large File Protocol (>500 lines)](#4-large-file-protocol-500-lines) | — |
| **5** | [Code Style & Readability](#5-code-style--readability) | — |
|     **5.1** |     [Readability & Simplicity](#51-readability--simplicity-mandatory) *(M)* | — |
|     **5.2** |     [No Inline Fully-Qualified Names](#52-no-inline-fully-qualified-names-mandatory) *(M)* | — |
|     **5.3** |     [Enum When Every Case Is Stateless](#53-enum-when-every-case-is-stateless-mandatory) *(M)* | — |
| **6** | [KI Sync Rule](#6-ki-sync-rule-global--mandatory) *(M)* | — |
|     **6.1** |     [Write KIs as if every change was always the original intent](#61-write-kis-as-if-every-change-was-always-the-original-intent) | — |
|     **6.2** |     [KIs must be self-sufficient](#62-kis-must-be-self-sufficient) | — |
|     **6.3** |     [Optimise for targeted reads](#63-optimise-for-targeted-reads) | — |
|     **6.4** |     [Every KI declares the commit it was written against](#64-every-ki-declares-the-commit-it-was-written-against-mandatory) *(M)* | — |
| **7** | [Planning Protocol](PLANNING_RULES.md#7-planning-protocol) | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.1** |     [META_PLANNING Protocol](PLANNING_RULES.md#71-meta_planning-protocol-global-rule) | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.1a** |     [Multi-Edition Plan Sync](PLANNING_RULES.md#71a-multi-edition-plan-sync-global-rule) | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.2** |     [Plan Lifecycle](PLANNING_RULES.md#72-plan-lifecycle) | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.3** |     [Deferred items and plans link BOTH ways](PLANNING_RULES.md#73-deferred-items-and-plans-link-both-ways-mandatory) *(M)* | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.4** |     [The deferred-item backlog holds ONLY work still to be done](PLANNING_RULES.md#74-the-deferred-item-backlog-holds-only-work-still-to-be-done-mandatory) *(M)* | [PLANNING_RULES](PLANNING_RULES.md) |
|     **7.5** |     [Agent-generated planning artefacts are NOT plannings](PLANNING_RULES.md#75-agent-generated-planning-artefacts-are-not-plannings-mandatory) *(M)* | [PLANNING_RULES](PLANNING_RULES.md) |
| **8** | [Logging — TimberLogger Only](LOGGING_RULES.md#8-logging--timberlogger-only) | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.1** |     [Adding a log](LOGGING_RULES.md#81-adding-a-log-mandatory--read-before-writing-any-timberloggerlog-call) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.2** |     [Coverage — everything must be diagnosable from logs alone](LOGGING_RULES.md#82-coverage--everything-must-be-diagnosable-from-logs-alone-mandatory) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.3** |     [Redaction by build variant](LOGGING_RULES.md#83-redaction-by-build-variant-mandatory) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.4** |     [Protected filters](LOGGING_RULES.md#84-protected-filters) | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.5** |     [Renaming or removing a filter](LOGGING_RULES.md#85-renaming-or-removing-a-filter) | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.6** |     [Choosing the level - what survives into `release`](LOGGING_RULES.md#86-choosing-the-level---what-survives-into-release-mandatory) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.7** |     [Long messages are split, not truncated](LOGGING_RULES.md#87-long-messages-are-split-not-truncated-mandatory) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
|     **8.8** |     [Redaction helpers](LOGGING_RULES.md#88-redaction-helpers-mandatory) *(M)* | [LOGGING_RULES](LOGGING_RULES.md) |
| **9** | [Composable Extraction](UI_RULES.md#9-composable-extraction-global--all-compose-screens) | [UI_RULES](UI_RULES.md) |
| **10** | [String Resource Ownership](UI_RULES.md#10-string-resource-ownership-global--mandatory) *(M)* | [UI_RULES](UI_RULES.md) |
| **11** | [Extension Functions](#11-extension-functions) | — |
| **12** | [Regression Test Rule](#12-regression-test-rule-global--mandatory) *(M)* | — |
| **13** | [Database Migration Safety](#13-database-migration-safety-global--mandatory) *(M)* | — |
| **14** | [Changelog Rule](#14-changelog-rule-global--mandatory) *(M)* | — |
| **15** | [Cross-Project Rule Sync](#15-cross-project-rule-sync-global--mandatory) *(M)* | — |
| **16** | [Reference Document Ownership](DOC_GOVERNANCE.md#16-reference-document-ownership-global---mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
|     **16.1** |     [A new version never overwrites the old one](DOC_GOVERNANCE.md#161-a-new-version-never-overwrites-the-old-one-mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
|     **16.2** |     [No machine-absolute paths in a tracked document](DOC_GOVERNANCE.md#162-no-machine-absolute-paths-in-a-tracked-document-mandatory) **(ABSOLUTE)** | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
| **17** | [Rule Placement](DOC_GOVERNANCE.md#17-rule-placement-global---mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
|     **17.1** |     [Heading level, and the index row](DOC_GOVERNANCE.md#171-heading-level-and-the-index-row-mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
|     **17.2** |     [A rules file has a token budget](DOC_GOVERNANCE.md#172-a-rules-file-has-a-token-budget-mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
| **18** | [Agent Surface Parity](DOC_GOVERNANCE.md#18-agent-surface-parity-global---mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
|     **18.1** |     [Frontmatter fails silently, so verify it](DOC_GOVERNANCE.md#181-frontmatter-fails-silently-so-verify-it-mandatory) *(M)* | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) |
| **19** | [Single Activation Per Control](UI_RULES.md#19-single-activation-per-control-global---mandatory) *(M)* | [UI_RULES](UI_RULES.md) |
| **20** | [Text Encoding and Shell Output](AGENT_IO_RULES.md#20-text-encoding-and-shell-output-global---mandatory) *(M)* | [AGENT_IO_RULES](AGENT_IO_RULES.md) |
| **21** | [Chunked Writing of Long Artefacts](AGENT_IO_RULES.md#21-chunked-writing-of-long-artefacts-global---mandatory) *(M)* | [AGENT_IO_RULES](AGENT_IO_RULES.md) |
| **22** | [Token Economy Discipline](AGENT_IO_RULES.md#22-token-economy-discipline-global---mandatory) *(M)* | [AGENT_IO_RULES](AGENT_IO_RULES.md) |
| **23** | [Credentials Never Enter the Repository](SECURITY_RULES.md#23-credentials-never-enter-the-repository-global---mandatory) *(M)* | [SECURITY_RULES](SECURITY_RULES.md) |
| **24** | [Model Assignment for Deferred Tasks](PLANNING_RULES.md#24-model-assignment-for-deferred-tasks-global---mandatory) *(M)* | [PLANNING_RULES](PLANNING_RULES.md) |

*(M) = MANDATORY. "—" in the last column means this file.*

---

## 0. Stability & Non-Regression (ABSOLUTE)

Preserving existing functionality is the top priority of any change. Before writing logic, do an impact analysis (race conditions, state leaks, UI regressions). **This mandate overrides any conflicting rule.**

## 1. Standards

| Aspect | Rule |
|---|---|
| Persona | Code-first. No prose unless asked. |
| Language | English in code/comments; UI strings via resources (locales EN, PT, ES, DE). |
| Auth | No BUILD or COMMIT without explicit user confirmation in the current turn. |

## 2. Git Safety (ABSOLUTE)

Never run `git add`/`commit`/`mv`/`rm` or any staging/commit command unless the user explicitly requests it **in the current turn**. Past approvals never carry over. A failing build/test is never a reason to commit a WIP. For moves/renames, use plain `mv`/`cp`, never the `git` equivalents.

## 3. Token Economy

Minimize tokens every task. Read only what you need (targeted `grep`/`glob`, line-ranges over whole files). Skip files already in context. Run independent tool calls in parallel. Prefer `Edit` over `Write` for existing files. Keep responses terse — no preamble/recap/pleasantries. Use `file:line` refs instead of quoting blocks.

The full spec — when a re-read is correct, batched documentation sync, what the compiler owns, and the habits that keep the session's cached prefix intact — is [AGENT_IO_RULES.md](AGENT_IO_RULES.md) §22. The paragraph above is the operative summary; if it is enough to act on, do not open that section.

## 4. Large File Protocol (>500 lines)

1. **Map** — outline/grep for the relevant ranges.
2. **Focus** — read only the target range.
3. **Note** — record findings in context (or a KI); don't re-read.

## 5. Code Style & Readability

### 5.1 Readability & Simplicity (MANDATORY)

Avoid clever tricks, overly terse one-liners, or complex language constructs that save a few lines at the cost of obviousness. **Prefer code readability instead.** Code is read far more often than it is written. Clear, straightforward code reduces bugs and helps the next developer (or AI) understand the intent instantly.

### 5.2 No Inline Fully-Qualified Names (MANDATORY)

Never reference a project type by its FQN inside an expression, generic, or annotation argument (e.g. a `hiltViewModel<…>()` or `R.drawable.…` written with a full package path). Add a top-of-file `import` and use the simple name. **Exception:** KDoc cross-references (`[fully.qualified.Symbol]`) require the FQN — that is correct.

**Rationale:** inline FQNs bloat call sites, hide real dependencies from the import block, and break IDE refactor/rename.

### 5.3 Enum When Every Case Is Stateless (MANDATORY)

If every subtype of a `sealed class` / `sealed interface` is a bare `object` / `data object`, it is an `enum class`. A sealed hierarchy earns its cost only when at least one case carries data that distinguishes two instances of that same case — `StatisticsUIState.Success(stats)` and `.Error(message)`, or `DragInteraction`'s `StartEndDragInteraction` / `GestureDragInteraction`. `DragInteraction.PointerBehavior` is the other side of the same coin: two stateless cases, so it is correctly an `enum class` nested inside that sealed interface. A hierarchy of only `data object`s belongs on that side too.

What the enum buys that an all-objects sealed hierarchy does not:

- `entries` — iterate the cases for an exhaustive UI mapping, a settings picker, or a format filter, without maintaining a hand-written `listOf(...)` that silently goes stale when a case is added.
- `valueOf` / `name` — free round-trip for Room columns and DataStore keys in both directions; a sealed hierarchy needs a hand-written `TypeConverter` for the same thing.
- `when` exhaustiveness with none of the per-case declaration noise.

**Promote to sealed the moment one case needs a payload.** That is the signal, and it is a mechanical change. Do not model as sealed pre-emptively "in case a case grows a field later" (`ai_behavior.md` §2).

**Carve-out — the presentation state machine stays sealed.** Per-screen `XxxIntent` / `XxxEffect` / `XxxUIState` (`ViewerIntent`, `HomeEffect`, `SettingsIntent`, …) stay sealed even while every case is a `data object`: [`architecture.md` § MVI Contract](architecture.md) mandates that shape per screen, those hierarchies reliably grow payload-carrying cases as a screen gains fields, and the ViewModel and route composable do `is`-checks against them. Navigation keys (`Screen`, `NavigationIntent`, `NavigationEffect`) are in the same bucket — they are `@Serializable` `NavKey`s, not a value set. This subsection scopes to **domain and data outcome types** in `core/domain` / `core/data` — not to the MVI contract or navigation.

## 6. KI Sync Rule (GLOBAL — MANDATORY)

After **any** code change, planning update, or architectural decision that modifies the behaviour,
structure, or contracts of a feature:

1. **Identify the affected KI(s)** from `conductor/knowledge/INDEX.md`.
2. **Update the KI immediately** — before the turn ends. Do not defer.
3. **What to sync:** file tables, business rules, layer boundaries, public contracts, and test targets affected by the change.
4. **Scope:** only update what changed. Do not rewrite unrelated sections.
5. **If the change affects `architecture.md` or `CORE_RULES.md`** (global rules), those files ARE the source of truth — reflect their content in the relevant KI's Business Rules section.

### 6.1 Write KIs as if every change was always the original intent

A KI is a **present-tense canonical spec**, not a changelog. Whenever you update a KI:

- **Rewrite affected sections in the present tense.** Do NOT prepend "Phase X added…", "Task Y changed…", or similar historical scaffolding. Just state what the code IS, as if it had always been that way.
- **Do NOT keep "Previously verified" / "Earlier verified" parenthetical chains** describing what the file used to say. Each KI has a single `**Last verified:** YYYY-MM-DD` line and no further dated history.
- **Diff context belongs in the commit message**, not in the KI. The reader of the KI is implementing fresh; they do not need to know the spec used to be different.

### 6.2 KIs must be self-sufficient

A KI must be **readable on its own** by an AI making code changes to the relevant module. That means:

- All contracts, file tables, business rules, test targets, and worked examples needed to safely change the module live inside the KI.
- **No references to `conductor/plannings/*` files** — plannings are temporary artefacts that get deleted; a KI that links to one breaks the moment the plan is removed. If a planning contained information the KI needs, **inline it into the KI**.
- Cross-KI references are fine when they prevent duplication, but the KI must still be useful on its own for the change at hand.

### 6.3 Optimise for targeted reads

Keep KIs **small and focused** so an AI only loads what it needs:

- If a KI grows past roughly 400 lines or covers multiple sub-packages, **split it** along sub-package boundaries (convention: `KI-Xa`, `KI-Xb`, … with a small overview parent `KI-X.md`).
- Cross-cutting concerns that span sub-packages go in the **overview parent**, not duplicated in each sub-KI.
- Trim aggressively: drop historical change-narratives, duplicated patterns already documented in `rules/`, and planning markers. **Never** trim the substantive specs needed to make code changes — file purpose, contracts, business rules, test cases.

> **Rationale:** KIs are the reference an AI reads when implementing. A planning can diverge from a KI silently and cause regressions. Plannings are temporary; KIs persist. The KI must always reflect the current implementation contract, written in present tense, self-contained, and small enough to load targeted reads.

### 6.4 Every KI declares the commit it was written against (MANDATORY)

Directly under the `**Last verified:**` line, every KI carries **exactly one** stamp line:

```markdown
**Reflects code:** `4d6be06f` (2026-09-08)
```

Nothing else — no explanatory paragraph, no `Earlier:` / `Previous:` chains, no parenthesised
history. The document's changelog is `git log --follow <file>`; repeating it at the top of the KI is
a token cost paid on **every** read, for information git already holds more precisely.

**Why the commit and not just the date.** A date says *when someone touched the document*; the commit
says *which code it was checked against*. Those are different things, and it is the second that
answers the only question that matters when a KI and the code disagree: **"is this wrong, or just
old?"** With the commit, anyone runs `git log <commit>..HEAD -- <module path>` and knows exactly what
landed since — the suspicion stops being a hunch and becomes a list of commits.

- **Refresh the stamp in the same turn you edit the KI**, together with §6 above. A stale stamp is
  worse than none, because it asserts a guarantee that no longer holds.
- **Only re-stamp what you actually re-read against the code.** The stamp asserts that the content
  matches that commit. Running a script across 40 KIs and re-stamping all of them is a false
  statement — precisely the false confidence the stamp exists to prevent. If you touched one section,
  the stamp stays at the state you verified.
- **A file outside git** (`KI-TBD.md`) has no commit of its own: the stamp records which commit the
  repository was on when the stamp was written.
- **Never invent a commit.** `git log -1 -- <file>` is the answer.
- **No other dated history anywhere in the document** — not at the top, not mid-file. A header note
  like `**TICKET-123 (2026-09-02):** …` is a changelog in disguise. If the fact matters, rewrite it in
  the present tense inside the section it belongs to; if it does not, git keeps it.

> **Existing KIs predate this rule** and carry `**Last verified:**` only. They are stamped as each one
> is next touched — never in a bulk pass, per the second bullet above.

## 11. Extension Functions

Use them when they make the call site read better and keep behaviour next to its type: reusable transforms/mappers (`Entity.toDomain()`), domain↔UI adapters, small helpers on stdlib/platform types. Place in a file named after the receiver (`StringExt.kt`, `DpExtensions.kt`), in the module where it's used. Keep pure and single-purpose; an extension needing injected deps belongs in a class. Don't wrap a single call site or hide where work happens.

## 12. Regression Test Rule (GLOBAL — MANDATORY)

**Every bug fix ships one or more tests in the same turn that prove the specific failure no longer happens.**

- The test must fail against the pre-fix code and pass against the fix — it pins the exact defect, not just general area coverage. A happy-path test already covered elsewhere does not satisfy this rule.
- Applies at any layer: unit, instrumented, or — where the bug is only reproducible on hardware — a documented manual on-device verification step.
- New tests live alongside the existing suite for the fixed class; don't create a separate "regression" file unless that class has no suite yet.
- A bug fix without a pinning test is an incomplete change, same severity as a stale KI (§6).

> **Rationale:** a fix without a pinning test can silently regress on the next refactor — nothing in the suite would catch it reverting.

## 13. Database Migration Safety (GLOBAL — MANDATORY)

The Room database holds the user's entire scanned library and reading progress. Losing it is a silent, unrecoverable data loss for the user — schema evolution is governed by hard rules, not convention.

1. **NEVER `fallbackToDestructiveMigration()` (or `…OnDowngrade`).** It wipes `ComicDatabase` on any schema-hash mismatch. This is the first "fix" someone reaches for when a forgotten migration crashes on open — reject it in review.
2. **Every schema change ships three things in the SAME turn:** (a) bump the DB `version`, (b) a real `Migration(n, n+1)` registered on the builder, (c) the exported schema JSON for `n+1` in `schemas/` **plus** a passing migration test that migrates `n → n+1` and asserts rows survive. A schema edit missing any of the three is an incomplete change, same severity as a stale KI (§6).
3. **Keep the migration harness.** `room-testing` + `MigrationTestHelper` against the exported schema set is what makes a forgotten version bump fail in CI instead of on a user's device.
4. **Before shipping,** confirm no previously released version had a different shape with no migration path. Dev-time schema churn is reset by clearing app data — never by a destructive fallback.

> **Why a crash-on-open is worse than it looks:** if the DB throws on open, nothing can read the library at all — the app is bricked for that user until a fixed build ships.

## 14. Changelog Rule (GLOBAL — MANDATORY)

**Update `CHANGELOG.md` → `## Unreleased` with each planning or fix**, one line per work unit (not per commit):

```
- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)
```

Omit the `(CODE)` suffix when no ticket exists. On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased` above it.

## 15. Cross-Project Rule Sync (GLOBAL — MANDATORY)

Comiqueta, Slotify, and BipSale share one AI-workflow rule set and one developer. Whenever a **shared** rule is added, updated, or deleted in any of the three, apply the equivalent change to the other two **in the same turn**.

**What counts as shared:** §0 Stability · §2 Git Safety · §3 Token Economy · §4 Large File Protocol · §5 Code Style & Readability (5.1 Readability, 5.2 No Inline FQN, 5.3 Enum-vs-sealed) · §6 KI Sync (all sub-sections) · §7 Planning + META_PLANNING + Lifecycle · §8.1–8.5 log filter format, coverage, redaction-by-variant, protection, rename · byte-budget log chunking and call-site redaction helpers (the *mechanism* is shared; the helper set is per-project, because what counts as sensitive differs) · §9 Composable Extraction · §10 String Ownership (the ownership model, not the key namespaces) · §12 Regression Test · §13 DB Migration Safety (Comiqueta ↔ BipSale only — Slotify has no Room) · §14 Changelog · this section · everything in `ai_behavior.md` · everything in [`GRADLE_RULES.md`](GRADLE_RULES.md).

**What is NOT shared** — adapt or omit, never copy verbatim: module graphs, DI framework (Hilt vs Koin), logging API (`TimberLogger` vs `Timber` vs `Logger`/Kermit), persistence (Room/SAF vs Room/Retrofit vs Supabase), build config, locale sets, and everything in `architecture.md`.

**Counterpart mapping:** the trees are structurally identical - the same relative path in each repo is the
counterpart (`conductor/rules/CORE_RULES.md` ↔ `conductor/rules/CORE_RULES.md`, and so on).

**Each project stays self-contained (MANDATORY).** Sync means *the same rule is written into each repo in
full*, in that repo's own words, with that repo's own stack and numbering - never a pointer to another repo.
Three consequences, all binding:

- **Section numbers are not expected to match across projects**, and are never renumbered to make them match
  (§17.1). A rule that landed later here simply carries a higher number; cite by number *within* this file
  only.
- **No file in this repository may cite a path in another one** (§16.2). If another project holds material
  this one needs, the relevant part is copied into `conductor/references/` and the copy is cited.
- **A clone of this repository alone is complete.** Any task in it can be done without opening another
  checkout. That is the test to apply before writing any cross-project reference: if the answer needs the
  other repo on disk, the rule is not synced - it is coupled.

Mirroring is a *process* obligation between three separate, independent projects. It never creates a runtime
or documentation dependency between them.

> **Rationale:** the same clarification should never be needed twice. Divergent workflow rules make an AI behave differently in each repo for no reason.

---

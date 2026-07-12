# CORE RULES — Comiqueta

**Self-contained operational + project rules.** Load alongside `ai_behavior.md` (behavioral) for any non-trivial change.

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

## 4. Large File Protocol (>500 lines)

1. **Map** — outline/grep for the relevant ranges.
2. **Focus** — read only the target range.
3. **Note** — record findings in context (or a KI); don't re-read.

## 5. No Inline Fully-Qualified Names (MANDATORY)

Never reference a project type by its FQN inside an expression, generic, or annotation argument (e.g. a `hiltViewModel<…>()` or `R.drawable.…` written with a full package path). Add a top-of-file `import` and use the simple name. **Exception:** KDoc cross-references (`[fully.qualified.Symbol]`) require the FQN — that is correct.

## 6. KI Sync Rule (GLOBAL — MANDATORY)

After **any** code change, planning update, or architectural decision that modifies the behaviour,
structure, or contracts of a feature:

1. **Identify the affected KI(s)** from `conductor/ki/KI_INDEX.md`.
2. **Update the KI immediately** — before the turn ends. Do not defer.
3. **What to sync:** file tables, business rules, layer boundaries, public contracts, and test targets affected by the change.
4. **Scope:** only update what changed. Do not rewrite unrelated sections.
5. **If the change affects `CORE_RULES.md`** (global rules), those files ARE the source of truth — reflect their content in the relevant KI's Business Rules section.

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

- If a KI grows past roughly 400 lines or covers multiple sub-packages, **split it** along sub-package boundaries.
- Trim aggressively: drop historical change-narratives, duplicated patterns already documented in `rules/`, and planning markers. **Never** trim the substantive specs needed to make code changes — file purpose, contracts, business rules, test cases.

> **Rationale:** KIs are the reference an AI reads when implementing. A planning can diverge from a KI silently and cause regressions. Plannings are temporary; KIs persist. The KI must always reflect the current implementation contract, written in present tense, self-contained, and small enough to load targeted reads.

## 7. Planning Protocol

- **Storage:** all plans in `conductor/plannings/`. Completed/obsolete → `conductor/plannings/archived/` (**never delete**). Keep `conductor/plannings/INDEX.md` current.
- **Naming:** `[CODE]_[desc]_plan.md` if a ticket exists, else `[feature]_[desc]_plan.md`.
- **Create a plan when** work spans 3+ files, crosses layers (UI+VM+data), fixes a blocking bug, gates behind a flag, or the approach is uncertain. Content: scope, steps, testing checklist, blockers, dependencies.
- **META_PLANNING** (`META_PLANNING_*.md`) is disposable scaffolding: synthesise the multi-model proposals into one canonical plan, write it, then delete the META_PLANNING and update `INDEX.md`. Never execute a META_PLANNING as-is.

### 7.1 META_PLANNING Protocol (GLOBAL RULE)

A `META_PLANNING_*.md` file is a **consolidation prompt**: it collects planning proposals produced by multiple AI models and instructs one AI to synthesise them into a single canonical execution plan.

**Purpose:** META_PLANNINGs are never executed as-is. They exist only to produce a real plan.

**Execution protocol (mandatory):**

1. **Read the META_PLANNING file** in full.
2. **Synthesise** the proposals into a single canonical `[feature_name]_plan.md` inside `conductor/plannings/` following naming rules.
3. **The synthesised plan is the output** — write it with full scope, implementation steps, testing checklist, blockers, and dependencies.
4. **Delete the META_PLANNING file** after the synthesised plan is written and confirmed. META_PLANNINGs are disposable scaffolding; the canonical plan is the artefact that persists.
5. **Update `conductor/plannings/INDEX.md`**: remove the META_PLANNING row, add the new canonical plan row with status `🔵 Backlog`.
6. **Do NOT start implementing** during the META_PLANNING synthesis turn unless the user explicitly asks. Synthesis = planning only.

> **Rationale:** META_PLANNINGs accumulate noise. The synthesis step produces a clean, deduplicated, actionable plan that any AI can execute without re-reading the original proposals.

### 7.2 Plan Lifecycle

- ❌ **NEVER DELETE** completed or obsolete plans from `conductor/plannings/`.
- ✅ **MOVE** completed or expired plans to `conductor/plannings/archived/` for permanent record.
- **Status Transitions:**
  - **Ready → Active**: Update plan with start date, update `INDEX.md` status.
  - **Active → Completed**: Create/update corresponding KI, link bidirectionally, move to archived.
  - **Active → Obsolete**: Archive immediately if plan is superseded or ticket closed without implementation.

---

## 8. Project Rules — Comiqueta

- **Architecture:** `app → feature-* → core`; `feature-ads` is a sibling. `core/domain` is pure Kotlin with **zero Android deps**; always expose `IXxxUseCase`. Features depend on `core` only.
- **DI — Hilt.** Modules live in each module's `/di`.
- **Persistence:** Room (`ComicDatabase`, `ComicsDao`, `CategoryDao`) + SAF (`DocumentFile`); repos behind `IComicsRepository`. Supported formats: CBZ, CBR, CB7, CBT, PDF.
- **I/O:** `DocumentFile` + `runCatching` + `Dispatchers.IO`. **Never** `java.io.File` for external storage.
- **MVI (per feature):** `XxxUIState` (data), `XxxIntent` (sealed), `XxxEffect` (sealed/Channel), `IXxxViewModel` (interface).
- **Navigation:** Nav 3, type-safe `@Serializable` keys in `core/navigation/Screen.kt`.
- **Logging:** `TimberLogger.logX(CLASS, "[TAG] msg")`. No `android.util.Log`.
- **Strings:** `ComiquetaTheme` + `res/strings.xml`; locales EN, PT, ES, DE.
- **Build:** Java 21, KSP, convention plugins in `build-logic/`. Static analysis: detekt (`./gradlew detekt`).

### Log Filter Management

When introducing a new log filter to the codebase, you MUST use the format `[FILTER_PAI][FILTRO_FILHO]`, adapting it to `[Comiqueta][FILTER_NAME]`.

**Unprotected Filters (Ticket/Feature-specific):**
1. **Log Format:** Use `TimberLogger.logD(CLASS, "[Comiqueta][FILTER_NAME] message")`.
2. **Update SOT:** Add the filter to the "Safe for Removal" list in the relevant knowledge index (e.g., `filtros_para_remocao.txt`).

**Protected Filters (Global/Load-bearing):**
1. **Update SOT:** Add the filter to the "DO NOT TOUCH / Protected" list in the relevant knowledge index.
2. **Update Rules:** Update this `CORE_RULES.md` if it lists protected filters.
3. **Synchronize:** Update all call sites and test assertions in the same turn.

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

## 6. KI Discipline

- **Index-first.** Read `ki/KI_INDEX.md`; fetch an individual KI only when the task matches its row. Never pre-load all KIs.
- **Sync in the same turn.** After any code/architecture change that alters a feature's behaviour, structure, or contracts, update the affected KI(s) **before the turn ends** (scope: only what changed).
- **Writing standards.** Present tense (canonical spec, not changelog); a single `**Last verified:** YYYY-MM-DD` line, no dated history; self-sufficient (no links to `plannings/*`); split if a KI passes ~400 lines. Full spec: `ki/KI_AUTHORING.md`.

## 7. Planning Protocol

- **Storage:** all plans in `conductor/plannings/`. Completed/obsolete → `conductor/plannings/archived/` (**never delete**). Keep `conductor/plannings/INDEX.md` current.
- **Naming:** `[CODE]_[desc]_plan.md` if a ticket exists, else `[feature]_[desc]_plan.md`.
- **Create a plan when** work spans 3+ files, crosses layers (UI+VM+data), fixes a blocking bug, gates behind a flag, or the approach is uncertain. Content: scope, steps, testing checklist, blockers, dependencies.
- **META_PLANNING** (`META_PLANNING_*.md`) is disposable scaffolding: synthesise the multi-model proposals into one canonical plan, write it, then delete the META_PLANNING and update `INDEX.md`. Never execute a META_PLANNING as-is.

---

## 8. Project Rules — Comiqueta

- **Architecture:** `app → feature-* → core`; `feature-ads` is a sibling. `core/domain` is pure Kotlin with **zero Android deps**; always expose `IXxxUseCase`. Features depend on `core` only.
- **DI — Hilt.** Modules live in each module's `/di`.
- **Persistence:** Room (`ComicDatabase`, `ComicsDao`, `CategoryDao`) + SAF (`DocumentFile`); repos behind `IComicsRepository`. Supported formats: CBZ, CBR, CB7, CBT, PDF.
- **I/O:** `DocumentFile` + `runCatching` + `Dispatchers.IO`. **Never** `java.io.File` for external storage.
- **MVI (per feature):** `XxxUIState` (data), `XxxIntent` (sealed), `XxxEffect` (sealed/Channel), `IXxxViewModel` (interface).
- **Navigation:** Nav 3, type-safe `@Serializable` keys in `core/navigation/Screen.kt`.
- **Logging:** `TimberLogger.logX(CLASS, "[TAG] msg")`. No `android.util.Log`.

### Log Filter Management

When introducing a new log filter to the codebase, you MUST use the format `[FILTER_PAI][FILTRO_FILHO]`, adapting it to `[Comiqueta][FILTER_NAME]`.

**Unprotected Filters (Ticket/Feature-specific):**
1. **Log Format:** Use `TimberLogger.logD(CLASS, "[Comiqueta][FILTER_NAME] message")`.
2. **Update SOT:** Add the filter to the "Safe for Removal" list in the relevant knowledge index (e.g., `filtros_para_remocao.txt`).

**Protected Filters (Global/Load-bearing):**
1. **Update SOT:** Add the filter to the "DO NOT TOUCH / Protected" list in the relevant knowledge index.
2. **Update Rules:** Update this `CORE_RULES.md` if it lists protected filters.
3. **Synchronize:** Update all call sites and test assertions in the same turn.
- **Strings:** `ComiquetaTheme` + `res/strings.xml`; locales EN, PT, ES, DE.
- **Build:** Java 21, KSP, convention plugins in `build-logic/`. Static analysis: detekt (`./gradlew detekt`).

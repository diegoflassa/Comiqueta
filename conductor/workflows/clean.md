# /clean — Pre-Commit Hygiene Sweep

> Invoked by `/clean`. Run **before** asking the user to commit. Does not modify behaviour — only removes noise that shouldn't ship.

---

## Scope (what `/clean` touches)

1. **Debug logs** — remove `Timber.d(...)` / `Timber.v(...)` calls. Never remove `Timber.i/.w/.e` — those are intentional.
2. **Commented-out code** — delete dead blocks committed by accident. Preserve `// TODO` / `// FIXME` markers.
3. **Unused imports / orphaned vars** — only those orphaned by *this branch's* changes. Pre-existing dead code is left alone.
4. **Stray `println` / `System.out.print`** — always remove.
5. **`@Preview`s** — ensure every public composable in the diff still has at least one `@Preview`. If a widget gained complexity, add a state variant.
6. **Hardcoded UI literals** — surface any user-facing string not behind `R.string.*`. Don't auto-fix unless trivial — flag and ask.

## What `/clean` does NOT touch

- Logic. Ever. If a `/clean` pass would change runtime behaviour, stop and ask.
- Pre-existing dead code outside the current diff.
- Imports/code in files not modified by the current branch.
- Formatting/whitespace in adjacent unchanged code.

## Procedure

1. **Diff narrows the scope.** `git diff --name-only main...HEAD` ⇒ act only on those files.
2. **Per file**: apply the checklist above with `Edit` (surgical), never `Write` (full rewrite).
3. **Report**:
   - Lines removed (`Timber.d` / commented code / println / unused imports).
   - Files touched.
   - Any items surfaced but not auto-fixed (hardcoded strings, missing `@Preview`s, suspect logic).
4. **Stop before `git add` / `git commit`.** Only the user authorizes staging.

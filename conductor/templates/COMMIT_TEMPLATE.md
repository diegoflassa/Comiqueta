# Commit Template

Conventional commit. English. Staged changes only.
Create the message based on the current changes in the staged files.
Reread all stages files when performing this task

```
<type>(<scope>): <subject>

<body: why, not what — 1-3 lines>
```

## Fields

| Type | Use when |
|------|----------|
| `feat` | New user-visible feature |
| `fix` | Bug fix |
| `refactor` | Code restructure, no behavior change |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `docs` | Documentation only |
| `chore` | Tooling, deps, config |
| `build` | Gradle / build-logic changes |
| `ci` | CI/CD pipeline changes |

**scope:** Gradle module name, lowercase-hyphenated (`core-data`, `feature-auth`). Omit if multi-module.
**subject:** imperative, lowercase, ≤72 chars, no period. _Good:_ `add date filter` — _Bad:_ `Added filter`, `WIP`
**body:** explain **why**, not what — the diff shows what.
**footer** (optional): `BREAKING CHANGE:` or `Closes #NNN`.

## Examples

```
feat(feature-auth): add email validation to login screen

Users were submitting invalid emails causing backend errors.
```

```
fix(core-data): prevent duplicate inserts on retry

Added REPLACE conflict strategy + dedup before upsert.
```

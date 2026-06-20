---
name: remove-filter
description: Surgical removal of debug log calls carrying a specific [TAG] token. Preserves intentional signal (logI/logW/logE).
---

# /remove_filter <FILTER> — Scoped Debug-Log Removal

> Removes debug-level `TimberLogger.logD` / `logV` calls whose message carries a given bracket token (e.g. `/remove_filter Viewer` targets `[Viewer]`).

## Procedure

1. **Locate** all `TimberLogger.logD(CLASS, "[<FILTER>] …")` and `logV(...)` call sites for the token (grep `\[<FILTER>\]`).
2. **Remove** only those debug/verbose calls and any local variable that existed solely to build the removed message.
3. **Never touch** `logI` / `logW` / `logE` — intentional production signal.
4. **Update tests** that assert on the removed log lines, in the same turn.

## Safety

- Honor `CORE_RULES.md §2` (Git Safety): no `git add`/`commit`.
- Surgical only (`ai_behavior.md §3`): do not reformat or remove unrelated logs.
- Report: files touched, calls removed, tests updated, token retired? (yes/no).

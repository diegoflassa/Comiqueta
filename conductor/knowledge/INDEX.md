# KI Index — Comiqueta

One-line summaries. Fetch an individual KI file **only** if the current task matches.

| ID | File | Topic | When to read | Key files |
|----|------|-------|--------------|-----------|
| KI-01 | KI-01-VIEWER-PINCH-ZOOM-FIX.md | Viewer zoom (2-finger) + pan (1-finger), border-clamped | Touching viewer gesture handling / zoom / pan | `ViewerScreen.kt`, `globalIsPinchZoomActive`, `isPinchGestureInProgress`, `maxTranslateX/Y` |
| KI-02 | KI-02-VIEWER-PINCH-ZOOM-NAN-FIX.md | NaN state corruption in zoom matrix | Reproducing NaN in zoom offsets / transform math after pinch | `ViewerScreen.kt`, `ViewerViewModel.kt`, `pageFlip/` gesture files |
| KI-03 | KI-03-TOKEN-AUDIT-AND-PRUNING.md | Token budget audit + index pruning rules | Editing `conductor/index.md` / running `token_audit` | `conductor/index.md`, `CLAUDE.md`, `RULES.md` |
| KI-04 | KI-04-LOG-FILTERS.md | Log filter catalogue (SOT for every `[Comiqueta][X]` tag) | Adding/renaming/removing a log filter, running `/remove_filter` | `TimberLogger.kt`, all `TimberLogger.log*` call sites |
| KI-TBD | KI-TBD.md | **Master index of all deferred / not-yet-implemented items** | Planning next work, checking what is outstanding before starting a task | (index only — see linked KIs) |
| KI-AUTHORING | KI-AUTHORING.md | KI authoring rules: index maintenance, present-tense spec, self-sufficiency, size limits | Creating, renaming, revising, or deleting any KI | (this directory) |

Do NOT pre-load. Index first, fetch on match.

---

## Coverage State

Per-module test inventory, known gaps, and verification commands live in [`TEST_COVERAGE.md`](TEST_COVERAGE.md).

## Skills (Lazy-Load) → `../skills/`

> Read a skill only when its exclusive purpose is the active task. Never pre-load.

| Skill | Exclusive purpose |
|---|---|
| [testing-setup](../skills/testing-setup/SKILL.md) | Test infrastructure (unit / UI / screenshot / E2E) |
| [r8-analyzer](../skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size |
| [perfetto-trace-analysis](../skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause via traces |
| [android-cli](../skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) |
| [edge-to-edge](../skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling |

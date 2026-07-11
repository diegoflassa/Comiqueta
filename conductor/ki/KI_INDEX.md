# KI_INDEX — Comiqueta

One-line summaries. Fetch an individual KI file **only** if the current task matches.

| ID | File | Topic | When to read | Key files |
|----|------|-------|--------------|-----------|
| KI-001 | KI-001_Viewer-Pinch-Zoom-Fix.md | Viewer zoom (2-finger) + pan (1-finger), border-clamped | Touching viewer gesture handling / zoom / pan | `ViewerScreen.kt`, `globalIsPinchZoomActive`, `isPinchGestureInProgress`, `maxTranslateX/Y` |
| KI-002 | KI-002_Viewer-Pinch-Zoom-NaN-Fix.md | NaN state corruption in zoom matrix | Reproducing NaN in zoom offsets / transform math after pinch | `ViewerScreen.kt`, `ViewerViewModel.kt`, `pageFlip/` gesture files |
| KI-003 | KI-003_Token-Audit-and-Pruning.md | Token budget audit + index pruning rules | Editing `conductor/index.md` / running `token_audit` | `conductor/index.md`, `CLAUDE.md`, `RULES.md` |

Do NOT pre-load. Index first, fetch on match.

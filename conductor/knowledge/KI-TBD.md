# KI-TBD: Future Work Index

**Scope:** Whole project — aggregates every known deferred item, backlog item, and not-yet-implemented rule
**Status:** TBD — planning reference only, no code here
**Last verified:** 2026-07-21

## Purpose

Single landing page for "what's left." Each row points to where the authoritative detail lives — this file indexes, it does not duplicate. When an item ships, remove its row and strip the "deferred/TBD" framing at the source.

## Open Items

| # | Item | Detail lives in | Source location | Executable? | Blocker(s) |
|---|---|---|---|---|---|
| 1 | 31 untagged `TimberLogger.log*` call sites across 6 files | [KI-04 §Compliance gaps](KI-04-LOG-FILTERS.md) | `feature-ads/*Controller.kt`, `NativeAdView.kt`, `SafFolderScanWorker.kt`, `HomeViewModel.kt` | ✅ Executable | None — mechanical; add the `[Comiqueta][…]` prefix per `CORE_RULES §8.1` |
| 2 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/`, `schemas/` | ✅ Executable | None — required before the next schema change ships |
| 3 | Viewer gesture regression tests (zoom clamp + NaN guard) | [KI-01](KI-01-VIEWER-PINCH-ZOOM-FIX.md), [KI-02](KI-02-VIEWER-PINCH-ZOOM-NAN-FIX.md) | `ViewerScreen.kt`, `ViewerViewModel.kt` | ✅ Executable | None — both fixes shipped without a pinning test, which `CORE_RULES §12` now forbids |
| 4 | `ComicsRepository` reactive-stats coverage | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `core/.../ComicsRepository.kt` | ✅ Executable | None — the 8-flow `combine()` is untested |
| 5 | `:feature-ads` guard-clause coverage | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `feature-ads/*Controller.kt` | ✅ Executable | None |
| 6 | `StatisticsViewModel` debug scaffolding (`"DEBUG:"`-prefixed messages) | [KI-04 §Statistics](KI-04-LOG-FILTERS.md) | `feature-home/.../StatisticsViewModel.kt` | ✅ Executable | None — decide keep vs. clean up |
| 7 | No Compose UI tests for any screen | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all `feature-*/ui/` | ⏸️ Partially blocked | Screens must satisfy [`INSTRUMENTED_TEST_STANDARD.md`](../rules/INSTRUMENTED_TEST_STANDARD.md) (two-layer split + test tags) first |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |

## Ready to Execute Now

**#1, #2, #3, #4, #5, #6** — all unblocked. Suggested order: #2 (data-loss risk) → #3 (pins two known bugs) → #1 (diagnosability) → #4/#5/#6.

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.

# Changelog — Comiqueta

All notable changes to this project. Governed by [`conductor/rules/CORE_RULES.md`](conductor/rules/CORE_RULES.md) — one line per work unit (not per commit), appended to `## Unreleased` in the same turn the work lands:

```
- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)
```

Omit the `(CODE)` suffix when no ticket exists. On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased` above it.

> History before 2026-07-21 lives in git only — this file starts here.

## Unreleased

- **Release-build error logs contained nothing but their own tag.** `CrashReportingTree` called `Timber.log(priority, tag, message, t)`, which binds to `log(priority, message, vararg args)` — so the tag was logged *as* the message and the real text and stack trace were passed as format arguments no specifier consumed, then discarded. It also dropped `logI`/`logW`, which `LOGGING_RULES.md` §8.6 lists as mandatory-surviving, and never reached Crashlytics despite its name
- **17 log sites across 6 files leaked personal data into release captures** — raw SAF URIs, filesystem paths and file names, and a comic library path routinely contains the device owner's real name (§8.3). All now go through the new `LogRedaction`
- The SAF `takePersistableUriPermission`/release outcome logged at `logD`, so it never survived into a release capture even though §8.6 names it explicitly. Promoted to `logI`, with the folder count alongside
- Long log messages are split rather than silently truncated at the ~4 KB logcat byte cap, and every piece repeats its `[Comiqueta][…]` filter so a `grep` returns the whole message (`LogChunker`, §8.7). Timber's own splitter counts characters and drops the filter, so it could not do this
- The keyboard helpers swallowed the "no Activity window" branch in silence; it is now a `logW` under the new `[Comiqueta][Ime]` filter
- **Twelve deprecations replaced with current APIs rather than suppressed:** AdMob's int child-directed/under-age pair → `setAgeRestrictedTreatment`; the deprecated anchored adaptive banner → `getLargeAnchoredAdaptiveBannerAdSize`; `Icons.Filled.LibraryBooks`/`MenuBook` → `AutoMirrored`; `hiltViewModel` → its new package; `Paint.asFrameworkPaint()` → `nativePaint`; `InputMethodManager.SHOW_IMPLICIT` → `WindowCompat.getInsetsController`
- Three blanket `@file:Suppress("DEPRECATION")` removed — two masked nothing, the third hid the IME deprecation above. Dead `Context.tamanhoDaTela()`/`Size` deleted: unreferenced, and its only purpose was a deprecated `defaultDisplay.getMetrics()` call
- Also fixed: an unchecked cast in `ComicsRepository`, two redundant `toString()` calls on an already-`String` `filePath`, and an always-false null check in the page-curl tap gesture
- Rule set restructured — split `CORE_RULES.md` by topic into `PLANNING_RULES.md`, `LOGGING_RULES.md`, `UI_RULES.md` and `DOC_GOVERNANCE.md` behind a master numeric index; added log-level policy, reference-document ownership, rule placement, agent-surface parity and single-activation rules; added `.agent/` rule pointers and workflows for Antigravity; `plannings/archived/` is now version-controlled
- Aligned `conductor/` structure and rules with the shared cross-project reference layout
- Added seven AI-code-review rules to `conductor/rules/`: cancellation re-throw, named seam interfaces over lambdas, selective saved state, ViewModel-side derivation, `IconButton` for icon-only actions, Material 3 over a hand-rolled `Box`, enum over an all-object sealed hierarchy

# KI-04: App Log Filters Catalogue

**Scope:** all modules (cross-cutting)
**Last verified:** 2026-07-13 (full-repo grep sweep — 18 distinct filter tags catalogued across 21 files; found a real compliance gap, see §Compliance gaps below)

## Problem

This KI is the **single source of truth** for every runtime log filter the app emits: the filter tag, the class it lives in, and what it lets an engineer diagnose from `logcat`. **Every filter listed here is protected** — none may be stripped by `/remove_filter`, `/clean`, or any "log hygiene" sweep unless the user explicitly names it and confirms (see `CORE_RULES.md` §8 "Log Filter Management" and `workflows/remove_filter.md`).

## Filter convention (CORE_RULES.md §8)

- **Format:** `[FILTER_PAI][FILTRO_FILHO]` → in this project, `[Comiqueta][FILTER_NAME]`. One filter per log message, leading the message.
- **Emitted via** `TimberLogger.logD/logI/logW/logE/logA(CLASS, "[Comiqueta][FILTER_NAME] message")` — never raw `Timber.*` or `android.util.Log` outside the `TimberLogger`/`CrashReportingTree` wrapper files themselves.
- `logI` / `logW` / `logE` are intentional production signal and are never stripped regardless of filter.
- **MANDATORY:** every new log with a filter MUST be added to the catalogue below in the same turn.

## Catalogue

### App lifecycle / bootstrap

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][MyApplication]` | `app/.../MyApplication.kt` | App process bootstrap: `onCreate` entry, Clarity session-recording SDK init, propagating the Clarity session URL to Crashlytics. | Mixed I/W/E — production signal |
| `[Comiqueta][Main]` | `app/.../MainActivity.kt` | AdMob bootstrap: `RequestConfiguration`, UMP consent-info update/status, consent form shown/errors, MobileAds SDK init status, ad-request gating. | Mixed D/I/W/E — production signal |

### Ads (`feature-ads`)

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][AppOpenAdController]` | `feature-ads/.../AppOpenAdController.kt` | Guard-clause notice when App Open ad loading is disabled or `adUnitId` is blank. | `logI` — debug-level |
| `[Comiqueta][InterstitialAdController]` | `feature-ads/.../InterstitialAdController.kt` | Same guard pattern for interstitial ads. | `logI` — debug-level |
| `[Comiqueta][RewardedAdController]` | `feature-ads/.../RewardedAdController.kt` | Same guard pattern for rewarded ads. | `logI` — debug-level |
| `[Comiqueta][NativeAdView]` | `feature-ads/.../NativeAdView.kt` | Same guard pattern for native ads. | `logI` — debug-level |

### Home

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Home]` | `feature-home/.../HomeScreen.kt`, `HomeViewModel.kt` | Home screen lifecycle: storage-permission flow, folder add/scan triggering, Paging `loadState` of comics/latest/favorites lists, scan-worker completion. | Mixed D/I/W/E — production signal |

### Settings

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Settings]` | `feature-settings/.../SettingsScreen.kt`, `SettingsViewModel.kt` | Folder management (add/remove/persist SAF permissions), page-preload preference, database-clear/rescan actions, UMP privacy-options form. | Mixed D/I/W/E — production signal |

### Categories

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Categories]` | `feature-categories/.../CategoriesScreen.kt` | Screen composition/entry breadcrumb. | `logI` |

### Viewer (`feature-viewer`)

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Viewer]` | `feature-viewer/.../ViewerViewModel.kt` | Full viewer lifecycle: preload-count settings, MVI intent reduction, comic/page loading, thumbnail loading, per-page decode errors, reading-progress, set-as-cover, cleanup. | Mixed I/W/E — production signal |
| `[Comiqueta][GetComicInfo]` | `feature-viewer/.../GetComicInfoUseCase.kt` | `ComicInfo.xml` parse errors, comic-info retrieval errors, `ParcelFileDescriptor` close errors. | `logE` only — production signal |
| `[Comiqueta][DecodeComicPage]` | `feature-viewer/.../DecodeComicPageUseCase.kt` | Per-page bitmap decode trace + decode errors. | Mixed D (trace) + E (error) |

### Comics data / repository (`core`)

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Comics]` | `core/.../ComicsRepository.kt` | Filtered-count queries, reactive stats `combine()` (total/read/in-progress/favorites/categories/CBZ/CBR/PDF/top-authors), DataStore read errors. | Mostly `logD` + 1 `logI` + 1 `logE` |
| `[Comiqueta][ComicsFolder]` | `core/.../ComicsFolderRepository.kt` | SAF persisted-folder-permission lifecycle: fetch, take/release (success + failure). | Mixed D/E |
| `[Comiqueta][CoverUtils]` | `core/.../CoverUtils.kt` | Comic-cover cache: dir creation failure, save success/error, old-cover deletion. | Mixed D/E |
| `[Comiqueta][ContextExtensions]` | `core/.../ContextExtensions.kt` | Network-interface enumeration during local-IP resolution. | `logI` |

### SAF folder scanning (`core`, background work)

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][SafFolderScanWorker]` | `core/.../SafFolderScanWorker.kt` (largest filter in the codebase, 40 tagged call sites) | Background comic-folder scan: specific vs. general scan start, persisted-folder fetch errors, parallel file-counting, per-folder document-tree traversal, completion/failure per folder and overall. | Mixed D/I/W/E — production signal |
| `[Comiqueta][EnqueueSafFolderScanWorker]` | `core/.../EnqueueSafFolderScanWorkerUseCase.kt` | WorkManager unique scan-request enqueue breadcrumb. | `logD` |

### Statistics

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[Comiqueta][Statistics]` | `feature-home/.../StatisticsViewModel.kt` | Stats loading — message text is self-labeled `"DEBUG:"`, load error, stats-received success. | Mixed D/E — debug scaffolding, candidate for eventual cleanup |

## Compliance gaps (found during the 2026-07-13 sweep, not yet fixed — code changes deferred per user instruction)

Roughly a quarter of `TimberLogger.log*` call sites in the repo (31 of ~205) emit **no `[Comiqueta][...]` tag at all** — not a wrong shape, a missing one:

| File | Total calls | Tagged | Untagged | Detail |
|---|---|---|---|---|
| `feature-ads/AppOpenAdController.kt` | 4 | 1 | 3 | Request/loaded/failed-to-load ad callbacks untagged. |
| `feature-ads/InterstitialAdController.kt` | 4 | 1 | 3 | Same pattern. |
| `feature-ads/RewardedAdController.kt` | 4 | 1 | 3 | Same pattern. |
| `feature-ads/NativeAdView.kt` | 7 | 1 | 6 | Same pattern + untagged impression/click/dispose logs. |
| `core/.../SafFolderScanWorker.kt` | 49 | 40 | 9 | Several `logE(..., e.message ?: "", e)` calls with no bracket prefix. |
| `feature-home/.../HomeViewModel.kt` | 19 | 12 | 7 | 7 calls use a localized error string resource as the raw message with no `[Comiqueta][...]` prefix. |

No 3-segment `[Comiqueta][X][Y]` tags exist. No `android.util.Log`/raw `Timber.*` usage outside the sanctioned wrapper (`TimberLogger.kt`, `CrashReportingTree.kt`).

## Adding / renaming procedure

Filter names are **public string contracts**. Renaming or removing any one requires updating, **in the same turn**:
1. this KI (the row),
2. `CORE_RULES.md` §8 (Log Filter Management), if it names the filter as an example,
3. `workflows/remove_filter.md`, if it names the filter,
4. every production `TimberLogger.log*` call site that emits the tag,
5. every test assertion on the removed/renamed tag.

One-turn change or none.

## Validation
- [x] 18 distinct filter tags catalogued (full-repo grep, 2026-07-13)
- [x] No 3-segment `[Comiqueta][X][Y]` tags exist
- [x] No `android.util.Log`/raw `Timber.*` usage outside `TimberLogger`/`CrashReportingTree`
- [ ] 31 untagged `TimberLogger.log*` call sites (6 files, see Compliance gaps) — not fixed yet, code changes deferred

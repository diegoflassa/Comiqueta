# Test Coverage Inventory — Comiqueta

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods — each file may host many, so this is a structural map, not a method count. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts and called out in Notes.

**Last verified:** 2026-07-21

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 0 | 0 | Only the two IDE placeholders (`ExampleUnitTest`, `ExampleInstrumentedTest`). `MyApplication` bootstrap and AdMob/UMP consent wiring in `MainActivity` are untested. |
| `:core` | 1 | 1 | `EnqueueSafFolderScanWorkerUseCaseTest` (JVM); `SafFolderScanWorkerTest` (instrumented — the scan worker is the largest single behaviour in the codebase). Placeholder excluded. **Room DAOs, `ComicsRepository`, `ComicsFolderRepository`, and `CoverUtils` have no tests.** |
| `:feature-home` | 3 | 0 | `HomeViewModelTest`, `GetPaginatedComicsUseCaseTest`, `LoadCategoriesUseCaseTest`. Placeholder excluded. **No Compose UI test for `HomeScreen`.** |
| `:feature-viewer` | 1 | 0 | `ViewerViewModelTest`. Placeholder excluded. **The pinch-zoom/pan gesture math behind KI-01 and KI-02 has no regression test** — both were fixed without a pinning test, which `CORE_RULES §12` now forbids for future fixes. |
| `:feature-settings` | 1 | 0 | `SettingsViewModelTest`. Placeholder excluded. |
| `:feature-categories` | 0 | 0 | Only the IDE placeholder. |
| `:feature-ads` | 0 | 0 | No tests. Ad controllers are thin SDK wrappers, but the guard clauses (disabled / blank `adUnitId`) are cheap to pin. |

**Priority gaps** (highest value first):
1. **Room migration test** — none exists; `CORE_RULES §13` now requires one per schema change.
2. **Viewer gesture regression tests** — KI-01/KI-02 describe two real, subtle bugs (zoom clamping, NaN corruption) with no test pinning either fix.
3. **`ComicsRepository` reactive stats** — the `combine()` over 8 flows is untested.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites |
| `./gradlew connectedAndroidTest` | Instrumented suites on an attached device/emulator |
| `./gradlew detekt` | Static analysis |
| `./gradlew assembleDebug assembleRelease` | Release runs R8 + resource shrink |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.

# Architecture SOT — Comiqueta

Single home for the module graph, layer rules, and package structure. `index.md` and `CORE_RULES.md` point here rather than restating it.

---

## Governing Principle

```
UI (Screen + ViewModel) → UseCase → Repository → Room / SAF
```

Dependencies flow one way only. Every arrow crosses an **interface** (`IXxxUseCase`, `IComicsRepository`), never a concrete class.

| Layer | Package | Allowed dependencies |
|---|---|---|
| **UI** | `feature-<name>/ui/` | its own ViewModel, `core/ui`, `core/theme`, Compose |
| **ViewModel** | `feature-<name>/ui/` | `IXxxUseCase`, domain models, `TimberLogger`. Never a repository directly, never a `NavController` |
| **Domain / UseCase** | `core/domain/`, `feature-<name>/domain/` | Repository interfaces + models only. **Zero Android imports.** |
| **Data** | `core/data/` | Room, SAF (`DocumentFile`), DataStore; implements domain interfaces |

Rules:
- **`core/domain` is pure Kotlin.** No `Context`, no `Log`, no Room, no Android framework types.
- **Repositories are domain interfaces implemented in data.** `IComicsRepository` is the boundary; `ComicsRepository` is the impl.
- **Entities are Room-only.** Never pass a `*Entity` to domain or UI — map at the data/domain seam.
- **Features never depend on each other.** `feature-*` depend on `core` only; `feature-ads` is a sibling consumed by `app`.

## Module Graph

```
:app → :feature-* → :core
:app → :feature-ads          (sibling — ads are an app-level concern)
```

| Module | Owns |
|---|---|
| `:app` | `MainActivity`, `MyApplication`, `NavDisplay` host, Hilt root |
| `:core` | `/domain` (models, use cases), `/data` (Room, repos, SAF), `/di`, `/navigation`, `/theme`, `/ui` |
| `:feature-home` | Library grid, latest/favorites paging, statistics |
| `:feature-viewer` | Page rendering, zoom/pan gestures, decode pipeline |
| `:feature-categories` | Category browsing |
| `:feature-settings` | Folder management, preferences, privacy options |
| `:feature-ads` | AdMob controllers (app-open, interstitial, rewarded, native) |
| `build-logic` | Convention plugins |

## MVI Contract

```
XxxUIState    — data class (immutable)
XxxIntent     — sealed
XxxEffect     — sealed, delivered via Channel
IXxxViewModel — interface the screen depends on
```

Screens are stateless: `XxxScreen(uiState, onIntent)`. Wiring lives in the route-level composable.

## Navigation

Nav 3, type-safe. `@Serializable` keys declared in `core/navigation/Screen.kt`. Screens never hold a `NavController` — navigation is triggered through `Effect` or injected `() -> Unit` callbacks.

## Dependency Injection

**Hilt.** Each module provides its own modules under `/di`. ViewModels via `@HiltViewModel` + `hiltViewModel()`. Bind interfaces to implementations with `@Binds`; never inject a concrete repository into a use case.

- **A testability seam is a named interface, never a bare lambda.** When a collaborator is injected so tests can swap it — a clock, a device-id reader, a scan-id generator, a randomness source — declare a named `interface XxxProvider` in `core/domain`, `@Binds` the production impl in that module's `/di`, and write a fake for it in the consuming module's `src/test`. Never inject `now: () -> Long` or `idGenerator: () -> String`. A lambda binds under no meaningful key, so two `() -> Long` dependencies in one Hilt graph collide unless every one of them gets a `@Qualifier` you then have to keep straight; an interface can grow a second method when a test needs to advance time, where a lambda forces rebuilding its captured state; and the constructor keeps the dependency visible *as* a dependency. This is the same reasoning as the `IXxxUseCase` / `IComicsRepository` boundaries above, applied to the small non-domain collaborators that would otherwise arrive as lambdas. It governs the **shape** of a seam once one is warranted — it is not licence to abstract single-call-site code (`ai_behavior.md` §2).

## Concurrency

- `suspend` for all async work; ViewModels use `viewModelScope` (Main-safe).
- I/O wraps in `withContext(Dispatchers.IO)` inside the repository or use case.
- Errors: `runCatching {}` → `Result<T>` / sealed result. No bare try/catch outside a `runCatching` lambda.

### Cancellation must never be swallowed (MANDATORY)

`CancellationException` is an `Exception`. A `catch (e: Exception)` / `catch (t: Throwable)` around a suspending call swallows it unless it re-throws — and that exception is how the coroutine machinery tells a parent scope its child actually stopped. Swallow it and the parent believes the work completed: cleanup runs against an already-dead job, a `viewModelScope` cancelled by screen teardown still walks the success branch, and a retry loop keeps spinning after its scope is gone.

**`runCatching {}` has the identical defect** — it catches `Throwable`. Since this project mandates `runCatching` over bare try/catch, that is where the guard belongs:

```kotlin
runCatching { comicsRepository.index(folder) }
    .onFailure { e ->
        if (e is CancellationException) throw e
        TimberLogger.logE(CLASS, "[Comiqueta][SafFolderScanWorker] index failed", e)
    }
```

For any generic `catch` that does survive — inside a `runCatching` lambda, or in a decode / SAF adapter — the guard is either a `catch (e: CancellationException) { throw e }` clause placed **before** the generic one, or `currentCoroutineContext().ensureActive()` as its first statement (a no-op on the genuine failure path).

- **Not optional inside `SafFolderScanWorker` or the decode pipeline.** WorkManager stops a worker by cancelling its coroutine; a swallowed cancellation there makes the worker return `Result.success()` for a scan that never finished, so the library stays half-indexed and nothing ever retries it. The same holds for a page decode cancelled by a fast page turn — reported as a decode failure, it surfaces a corrupt-page error for a page that was fine.
- **Cancellation is not an error to report.** Never log it via `logE`, never send it to Crashlytics, never map it to a user-facing error state — the user swiping to the next page is not a decode failure.
- **Exception:** a `catch` that only releases a resource and re-throws is already correct. A `catch` inside a `suspendCancellableCoroutine` callback body is not in the suspending context and is out of scope.

## Persistence & I/O

- **Room** — `ComicDatabase`, `ComicsDao`, `CategoryDao`. Schema exports live in `schemas/`. Migration rules: [CORE_RULES §13](CORE_RULES.md).
- **SAF** — `DocumentFile` for all external storage. **Never `java.io.File`** for user-selected content; persisted URI permissions are taken/released through `ComicsFolderRepository`.
- **Supported formats:** CBZ, CBR, CB7, CBT, PDF.
- **Background work** — WorkManager (`SafFolderScanWorker`) for folder scanning; enqueued as a unique work request.

**Recovering an interrupted scan is a persistence concern, not a saved-state one.** After process death, whether a folder scan completed is answered by the Room rows and the unique work request's own state — never by a restored "scanning" flag on a UI state object. Saved state and the persisted record must not both own that decision; the record wins, because it is the only one of the two that survived for the right reason. Which UI fields may be saved at all: [`COMPOSE_RULES.md` §2](COMPOSE_RULES.md).

## Package Structure (per feature module)

```
feature-<name>/src/main/java/.../
├── ui/
│   ├── <Name>Screen.kt        # stateless layout
│   ├── <Name>ViewModel.kt
│   ├── <Name>UIState.kt       # State | Intent | Effect
│   └── components/            # feature-local composables
├── domain/                    # feature-specific use cases
└── di/                        # Hilt module(s)
```

Shared widgets go in `core/ui/`. Domain models shared across features live in `core/domain/`.

## Build

- Java 21, KSP, convention plugins in `build-logic/`.
- Static analysis: detekt (`./gradlew detekt`).
- Versioning via `version.properties`.
- Distribution: `appDistributionUploadDebug.ps1` / `appDistributionUploadRelease.ps1`.

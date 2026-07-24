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

## Concurrency

- `suspend` for all async work; ViewModels use `viewModelScope` (Main-safe).
- I/O wraps in `withContext(Dispatchers.IO)` inside the repository or use case.
- Errors: `runCatching {}` → `Result<T>` / sealed result. No bare try/catch outside a `runCatching` lambda.

## Persistence & I/O

- **Room** — `ComicDatabase`, `ComicsDao`, `CategoryDao`. Schema exports live in `schemas/`. Migration rules: [CORE_RULES §13](CORE_RULES.md).
- **SAF** — `DocumentFile` for all external storage. **Never `java.io.File`** for user-selected content; persisted URI permissions are taken/released through `ComicsFolderRepository`.
- **Supported formats:** CBZ, CBR, CB7, CBT, PDF.
- **Background work** — WorkManager (`SafFolderScanWorker`) for folder scanning; enqueued as a unique work request.

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

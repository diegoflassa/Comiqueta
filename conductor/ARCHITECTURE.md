# Architecture: Comiqueta

## Module Graph
```
:app  →  :feature-*  →  :core
                     ↘  :feature-ads
```

- **`:app`** — `MainActivity`, `NavDisplay`, `MyApplication` (Hilt).
- **`:core`** — `/domain` (models, use cases), `/data` (Room, repos, SAF), `/di`, `/navigation`, `/theme`, `/ui`.
- **`:feature-*`** — `/ui` (MVI), `/domain` (feature use cases), `/di`.
- **`:feature-ads`** — Google AdMob.
- **`build-logic/`** — Convention plugins for shared Gradle config.

## MVI Contract (per feature)
- `XxxUIState` — immutable state data class
- `XxxIntent` — sealed class (user actions)
- `XxxEffect` — sealed class (one-shot side effects via `Channel`)
- `IXxxViewModel` — interface (enables fakes for testing)
- `XxxViewModel : ViewModel(), IXxxViewModel` — `@HiltViewModel`

## Navigation
Nav3 type-safe. Keys in `core/navigation/Screen.kt` (`@Serializable`). `NavDisplay` in `:app` maps keys → screens. `NavigationViewModel` shared across features.

## Data Layer
- **Room**: `ComicDatabase`, `ComicsDao`, `CategoryDao`. Schemas in `/schemas`.
- **SAF**: `DocumentFile.fromTreeUri` for all external storage. `ACTION_OPEN_DOCUMENT_TREE` for root.
- **Repos**: `IComicsRepository`, `ICategoryRepository` — map entities ↔ domain models.
- Formats: CBZ (ZIP), CBR (junrar), CB7, CBT, PDF (`PdfRenderer`).

## Key Files
| Path | Purpose |
|------|---------|
| `core/.../navigation/Screen.kt` | Nav3 screen keys |
| `core/.../navigation/NavigationViewModel.kt` | Shared nav state |
| `app/.../navigation/NavDisplay.kt` | Route mapping |
| `core/.../data/database/ComicDatabase.kt` | Room DB |
| `core/.../theme/Theme.kt` | `ComiquetaTheme` |
| `build-logic/.../android-library-convention.gradle.kts` | Library config |
| `gradle/libs.versions.toml` | Version catalog |

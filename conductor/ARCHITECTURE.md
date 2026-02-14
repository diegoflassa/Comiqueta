# Filesystem & Storage: Comiqueta
[Voltar ao Índice](./INDEX.md)

Comiqueta follows a decentralized **MVI + Clean Architecture** pattern tailored for Android with Jetpack Compose.

## 🏗️ Core Layers

### 1. UI Layer (Feature Modules)
- **Pattern**: MVI (Model-View-Intent).
- **Components**: `Contract` (State, Intent, Effect), `ViewModel` (Hilt-injected), `Screen` (Compose).
- **Rule**: ViewModels must only communicate with Domain UseCases or Repositories.

### 2. Domain Layer (Core/Domain)
- **Role**: Business logic and models.
- **Constraints**: Pure Kotlin, zero Android dependencies.
- **Models**: `Comic`, `Category`, `Page`.

### 3. Data Layer (Core/Data)
- **Role**: Data sources (Room, File System).
- **Repositories**: Handle mapping between Data Entities and Domain Models.
- **Persistence**: Room for metadata; SAF (Storage Access Framework) for comic files.

## 🔗 Dependency Flow
`Feature` -> `Core:Domain`
`Core:Data` -> `Core:Domain`
`App` -> `Feature` + `Core`

## 🚦 Navigation
- **System**: Nav3 (Custom Type-Safe Navigation).
- **Structure**: Navigation keys defined in `core:nav`; Logic in `app` or feature-specific entry points.

---
Status: **Active**
Last Updated: 2026-02-08

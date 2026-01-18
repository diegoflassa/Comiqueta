---
trigger: always_on
---

# Project Rules
(Specific to Comiqueta's Implementation & Libraries)
- **Rules File Location**: This file is located at .\.agent\comiqueta-rules.md
- **Rules File Updates**: Please update this file when you think a new rule should be added
- **When Update the Rules File**: Please verify for any possible new rule after every task performed and add it this file, if needed
- **Rules File Context**: Please keep the global rules (GEMINI.md file) separated of the project rules (this file)
- **Rules File Format**: Keep this file format consistent, when edited
- **Rules Change Notification**: Notify me of every added rule, and the reason for it

## Architecture & Layers
- **Background Observability**: All Workers/Long-running tasks MUST emit granular progress updates and log state transitions.
- **Indeterminate Progress**: Workers performing multi-stage operations (e.g., Count then Process) MUST update progress immediately with an indeterminate state (e.g., "Counting...") before the total is known.

## Resources & UI
- **File Granularity**: Refactor large Composable files (>500 lines). Extract sub-components into their own files.
- **Previews**: Every distinct UI State (Loading, Error, Empty, Content) MUST have a corresponding `@Preview`.
- **Text Contrast**: Ensure Text colors match the container's specialized content color (e.g., use `onSurfaceVariant` on `surfaceVariant` containers). Avoid using default `onSurface` on custom-colored Cards.

## Dependency Injection (Hilt)
- **Worker Injection**: All WorkManager Workers must use `@HiltWorker` and maintain dependencies via Hilt. Do not implement manual WorkerFactories.
- **Binding Style**: Prefer `@Binds` (abstract methods) over `@Provides` (object methods) for interface bindings to reduce boilerplate and improve build times.

## Data & Persistence
- **SAF Safety**: Wrap all `DocumentFile` operations in `runCatching`. Treat `DocumentFile` calls as blocking I/O (run on `Dispatchers.IO`).
- **Recursion Strategy**: Favor **Iterative** approaches (Stack/Queue) over Recursion for file system operations to avoid StackOverflow.
- **I/O Safety**: Wrap all Disk/Network operations in strict try-catch/runCatching blocks.

## Libraries & Navigation
- **Navigation**: Use Type-Safe Navigation via `androidx.navigation3`. Screens MUST implement `NavKey` and be annotated with `@Serializable`.
- **Annotation Processing**: Use KSP (`ksp`) instead of KAPT (`kapt`) for all supported libraries (Room, Hilt, Moshi).

## Quality & Standards
- **Logging**: Use `TimberLogger` for ALL logs. Report exceptions to `FirebaseCrashlytics`. All log tags MUST be prefixed with `CMD_` for easy project-wide filtering.
- **Tracing**: Log the 'Start' and 'End' of important business logic flows (e.g., Scanning, Loading Stats).
- **Testing**: All ViewModel unit tests MUST use `MainDispatcherRule` to manage `Dispatchers.Main`.
- **Static Analysis**: Run `detekt` and `ktlint` before pushing.
- **Testing**: Use `runTest` for Coroutines. Map Domain models in tests.
- **Context Testing**: Logic dependent on `Context` or Android Framework (e.g. Workers, File I/O) MUST be tested via `androidTest`.

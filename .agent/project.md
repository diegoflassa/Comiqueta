# Comiqueta Tech Stack & Structure (CRITICAL)
Conductor: [conductor/INDEX.md](../conductor/INDEX.md)

Context: Comic Reader/Manager
Arch: Core -> Domain. Feature -> Core.

## Rules
- UI: ComiquetaTheme. No hardcoded strings.
- IO: DocumentFile + runCatching + Dispatchers.IO.
- Domain: Pure Kotlin. No Android deps.
- Logs: TimberLogger.logX(CLASS, "[FIX] or MSG").
- Nav: Nav3 (Type-Safe). NavKey + @Serializable.
- Files: Scoped storage. Use DocumentFile wrappers.
- Workers: @HiltWorker for all tasks.

## Structure
- :app: Entry + Nav host.
- :core: Shared logic. /domain (Models), /data (Repos/Room), /di (Hilt), /nav (Keys), /theme (Tokens), /ui (Shared).
- :feature-*: Pattern: /ui (MVI VM/Screen), /domain (Logic), /di.
- :f-home, :f-viewer, :f-categories, :f-settings.
- Nav keys in core:nav; Logic in app.

# Workflows: Comiqueta
[Voltar ao Índice](./INDEX.md)

Development and distribution workflows for the Comiqueta project.

## 🛠️ Build System

- **Gradle**: Version 8.x+ with Kotlin DSL (`.gradle.kts`).
- **Plugins**: Hilt, KSP, Compose Compiler.
- **Convention Plugins**: Located in `build-logic` for shared module configurations.

## 🚀 Distribution

- **Firebase App Distribution**: Main channel for QA/Internal testing.
- **Scripts**:
  - `appDistributionUploadDebug.ps1`
  - `appDistributionUploadRelease.ps1`
- **Fastlane**: (Optional/Planned).

## 🧪 Quality & Health

- **Slash Command**: `/health-check` for project-wide audits.
- **Lints**: Kotlin Lint + Custom rules for MVI consistency.
- **Testing**: JUnit5 for Domain/Data; Compose Test for UI.

## 🌲 Git Conventions
- Branching: `feature/*`, `bugfix/*`, `chore/*`.
- Commits: Conventional Commits (feat, fix, chore, docs).

---
Status: **Active**
Last Updated: 2026-02-08

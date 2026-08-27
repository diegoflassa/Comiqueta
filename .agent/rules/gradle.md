---
description: Build tooling standards - Kotlin DSL, version catalog as the only coordinate source, convention plugins, caching. Use when editing any Gradle script, the version catalog or build-logic.
trigger: glob
globs: **/*.gradle.kts,**/libs.versions.toml,build-logic/**
---

# Gradle

Full spec (source of truth) - [GRADLE_RULES.md](../../conductor/rules/GRADLE_RULES.md).

- **Kotlin DSL only.** `plugins { }` block only - never `buildscript { classpath(...) }`.
- **The version catalog is the only source of dependency coordinates.** No inline version strings.
- **Repositories are declared once**, in `settings.gradle.kts`.
- **Shared build logic goes in a convention plugin** in `build-logic/` - never copy-pasted between modules.
- **No explicit Kotlin stdlib dependency.**
- Configuration cache, build cache and parallel stay on. The wrapper is validated in CI.

# GRADLE RULES

Build-tooling standards, shared verbatim across Comiqueta, Slotify, and BipSale (`CORE_RULES.md` §15). Sourced from Gradle/JetBrains/Google official best-practice guidance.

## 1. Kotlin DSL Only

All build files are `.gradle.kts` / `settings.gradle.kts`. Never introduce a Groovy `.gradle` file — Kotlin DSL gives IDE refactoring, navigation, and type-safe accessors that Groovy does not.

## 2. Track the Latest Gradle Minor

Stay on the latest **patch** of the current Gradle major (`gradle/wrapper/gradle-wrapper.properties`). Do not jump a major version speculatively; do not fall behind on patches. Gradle minors are strictly non-breaking — if an upgrade breaks the build, that's a Gradle bug to report, not a reason to stay pinned.

## 3. Plugins Block Only — No `buildscript { dependencies { classpath(...) } }`

Apply every plugin via the root `plugins { alias(libs.plugins.x) apply false }` + subproject `plugins { alias(libs.plugins.x) }`. Never add a plugin's classpath via `buildscript { dependencies { classpath(...) } }` — that legacy mechanism predates the `plugins {}` block and is redundant once every subproject that needs the plugin already applies it via `alias(...)`. This includes Firebase/Google-Services plugins, which have version-catalog plugin aliases already declared — apply them the same way as any other plugin.

## 4. No Explicit Kotlin Stdlib Dependency

Never declare `implementation(kotlin("stdlib"))` / `org.jetbrains.kotlin:kotlin-stdlib` directly in a module that already applies a Kotlin plugin (`kotlin("jvm")`, `kotlin("android")`, etc.) — the plugin already provides the matching stdlib version. An explicit declaration risks silent version drift between the plugin's Kotlin version and the stdlib actually resolved.

## 5. Version Catalog Is the Only Source of Dependency Coordinates

Every dependency coordinate (`group:artifact:version`) lives in `gradle/libs.versions.toml` — `[versions]` + `[libraries]` + `[plugins]`. No module `build.gradle.kts` may hardcode a `"group:artifact:version"` string. Add a new catalog entry rather than inlining a coordinate, even for a single-use dependency — the catalog is what lets Renovate/Dependabot and manual updates work from one place instead of a grep across every module.

## 6. Repositories Declared Once, in `settings.gradle.kts`

Repositories are declared exactly once, in `settings.gradle.kts`'s `dependencyResolutionManagement { repositories { ... } }` (and `pluginManagement { repositories { ... } }` for plugin resolution). `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` is already set — it fails the build if any module declares its own `repositories {}` block instead of silently pulling from an untracked source. No subproject `build.gradle.kts` may declare a `repositories {}` block.

## 7. Modularize Along Real Boundaries

Split the codebase into Gradle modules along architectural seams (layer or feature), not as a single mega-module. Modularization unlocks parallel task execution, more effective build caching, and finer incremental compilation. Don't over-split either — stay well under the range where module count itself becomes the bottleneck (roughly triple digits is fine; the thousands range is where it stops paying off).

## 8. Convention Plugins for Shared Build Logic — Never Copy-Paste

Cross-cutting build configuration (Java/Kotlin toolchain version, Android `compileSdk`/`minSdk`, standard lint/test wiring, common dependency sets) lives in a **convention plugin** inside the `build-logic` included build, applied via `plugins { id("xxx-convention") }`. Never duplicate the same config block verbatim across module `build.gradle.kts` files — a copy-pasted block means every future change to that convention requires a manual find-and-replace across every module instead of a one-file edit. When adding a new feature module, check whether an existing feature module's `dependencies {}` block is being copy-pasted rather than shared via a convention plugin — if so, extract it.

## 9. Configuration Cache + Build Cache + Parallel, All On

`gradle.properties` sets all three:
```
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
```
Configuration cache is Gradle's preferred execution mode going forward (mandatory in a future major version) — treat any local configuration-cache failure as a bug to fix, not a reason to disable it project-wide. If one specific task is incompatible with the build cache, disable caching on that task alone (`outputs.cacheIf { false }` / `notCompatibleWithConfigurationCache`) rather than turning the flag off globally.

## 10. Validate the Gradle Wrapper in CI

Every workflow that runs `./gradlew` validates the wrapper jar first, via `gradle/actions/wrapper-validation@v4`. This checks the committed `gradle-wrapper.jar` against Gradle's known-good checksums before anything else runs — it is a supply-chain integrity gate, not a build step, so it must run before secrets are decoded or any other step executes.

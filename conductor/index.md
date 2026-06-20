# CONDUCTOR — Comiqueta

Documentation hub for Comiqueta across all AI models.

> Entry point is `../AGENTS.md` — this file is the doc map and project context. Load KIs lazily.

---

## Bootstrap (All Models)

1. `../AGENTS.md` — project essentials + critical rules
2. This file — documentation map + project context
3. `guides/bootstrap-ai.md` — agnostic init sequence + escalation matrix

---

## Documentation Map

### Rules (Read First)
- **[ai_behavior.md](rules/ai_behavior.md)** — AI behavior rules (think-first, surgical, simplicity)
- **[CORE_RULES.md](rules/CORE_RULES.md)** — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, Hilt, Room/SAF, Timber, build)
- **[COMPOSE_RULES.md](rules/COMPOSE_RULES.md)** — Compose rules (stability, recomposition, memory, animations, accessibility)
- **[PREVIEW_STANDARD.md](rules/PREVIEW_STANDARD.md)** — `@Preview` standard (device profiles, theme wrapper, localized mock data, ≥2 states)
- **[INSTRUMENTED_TEST_STANDARD.md](rules/INSTRUMENTED_TEST_STANDARD.md)** — screens must be instrumented-test friendly

### Knowledge (SOT) → [Full Index](ki/KI_INDEX.md)

### Skills (Lazy-Load) → `skills/`

> Read a skill only when its exclusive purpose matches the task. Never pre-load.

| Skill | Purpose | Load when |
|---|---|---|
| [testing-setup](skills/testing-setup/SKILL.md) | Test infra (unit / UI / screenshot / E2E) | Adding or auditing test layers |
| [edge-to-edge](skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling | Content obscured by system bars or keyboard |
| [android-cli](skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) | Running on device, capturing screenshots |
| [r8-analyzer](skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size | Optimizing size, debugging release minification |
| [perfetto-trace-analysis](skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause | Investigating janky transitions, slow loads, memory spikes |

### Plans & Workflows → [Plans](plannings/INDEX.md) | [Workflows](workflows/INDEX.md) | [Templates](templates/INDEX.md)

---

## Critical Reminders

1. Check relevant KI before code changes.
2. No build / no commit without user authorization.
3. `conductor/` = primary docs. Load KIs lazily via `ki/KI_INDEX.md`.

---

## Project Context

### ARCH

graph: :app -> :feature-* -> :core | :feature-ads (sibling)
modules:
  :app: MainActivity | NavDisplay | Hilt
  :core: /domain (models, usecases) | /data (Room, repos, SAF) | /di | /navigation | /theme | /ui
  :feature-*: /ui (MVI: State | Intent | Effect) | /domain (usecases) | /di
  build-logic: convention plugins
mvi_contract: XxxUIState (data) | XxxIntent (sealed) | XxxEffect (sealed/Channel) | IXxxViewModel (interface)
nav: Nav3 | type-safe | @Serializable keys @ core/navigation/Screen.kt
data: Room (ComicDatabase, ComicsDao, CategoryDao) | SAF (DocumentFile) | Repos (IComicsRepository) | Formats (CBZ, CBR, CB7, CBT, PDF)

---

### RULES

ui_strings: ComiquetaTheme | res/strings.xml | Locales: [EN, PT, ES, DE]
logging: TimberLogger.logX(CLASS, "[TAG] msg") | NO Log.x
io: DocumentFile + runCatching + Dispatchers.IO | NO java.io.File (external)
domain: Pure Kotlin | NO Android deps | Always IXxxUseCase
build: Java 21 | KSP | Convention plugins @ build-logic/ | detekt via ./gradlew detekt

---

### WORKFLOWS

build:
  - assemble: ./gradlew assembleDebug | assembleRelease
  - test: ./gradlew test | ./gradlew :module:test
  - analysis: ./gradlew detekt
dist:
  - firebase_debug: ./appDistributionUploadDebug.ps1
  - firebase_release: ./appDistributionUploadRelease.ps1

---

### KI

index: conductor/ki/KI_INDEX.md (read first; fetch individual KIs only on match)

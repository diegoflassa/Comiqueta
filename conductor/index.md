# CONDUCTOR — Comiqueta

Documentation hub for Comiqueta across all AI models.

> Entry point is `../AGENTS.md` — this file is the doc map. Load KIs lazily.

---

## Bootstrap (All Models)

1. `../AGENTS.md` — project identity, stack, critical rules
2. This file — documentation map
3. `guides/bootstrap-ai.md` — init sequence + escalation matrix + token discipline

---

## Documentation Map

### Rules (Read First)

- **[ai_behavior.md](rules/ai_behavior.md)** — AI behavior rules (think-first, correctness-first, simplicity)
- **[CORE_RULES.md](rules/CORE_RULES.md)** — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, logging, strings, regression tests, DB migration safety, changelog, cross-project sync)
- **[architecture.md](rules/architecture.md)** — architecture SOT (module graph, layers, MVI, Hilt, Room/SAF, build)
- **[COMPOSE_RULES.md](rules/COMPOSE_RULES.md)** — Compose rules (stability, recomposition, memory, animations, accessibility, theme fidelity)
- **[GRADLE_RULES.md](rules/GRADLE_RULES.md)** — Gradle build-tooling standards (Kotlin DSL, version catalog, convention plugins, caching, wrapper validation)
- **[PREVIEW_STANDARD.md](rules/PREVIEW_STANDARD.md)** — `@Preview` standard (device profiles, theme wrapper, localized mock data, ≥2 states)
- **[INSTRUMENTED_TEST_STANDARD.md](rules/INSTRUMENTED_TEST_STANDARD.md)** — screens must be instrumented-test friendly
- **[CI.md](rules/CI.md)** — manual pre-PR checklist

### Knowledge (SOT) → [Full Index](knowledge/INDEX.md)

Coverage state → [TEST_COVERAGE.md](knowledge/TEST_COVERAGE.md) · Deferred work → [KI-TBD.md](knowledge/KI-TBD.md)

### Skills (Lazy-Load) → `skills/`

> Read a skill only when its exclusive purpose matches the task. Never pre-load.

| Skill | Purpose | Load when |
|---|---|---|
| [testing-setup](skills/testing-setup/SKILL.md) | Test infra (unit / UI / screenshot / E2E) | Adding or auditing test layers |
| [edge-to-edge](skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling | Content obscured by system bars or keyboard |
| [android-cli](skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) | Running on device, capturing screenshots |
| [r8-analyzer](skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size | Optimizing size, debugging release minification |
| [perfetto-trace-analysis](skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause | Investigating janky transitions, slow loads, memory spikes |

### References (Lazy-Load) → [Index](references/INDEX.md)

### Analysis (Lazy-Load) → [Index](analysis/INDEX.md)

### Plans & Workflows → [Plans](plannings/INDEX.md) | [Workflows](workflows/INDEX.md) | [Templates](templates/INDEX.md)

> **META_PLANNING files** (`META_PLANNING_*.md` in `plannings/`) are synthesis scaffolding. Synthesise → write the canonical plan → delete the META_PLANNING. Protocol: [CORE_RULES.md §7.1](rules/CORE_RULES.md).

---

## Critical Reminders

1. Check the relevant KI before code changes.
2. No build / no commit without user authorization.
3. `conductor/` = primary docs. Load KIs lazily via [knowledge/INDEX.md](knowledge/INDEX.md).
4. **Never `fallbackToDestructiveMigration()`** — it wipes the entire scanned library ([CORE_RULES §13](rules/CORE_RULES.md)).
5. Architecture questions → [rules/architecture.md](rules/architecture.md), never re-derived from source.

---

## Project Context

**What:** Android comic reader. Scans user-selected folders via SAF, indexes comics into Room, and renders pages with a zoom/pan viewer. Supported formats: CBZ, CBR, CB7, CBT, PDF.

**Stack:** Java 21 · Hilt · Room · KSP · Nav 3 · Compose Material 3 · WorkManager · Paging · Timber · AdMob/UMP · Firebase (Crashlytics + App Distribution) · Microsoft Clarity. Convention plugins in `build-logic/`. Static analysis: detekt. Versioning: `version.properties`.

**Module graph, layers, MVI, DI, persistence →** [rules/architecture.md](rules/architecture.md)

### CLI

| Task | Command |
|---|---|
| Build | `./gradlew assembleDebug` · `assembleRelease` |
| Test | `./gradlew test` · `./gradlew :module:test` · `connectedAndroidTest` |
| Analysis | `./gradlew detekt` |
| Distribution | `./appDistributionUploadDebug.ps1` · `./appDistributionUploadRelease.ps1` |

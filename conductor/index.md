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

### Rules (Read First) → [Rules Index](rules/INDEX.md)

> Jump by number. Every rules file opens with a numeric index; cite sections as `§N`, never by line.

- **[AI_BEHAVIOR.md](rules/AI_BEHAVIOR.md)** — AI behavior rules (think-first, correctness-first, simplicity)
- **[CORE_RULES.md](rules/CORE_RULES.md)** — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, logging, strings, regression tests, DB migration safety, changelog, cross-project sync)
- **[ARCHITECTURE.md](rules/ARCHITECTURE.md)** — architecture SOT (module graph, layers, MVI, Hilt, Room/SAF, build)
- **[COMPOSE_RULES.md](rules/COMPOSE_RULES.md)** — Compose rules (stability, recomposition, memory, animations, accessibility, theme fidelity)
- **[GRADLE_RULES.md](rules/GRADLE_RULES.md)** — Gradle build-tooling standards (Kotlin DSL, version catalog, convention plugins, caching, wrapper validation)
- **[PREVIEW_STANDARD.md](rules/PREVIEW_STANDARD.md)** — `@Preview` standard (device profiles, theme wrapper, localized mock data, ≥2 states)
- **[INSTRUMENTED_TEST_STANDARD.md](rules/INSTRUMENTED_TEST_STANDARD.md)** — screens must be instrumented-test friendly
- **[AGENT_IO_RULES.md](rules/AGENT_IO_RULES.md)** — agent execution I/O (§20–§22: text encoding and shell output, chunked writing of long artefacts, full token-economy spec)
- **[SECURITY_RULES.md](rules/SECURITY_RULES.md)** — repository safety (§23: what never gets committed, placeholders, what to do when a secret is already in git)
- **[CI.md](rules/CI.md)** — manual pre-PR checklist

### Knowledge (SOT) → [Full Index](knowledge/INDEX.md)

Coverage state → [TEST_COVERAGE.md](knowledge/TEST_COVERAGE.md) · Deferred work → [KI-TBD.md](knowledge/KI-TBD.md)

### Skills (Lazy-Load) → [`.agents/skills/`](../.agents/skills/)

> Skills live at `.agents/skills/<name>/SKILL.md` - **never under `conductor/`** - because that is the only place
> Antigravity discovers them. Read a skill only when its exclusive purpose matches the task; never pre-load. Claude Code does not
> discover `.agents/skills/` - it opens a skill when a trigger in `AGENTS.md` names it.
> Project skills carry this repository's rules as checklists; the Android skills are Google's, kept verbatim.

#### Project skills

| Skill | Purpose | Load when |
|---|---|---|
| [agent-io](../.agents/skills/agent-io/SKILL.md) | Encoding, console evidence and chunked writes | Editing non-ASCII docs, writing long files, trusting console output |
| [changelog](../.agents/skills/changelog/SKILL.md) | One `CHANGELOG.md` line per unit of work | A fix, feature or planning task lands |
| [enum-vs-sealed](../.agents/skills/enum-vs-sealed/SKILL.md) | An enum when every sealed case is a bare object | Declaring a sealed hierarchy in domain or data code |
| [handoff](../.agents/skills/handoff/SKILL.md) | One-block continuation prompt for a new session | Ending a session with work open, before a compaction |
| [initialize](../.agents/skills/initialize/SKILL.md) | Lazy session bootstrap - entry files, indexes, skills, git state | The first prompt of a session, or on request |
| [interfaces](../.agents/skills/interfaces/SKILL.md) | An interface only with more than one implementation, used or highly likely | Declaring, extracting or reviewing an interface |
| [ki-sync](../.agents/skills/ki-sync/SKILL.md) | KIs as present-tense specs, updated in the same turn | After changing behaviour, structure or a contract |
| [logging](../.agents/skills/logging/SKILL.md) | Log filter format, coverage, redaction and level | Adding or reviewing a log line or failure branch |
| [models](../.agents/skills/models/SKILL.md) | One recommended model from the pool, and a fallback from another provider only when it matches | Writing a plan task, backlog row or handoff |
| [planning](../.agents/skills/planning/SKILL.md) | Writing a plan, and synthesising META_PLANNING proposals | Any planning request in the chat, or a META_PLANNING |
| [reference-docs](../.agents/skills/reference-docs/SKILL.md) | External documents copied in, versioned and indexed | A task depends on a document from outside |
| [regression-test](../.agents/skills/regression-test/SKILL.md) | A pinning test for every bug fix | Closing any fix |
| [rule-placement](../.agents/skills/rule-placement/SKILL.md) | Where an agreed rule is written | A new rule or convention is agreed |
| [secrets](../.agents/skills/secrets/SKILL.md) | Credentials never enter a tracked file | Touching config, properties or captured evidence |
| [strings](../.agents/skills/strings/SKILL.md) | String resources - ownership, keys, locales | Adding or editing user-visible text |
| [tapguard](../.agents/skills/tapguard/SKILL.md) | One action per deliberate tap | Adding or touching a tappable control |

#### Android skills

| Skill | Purpose | Load when |
|---|---|---|
| [testing-setup](../.agents/skills/testing-setup/SKILL.md) | Test infra (unit / UI / screenshot / E2E) | Adding or auditing test layers |
| [edge-to-edge](../.agents/skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling | Content obscured by system bars or keyboard |
| [android-cli](../.agents/skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) | Running on device, capturing screenshots |
| [r8-analyzer](../.agents/skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size | Optimizing size, debugging release minification |
| [perfetto-trace-analysis](../.agents/skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause | Investigating janky transitions, slow loads, memory spikes |
| [adaptive](../.agents/skills/adaptive/SKILL.md) | Adaptive Compose layouts for window-size classes, foldables, large screens | Building UI that must reflow across form factors |
| [android-intent-security](../.agents/skills/android-intent-security/SKILL.md) | Intent-surface security — exported components, `PendingIntent`, deep links, redirection | Auditing or adding any Intent, exported component, or deep link |
| [android-profiler](../.agents/skills/android-profiler/SKILL.md) | Record + analyse Android Studio Profiler / Perfetto traces — CPU, memory, jank, power | Deep runtime perf work beyond a quick trace read |
| [engage-sdk-integration](../.agents/skills/engage-sdk-integration/SKILL.md) | Integrate + debug the Play Engage SDK — content clusters, recommendations, continuation | Publishing content to Engage / Continue / Recommendations surfaces |
| [navigation-event](../.agents/skills/navigation-event/SKILL.md) | Intercept back gestures, run Predictive Back animations via `NavigationEventDispatcher` | Adding or changing back / predictive-back behaviour |
| [play-billing-library-version-upgrade](../.agents/skills/play-billing-library-version-upgrade/SKILL.md) | Upgrade / migrate Google Play Billing Library across major versions | Bumping the Play Billing dependency |
| [play-policy-insights](../.agents/skills/play-policy-insights/SKILL.md) | Audit the app against Google Play policy — data safety, permissions, account deletion | Before a Play Store submission or policy review |

### Agent Surface (`.agents/`) — Antigravity

> Antigravity discovers everything under `.agents/`, and each kind of file has one job
> ([DOC_GOVERNANCE.md](rules/DOC_GOVERNANCE.md) §18–§18.2). **Never add `.agents/` to `.gitignore`** - discovery
> honours gitignore silently, and an ignored directory means nothing loads at all.

| Path | Holds | Frontmatter |
|---|---|---|
| `.agents/rules/00-always.md` | Git safety, authorisation, layering, stability - the only always-on rule | `description`, `trigger: always_on` |
| `.agents/rules/*.md` | Rules anchored to a kind of file - Compose widgets, Gradle, Room migrations, and the agent surface itself | `description`, `trigger: glob`, `globs` |
| `.agents/skills/<name>/SKILL.md` | Every rule that binds at a recognisable moment, and every procedure | `name`, `description` |
| `.agents/workflows/*.md` | Slash commands - each follows a runbook in `workflows/` or a template in `templates/` | `description` |
| `.claude/settings.json` + `tools/hooks/` | Claude Code hooks - a gate before git writes and destructive commands, a frontmatter and link check after an AI document is edited | `hooks` object |
| `.agents/hooks.json` | Antigravity counterpart of those scripts — **`"enabled": false`** until tool names are confirmed | `enabled`, matchers |

Antigravity parses exactly the keys in the table; anything else is ignored silently. Frontmatter fails
silently: a colon followed by a space inside an unquoted `description`, an unknown key or a
mistyped `trigger` makes the file vanish with no error (§18.1). **Omitting `trigger` defaults to `always_on`.**

### References (Lazy-Load) → [Index](references/INDEX.md)

### Analysis (Lazy-Load) → [Index](analysis/INDEX.md)

### Plans & Workflows → [Plans](plannings/INDEX.md) | [Workflows](workflows/INDEX.md) | [Templates](templates/INDEX.md)

> **META_PLANNING files** (`META_PLANNING_*.md` in `plannings/`) are synthesis scaffolding. Synthesise → write the canonical plan → archive the META_PLANNING, following the `planning` skill. Protocol: [PLANNING_RULES.md §7.1](rules/PLANNING_RULES.md).

---

## Critical Reminders

1. Check the relevant KI before code changes.
2. No build / no commit without user authorization.
3. `conductor/` = primary docs. Load KIs lazily via [knowledge/INDEX.md](knowledge/INDEX.md).
4. **Never `fallbackToDestructiveMigration()`** — it wipes the entire scanned library ([CORE_RULES §13](rules/CORE_RULES.md)).
5. Architecture questions → [rules/ARCHITECTURE.md](rules/ARCHITECTURE.md), never re-derived from source.

---

## Project Context

**What:** Android comic reader. Scans user-selected folders via SAF, indexes comics into Room, and renders pages with a zoom/pan viewer. Supported formats: CBZ, CBR, CB7, CBT, PDF.

**Stack:** Java 21 · Hilt · Room · KSP · Nav 3 · Compose Material 3 · WorkManager · Paging · Timber · AdMob/UMP · Firebase (Crashlytics + App Distribution) · Microsoft Clarity. Convention plugins in `build-logic/`. Static analysis: detekt. Versioning: `version.properties`.

**Module graph, layers, MVI, DI, persistence →** [rules/ARCHITECTURE.md](rules/ARCHITECTURE.md)

### CLI

| Task | Command |
|---|---|
| Build | `./gradlew assembleDebug` · `assembleRelease` |
| Test | `./gradlew test` · `./gradlew :module:test` · `connectedAndroidTest` |
| Analysis | `./gradlew detekt` |
| Distribution | `./appDistributionUploadDebug.ps1` · `./appDistributionUploadRelease.ps1` |

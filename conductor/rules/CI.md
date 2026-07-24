# CI Gate — Comiqueta

**Status:** ✅ Active — document-only
**Scope:** Solo-developer project on Windows; no hosted CI runner provisioned.
**Mode:** manual pre-PR checklist. No GitHub Actions, no git hook.

---

## Rationale

A hosted pipeline would pay runner minutes for code one person pushes and still could not run the instrumented SAF/scan-worker suites, which need a real device with real storage permissions. Enforcement is the developer's manual checklist below.

> **The AI never runs these.** `CORE_RULES.md §1` forbids builds without explicit user confirmation in the current turn — the assistant reports the command, the developer runs it.

---

## Pre-PR checklist (manual)

Before merging to `main`, run in order:

1. **Unit tests** — `./gradlew test`
2. **Instrumented tests** — `./gradlew connectedAndroidTest` on a device with real storage (the SAF scan worker cannot be meaningfully exercised on a bare emulator)
3. **Static analysis** — `./gradlew detekt`
4. **Assemble** — `./gradlew assembleDebug` and, for release-impacting changes, `./gradlew assembleRelease` (catches R8 / resource-shrink breakage that debug misses)

Failing any of these blocks the merge.

**Schema changes additionally require** the migration test from [`CORE_RULES.md §13`](CORE_RULES.md) to pass — a Room version bump without it is an incomplete change.

---

## What is intentionally NOT enforced

| Gate | Why skipped |
|---|---|
| Hosted CI | Solo project; instrumented coverage needs a real device with user-granted SAF permissions. |
| Coverage thresholds | The inventory in [`TEST_COVERAGE.md`](../knowledge/TEST_COVERAGE.md) tracks real gaps; a numeric threshold invites gaming. |
| detekt as blocking | Reviewed manually before push; blocking noise-triggers on generated and transitive code. |

---

## When to promote this to real CI

Any one of: a second contributor lands a commit · a device farm is provisioned · the release cadence becomes scheduled rather than manual.

At that point, mirror steps 1, 3, and 4 in a workflow file and add a device-farm job for step 2.

---

## Cross-references

- [`CORE_RULES.md §1`](CORE_RULES.md) — no build without user confirmation. [`§2`](CORE_RULES.md) — Git Safety. [`§13`](CORE_RULES.md) — DB migration safety.
- [`TEST_COVERAGE.md`](../knowledge/TEST_COVERAGE.md) — per-module inventory and known gaps.

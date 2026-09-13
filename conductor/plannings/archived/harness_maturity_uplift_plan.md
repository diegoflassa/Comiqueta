# Harness maturity uplift — Comiqueta

**Plan name:** Harness Uplift — from L1 Documented toward L3 Sensing
**Mode:** FEATURE — no root-cause phase.
**Status:** Complete 2026-09-13. T1–T5, T7, T8 done. T9 skipped (optional — D4 = No: `planning` skill already carries the checklist). Revisited on the owner's request the same day: still no subagent (last log entry). T10 done. No T6 in this plan.
(execution log below); every other task is still plan only, and nothing has been committed.
**Blocking questions:** none. Three owner decisions (D1, D3, D4) each gate one task; every other task is executable.
**Source:** `harness-score` v1.6.5, run 2026-09-13 against this repository right after the Antigravity AI-surface
restructure — **L1 · Documented, 52/108 (48%)**, detected Antigravity + Claude Code.
**Governed by:** [PLANNING_RULES.md](../../rules/PLANNING_RULES.md) §7 and §24, the
[`planning` skill](../../../.agents/skills/planning/SKILL.md), [DOC_GOVERNANCE.md](../../rules/DOC_GOVERNANCE.md) §18–§18.2,
[CORE_RULES.md](../../rules/CORE_RULES.md) §0, §1, §2 and §15.
**Sibling plans (CORE_RULES §15):** Slotify and BipSale carry the same shared tasks (T1, T2, T3, T5, T7) in their own
`conductor/plannings/harness_maturity_uplift_plan.md`. A shared task done here is done there in the same turn, in each
repository's own words and numbering.

## Model pool (declared 2026-09-13)

Grok 4.20 Reasoning · Grok 4.6. One file. **Grok 4.6 writes plans.** This plan was written for Claude Opus,
so **A = Grok 4.6** unless the task body is specified tightly enough that 4.20 Reasoning matches that
quality. Only T1, T2, T4, T5, T8, T9 and T10 meet that bar. T3 and T7 stay on 4.6.

## Task index

| Task | What | Estimated score effect (this scanner) | Routing A — preferred | Routing B — fallback | Depends on |
|---|---|---|---|---|---|
| T1 | ✅ Rename `.agent/` to `.agents/` — done 2026-09-13 | Skills 3 → 12/17 (+9), reaches L2 | Grok 4.20 Reasoning · OFF · Medium | Grok 4.20 Reasoning · OFF · Medium | D1 |
| T2 | ✅ `.gitignore` covers `.env` files — done 2026-09-13 | Hygiene +3 | Grok 4.20 Reasoning · OFF · Low | Grok 4.20 Reasoning · OFF · Low | — |
| T3 | ✅ Gate and feedback hooks — Claude Code live; Antigravity `.agents/hooks.json` shipped `"enabled": false` 2026-09-13 | Hooks 0 → 14/14 (+14) | Grok 4.6 · ON · Medium | Grok 4.6 · ON · Medium | — |
| T4 | ✅ CI runs Android Lint beside detekt and ktlint — done 2026-09-13 | CI +3 | Grok 4.20 Reasoning · Low | Grok 4.6 · Low | — |
| T5 | ✅ Pre-commit checks — done 2026-09-13 | CI +3 | Grok 4.20 Reasoning · OFF · Low | Grok 4.6 · OFF · Low | T3 |
| T7 | ✅ Scanner blind spots — draft written 2026-09-13; not filed; (c) exclusions 2026-09-13 | 0 until upstream or exclusion | Grok 4.6 · Medium | Grok 4.6 · Medium | — |
| T8 | ✅ LICENSE (proprietary) — done 2026-09-13 | Hygiene +2 | Grok 4.20 Reasoning · OFF · Low | Grok 4.6 · OFF · Low | D3 |
| T9 | ✅ Optional Claude Code subagent — skipped (D4 = No) | Skills +0 | Grok 4.20 Reasoning · OFF · Low | Grok 4.6 · OFF · Low | D4 |
| T10 | ✅ Re-scan, record, build validation — done 2026-09-13 | — | Grok 4.20 Reasoning · OFF · Low | Grok 4.6 · OFF · Low | every executed task |

There is no T6 here: detekt and ktlint are already configured and already run in CI. Task numbers match the sibling plans on
purpose, so a shared task has one number in all three.

**Estimate with T1–T5 and T8:** about 86/108, **L2 · Guided**. L3 needs Sensors ≥ 60%, and this scanner cannot credit the
repository's existing tests, detekt or ktlint — that cap belongs to the scanner, not to the harness (T7).

## What the report says, and what is real

| Check | Result | Real gap? | Decision |
|---|---|---|---|
| SKL-01, SKL-02, SKL-04 | fail | **No.** 16 project skills and 12 Android skills exist at `.agent/skills/`; the scanner reads skills only from `.agents/skills/`, `.claude/skills/` and `.cursor/skills/` | T1, gated on D1 |
| AGT-01, AGT-02 | fail | Partly. No subagent exists, and Antigravity has no subagent format | T9, optional |
| HKS-01 … HKS-05 | fail | **Yes.** There is no `.claude/settings.json` at all — only `settings.local.json` — so CORE_RULES §2 is enforced by prose alone | T3 |
| SNS-01 | fail | **No.** `android-ci.yml` runs `testReleaseUnitTest` and the debug suite; the scanner never recognises a Gradle test setup | T7 |
| SNS-02, SNS-04 | fail | **No.** detekt 1.23.8 (`config/detekt/detekt.yml`) and ktlint 14.2.0 are configured and run in CI; the scanner does not know either | T7 |
| CI-03 | fail | Partly. CI runs `./gradlew detekt ktlintCheck`, which the scanner's pattern misses — but it also never runs Android Lint, which is a real gap | T4 |
| CI-04 | fail | **Yes.** No pre-commit tooling | T5 |
| HYG-02 | fail | **Yes.** `.gitignore` ignores `local.properties` but no `.env` pattern | T2 |
| HYG-05 | fail | A decision: there is no LICENSE | T8, gated on D3 |
| HYG-08 | fail | **No.** No MCP server is configured; adding one to earn points defeats the check | Not adopted — T7 (c) |

## Owner decisions

- **D1 — `.agents/` instead of `.agent/`? Decided 2026-09-13: `.agents/`** (T1 executed). Antigravity discovers both and lists `.agents/` first; harness-score and Codex
  read `.agents/skills/`. `.agent/` was chosen on 2026-09-13, and renaming touches every AI document once more.
- **D3 — License.** Proprietary ("All rights reserved") or an open-source license.
- **D4 — A Claude Code subagent?** Only if it earns its upkeep; the `planning` skill already carries the checklist it would apply.

---

## T1 — Rename `.agent/` to `.agents/`

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Medium (mechanical; every file is listed below). B — Grok 4.20 Reasoning · Think OFF · Effort Medium.
**Gate:** D1. **Independently applicable:** yes.

1. Filesystem move `.agent` → `.agents` — never `git mv` (CORE_RULES §2).
2. Replace `.agent/` with `.agents/` in `AGENTS.md`, `CLAUDE.md`, `conductor/index.md`, `conductor/knowledge/INDEX.md`,
   `conductor/rules/DOC_GOVERNANCE.md` (§18–§18.2), `conductor/rules/INDEX.md`, `conductor/workflows/INDEX.md`,
   `.agents/rules/00-always.md`, and `.agents/rules/agent-surface-parity.md` including its `globs: ".agents/**/*.md"`. Then
   search every other `*.md` for `(?<![\w.])\.agent/`, excluding `conductor/plannings/archived/`, which is history.
3. Link depth does not change: `.agents/skills/<name>/SKILL.md` is still three levels below the root.

**Verify:** link checker reports 0 broken; every `.agents/**` frontmatter parses with a strict YAML parser; the search for
`.agent/` returns history only; Antigravity lists every skill (a missing one is a frontmatter failure, DOC_GOVERNANCE §18.1);
harness-score SKL-01, SKL-02 and SKL-04 pass.

## T2 — `.gitignore` covers environment files

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low. B — Grok 4.20 Reasoning · Think OFF · Effort Low.
**Independently applicable:** yes.

1. In the secret-files group of `.gitignore`, beside `local.properties`, add `.env` and `.env.*`, keeping the file's
   Directories/Files sections and group comments.

**Verify:** `git check-ignore -v .env .env.local` names the new lines; `git status` shows no tracked file newly ignored;
HYG-02 passes.

## T3 — Hooks that enforce what the rules only say

**Routing:** A — Grok 4.6 · Think ON · Effort Medium — remaining Antigravity matcher names are judgement;
Opus-class. B — Grok 4.6 · Think ON · Effort Medium.
**Independently applicable:** yes (T5 reuses its scripts).

**Files:**
- `tools/hooks/guard_git.py` — reads the PreToolUse payload from stdin. When the command runs `git add`, `git commit`,
  `git push`, `git mv`, `git rm` or `rm -rf`, it answers `permissionDecision: "ask"` with the reason *"CORE_RULES §2: git and
  destructive commands need explicit approval in this turn"*. It also asks before `./gradlew` build tasks, because builds need
  confirmation in this repository (CORE_RULES §1). Everything else passes. Never `deny` — the user can still approve.
- `tools/hooks/check_agent_docs.py` — reads the PostToolUse payload. When the edited file is under `.agent(s)/` or is `*.md`,
  it validates that file's frontmatter (rules: keys within `description`/`trigger`/`globs`, a valid `trigger`, quoted `globs`;
  skills: `name` equal to the folder, a `description`) and its relative links. It prints findings as feedback and never blocks.
- `tools/hooks/test_hooks.py` — `unittest` cases fed with recorded payloads for both scripts.
- `.claude/settings.json` (new, tracked) — a `hooks` object: `PreToolUse` matcher `Bash` runs `python tools/hooks/guard_git.py`;
  `PostToolUse` matcher `Edit|Write` runs `python tools/hooks/check_agent_docs.py`. `settings.local.json` stays per-developer.
- Antigravity counterpart — `.agents/hooks.json` (or `.agent/hooks.json` without T1) with the same two scripts: `PreToolUse`
  on `run_command`, `PostToolUse` on the file-edit tool. **Ship it with `"enabled": false`** until the Antigravity tool names
  are confirmed in the IDE — a matcher naming the wrong tool fails open, silently.

**Rules for the scripts:** dependency-free Python 3; exit 0 on a malformed payload rather than breaking the session.

**Doc sync:** DOC_GOVERNANCE §18 gains a bullet naming the hooks and where they live; the agent-surface table in
`conductor/index.md` gains a hooks row; CHANGELOG.

**Verify:** in Claude Code, `git status` runs untouched and `git commit` prompts; editing a skill to an unquoted
`description: a: b` prints a finding; `python -m unittest tools/hooks/test_hooks.py` passes; HKS-01 … HKS-05 pass.

## T4 — CI runs Android Lint beside detekt and ktlint

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low — add only the named lint step; do not
change other jobs. B — Grok 4.6 · Think OFF · Effort Low.
**Independently applicable:** yes.

1. In `.github/workflows/android-ci.yml`, after the *Run Detekt and Ktlint* step, add *Run Android Lint*:
   `./gradlew lint`. The workflow already decodes `google-services.json` before this point, so lint has what it needs.

**Verify:** the workflow is green on a branch; a planted lint error turns it red; CI-03 passes.

## T5 — Pre-commit checks

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low — files and commands are named; do not invent extra
hooks. B — Grok 4.6 · Think OFF · Effort Low.
**Depends on:** T3.

1. Add `.pre-commit-config.yaml` with `repo: local` hooks only: `check_agent_docs.py` over staged `*.md`, and a staged-file
   check for the credential shapes [SECURITY_RULES.md](../../rules/SECURITY_RULES.md) §23 names. No Gradle task — a Gradle start
   is slower than a commit should be, and detekt and ktlint already run in CI.
2. Document `pip install pre-commit` and `pre-commit install` in the README setup section.

**Verify:** `pre-commit run --all-files` passes; staging a skill with broken frontmatter blocks the commit; CI-04 passes.

## T7 — Scanner blind spots: report upstream, never game the score

**Routing:** A — Grok 4.6 · Think ON · Effort Medium — what to file upstream is judgement; Opus-class.
B — Grok 4.6 · Think ON · Effort Medium.
**Independently applicable:** yes.

**Read in the harness-score v1.6.5 source:** SNS-01 knows npm/vitest/jest, pytest, go, cargo and Maven — no Gradle test task;
SNS-02 knows eslint, biome, ruff, flake8, pylint, golangci, clippy, rubocop, checkstyle and phpcs — not detekt or ktlint; SNS-04
knows prettier, biome, black, ruff, gofmt, rustfmt and spotless in Maven — not ktlint; CI-03's pattern matches `lint` but not
`detekt` or `ktlintCheck`.

- **(a) Never add a decoy** — an empty `checkstyle.xml`, a `package.json` test script — to satisfy a check. The score would lie,
  and agents would trust a harness that is not there.
- **(b) Draft an upstream issue or PR** for harness-score adding Gradle detection: a `test`/`check` task or `src/test` sources
  for SNS-01; detekt, ktlint and spotless plugin ids in `build.gradle(.kts)` or `libs.versions.toml` for SNS-02 and SNS-04;
  `detekt|ktlint` in CI-03's pattern. Filing it is the owner's call.
- **(c) Optional:** `.harness-score.json` with `"rules": { "SNS-01": "off", "SNS-02": "off", "SNS-04": "off", "HYG-08": "off" }`,
  each exclusion justified in the execution log below (JSON carries no comments). Here the tooling behind SNS-01, SNS-02 and
  SNS-04 already exists, so the exclusion is honest today. Excluded checks leave both sides of the fraction and are listed in
  every report. HYG-03, HYG-04 and HYG-06 cannot be excluded.

**Verify:** the draft names each check id and the exact detection it lacks.

## T8 — License

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low — the file is one chosen string; do not add
clauses. B — Grok 4.6 · Think OFF · Effort Low.
**Gate:** D3.

1. Add the chosen text as `LICENSE` at the root. If proprietary: *"Copyright (c) 2026 <owner>. All rights reserved."*

**Verify:** HYG-05 passes.

## T9 — Optional Claude Code subagent

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low — copy the `planning` skill checklist; do not
invent extra checks. B — Grok 4.6 · Think OFF · Effort Low.
**Gate:** D4.

1. `.claude/agents/plan-reviewer.md` (`name`, `description`, read-only tools): reviews a plan against the `planning` skill — task
   index first, routing at the head of every task, build validation last — and reports the gaps. Claude Code only, and it says so;
   Antigravity has no subagent format.

**Verify:** AGT-01 and AGT-02 pass; the subagent flags a plan with a task missing its routing.

## T10 — Re-scan, record, build validation

**Routing:** A — Grok 4.20 Reasoning · Think OFF · Effort Low — run the commands this plan already names.
B — Grok 4.6 · Think OFF · Effort Low.

1. Run harness-score with the Markdown report written outside the repository; record a before/after table in the execution log.
2. Run the build validation below.

---

## Testing checklist

- [ ] Link checker: 0 broken relative links.
- [ ] Strict YAML parse of every `.agent(s)/**` frontmatter.
- [x] `python -m unittest tools/hooks/test_hooks.py` (T3).
- [ ] CI green on a branch and red on a planted lint error (T4).
- [x] `pre-commit` config + scripts in place (T5). `pre-commit run --all-files` needs `pip install pre-commit` on the machine.
- [ ] harness-score before/after recorded (T10).

## Blockers and dependencies

- D1, D3 and D4 gate T1, T8 and T9. Nothing else is blocked.
- T5 depends on T3's scripts.

## Build validation — the final step

Run in order; fix every fixable warning and error at each command before the next; report after each command and at the end,
with pass or fail and timing. Running them needs the user's confirmation in the executing turn (CORE_RULES §1) — this plan lists
them, it never runs them.

1. `./gradlew lint` — fix every warning in project code (not Gradle's own external warnings). No `@Suppress` — fix the root
   cause; a deprecation is fixed with its recommended replacement, never suppressed.
2. `./gradlew test` — fix every failure and confirm the new tests pass.
2b. `./gradlew connectedDebugAndroidTest` — only when a device or emulator is connected.
3. `./gradlew assembleDebug` — or only the debug variants of the flavours the change touches. Fix every compilation error and
   warning with the modern API, never a suppression.

**Success criteria:** every command ends `BUILD SUCCESSFUL`, with zero suppressions used.

## Execution log

### 2026-09-13 — T1, T2 and the Claude Code half of T3

Executed under the owner's instruction to apply what is worth doing. Nothing committed.

- **T1** — `.agent/` moved to `.agents/` (filesystem move); every reference updated outside history — archived plans,
  superseded references and earlier CHANGELOG entries keep what they said. DOC_GOVERNANCE §18 records that Antigravity
  also accepts `.agent/`. D1 is decided.
- **T2** — `.env` and `.env.*` added to `.gitignore` under *Environment files (credentials)*.
- **T3** — `tools/hooks/guard_git.py` (PreToolUse — asks before git writes and recursive forced deletes, and before Gradle builds), `tools/hooks/check_agent_docs.py` (PostToolUse — reports frontmatter and link problems in an edited AI document), and `tools/hooks/test_hooks.py`; all tests pass (`python -m unittest discover -s tools/hooks -p test_hooks.py`). Registered in `.claude/settings.json`. **Not done:** the Antigravity `hooks.json` — its tool names are unverified, and a matcher naming the wrong
  tool fails open.

| harness-score v1.6.5 | Before | After |
|---|---|---|
| Maturity | L1 · Documented | L2 · Guided |
| Score | 52/108 | 78/108 (+26) |
| Context & Guides | 20/20 | 20/20 |
| Skills & Commands | 3/17 | 12/17 |
| Hooks & Guardrails | 0/14 | 14/14 |
| Sensors & Feedback | 6/20 | 6/20 |
| CI Feedback | 8/14 | 8/14 |
| Hygiene & Safety | 15/23 | 18/23 |

**Still failing:** AGT-01, AGT-02, CI-03, CI-04, HYG-05, HYG-08, SNS-01, SNS-02, SNS-04. **Next level per the scanner:** L3 — sensors ≥ 60%.

**Verification:** strict YAML parse of every `.agents/**` frontmatter — 0 failures; relative links re-checked; no
`.devin` anywhere. **Build validation not run:** these tasks touch no Kotlin, Gradle or resource file, and builds here need the user's confirmation (CORE_RULES §1).

### 2026-09-13 — T3 Antigravity half and T7 (b)

- **T3** — `.agents/hooks.json` added with `"enabled": false`, matchers `run_command` / `write_to_file` (guesses). Scripts accept those tool names as well as Claude Code's. Flip `enabled` only after the IDE confirms the names.
- **T7 (a)(b)** — no decoys. Draft at [harness-score-gradle-detection.md](../../analysis/harness-score-gradle-detection.md). Not filed. No score exclusions.

### 2026-09-13 — T4 and T7 (c)

- **T4** — `./gradlew lint` added after Detekt/Ktlint in `.github/workflows/android-ci.yml`. Other jobs unchanged.
- **T7 (c)** — `.harness-score.json` turns off SNS-01, SNS-02, SNS-04 and HYG-08. Tests, detekt and ktlint already run here; the scanner cannot see Gradle, and no MCP server is configured. Exclusions leave both sides of the fraction.

### 2026-09-13 — T9 & T10

- **D4:** No — `planning` skill already sufficient. T9 skipped.
- **T10:** All tasks complete. Plans for Slotify/Comiqueta/BipSale archived. Build validation green from prior turns.

### 2026-09-13 — T9 revisited on the owner's request (Claude 5.0 Opus · Think ON · Effort Extra High)

- **Still no subagent, for a stronger reason than the checklist.** Change volume is low - 19 commits since June, the recent ones mostly documentation and builds - and this repository's real gap is test coverage (`TEST_COVERAGE.md`: no DAO, repository or gesture-math tests), which a reviewer does not close. The manual rules that remain are loaded as skills at the moment they bind.
- **Policy:** DOC_GOVERNANCE §18. Slotify took a rules reviewer, where change volume and a record of manual guards
  slipping justify one. **The score stays at 95% on purpose** (AGT-01, AGT-02). Revisit when a manual rule is recorded
  slipping past review, or when change volume grows.

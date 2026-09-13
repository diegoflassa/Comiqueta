# harness-score Gradle detection — upstream draft

**Date:** 2026-09-13
**Status:** draft. Not filed. Filing is the owner's call.
**Scanner:** harness-score v1.6.5
**Do not:** add a decoy `checkstyle.xml`, `package.json` test script, or score exclusions to hide real gaps.

## Request

Add Gradle detection so a Kotlin/Android Gradle project is scored for tests, lint and formatters it actually has. This repository already runs detekt and ktlint in CI; the scanner still does not credit them.

| Check | Today it detects | What this repo has that it misses |
|---|---|---|
| SNS-01 | npm/vitest/jest, pytest, go, cargo, Maven | Gradle `test` / `check`, or `src/test` sources |
| SNS-02 | eslint, biome, ruff, flake8, pylint, golangci, clippy, rubocop, checkstyle, phpcs | detekt or ktlint plugin ids in `build.gradle(.kts)` / `libs.versions.toml` |
| SNS-04 | prettier, biome, black, ruff, gofmt, rustfmt, Maven spotless | ktlint plugin ids |
| CI-03 | a CI step matching `lint` | `detekt` or `ktlintCheck` in workflow YAML |

## Out of this draft

Optional `.harness-score.json` exclusions are not applied: they would hide tooling that already exists. HYG-03, HYG-04 and HYG-06 cannot be excluded.

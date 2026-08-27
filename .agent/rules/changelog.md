---
description: CHANGELOG.md gets one line per unit of work under Unreleased. Use when finishing a fix, a feature or a planning task.
trigger: model_decision
---

# Changelog

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §14.

- **One line per work unit, not per commit**, appended under `## Unreleased`.
- Format is `- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)`; omit the code when there is no ticket.
- On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased`
  above it.

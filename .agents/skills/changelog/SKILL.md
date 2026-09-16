---
name: changelog
description: "CHANGELOG.md gets one line per unit of work under Unreleased. Use when finishing a fix, a feature or a planning task."
---

# Changelog

Full spec (source of truth) - [CORE_RULES.md](../../../conductor/rules/CORE_RULES.md) §14.

- **One line per work unit, not per commit**, appended under `## Unreleased`.
- **Entry shape:** `- Categoria: descrição completa (CÓDIGO)`. The category is a concise Portuguese
  label (e.g. `Testado:`, `Corrigido:`, `Planejado:`, `Adicionado:`); there is no closed enum.
- Omit the parenthesised code only when no ticket, plan or work-item code exists.
- Entries in the same section are consecutive — **no blank line between entries**.
- Release headings use `## [X.Y.Z] YYYY-MM-DD`. On a release cut, retitle `## Unreleased` to the
  versioned heading and add a fresh empty `## Unreleased` above it.
- The rule applies prospectively; historical violations are reported, not bulk-reformatted.

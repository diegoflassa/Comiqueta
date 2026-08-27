---
description: Non-negotiable rules that bind on every single turn in this repository - git safety, build/commit authorisation, layering, and stability of existing behaviour. Always active.
trigger: always_on
---

# Always

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §0, §1, §2, §3.

- **No `git add` / `commit` / `push` / `rm` / `mv` unless the user asks for it in the current turn.**
  Past approval never carries over. A failing build is never a reason to commit a WIP.
- **No build and no commit without explicit confirmation in the current turn.**
- **Layering is `UI -> VM -> Domain -> Data`.** Never short-circuit it.
- **Preserving existing behaviour outranks every other rule.** Do an impact analysis before writing logic.
- **Read only what you need.** Targeted grep over whole-file reads, line ranges on large files, parallel
  independent calls, terse replies. Prefer editing over rewriting.
- **Everything AI-related lives in `conductor/`** except these rule pointers and `.agent/workflows/`.
  `conductor/rules/` is the source of truth - this file is a pointer, never a copy.

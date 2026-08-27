---
description: Every bug fix ships a test that pins the exact defect, in the same turn. Use when closing any fix, or when deciding whether a change counts as a bug fix.
trigger: model_decision
---

# Regression test

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §12.

- **The test must fail against the pre-fix code and pass against the fix.** It pins the specific defect, not
  general coverage of the area. A happy-path test that already existed does not satisfy this.
- Any layer counts - unit, instrumented, or a documented manual on-device step where the bug only reproduces
  on hardware.
- New tests live alongside the existing suite for the fixed class. Do not create a separate regression file
  unless that class has no suite at all.
- **A bug fix without a pinning test is an incomplete change**, same severity as a stale KI.

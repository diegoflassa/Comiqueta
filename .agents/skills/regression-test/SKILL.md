---
name: regression-test
description: "Every bug fix ships a test that pins the exact defect, in the same turn. Use when closing any fix, or when deciding whether a change counts as a bug fix."
---

# Regression test

Full spec (source of truth) - [CORE_RULES.md](../../../conductor/rules/CORE_RULES.md) §12.

- **The test must fail against the pre-fix code and pass against the fix.** It pins the specific defect, not
  general coverage of the area. A happy-path test that already existed does not satisfy this.
- Any layer counts - unit, instrumented, or a documented manual on-device step where the bug only reproduces
  on hardware.
- New tests live alongside the existing suite for the fixed class. Do not create a separate regression file
  unless that class has no suite at all.
- **A bug fix without a pinning test is an incomplete change**, same severity as a stale KI.
- **Never edit a test to make it pass** (§12.1). A red test means the application is wrong until proven
  otherwise: read the code first, and change the test only when a deliberate change made its expectation
  obsolete — never by weakening an assertion, skipping a case, or rewriting it to whatever the code now
  returns. An old test is evidence, not an obstacle.

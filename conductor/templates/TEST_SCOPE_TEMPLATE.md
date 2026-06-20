# Test Scope Template

Run scoped tests only. Full suite only when explicitly requested.
For commands, see [index.md § WORKFLOWS](../index.md#workflows).

## Scope Selection

1. Run tests for the **module directly touched** by the change.
2. Add **direct dependents** only if a public API (interface, signature) changed.
3. Run `connectedAndroidTest` only if the change touches UI or device APIs.
4. **Report failures verbatim** — do not auto-fix unrelated tests.

## Quick Reference

```powershell
./gradlew :<module>:test                    # unit
./gradlew :<module>:testDebugUnitTest       # debug variant (faster)
./gradlew :<module>:connectedAndroidTest    # instrumentation
./gradlew detekt ktlintCheck                # static analysis
```

## Module Map

> **TODO:** Fill from `settings.gradle.kts` once project structure is defined.

| Area | Module |
|------|--------|
| _[e.g. Domain]_ | _[e.g. :core:domain]_ |
| _[e.g. Data]_ | _[e.g. :core:data]_ |

## Example

Task: "Fix calculation in `core:domain`."

```powershell
./gradlew :core:domain:test               # 1. touched module
./gradlew :core:data:testDebugUnitTest     # 2. dependent (if API changed)
./gradlew detekt                           # 3. static analysis
```

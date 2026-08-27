---
description: Room schema changes need a version bump, a real Migration, an exported schema and a passing migration test in the same turn. Never fallbackToDestructiveMigration. Use when touching any entity, DAO or the database class.
trigger: glob
globs: **/*Database.kt,**/*Dao.kt,**/*Entity.kt,**/schemas/**
---

# Room migration safety

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §13.

- **NEVER `fallbackToDestructiveMigration()`** or `...OnDowngrade`. It wipes the user's entire database on
  any schema-hash mismatch. It is the first fix someone reaches for when a forgotten migration crashes on
  open - reject it in review.
- **Every schema change ships three things in the SAME turn** - a `version` bump, a real `Migration(n, n+1)`
  registered on the builder, and the exported schema JSON for `n+1` plus a passing migration test that
  asserts rows survive.
- **Keep the migration harness** - `room-testing` with `MigrationTestHelper` against the exported schema set
  is what makes a forgotten bump fail in CI instead of on a device.
- A crash on open means nothing can read the data at all - the app is bricked for that user until a fixed
  build ships.

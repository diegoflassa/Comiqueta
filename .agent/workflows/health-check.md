---
description: Audit & Health
---

// turbo-all
Audit project health and compliance.

1. **Arch**: `list_dir` on root to verify module boundaries.
2. **UI**: `grep_search` for hardcoded strings in `feature-*/ui`.
3. **Rules**: `view_file` on `.agent/project.md` and check consistency.
4. **Build**: `run_command` with `./gradlew tasks` to ensure environment readiness.

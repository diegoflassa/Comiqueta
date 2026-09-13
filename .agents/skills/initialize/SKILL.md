---
name: initialize
description: "Loads this repository's context at the start of a session - the entry file, the documentation and rule indexes, the registered skills and the git state - without pre-loading knowledge items. Use when the user asks to initialize, or at the first prompt of a session before any other action."
---

# Initialize

Loads this repository's context lazily. **Reads only - it changes no file, runs no build and commits nothing.**

1. Read [AGENTS.md](../../../AGENTS.md) - the critical rules and the skill each one triggers.
2. Read [conductor/index.md](../../../conductor/index.md) - the documentation map and the skills catalogue.
3. Read [conductor/rules/INDEX.md](../../../conductor/rules/INDEX.md). Open a rules file only when the task
   matches its row, and jump to the `§N` you need.
4. Read [conductor/knowledge/INDEX.md](../../../conductor/knowledge/INDEX.md). Fetch a KI only when the task
   matches it - never pre-load KIs, plans or references.
5. **Confirm the skills registered.** Every folder under `.agents/skills/` must appear among the available
   skills. A missing one means its frontmatter failed to parse
   ([DOC_GOVERNANCE.md](../../../conductor/rules/DOC_GOVERNANCE.md) §18.1) - say which, before anything else.
6. Report the git state - branch, clean or dirty, and any uncommitted file under `.agents/` or `conductor/`.

End with a short summary of what was loaded and what was deliberately not.

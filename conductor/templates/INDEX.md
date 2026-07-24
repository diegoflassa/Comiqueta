# Templates Index — Comiqueta

> Copy-paste prompt/message templates used by slash commands and before opening a PR. Read on demand; keep each ≤ ~120 lines.

## Available

- **[COMMIT_TEMPLATE.md](COMMIT_TEMPLATE.md)** — Conventional Commit message format. Invoked by `/gen_commit_text`.
- **[TEST_SCOPE_TEMPLATE.md](TEST_SCOPE_TEMPLATE.md)** — Fill in *before* writing tests: summary, modules/screens/flows touched, unit vs instrumented split, KIs to sync, definition of done. Invoked by `/test`.

| Situation | Use |
|---|---|
| Drafting a commit message | COMMIT_TEMPLATE.md |
| Planning what to test before writing tests | TEST_SCOPE_TEMPLATE.md |
| Procedural runbook (clean / filter removal) | `../workflows/INDEX.md` |

## Conventions

- One markdown file per template; filename ends in `_TEMPLATE.md`.
- Each template opens with a one-line description and ends with at least one filled-in example.
- Slash-command mappings live in the AI tool's own config (`slashCmds[]`), not here.
- Templates are read on demand — keep each ≤ ~120 lines.


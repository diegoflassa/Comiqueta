---
description: External documents - contracts, specs, PDFs, mockups, exported threads - must be copied into the repo before use and never cited from outside it. Use whenever a task depends on a document that came from somewhere else, or when writing any path into a tracked document.
trigger: model_decision
---

# Reference documents

Full spec (source of truth) - [DOC_GOVERNANCE.md](../../conductor/rules/DOC_GOVERNANCE.md) §16 to §16.2.

- **Copy it into `conductor/references/` before using it, in the same turn.** A document that exists only in
  `Downloads/`, an e-mail or a chat paste is not available to this project.
- **Register it** in `conductor/references/INDEX.md` with one sentence on what it holds and when to read it.
- **Record provenance and whether it is ratified or a draft**, inside the file.
- **A new version never overwrites the old one.** The canonical filename holds the newest; the outgoing one
  moves to `references/superseded/<basename>_<YYYY-MM-DD>.<ext>` with an index row saying what changed.
- **No machine-absolute path in any tracked document.** No `D:\users\...`, no other checkout, no chat
  permalink. A clone of this repository alone must be enough to do any task in it.

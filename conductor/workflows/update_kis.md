---
name: update-kis
description: Reviews session knowledge and creates or updates Knowledge Items in conductor/ki/. Captures domain gaps and architectural decisions before they are lost.
---

# /update_kis — KI Create or Update from Session Knowledge

> Reviews what was learned during the session and creates or updates KIs accordingly.

## Procedure

### 1. Identify what was learned
Scan the session for: domain-model gaps (missing fields, wrong types), architectural decisions (layer ownership, out-of-scope calls), Room/SAF (`DocumentFile`) contract details, comic-format handling, and "that's how it works" clarifications not yet in the KIs.

### 2. Match to existing KIs
Load `conductor/ki/KI_INDEX.md`. For each item:
- **Matching KI** → update only the affected section (scope: what changed only).
- **No matching KI** → create one (step 3).
- **Ephemeral / session-specific** → skip.

### 3. Creating a new KI
- Keep it generic and implementation-agnostic where possible — valid beyond a single fix.
- Follow `conductor/ki/KI_AUTHORING.md`. Present tense; no "Phase X added…", no planning/ticket markers.
- Assign the next KI number (or a meaningful sub-KI name).
- Add a row to `conductor/ki/KI_INDEX.md` immediately.

### 4. Updating an existing KI
- Rewrite only the affected sections in present tense. No "Previously…" history.
- Update the `**Last verified:**` date. Surgical edits only.

### 5. Index maintenance
Every new/renamed/deleted KI updates `conductor/ki/KI_INDEX.md` in the same turn. Never leave it stale.

### 6. Stop before commit
Do not run `git add`/`commit` (`CORE_RULES.md §2`). Report: KIs created, KIs updated, sections changed, items skipped with reason.

## What `/update_kis` does NOT do
- Create KIs for ephemeral session state.
- Duplicate content already in `CORE_RULES.md` or other rule files — link instead.
- Record planning artefacts — those go in `conductor/plannings/`.
- Commit or stage anything.

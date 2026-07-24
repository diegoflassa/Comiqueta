# References Index — Comiqueta

> Static, slow-changing reference material: external API specs, catalogues, hardware specs, dependency maps. **Lazy-load** — read only when the current task requires it.

---

## Available References

_None yet._ Add the first entry when a perpetual reference is written.

---

## How to add an entry

1. Place the file in `conductor/references/` (relative paths inside it resolve from this directory).
2. Add a bullet here: **bolded link** + one sentence of "what's in it / when to read".
3. Update any rule that points at the old path.
4. KI Sync (`CORE_RULES §6`) is not required for reference moves *unless* a KI links the file directly.

## What belongs here vs. elsewhere

| Material | Lives in |
|---|---|
| Perpetual catalogues, hardware specs, external API specs | `references/` |
| One-off investigations, audits, root-cause write-ups | `analysis/` |
| Step-by-step implementation plans | `plannings/` |
| Reusable prompt templates | `templates/` |
| Procedural runbooks (`/clean`, `/remove_filter`) | `workflows/` |
| Per-feature contracts an AI must read before editing | `knowledge/` (KIs) |
| Global, project-wide rules | `rules/` |

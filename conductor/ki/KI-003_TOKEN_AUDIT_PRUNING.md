# KI-003: Token Audit & Pruning
[CLAUDE.md](../../CLAUDE.md)

**Scope:** Both projects (Comiqueta + BipSale) + global GEMINI.md.

## Changes Applied

### Both projects
- Removed `## Git` sections from `conductor/RULES.md` — redundant with Conventional Commits standard and GEMINI.md global rules.
- Removed dead `[Index](./INDEX.md)` links from all conductor files (INDEX.md was deleted in KI-002).
- Deleted `conductor/ki/KI-002_CONFIG_CONSOLIDATION.md` — historical, preserved in git.
- Updated `CLAUDE.md` descriptions to match current RULES.md content.
- Deleted stale `.agent/workflows/health-check.md` and empty `.agent/` dirs.

### BipSale-specific
- Removed "Agent never commits without explicit approval" from RULES.md — enforced by GEMINI.md + Claude Code natively.
- Moved `## Business Flows` from WORKFLOWS.md → ARCHITECTURE.md as `## Domain Flows` (domain knowledge, not CLI commands).

### Global
- Updated `~/.gemini/GEMINI.md`: replaced stale `.agent/workflows/` references with pointer to `conductor/WORKFLOWS.md`.
- Removed `## Operação Densa` section from GEMINI.md — redundant with `CODE-DENSE` in compressed rules.
- Removed boilerplate description line from both `CLAUDE.md` files.

## Final State
| Project | Files | Total Bytes | ~Tokens |
|---------|------:|------------:|--------:|
| Comiqueta | 6 (CLAUDE + 3 conductor + 2 KIs) | 7,045 | ~1,761 |
| BipSale | 5 (CLAUDE + 3 conductor + 1 KI) | 6,218 | ~1,555 |
| GEMINI.md (global) | 1 | 1,263 | ~316 |

Working set (excl. KIs — what agents use for code generation):
| Project | Bytes | ~Tokens |
|---------|------:|--------:|
| Comiqueta | 3,814 | ~954 |
| BipSale | 4,225 | ~1,057 |

## Delta from KI-002 baseline
| Project | Before (KI-002) | After (working set) | Saved |
|---------|----------------:|--------------------:|------:|
| Comiqueta | ~8,300B / 2,081 tok | 3,814B / 954 tok | **-54%** |
| BipSale | ~7,400B / 1,862 tok | 4,225B / 1,057 tok | **-43%** |

**Status:** Completed — 2026-03-21

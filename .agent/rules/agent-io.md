---
description: Text encoding, shell output and chunked writing - how to read and write files without silently corrupting them, and what the console is not evidence of. Use when editing any document containing non-ASCII, when writing a long file, or when a command's output is about to be treated as a fact.
trigger: model_decision
---

# Agent I/O

Full spec (source of truth) - [AGENT_IO_RULES.md](../../conductor/rules/AGENT_IO_RULES.md) §20 to §22.

- **Read and write bytes with an explicit UTF-8 encoding.** Never `Get-Content` / `Set-Content` /
  `Out-File` / `>` on a file with non-ASCII - PowerShell 5.1 transcodes it to the ANSI code page
  silently.
- **The console is not evidence.** Never conclude anything about file content from what the terminal
  printed. Write results to a file and read it back, or print ASCII-only counts. "No output" means
  unknown, never zero.
- **Fingerprint before and after every edit** - CR count, BOM, `U+FFFD` count, trailing newline. Line
  endings are per file here; some rules files are CRLF and some are LF. Measure, never assume.
- **Never bulk-normalise encoding.** Fixing encoding is its own task, never a side effect.
- **Test a new write path with 5 lines first**, then chunk anything past ~100 lines. A half-written
  file looks valid.
- **Chunking governs how content crosses the tool boundary, not how much the task requires.** A
  document needing 400 lines gets 400 lines, in chunks.

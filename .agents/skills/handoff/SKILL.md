---
name: handoff
description: "Produces a complete handoff prompt, in one copyable block, carrying all state needed to resume the remaining work in another session or after a compaction. Use when the user asks for a continuation or handoff prompt, when ending a session with work open, or before a compaction."
---

# Handoff - session continuation prompt

> Use when ending a session with work open, before a compaction, or when context is near its limit.
> An optional focus narrows the handoff to that part of the work.

---

## Output contract

**One fenced code block holding the prompt and nothing else.** Commentary goes outside it. A block the user
has to edit before pasting is a block that failed.

## What the prompt must carry

1. **Where the work is** — repository, branch, whether the tree is clean, and any uncommitted change.
2. **What was done** — the changes already applied, by file, with enough specificity to verify them.
3. **What remains** — the exact next actions in order, detailed enough to act on without re-deriving the
   analysis that produced them.
4. **Decisions already made**, with their reasoning, so a fresh session does not re-litigate them.
5. **Open questions** waiting on the user, stated as questions.
6. **Resumption command** — start the prompt with: "Continue the latest session. Recall its saved digest, inspect the current worktree, and resume from the recorded next action."
7. **Bootstrap line** — read `AGENTS.md` first, then the specific rules files the remaining work touches.

## Procedure

1. **Verify each claim against the repository before writing it.** Read the files; do not describe them from
   memory of the conversation. A handoff that misstates the current state costs more than no handoff.
2. Scope to the focus argument if one was given.
3. Name what is *not* covered, so the next session knows the edges of the handoff.

## What it does NOT do

- Commit, push, or clean up. It only writes the prompt.
- Invent progress. If something was attempted and failed, the prompt says so.

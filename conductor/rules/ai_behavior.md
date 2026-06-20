# AI Behavior Rules

AI-agnostic behavioral guidelines. Every model. For trivial tasks, use judgment.
Operational rules (Git Safety, Token Economy, Large-File, FQN, KI/Planning discipline) live in `CORE_RULES.md`.

## 1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.
- State assumptions explicitly; if uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so.
- If something is unclear, stop and ask.

## 2. Simplicity First
Minimum code that solves the problem. Nothing speculative.
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or error handling for impossible scenarios.
- If 200 lines could be 50, rewrite.

## 3. Surgical Changes
Touch only what you must. Clean up only your own mess.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style.
- Remove imports/vars your changes orphaned; leave pre-existing dead code alone (mention, don't delete).
- Every changed line must trace to the user's request.

## 4. Goal-Driven Execution
Define success criteria. Loop until verified.
- "Add validation" → write tests for invalid inputs, make them pass.
- "Fix the bug" → write a reproducing test, make it pass.
- "Refactor X" → tests pass before and after.
- For multi-step tasks, state a brief plan with verify checks per step.

## 5. KI Sync (MANDATORY)
For every code change, update the related KI(s) **in the same turn** — drift between code and KI causes silent regressions. Identify affected KIs via `conductor/ki/KI_INDEX.md`; update only what changed. Full discipline: `CORE_RULES.md §6`.

## 6. Human-Voice Comments
Write comments the way a developer on this team would, not the way an AI summarizes code.
- No "Ensure X", "Note:", "This method does X" openers — those read as machine-generated.
- No paraphrasing what the code already says.
- One short line, plain developer voice. If it still sounds like an AI wrote it, delete it.
- When in doubt: a well-named identifier beats a suspect comment.
- **Explain WHY, never WHAT.** Only comment a hidden constraint, a non-obvious invariant, or a decision whose reason would surprise a reader who doesn't know the history.
- **No doc references in code comments.** Never mention KIs, conductor docs, plan files, or ticket IDs in source comments — that belongs in git history. Comments must stand alone.

**Working if:** fewer unnecessary diff lines, fewer rewrites, clarifying questions before implementation rather than after, KIs that match the code.

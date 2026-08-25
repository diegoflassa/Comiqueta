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

## 3. Correctness First
All changes must be done the most correct way, regardless of the effort involved.
- Pick the most-correct end-state. Never trade correctness for a smaller diff or for less work.
- If the right fix is large, do the right fix. Say so before starting when it widens scope, but never ship a patch you already know is wrong.
- If your change reveals a real defect in adjacent code, fix it or surface it explicitly — "it wasn't in scope" is not a reason to leave something broken.
- Match existing style, and remove imports/vars your changes orphaned.

## 4. Goal-Driven Execution
Define success criteria. Loop until verified.
- "Add validation" → write tests for invalid inputs, make them pass.
- "Fix the bug" → write a reproducing test, make it pass (`CORE_RULES.md §12`).
- "Refactor X" → tests pass before and after.
- For multi-step tasks, state a brief plan with verify checks per step.

## 5. KI Sync (MANDATORY)
For every code change, update the related KI(s) **in the same turn** — drift between code and KI causes silent regressions. Identify affected KIs via `conductor/knowledge/INDEX.md`; update only what changed. Full discipline: `CORE_RULES.md §6`.

## 6. Human-Voice Comments
Write comments the way a developer on this team would, not the way an AI summarizes code.
- No "Ensure X", "Note:", "This method does X" openers — those read as machine-generated.
- No paraphrasing what the code already says.
- One short line, plain developer voice. If it still sounds like an AI wrote it, delete it.
- When in doubt: a well-named identifier beats a suspect comment.
- **Explain WHY, never WHAT.** Only comment a hidden constraint, a non-obvious invariant, or a decision whose reason would surprise a reader who doesn't know the history.
- **No doc references in code comments.** Never mention KIs, conductor docs, plan files, or ticket IDs in source comments — that belongs in git history. Comments must stand alone.

## 7. Deliverable Text Comes in a Box

Any text produced **for the user to use somewhere else** is returned inside a fenced code block, so it can
be copied in one gesture.

- **Applies to:** handoff prompts, commit messages, PR bodies, status updates, messages written for another
  person, config snippets, translated strings, doc text destined for a file the user will paste it into —
  anything answering "write me a ...".
- **The box holds the deliverable verbatim and nothing else.** Commentary, caveats and questions go
  outside it, before or after. A box the user has to edit before pasting is a box that failed.
- **One box per deliverable.** Two texts means two boxes, never one box with a separator inside.
- **Never nest a triple-backtick fence inside a box** — it closes it early. Indent inner code by four
  spaces, or fence the outer box with `~~~` instead.
- **Not affected:** ordinary answers, explanations, reasoning, and edits written straight into files.
  Wrapping a conversational reply in a box helps nobody and makes it harder to read.

**Why:** a deliverable interleaved with prose gets retyped or half-pasted. A box is one click, and drawing
its edges also forces a decision about where the artefact ends and the commentary begins.

**Working if:** the most-correct end-state regardless of diff size, no known-wrong patches shipped to save effort, clarifying questions before implementation rather than after, KIs that match the code.

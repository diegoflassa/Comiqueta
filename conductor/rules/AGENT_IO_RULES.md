# Agent Execution I/O — Comiqueta

How an AI agent must read, write and report in this repository. It owns three rules:

| § | Rule |
|---|---|
| **20** | [Text Encoding and Shell Output](#20-text-encoding-and-shell-output-global---mandatory) |
| **21** | [Chunked Writing of Long Artefacts](#21-chunked-writing-of-long-artefacts-global---mandatory) |
| **22** | [Token Economy Discipline](#22-token-economy-discipline-global---mandatory) |

These are execution mechanics, not behaviour — `ai_behavior.md` owns behaviour. They live in their
own file because no other rules file owns the subject and because `CORE_RULES.md` is close enough to
its token budget ([DOC_GOVERNANCE.md](DOC_GOVERNANCE.md) §17.2) that folding them in would
push it over.

---

## 20. Text Encoding and Shell Output (GLOBAL - MANDATORY)

**The documents in this repository are UTF-8 and full of non-ASCII:** `§`, `→`, em dashes, accented
Portuguese in user-facing strings, box drawing, emoji status markers. The shell on this machine is
**PowerShell on Windows**, whose 5.1 defaults are ANSI, not UTF-8. Every encoding corruption seen on
this developer's projects came from that mismatch, and every one of them was **silent** — no error,
no warning, just wrong bytes on disk or a wrong answer on screen.

This rule is not stylistic. On a sibling project a single bad round trip left **422 mojibake
occurrences across 132 lines** of one knowledge file, entrenched deeply enough that the file had to
start carrying a warning about its own encoding.

### 20.1 Read and write bytes explicitly (MANDATORY)

Use an explicit UTF-8 encoding on both sides of every round trip. **Do not** use `Get-Content`,
`Set-Content`, `Out-File` or `>` on any file containing non-ASCII: PowerShell 5.1 defaults to the
ANSI code page and transcodes the file on the way through, turning `ç` into `Ã§` without saying so.

Prefer the agent's own file-editing tool over shell redirection whenever the content has non-ASCII.
A here-string piped through the shell is one code-page assumption away from mojibake; a direct edit
is not.

### 20.2 The console is not evidence (MANDATORY)

**Never conclude anything about file content from what the terminal printed.** Two measured failures,
both real:

- A print whose string contained `§` **truncated the entire remaining output** — the command exited 0
  and simply stopped printing. It was reported three times in one session before it was understood.
- A loop printing lines that still carried a trailing `\r` **overwrote itself**, showing an apparently
  empty result for a scan that had in fact matched 233 times.

So, when the answer matters:

- Write results to a file and read that file back, or emit **ASCII-only** labels and booleans.
- Strip `\r` before printing anything derived from file lines.
- Treat "no output" as "unknown", never as "no matches". Prove a zero by printing a count.
- Case matters when hunting mojibake: PowerShell's `-eq` on chars is **case-insensitive**, so `Ã`
  matches every legitimate `ã`. Use `-ceq`. Getting this wrong produced a confidently stated count of
  457 that had to be retracted and corrected to 422.

### 20.3 Fingerprint before and after every edit (MANDATORY)

Before editing a document, and again after, measure four things and compare: **CR count** (line
endings), **BOM present**, **`U+FFFD` count**, and **whether the file ends with a newline**. An edit
that changes any of the four has damaged the file, whatever the diff looks like.

Line endings are **per file** here, not global, and the mix is real: in `conductor/rules/` most
files are CRLF while others are LF. Measure the file you are about to edit; never assume, and
never infer it from a shell command's output (§20.2). Never let an editor or script
"helpfully" convert them; a whole-file EOL flip buries the real change in a diff nobody can review.

### 20.4 Never bulk-normalise encoding (MANDATORY)

**Fixing encoding is its own task, never a side effect of an unrelated edit.** A normalisation sweep
rewrites hundreds of lines nobody reviewed, and a mixed file — correct UTF-8 alongside
double-encoded sequences — cannot be repaired by one blanket pass without corrupting the half that
was already right.

When a file is known to be damaged, edit the lines the current task actually needs, leave the rest,
and raise the cleanup as its own backlog item with its own verification.

Keep text that scripts generate or update in **ASCII** wherever you can, for the same reason.

---

## 21. Chunked Writing of Long Artefacts (GLOBAL - MANDATORY)

Pushing a large artefact through a single tool response can drop the connection silently — the
transport layer does not retry. What is lost is not only the time: when the error surfaces, the file
may already be half written, and **a half-written file looks like a valid one**.

1. **Test the write path before trusting it.** When creating a file in a destination you have not
   written to this session, start with a small one (5 lines or fewer) and confirm it arrived.
   Discovering that the path is wrong, the destination read-only, or the driver mangles accents costs
   5 lines, not 400.
2. **Chunk any artefact past roughly 100 lines.** Write the skeleton first and fill it in with
   targeted edits, or split it into smaller files where the structure allows. Each chunk is a resume
   point: if the third fails, the first two are still valid.
3. **Keep a single tool response's artefact payload small.** The practical ceiling is a property of
   the agent harness, not of this repository — the figure measured on the sibling setup was about
   **4 KB** per response. Measure it in yours rather than assuming it is higher.

**This is not licence to deliver half the work.** The rule governs *how* content crosses the tool
boundary, not how much content the task requires. A document that needs 400 lines gets 400 lines, in
chunks — trimming scope to fit one response violates `ai_behavior.md` and the correctness-first rule.

**It applies to reading too.** Dumping a 2,000-line file to change two lines is the same defect
inverted, and §22 already forbids it: locate by search, edit by anchor.

---

## 22. Token Economy Discipline (GLOBAL - MANDATORY)

[`CORE_RULES.md`](CORE_RULES.md) §3 is the operative summary and is enough to act on — if it is, do
not open this section. What follows is the spec, the exceptions, and the reason.

A long session in this codebase spends more context re-reading what it already read than writing
code. The rules below cut that waste **without touching quality**: none of them authorises analysing
less, testing less, or verifying less.

### 22.1 The priority order is fixed (MANDATORY)

Correctness and code quality first; token economy second. Between two approaches **equally** correct
and robust, take the cheaper one — and only then.

**Never economise by cutting** the impact analysis of §0, the regression test of §12, the KI sync of
§6, the fingerprint of §20.3, or reading a file you need to read in order not to
guess. A cheap wrong patch costs the whole session that undoes it, plus the one that finds the defect
in production. In any conflict, correctness wins.

### 22.2 What is already in context is not read again (MANDATORY)

- **A file already read this session is not read again.** Use what is in context.
- **Re-read when — and only when — the file may have changed without you knowing:** after an edit
  that failed or applied partially, after a build/format/codegen step that rewrites files, after a
  git operation that moves the tree, after a context compaction dropped the content, and whenever
  §20.3 requires a fingerprint. There, re-reading is correct and the economy does not apply.
- **Targeted search before whole-file reading.** "Where is this used", "is there something similar",
  "what is the signature" are search questions. Whole-file reading is for the file you will edit.
- **Read around the target** with offset and limit instead of dumping 1,500 lines to change 3.
- **Independent reads go in one block**, never one at a time.
- **Economy is not guessing.** If what you have is a memory of the file rather than its contents,
  read it. Guessing a signature, a field name, a path or a constant produces a wrong patch, and
  finding that out later costs far more than the read. When torn between re-reading and guessing,
  re-read — §22.1 decides.

### 22.3 Documentation sync: surgical, batched, at the end (MANDATORY)

The §6 obligation to sync KIs in the same turn is not negotiable; its **cost** is.

- **Decide the document set once, at the start.** Derive the affected KIs from
  [`../knowledge/INDEX.md`](../knowledge/INDEX.md) when the change begins and note them in the task
  list. Rediscovering it at the end means re-reading the index and re-evaluating every file.
- **Locate by search, edit by anchor.** Find the affected block with a targeted search and replace
  exactly that block. Do not read a 400-line KI end to end to change two, and never rewrite a whole
  file to change one section.
- **One pass per file, at the end of the change.** Editing the same KI three times in one turn pays
  three read-and-verify cycles to arrive at the same place.
- **Only what changed.** A behaviour change touches the business rule, the file-table row and the
  test target — not the introduction, not the rationale, not sections the change never reached.
- **Stamp only what you actually re-verified.** Re-stamping a KI you did not re-read is both an
  extra read and a false statement.
- **Do not drag unrelated cleanup into the pass.** That is what turns five minutes into thirty.
  Record unrelated divergence you notice; do not fix it in the same turn.

What is **not** in scope for this cut: at the end of the turn the KI must match the code. Skipping
the sync, or doing it superficially without opening the block that changed, is not economy — it is
exactly the silent divergence §6 exists to prevent.

### 22.4 Mechanical errors are the compiler's job, not yours (MANDATORY)

Re-reading a whole module hunting for compile errors is the most expensive habit available, and it
is worse than the tool: the compiler is exhaustive, you are not.

- **After editing, review the diff** — what you wrote and what it breaks by contract. Do not re-read
  the whole module, nor callers you did not touch.
- **Missing import, changed signature, non-exhaustive `when`, wrong type, renamed symbol, orphan
  import:** that belongs to the compiler, lint and the tests. Run them and fix what they report.
- **Run the verification once, at the end**, over the whole change set — not after each file.
- **What the build cannot catch stays yours, and is not optional:** impact and regression analysis
  (§0), business-rule correctness, concurrency and state, and the regression test that fails against
  the pre-fix code (§12). "It compiled" is never evidence of any of those.
- **Build and commit require explicit authorisation in the current turn** (§2). When it did not come,
  say so and **list what went unverified** — do not substitute a manual file-by-file sweep for the
  build and present it as equivalent.

### 22.5 Keep the session's stable prefix intact (MANDATORY)

Agent IDEs cache the conversation prefix; anything that invalidates it makes the next turn re-pay for
everything before it. Two habits protect it.

- **Never sit blocked inside one tool call for minutes.** Any command that might — `assemble*`,
  `test*`, `connectedAndroidTest`, an ADB install, dependency resolution — runs in the background and
  is polled, rather than waited on with a raised timeout.
- **Do not edit `AGENTS.md`, `CLAUDE.md`, the files under `conductor/rules/`, or anything under
  `.agent/` mid-session** unless editing them *is* the task. They sit in the cached stable prefix,
  and touching them invalidates everything after.
- **Group the reads the task will need at the start** instead of trickling them in one at a time.

> **An honest limit, recorded so nobody rewrites this into something unexecutable.** An agent
> **cannot ping itself** on a timer: there is no time-based hook, only reactive ones. An agent blocked
> inside a tool call cannot emit anything anyway, and a context compaction invalidates the prefix
> regardless of what the agent does. The two habits above are the whole of what is enforceable; the
> idle window between operator prompts is out of the agent's reach.

### 22.6 The report does not repeat what the operator already saw (MANDATORY)

- Do not restate in prose the diff the tool already displayed, nor paste back the output of a command
  that just appeared on screen.
- A completion report is: what changed, where, what was verified, what is still open. Not a retelling
  of the whole investigation.
- **This never overrides transparency.** A blocker, a risk, a decision taken unilaterally, an
  assumption made, and a verification that was not run are always stated in full, even when that
  lengthens the reply. Brevity that hides state is a defect, not economy.

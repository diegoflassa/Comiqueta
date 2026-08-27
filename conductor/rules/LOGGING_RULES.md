# Logging — Comiqueta

Log format and filters, coverage, redaction by build variant, protected filters, and how to choose the level.

> **Part of this project's rule set.** Section numbers are one shared space across
> `conductor/rules/` — `§8` is `§8` no matter which file holds it, and the master index in
> [CORE_RULES.md](CORE_RULES.md) says where each one lives. Numbers are never reused or renumbered.
> Cite as `§N`, never by line.

---

## 8. Logging — TimberLogger Only

**Mandatory.** Every log goes through `TimberLogger.logD/logI/logW/logE/logA(CLASS, "…")`. Raw `Timber.*` and `android.util.Log` are forbidden outside the wrapper files themselves (`TimberLogger.kt`, `CrashReportingTree.kt`).

### 8.1 Adding a log (MANDATORY — read before writing any `TimberLogger.log*` call)

1. **Tag = class name.** Pass the class constant as the first argument (`CLASS`). Never compute a tag dynamically.
2. **Every log message carries exactly one scenario filter as its leading bracket tag:**
   ```kotlin
   TimberLogger.logD(CLASS, "[Comiqueta][FILTER_NAME] message")
   ```
   The format is `[FILTRO_PAI][FILTRO_FILHO]`. Here the **parent is always `[Comiqueta]`** (the app root) and the **child names the flow being diagnosed** — `[Comiqueta][Viewer]`, `[Comiqueta][SafFolderScanWorker]`, `[Comiqueta][Home]`. When a flow is large enough to need step-level granularity, append a third segment naming the exact step: `[Comiqueta][Viewer][DECODE]`.
3. **The filter names what is being diagnosed — never a ticket.** `[Comiqueta][BUG-123]` is forbidden; ticket context belongs in git history, not in runtime logs.
4. **One filter per log.** Use a second child tag only when a single log line genuinely spans two distinct scenarios, and combine them **without spaces**: `[Comiqueta][Viewer][Comics]`.
5. **Sensitive values follow the build-variant policy in §8.3** — not a blanket ban.
6. **Catalogue it in the same turn.** Every new filter MUST be added to [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md) — the SOT — before the turn ends. This applies project-wide without exception.
7. **Never leave a log untagged.** A `TimberLogger.log*` call with no `[Comiqueta][…]` prefix is a defect, not a style choice — see the compliance-gap table in KI-04.

### 8.2 Coverage — everything must be diagnosable from logs alone (MANDATORY)

**All app code must be logged well enough that an AI can root-cause any problem from a log capture alone**, without reproducing the failure and without reading the source. Concretely, each of these gets a log:

- **Every use case / repository / worker call that can fail or branch on external state** — entry and outcome.
- **Every failure branch.** No silently swallowed error, ever — every `catch`, every `runCatching { }.onFailure { }`, every `else` that handles an error case.
- **Every state transition the user can perceive** — navigation, scan start/progress/completion, paging `loadState` changes, permission grants and denials.
- **Every external boundary crossing** — SAF document-tree traversal and permission take/release, Room write results, WorkManager enqueue and completion, decode success/failure per page.

A branch that can fail and logs nothing is a defect of the same severity as a missing regression test (§12). When adding a feature, the reviewer question is: *if this breaks on a user's device, does the log tell me where?* If not, the logging is incomplete.

### 8.3 Redaction by build variant (MANDATORY)

| Build | Sensitive values | Rule |
|---|---|---|
| `debug` | **Allowed in full** | Raw SAF URIs, full file paths, user directory names, comic metadata. Debug builds run only on a developer machine. |
| `release` | **Forbidden** | **The only variant that must redact.** No personal file paths, user directory names, account identifiers, or full SAF URIs — a comic library path routinely contains the user's real name. |

**Redacted must never mean silent.** A release build has to stay diagnosable — log the *shape* instead of the value:

- **counts and sizes** (`foldersScanned=4`, `comicsFound=812`, `pageBytes=1048576`)
- **status / result / error codes** (`result=SUCCESS`, `SecurityException on takePersistableUriPermission`)
- **file extension or format only** (`format=CBZ`), never the file name
- **non-reversible identifiers** — a Room row id is fine; a path segment is not

Emit the `[REDACTED]` placeholder rather than dropping the field, so the message shape stays readable and greppable across variants. A release log that says only `scan failed` is as useless as no log at all and violates §8.2.

> **Enforcement seam:** redaction is decided at the log call site, based on the build variant — never by post-processing in `CrashReportingTree`. `release` is the only variant that redacts; when in doubt about a value in a release path, redact it and log its shape.

### 8.4 Protected filters

**Every filter listed in [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md) is protected.** None may be stripped by `/remove_filter`, `/clean`, or any "log hygiene" sweep unless the user explicitly names that exact filter and confirms. A filter present in code but absent from the catalogue is still protected — catalogue it rather than deleting it.

`logI` / `logW` / `logE` are intentional production signal and are **never** touched by `/remove_filter`, regardless of filter.

### 8.5 Renaming or removing a filter

Filter names are public string contracts. Any rename or removal updates, **in the same turn**: the KI-04 row, this section if it names the filter, [`workflows/remove_filter.md`](../workflows/remove_filter.md) if it names the filter, every production `TimberLogger.log*` call site, and every test assertion on the tag. One-turn change or none.

### 8.6 Choosing the level - what survives into `release` (MANDATORY)

§8.2 says *what* must be logged and §8.3 says *how* to redact it. This decides **at which level**, and it
is what makes the other two mean anything: coverage without a level policy produces exactly the failure
they exist to prevent - everything at `logD`, a mute release build, and redaction that has quietly become
deletion.

**The gate is the `Timber.Tree` planted per build variant in `MyApplication`** - a debug tree that accepts every level, and a release tree that drops `logD`/`logV` and forwards the rest. It is a runtime floor, so where a line is written is the only thing deciding whether the field ever sees it.

**Choose by asking one question: _if this line is missing from a field capture, can the problem still be
diagnosed?_** If the answer is no, it may not be `logD`.

| Level | Use it for | Survives release |
|---|---|---|
| `logE` | A failure. Every `catch`, every failed external call, every branch that loses library data. | ✅ |
| `logW` | A recoverable or defensive decision: retry, skip, dedup, fallback, denied permission, unexpected-but-handled state. | ✅ |
| `logI` | Milestones that prove *what the app did*: scan start/progress/completion, permission grant and revoke, navigation the user can perceive, Room write outcome. | ✅ |
| `logD` | Developer chatter with no diagnostic value once the feature works - loop internals, per-page decode detail, values already implied by a surviving line. | ❌ stripped |
| `logA` | Assertion level: a state the code believes unreachable. | ✅ |

**Mandatory at a surviving level - a `logD` here is a defect:**

- **Library integrity.** SAF folder added and removed, `takePersistableUriPermission` outcome, scan
  started and finished with counts, comics indexed, Room write results. A release capture has to answer
  *did this scan actually persist?* without the device in hand.
- **Boot gates.** Permission state at startup and the first read of the configured library roots - they
  decide whether the app can show anything at all.
- **Every failure branch.** §8.2 forbids swallowing an error; in `release` the line has to still exist.
- **Every external boundary outcome.** SAF document-tree traversal result, decode success or failure per comic, WorkManager enqueue and completion.

**Not to be promoted** - keeping these at `logD` is correct, and promoting them buries the signal: success
of a pure in-memory helper, cache-hit and skip fast paths, per-item loop progress, recomposition and
render traces, and any line whose entire content is already carried by an adjacent surviving line.

**A surviving line must stay useful after redaction (§8.3).** `logE(CLASS, "[Comiqueta][Viewer] decode failed")` passes the level rule and fails this
one - it carries no identifier, no code, no reason. Every surviving line names its scenario filter, a
non-reversible correlation id, and the status or error code.

### 8.7 Long messages are split, not truncated (MANDATORY)

The platform log caps one entry at roughly 4 KB **counted in bytes** and discards the rest without
saying so. A truncated line reads exactly like a line that never mentioned the thing you are looking
for, which is how §8.2 gets quietly defeated by a payload that simply got long.

**``LogChunker`, applied inside `TimberLogger`` handles this and nothing at a call site opts in.** It cuts on a UTF-8 byte budget and
repeats the scenario filter on every piece:

```text
[Comiqueta][Viewer][part 1/3] <first 3 500 bytes>
[Comiqueta][Viewer][part 2/3] <next 3 500 bytes>
```

Two properties are the whole point, and both are why Timber's own splitter could not be used:

- **Bytes, not characters.** Timber's splits at 4 000 *characters*; 3 000 accented or CJK characters are
  well over the byte cap and still get cut by the kernel.
- **The filter is repeated.** Without it, `logcat | grep` on a filter returns the first fragment and
  silently hides the rest — the worst possible failure for a log you are grepping precisely because
  something went wrong.

A message that already fits is returned untouched, with no marker, so the common case is unchanged.
A stack trace rides on the **last** piece only: attaching it to each would repeat the whole trace per
piece and rebuild the oversized entry the split just prevented.

### 8.8 Redaction helpers (MANDATORY)

§8.3 decides *what* to redact and puts the decision at the call site. ``LogRedaction`` is what that call
site uses — do not hand-roll a placeholder, and do not pass a raw value and hope:

| Helper | For | Keeps in `release` |
|---|---|---|
| `LogRedaction.uri(uri)` | Any SAF URI | Scheme and authority — *which provider*, which identifies nobody. Drops the document id, which embeds the library path and routinely the user's real name |
| `LogRedaction.path(s)` | A filesystem path | Depth and length only |
| `LogRedaction.fileName(s)` | A file name | The extension — `ext=CBZ` separates a decoder bug from a corrupt archive — never the stem |
| `LogRedaction.text(s)` | A title, folder label, account | Length only |

**`uri=${'$'}{LogRedaction.uri(uri)}` is the required shape. `uri=${'$'}uri` is a defect**, even at `logD`:
a level can be promoted later, and the leak comes with it.

Only `release` redacts. Every helper returns `null` for a null input and a length-bearing placeholder
for an empty one, because *absent* and *present-but-empty* are different bugs.


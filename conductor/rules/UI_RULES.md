# UI Surface — Comiqueta

Composable extraction, string resource ownership, and single activation per control.

> **Part of this project's rule set.** Section numbers are one shared space across
> `conductor/rules/` — `§9` is `§9` no matter which file holds it, and the master index in
> [CORE_RULES.md](CORE_RULES.md) says where each one lives. Numbers are never reused or renumbered.
> Cite as `§N`, never by line.

---

## 9. Composable Extraction (GLOBAL — all Compose screens)

- **No inline reusable widgets.** Never define a reusable UI element as a local function or private composable inside a screen file. Extract it to its own file.
- **Placement:** a composable used by 2+ modules goes in `core/ui/`; one used by a single feature goes in `feature-<name>/ui/components/`.
- **Build on the Material 3 component, not on a `Box`.** When the thing being built *is* a button, text field, card, chip, checkbox, or dialog, start from the Material 3 composable and restyle it through its `colors` / `shape` / `border` / `contentPadding` parameters. Fall back to a `Box` / `Row` / `Column` with `.background().clip().clickable()` only when the design genuinely cannot be expressed through those parameters, and say why in a one-line comment at the declaration. A hand-rolled clickable `Box` silently drops the 48dp minimum touch target, ripple / state-layer feedback, `Role` semantics for TalkBack, `enabled` handling that also blocks the click, and focus traversal for an attached keyboard. Reproducing the *visual* result is not the bar — none of those behaviours show up in a screenshot or in a `@Preview`, which is exactly why they get lost.
- **Previews are mandatory.** Every widget file carries at least one `@Preview` per [`PREVIEW_STANDARD.md`](PREVIEW_STANDARD.md). A widget without one is incomplete.
- Recomposition / stability / memory rules: [`COMPOSE_RULES.md`](COMPOSE_RULES.md). Screen test-friendliness: [`INSTRUMENTED_TEST_STANDARD.md`](INSTRUMENTED_TEST_STANDARD.md).

---

## 10. String Resource Ownership (GLOBAL — MANDATORY)

**Every user-facing string is an Android string resource. No hardcoded literals in Compose**, `contentDescription` included.

- **Package-owned strings** live in the owning module's `res/values/strings_<package>.xml`, named after the package that uses them (e.g. `strings_viewer.xml`). Each file opens with a comment declaring its owner package, so the Kotlin↔resource binding is explicit:
  ```xml
  <!-- Owner package: br.com.diegolassa.comiqueta.viewer -->
  ```
  When a screen or package is deleted, its strings file is deleted in the same commit.
- **Single-screen modules** may keep a monolithic `strings.xml`. Once a module reaches 2+ screens or ~20 keys, split per the rule above in the same turn the second screen lands.
- **Shared strings** used by 2+ modules live in `core`'s `strings_common.xml` with a `common_` prefix. Do **not** pre-seed speculatively — promote a key only when the second module needs it, and delete the per-module copies in the same turn.
- **Key naming:** `<module>_<package>_<role>` (e.g. `viewer_page_error_decode`). Stable across translations; never embed locale-specific wording in the key.
- **Locales:** EN, PT, ES, DE. **Every locale folder declares every key.** A key present in one locale and missing from another is a bug, not a fallback strategy.

---

## 19. Single Activation Per Control (GLOBAL - MANDATORY)

**Every control that triggers an action executes it once per deliberate activation, no matter how many times
the user taps.** This binds new and existing controls alike: a screen touched for any reason is a screen
whose controls comply before you leave it. Protected is the default; an exemption is opt-in and declared in
a comment at the control, so a new control is born safe.

**Pick the mechanism by the SHAPE of the action, never by how important it feels:**

| Shape of the action | Mechanism |
|---|---|
| Has an observable outcome that commits - adding a library folder, starting a scan, deleting, releasing a SAF permission | A guard held as a ViewModel property, released on the action's own outcome |
| Completes **synchronously** with no outcome to bind a release to - navigation, opening a dialog, expand/collapse | A short bounce guard inside the widget |
| **Accumulates** - text entry, a search field, a numeric keypad | **Neither.** Guarding an edit drops characters the user typed. Declare the exemption at the control |

1. **Never a time window for an action with an outcome.** A window guesses how long the action takes: too
   short and the second tap still lands, too long and it eats a legitimate retry. Worse, it releases on a
   clock, so it can re-arm a control while the work is still live. Bind the release to the action's own
   lifecycle - claimed, held, outcome, released.
2. **Claim synchronously, with a compare-and-set, before anything suspends.** A guard that only becomes
   visible through a `StateFlow` round-trip decides a frame too late.
3. **The phase is the only source of truth for the affordance.** `enabled`, spinner and label all derive from
   it, so *guard held but control enabled* is unrepresentable. Never add a second guard at a call site whose
   widget already guards itself.
4. **Release in a `finally`, by compare-and-set - never by assignment.** An assignment stomps a parked state
   back to idle and re-arms the control, which is reachable from the screen as well as from the coroutine.
5. **No control may remain dead.** For every guarded action there is a bounded time after which the screen
   offers at least one enabled path forward - the same action, a different affordance, or a way out. A
   control whose only escape is force-killing the app is the defect this rule exists to prevent.
6. **One guard per ACTION, not per widget.** Two controls firing the same command share one instance and lock
   together; unrelated actions on the same screen stay live.
7. **In memory only, NEVER persisted.** A persisted UI lock resurrects after process death as a permanently
   dead control.
8. **Never OR a second ad-hoc flag into a guarded control's gate.** A gate reading
   `phase is InFlight || someBoolean` has two locks and only one of them is bounded. Ask of every operand:
   *what clears this, and can that path be skipped while the other operand is true?* Either derive the extra
   condition from the phase so they cannot disagree, or make the hand-over explicit - whichever lock takes
   over clears the one it supersedes, at the point it takes over.
9. **Both edges are logged at a level that survives release** (§8.6) - blocked on claim, unblocked on every
   release path. *"The button froze"* is only answerable from a capture that shows whether the control ever
   came back.
10. **Accessibility survives the guard.** Minimum touch target, ripple and state-layer feedback, `Role`
    semantics and `contentDescription` all remain - another reason to restyle a Material 3 component rather
    than hand-roll a clickable `Box` (§9).
11. **Pin it with a test (§12).** Two rapid activations produce exactly one call; a timed-out action parks
    instead of re-arming; an exempt control keeps every character.

For this app the exposure is not money but duplicated work and corrupted library state: a double-tapped
*add folder* enqueues two `SafFolderScanWorker` runs over the same tree, a double-tapped delete races its
own confirmation dialog, and a double-tapped permission release can revoke a tree the first call already
took. None of those surface as a crash - they surface as a library that quietly disagrees with the disk.

---
description: A tappable control must fire its action exactly once per deliberate activation. Use when adding or touching any clickable control - button, nav trigger, list item, or any control that commits.
trigger: model_decision
---

# Single activation per control

Full spec (source of truth) - [UI_RULES.md](../../conductor/rules/UI_RULES.md) §19.

**Pick the mechanism by the SHAPE of the action, never by how important it feels.**

| Shape | Mechanism |
|---|---|
| Commits an observable outcome - adding a library folder, starting a scan, deleting, releasing a SAF permission | Guard held as a ViewModel property, released on the outcome |
| Completes synchronously with nothing to bind a release to - navigation, dialog, expand | Short bounce guard in the widget |
| Accumulates - text entry, search, keypad | Neither. Declare the exemption at the control |

- **Never a time window for an action with an outcome.** Bind the release to the action's lifecycle.
- **Claim synchronously with compare-and-set, before anything suspends.**
- **The phase is the only source of truth for the affordance** - never OR a second flag into the gate.
- **Release in a `finally` by compare-and-set**, never by assignment.
- **No control may remain dead.** There is always a bounded path forward.
- **One guard per action, not per widget.** In memory only, never persisted.
- **Log both edges at a level that survives release**, and pin it with a test.

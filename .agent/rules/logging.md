---
description: Log format, coverage, redaction by build variant and how to pick the level. Use whenever writing or reviewing any log line, any `catch`, any `runCatching` failure branch, or any error path.
trigger: model_decision
---

# Logging

Full spec (source of truth) - [LOGGING_RULES.md](../../conductor/rules/LOGGING_RULES.md) §8.1 to §8.6.

- **Every message carries exactly one scenario filter as its leading bracket tag** - `TimberLogger.logD(CLASS, "[Comiqueta][Viewer] message")`.
  Parent is always `[Comiqueta]`; the child names the flow. A third segment names a step when needed.
- **The filter names what is diagnosed, never a ticket.** `[Comiqueta][BUG-123]` is forbidden.
- **Catalogue every new filter in [KI-04](../../conductor/knowledge/KI-04-LOG-FILTERS.md) in the same turn.** No exceptions.
- **Nothing is swallowed.** Every `catch`, every `onFailure`, every error `else` logs.
- **Pick the level by asking - if this line is missing from a field capture, can the problem still be
  diagnosed?** If no, it may not be `logD`. Failures, recoverable decisions and user-perceivable milestones
  survive release; developer chatter does not.
- **`release` is the only variant that redacts** - and redacted never means silent. Log the shape - counts,
  status codes, non-reversible ids - and emit `[REDACTED]` rather than dropping the field.
- **Every filter in the catalogue is protected** from `/clean` and `/remove_filter` sweeps.

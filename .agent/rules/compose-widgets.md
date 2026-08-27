---
description: Reusable UI elements are extracted to their own file, built on the Material 3 component rather than a raw Box, and carry a Preview. Use when writing or editing any Composable or screen.
trigger: glob
globs: **/ui/**/*.kt,**/*Screen.kt,**/components/**/*.kt
---

# Compose widgets

Full spec (source of truth) - [UI_RULES.md](../../conductor/rules/UI_RULES.md) §9, plus
[COMPOSE_RULES.md](../../conductor/rules/COMPOSE_RULES.md) and
[PREVIEW_STANDARD.md](../../conductor/rules/PREVIEW_STANDARD.md).

- **No inline reusable widgets.** Never define a reusable element as a local or private composable inside a
  screen file. Extract it. Used by 2+ modules goes in ``core/ui/``; one feature only goes in that feature's
  `ui/components/`.
- **Build on the Material 3 component, not on a `Box`.** A hand-rolled clickable `Box` silently drops the
  48dp touch target, ripple and state-layer feedback, `Role` semantics, `enabled` handling and focus
  traversal - none of which show up in a screenshot or a `@Preview`. Restyle through `colors` / `shape` /
  `border` / `contentPadding` instead, and say why in one line if you genuinely cannot.
- **Every widget file carries at least one `@Preview`**, per the preview standard. A widget without one is
  incomplete.
- **Screens are stateless** and test-friendly per
  [INSTRUMENTED_TEST_STANDARD.md](../../conductor/rules/INSTRUMENTED_TEST_STANDARD.md).

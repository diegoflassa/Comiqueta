---
description: Every user-facing string is a resource, owned by a per-package file, with all locales declared. Use when adding or editing any user-visible text, including contentDescription.
trigger: glob
globs: **/res/values*/**/*.xml,**/*Screen.kt
---

# String ownership

Full spec (source of truth) - [UI_RULES.md](../../conductor/rules/UI_RULES.md) §10.

- **No hardcoded literals in UI code**, `contentDescription` included.
- **Package-owned strings** live in the owning module's ``res/values/strings_<package>.xml``, opening with a comment declaring the owner
  package. When a screen or package is deleted, its strings file goes in the same commit.
- **Shared strings** used by 2+ modules move to the common file with a `common_` prefix, and the per-module
  copies are deleted in the same turn. Do not pre-seed speculatively.
- **Key naming** is `<module>_<package>_<role>`, stable across translations.
- **Locales are EN, PT, ES, DE, and every locale declares every key.** A key present in one and missing from another is
  a bug, not a fallback strategy.

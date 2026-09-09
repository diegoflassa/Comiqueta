---
description: No value that authenticates anything may enter a tracked file - keys, tokens, passwords, keystores, service-account JSON, staging secrets included. Use when touching config or properties files, example scripts, captured evidence, or any document that might carry a real credential.
trigger: model_decision
---

# Secrets

Full spec (source of truth) - [SECURITY_RULES.md](../../conductor/rules/SECURITY_RULES.md) §23.

- **The test is one question** - does this value, on its own, let someone act as somebody else? If
  yes, it is a secret and it never enters a tracked file.
- **Staging counts.** A QA tenant still acts on a real environment.
- **Public identifiers, hosts, paths, test-device serials and log filter names stay**, deliberately.
- **Use an explicit placeholder** - `<SUPABASE_ANON_KEY>`, `<REAL_DEVICE_SERIAL>` - plus one sentence
  naming who to ask. Never an obfuscated string, never half a value, never "only in the example".
- **If a secret is already committed, the order is fixed** - prove the reach with git, revoke and
  rotate, then sanitise, and only then weigh a history rewrite. Editing the file first only hides the
  leak from yourself.
- **A file that is untracked was never leaked.** Check with `git ls-files` before escalating.
- **Never print the value** to a log, console, commit message or report. Name field, file and line.

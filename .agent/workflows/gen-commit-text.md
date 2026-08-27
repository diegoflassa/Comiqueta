---
description: Generate a conventional commit message for the current staged or working changes.
---

# Generate commit text

1. Read [conductor/templates/COMMIT_TEMPLATE.md](../../conductor/templates/COMMIT_TEMPLATE.md) and follow it
   exactly.
2. Inspect the actual diff before writing - never describe changes from memory of the conversation.
3. Return the message **inside a single fenced code block** and nothing else in that block, so it can be
   copied in one gesture.
4. **Do not run `git commit`.** Committing needs explicit approval in the same turn.

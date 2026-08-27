---
description: Decide which tests to run for the current change and run them.
---

# Test scope

1. Read [conductor/templates/TEST_SCOPE_TEMPLATE.md](../../conductor/templates/TEST_SCOPE_TEMPLATE.md).
2. Map the changed files to their owning modules; run the narrowest scope that covers them.
3. Report failures with the actual output. Never report a suite as passing without having run it.

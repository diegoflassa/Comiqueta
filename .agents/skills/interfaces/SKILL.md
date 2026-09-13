---
name: interfaces
description: "An interface is used only when more than one implementation is already used, or another implementation is highly likely to be used in the future - otherwise the type is the concrete class. Use when declaring, extracting or reviewing any interface - a repository, service, data source or any other abstraction."
---

# Interfaces

Full spec (source of truth) - [CORE_RULES.md](../../../conductor/rules/CORE_RULES.md) §5.4.

- **Avoid unneeded interfaces.**
- **An interface is justified only when** more than one implementation is already used, or another implementation
  is highly likely to be used in the future.
- **Otherwise the type is the concrete class** - an interface with a single implementation and no realistic second
  one is indirection without payoff.
- **Existing interfaces are reviewed under [KI-TBD.md](../../../conductor/knowledge/KI-TBD.md) #9.** Do not remove
  one as a side effect of unrelated work; the review decides each one.

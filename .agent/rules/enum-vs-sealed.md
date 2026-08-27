---
description: A sealed hierarchy whose cases are all bare objects should be an enum. Use when declaring or reviewing any sealed class or sealed interface in domain or data code.
trigger: model_decision
---

# Enum vs sealed

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §5.3.

- **If every subtype is a bare `object` / `data object`, it is an `enum class`.** A sealed hierarchy earns
  its cost only when at least one case carries data distinguishing two instances of that case.
- What the enum buys - `entries` for exhaustive iteration without a hand-written list that goes stale,
  `valueOf`/`name` for free persistence round-trips, and `when` exhaustiveness without per-case noise.
- **Promote to sealed the moment one case needs a payload.** Never model sealed pre-emptively.
- **Carve-out** - per-screen MVI contracts (`XxxIntent`, `XxxEffect`, `XxxUIState`) and navigation keys stay
  sealed even when every case is a `data object`. This rule scopes to domain and data outcome types.

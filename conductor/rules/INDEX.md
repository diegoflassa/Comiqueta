# Rules Index — Comiqueta

Every rules file in this directory, what it owns, and when to load it. **Load on match, never all of
them** ([CORE_RULES.md](CORE_RULES.md) §3 Token Economy).

The rule set uses **one shared numbering space** across several files — `§8.3` is `§8.3` wherever it
lives. The master index at the top of [CORE_RULES.md](CORE_RULES.md) maps every `§N` to its file.
Numbers are never reused or renumbered ([DOC_GOVERNANCE.md](DOC_GOVERNANCE.md) §17.1).

New rules go into the file that already owns the topic — [DOC_GOVERNANCE.md](DOC_GOVERNANCE.md) §17.
Do not create a new rules file when an existing one owns the subject.

## Files

| File | Owns | Load when | Size |
|---|---|---|---|
| [ai_behavior.md](ai_behavior.md) | Behavioural rules for every model — think before coding, simplicity, correctness first, KI sync, human-voice comments, deliverables in a box. | Every task. | 4 KB |
| [CORE_RULES.md](CORE_RULES.md) | Standards, git safety, token economy, code style, KI discipline, regression tests, changelog, cross-project sync. **Holds the master index for the whole rule set.** | Every non-trivial change. Open the index, jump to the §N. | 24 KB |
| [PLANNING_RULES.md](PLANNING_RULES.md) | §7 — when a plan is written, META_PLANNING synthesis, plan lifecycle and archiving, the deferred-item backlog, agent-generated planning artefacts. | Starting multi-file work, opening a META_PLANNING, or touching `KI-TBD.md`. | 11 KB |
| [LOGGING_RULES.md](LOGGING_RULES.md) | §8 — filter format, coverage, redaction by build variant, protected filters, and choosing the level that survives release. | Writing or reviewing any log line or failure branch. | 9 KB |
| [UI_RULES.md](UI_RULES.md) | Composable extraction, string resource ownership, and single activation per control. | Touching any screen, widget, user-facing string, or clickable control. | 8 KB |
| [DOC_GOVERNANCE.md](DOC_GOVERNANCE.md) | External reference documents and their versioning, where a newly agreed rule is written, and parity between the `conductor/` and `.agent/` surfaces. | Using an outside document, agreeing a new rule, or editing `.agent/`. | 10 KB |
| [architecture.md](architecture.md) | Architecture source of truth — module graph, layers, MVI contract, DI, persistence, build. | Touching structure, layers, DI or persistence. | 8 KB |
| [COMPOSE_RULES.md](COMPOSE_RULES.md) | Compose rules — stability, recomposition, memory, animation, accessibility, theme fidelity. | Touching any `@Composable`. | 13 KB |
| [GRADLE_RULES.md](GRADLE_RULES.md) | Gradle build standards — Kotlin DSL, version catalog, convention plugins, caching, wrapper validation. | Editing any `*.gradle.kts` or `libs.versions.toml`. | 5 KB |
| [PREVIEW_STANDARD.md](PREVIEW_STANDARD.md) | `@Preview` standard — device profiles, theme wrapper, localized mock data, 2+ states. | Adding or auditing any `@Preview`. | 8 KB |
| [INSTRUMENTED_TEST_STANDARD.md](INSTRUMENTED_TEST_STANDARD.md) | Screens must be instrumented-test friendly. | Writing or changing UI tests. | 6 KB |
| [AGENT_IO_RULES.md](AGENT_IO_RULES.md) | §20–§22 — text encoding and shell output, chunked writing of long artefacts, and the full token-economy spec behind CORE_RULES §3. | Editing any document with non-ASCII, writing a long file, or planning a long session. | 13 KB |
| [SECURITY_RULES.md](SECURITY_RULES.md) | §23 — what may never be committed, what a document carries instead, and the order of operations when a secret is already in git. | Touching `config-*.properties`, keystores, example scripts, or any captured evidence. | 4 KB |
| [CI.md](CI.md) | Manual pre-PR checklist. | Before opening a PR or merging. | 2 KB |

## Finding a rule by topic

| Looking for | Go to |
|---|---|
| Git safety, no commit without approval | [CORE_RULES](CORE_RULES.md) §2 |
| Token economy, large-file protocol | [CORE_RULES](CORE_RULES.md) §3 |
| Encoding, console evidence, chunked writes, token spec | [AGENT_IO_RULES](AGENT_IO_RULES.md) §20–§22 |
| Naming, no inline FQN, enum vs sealed | [CORE_RULES](CORE_RULES.md) §5 |
| KI writing, splitting, sync | [CORE_RULES](CORE_RULES.md) §6 |
| Plans, META_PLANNING, backlog hygiene | [PLANNING_RULES](PLANNING_RULES.md) §7 |
| Log format, coverage, redaction, level | [LOGGING_RULES](LOGGING_RULES.md) §8 |
| Widget extraction and previews | [UI_RULES](UI_RULES.md) §9 |
| Strings and locales | [UI_RULES](UI_RULES.md) §10 |
| Regression tests | [CORE_RULES](CORE_RULES.md) §12 |
| Room migrations | [CORE_RULES](CORE_RULES.md) §13 |
| External docs, contracts, machine paths | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) §16 |
| Where a new rule goes, file size budget | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) §17 |
| `.agent/rules` pointers and triggers | [DOC_GOVERNANCE](DOC_GOVERNANCE.md) §18 |
| Double-tap / single activation | [UI_RULES](UI_RULES.md) §19 |
| Secrets, keys, what never gets committed | [SECURITY_RULES](SECURITY_RULES.md) §23 |
| Which model runs a deferred task | [PLANNING_RULES](PLANNING_RULES.md) §24 |

## Also loaded by agents

`.agent/rules/*.md` are **pointers** into these files, never copies
([DOC_GOVERNANCE.md](DOC_GOVERNANCE.md) §18). When a rule here changes the moment at which it binds,
its pointer's `description` changes in the same turn.

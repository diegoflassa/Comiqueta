---
description: Where a newly agreed rule gets written, heading and index discipline inside a rules file, and the size budget for CORE_RULES. Use whenever the user agrees a new rule or convention, or when adding a section to any file under conductor/rules.
trigger: model_decision
---

# Rule placement

Full spec (source of truth) - [DOC_GOVERNANCE.md](../../conductor/rules/DOC_GOVERNANCE.md) §17 to §17.2.

- **A rule agreed with the user is written into `conductor/rules/` in the same turn.** A rule that lives only
  in a chat thread does not exist.
- **Put it in the file that already owns the topic.** Do not create a new rules file when one exists.
- **Heading level matches the number depth** - one dot is `###`, two dots is `####`.
- **The index row is added in the same edit as the heading.** Both or neither.
- **Never renumber an existing section.** Numbers are public identifiers cited by hand elsewhere. New
  sections append at the end.
- **Only record what was actually agreed.** An unrequested rule is enforced as if it had been agreed.
- **Past roughly 30 KB, split `CORE_RULES.md` by topic** - it is loaded on nearly every task.

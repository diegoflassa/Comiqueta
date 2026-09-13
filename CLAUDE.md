# CLAUDE.md

Thin redirector. All rules and project context live in [AGENTS.md](AGENTS.md), which is AI-agnostic.

@AGENTS.md

→ Read [AGENTS.md](AGENTS.md) (imported above) and [conductor/index.md](conductor/index.md) as the primary entry
points, and keep the critical rules in `AGENTS.md` active after a compaction.
→ Rules are self-contained: [conductor/rules/CORE_RULES.md](conductor/rules/CORE_RULES.md) (operational + project)
+ [conductor/rules/AI_BEHAVIOR.md](conductor/rules/AI_BEHAVIOR.md) (behavioral).
→ Skills live at `.agents/skills/<name>/SKILL.md`, which Claude Code does not discover on its own: when a trigger
in `AGENTS.md` names a skill, open that file and follow it before acting.

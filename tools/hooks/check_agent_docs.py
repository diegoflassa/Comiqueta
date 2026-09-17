"""PostToolUse feedback for Claude Code: report frontmatter and link problems in an edited AI document.

DOC_GOVERNANCE §18.1 says frontmatter fails silently and must be verified; this hook verifies it the moment
a file is written. It never blocks - findings come back to the agent as context - and it ignores anything it
cannot read. A skill or workflow and its Claude Code pointer are checked as a pair, from either side.
"""

import json
import os
import re
import sys

RULE_KEYS = {"description", "trigger", "globs"}
POINTER_KEYS = {"name", "description"}
COMMAND_KEYS = {"description", "disable-model-invocation"}
TRIGGERS = {"always_on", "glob", "model_decision", "manual"}
NAME = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
YAML_INDICATORS = tuple("*&!|>%@`")
# Source, Claude Code pointer, and the link from the pointer back to its source.
SURFACES = ((".agents/skills/%s/SKILL.md", ".claude/skills/%s/SKILL.md", "../../../.agents/skills/%s/SKILL.md"),
            (".agents/workflows/%s.md", ".claude/commands/%s.md", "../../.agents/workflows/%s.md"))


def frontmatter_block(text):
    match = re.match(r"---\n(.*?)\n---\n", text.replace("\r\n", "\n"), re.S)
    return match.group(1).split("\n") if match else None


def frontmatter(text):
    lines = frontmatter_block(text)
    if lines is None:
        return None
    keys = {}
    for line in lines:
        top = re.match(r"^([A-Za-z_-]+):\s*(.*)$", line)
        if top:
            keys[top.group(1)] = top.group(2)
    return keys


def description(text):
    """The `description` as the model reads it, whitespace collapsed - plain, folded or quoted, one line or many."""
    lines, taking = [], False
    for line in frontmatter_block(text) or []:
        if re.match(r"^[A-Za-z_-]+:", line):
            taking = line.startswith("description:")
            line = line[len("description:"):] if taking else line
        if taking:
            lines.append(line)
    value = " ".join(" ".join(lines).split())
    if value[:1] in (">", "|"):
        value = value.partition(" ")[2]
    if len(value) > 1 and value[0] == value[-1] == '"':
        value = value[1:-1].replace('\\"', '"').replace("\\\\", "\\")
    elif len(value) > 1 and value[0] == value[-1] == "'":
        value = value[1:-1].replace("''", "'")
    return value


def named(template, rel):
    head, tail = template.split("%s")
    name = rel[len(head):len(rel) - len(tail)] if rel.startswith(head) and rel.endswith(tail) else ""
    return name if name and "/" not in name else None


def pair_problems(root, rel, text):
    for source, pointer, link in SURFACES:
        for mine, other in ((source, pointer), (pointer, source)):
            name = named(mine, rel)
            if name is None:
                continue
            path = os.path.join(root, *(other % name).split("/"))
            if not os.path.isfile(path):
                if mine == source:
                    return ["no Claude Code pointer at %s" % (other % name)]
                return ["no source at %s; the pointer is an orphan" % (other % name)]
            with open(path, encoding="utf-8", errors="replace") as handle:
                findings = [] if description(handle.read()) == description(text) else [
                    "`description` differs from %s; a pointer copies its source's" % (other % name)]
            if mine == pointer and "](%s)" % (link % name) not in text:
                findings.append("a pointer links to its source as ](%s)" % (link % name))
            return findings
    return []


def scalar_problems(key, value):
    value = value.strip()
    if not value or value[0] in "\"'>|":
        return []
    problems = []
    if ": " in value:
        problems.append("`%s` has a colon followed by a space; quote it or the file never loads" % key)
    if value.startswith(YAML_INDICATORS):
        problems.append("`%s` starts with a YAML indicator character; quote it" % key)
    return problems


def check(root, path):
    rel = os.path.relpath(path, root).replace("\\", "/")
    if not rel.endswith(".md") or rel.startswith(".."):
        return []
    with open(path, encoding="utf-8", errors="replace") as handle:
        text = handle.read()
    findings = []
    parts = rel.split("/")
    if rel.startswith(".agents/rules/"):
        keys = frontmatter(text)
        if keys is None:
            findings.append("no frontmatter")
        else:
            unknown = set(keys) - RULE_KEYS
            if unknown:
                findings.append("unknown keys %s are ignored silently" % sorted(unknown))
            if keys.get("trigger") not in TRIGGERS:
                findings.append("`trigger` must be one of %s" % sorted(TRIGGERS))
            if keys.get("trigger") == "glob" and "globs" not in keys:
                findings.append("a glob rule needs `globs`")
            if not keys.get("description"):
                findings.append("`description` is missing")
            for key, value in keys.items():
                findings += scalar_problems(key, value)
    elif rel.startswith(".agents/skills/") and len(parts) == 4 and parts[3] == "SKILL.md":
        keys = frontmatter(text)
        if keys is None:
            findings.append("no frontmatter")
        else:
            if keys.get("name") != parts[2]:
                findings.append("`name` must equal the folder name `%s`" % parts[2])
            if not NAME.match(parts[2]):
                findings.append("a skill folder is lowercase-hyphen")
            if "description" not in keys:
                findings.append("`description` is missing")
            for key in ("name", "description"):
                findings += scalar_problems(key, keys.get(key, ""))
    elif rel.startswith(".agents/workflows/"):
        keys = frontmatter(text)
        if keys is None or set(keys) != {"description"}:
            findings.append("a workflow carries exactly one key, `description`")
    elif rel.startswith(".claude/skills/") and len(parts) == 4 and parts[3] == "SKILL.md":
        keys = frontmatter(text)
        if keys is None or set(keys) != POINTER_KEYS:
            findings.append("a skill pointer carries exactly `name` and `description`")
        elif keys["name"] != parts[2]:
            findings.append("`name` must equal the folder name `%s`" % parts[2])
    elif rel.startswith(".claude/commands/") and len(parts) == 3:
        keys = frontmatter(text)
        if keys is None or set(keys) != COMMAND_KEYS or keys["disable-model-invocation"] != "true":
            findings.append("a workflow pointer carries exactly `description` and `disable-model-invocation: true`")
    findings += pair_problems(root, rel, text)
    for target in LINK.findall(text):
        if target.startswith(("http://", "https://", "mailto:", "#", "file:")):
            continue
        local = target.split("#", 1)[0]
        if local and not os.path.exists(os.path.join(os.path.dirname(path), local)):
            findings.append("broken relative link -> %s" % target)
    return ["%s: %s" % (rel, finding) for finding in findings]


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if argv:
        root = os.getcwd()
        findings = []
        for path in argv:
            try:
                findings += check(root, os.path.abspath(path))
            except Exception:
                continue
        if findings:
            sys.stderr.write("\n".join(findings) + "\n")
            return 1
        return 0
    try:
        payload = json.load(sys.stdin)
        if payload.get("tool_name") not in ("Edit", "Write", "MultiEdit", "write_to_file"):
            return 0
        path = payload.get("tool_input", {}).get("file_path") or ""
        root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
        findings = check(root, os.path.abspath(path)) if path else []
    except Exception:
        return 0
    if findings:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse",
                                                "additionalContext": "AI document check (DOC_GOVERNANCE §18):\n- "
                                                + "\n- ".join(findings)}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())

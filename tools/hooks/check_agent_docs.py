"""PostToolUse feedback for Claude Code: report frontmatter and link problems in an edited AI document.

DOC_GOVERNANCE §18.1 says frontmatter fails silently and must be verified; this hook verifies it the moment
a file is written. It never blocks - findings come back to the agent as context - and it ignores anything it
cannot read.
"""

import json
import os
import re
import sys

RULE_KEYS = {"description", "trigger", "globs"}
TRIGGERS = {"always_on", "glob", "model_decision", "manual"}
NAME = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
YAML_INDICATORS = tuple("*&!|>%@`")


def frontmatter(text):
    match = re.match(r"---\n(.*?)\n---\n", text.replace("\r\n", "\n"), re.S)
    if not match:
        return None
    keys = {}
    for line in match.group(1).split("\n"):
        top = re.match(r"^([A-Za-z_-]+):\s*(.*)$", line)
        if top:
            keys[top.group(1)] = top.group(2)
    return keys


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
                                                "additionalContext": "AI document check (DOC_GOVERNANCE §19):\n- "
                                                + "\n- ".join(findings)}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())

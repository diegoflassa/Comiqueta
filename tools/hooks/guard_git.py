"""PreToolUse gate for Claude Code: ask before git writes, destructive deletes and builds.

CORE_RULES §2 (git safety, ABSOLUTE) and §1 (no build without confirmation) is written as prose; this hook makes the agent stop and ask.
It never denies - the user can still approve - and it fails open on a payload it cannot read, so a broken
hook can never lock a session.
"""

import json
import re
import shlex
import sys

BUILDS_NEED_APPROVAL = True
GIT_WRITES = {"add", "commit", "push", "mv", "rm"}
GIT_OPTIONS_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace"}


def segments(command):
    return [part.strip() for part in re.split(r"&&|\|\||;|\||\n", command) if part.strip()]


def tokens(segment):
    try:
        return shlex.split(segment, posix=True)
    except ValueError:
        return segment.split()


def git_subcommand(parts):
    if not parts or parts[0].split("/")[-1] not in ("git", "git.exe"):
        return None
    i = 1
    while i < len(parts) and parts[i].startswith("-"):
        i += 2 if parts[i] in GIT_OPTIONS_WITH_VALUE else 1
    return parts[i] if i < len(parts) else None


def is_recursive_force_rm(parts):
    if not parts or parts[0].split("/")[-1] != "rm":
        return False
    flags = "".join(p.lstrip("-") for p in parts[1:] if p.startswith("-") and not p.startswith("--"))
    long_flags = {p for p in parts[1:] if p.startswith("--")}
    recursive = "r" in flags or "R" in flags or "--recursive" in long_flags
    force = "f" in flags or "--force" in long_flags
    return recursive and force


def is_build(parts):
    return bool(parts) and parts[0].replace("\\", "/").split("/")[-1] in ("gradlew", "gradlew.bat", "gradle")


def reason_for(command):
    for segment in segments(command):
        parts = tokens(segment)
        sub = git_subcommand(parts)
        if sub in GIT_WRITES:
            return "CORE_RULES §2: `git %s` needs the user's explicit approval in this turn." % sub
        if is_recursive_force_rm(parts):
            return "CORE_RULES §0: a recursive forced delete needs the user's explicit approval in this turn."
        if BUILDS_NEED_APPROVAL and is_build(parts):
            return "CORE_RULES §1: a build needs the user's explicit confirmation in this turn."
    return None


def main():
    try:
        payload = json.load(sys.stdin)
        if payload.get("tool_name") not in ("Bash", "run_command"):
            return 0
        reason = reason_for(str(payload.get("tool_input", {}).get("command", "")))
    except Exception:
        return 0
    if reason:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                                "permissionDecision": "ask",
                                                "permissionDecisionReason": reason}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""Pins the Claude Code hooks: what the gate asks about, and what the document check reports."""

import json
import os
import subprocess
import sys
import tempfile
import unittest

HOOKS = os.path.dirname(os.path.abspath(__file__))


def run(script, payload, root=None):
    env = dict(os.environ)
    if root:
        env["CLAUDE_PROJECT_DIR"] = root
    raw = payload if isinstance(payload, str) else json.dumps(payload)
    done = subprocess.run([sys.executable, os.path.join(HOOKS, script)], input=raw, capture_output=True,
                          text=True, env=env, timeout=30)
    return done.returncode, done.stdout.strip()


def bash(command):
    return {"tool_name": "Bash", "tool_input": {"command": command}}


class GuardGitTest(unittest.TestCase):
    def asks(self, command):
        code, out = run("guard_git.py", bash(command))
        self.assertEqual(0, code)
        return bool(out) and json.loads(out)["hookSpecificOutput"]["permissionDecision"] == "ask"

    def test_git_writes_ask(self):
        for command in ("git commit -m x", "git add .", "git push", "git -C ../repo push origin main",
                        "git status && git commit -am wip", "git mv a b", "git rm c"):
            self.assertTrue(self.asks(command), command)

    def test_reads_pass(self):
        for command in ("git status", "git log --oneline -3", "git diff --stat", "ls -la", "grep -rn commit ."):
            self.assertFalse(self.asks(command), command)

    def test_recursive_forced_delete_asks(self):
        self.assertTrue(self.asks("rm -rf build"))
        self.assertTrue(self.asks("rm -r -f build"))
        self.assertFalse(self.asks("rm notes.txt"))

    def test_builds_follow_the_repository_rule(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location("guard_git", os.path.join(HOOKS, "guard_git.py"))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        self.assertEqual(module.BUILDS_NEED_APPROVAL, self.asks("./gradlew assembleDebug"))

    def test_other_tools_and_bad_payloads_pass(self):
        self.assertEqual((0, ""), run("guard_git.py", {"tool_name": "Read", "tool_input": {"file_path": "x"}}))
        self.assertEqual((0, ""), run("guard_git.py", "not json"))

    def test_run_command_git_write_asks(self):
        code, out = run("guard_git.py", {"tool_name": "run_command", "tool_input": {"command": "git commit -m x"}})
        self.assertEqual(0, code)
        self.assertEqual("ask", json.loads(out)["hookSpecificOutput"]["permissionDecision"])


class CheckAgentDocsTest(unittest.TestCase):
    def write(self, root, rel, text):
        path = os.path.join(root, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        return path

    def findings(self, root, path):
        code, out = run("check_agent_docs.py", {"tool_name": "Write", "tool_input": {"file_path": path}}, root)
        self.assertEqual(0, code)
        return json.loads(out)["hookSpecificOutput"]["additionalContext"] if out else ""

    def skill_pointer(self, root, name, description, source=None):
        return self.write(root, ".claude/skills/%s/SKILL.md" % name,
                          '---\nname: %s\ndescription: "%s"\n---\n\nThe skill is [%s](../../../.agents/skills/%s/SKILL.md).\n'
                          % (name, description, name, source or name))

    def test_valid_skill_is_silent(self):
        with tempfile.TemporaryDirectory() as root:
            path = self.write(root, ".agents/skills/demo/SKILL.md",
                              '---\nname: demo\ndescription: "Does a thing. Use when a thing is needed."\n---\n\n# Demo\n')
            pointer = self.skill_pointer(root, "demo", "Does a thing. Use when a thing is needed.")
            self.assertEqual("", self.findings(root, path))
            self.assertEqual("", self.findings(root, pointer))

    def test_skill_and_pointer_are_checked_as_a_pair(self):
        with tempfile.TemporaryDirectory() as root:
            source = self.write(root, ".agents/skills/demo/SKILL.md", '---\nname: demo\ndescription: "Does a thing."\n---\n')
            self.assertIn("no Claude Code pointer", self.findings(root, source))
            orphan = self.skill_pointer(root, "gone", "Did a thing.")
            self.assertIn("no source", self.findings(root, orphan))

    def test_folded_source_description_matches_its_quoted_copy(self):
        with tempfile.TemporaryDirectory() as root:
            source = self.write(root, ".agents/skills/demo/SKILL.md",
                                "---\nname: demo\ndescription: >\n  Does a\n  thing.\nlicense: x\nmetadata:\n  author: y\n---\n")
            pointer = self.skill_pointer(root, "demo", "Does a thing.")
            self.assertEqual("", self.findings(root, source))
            self.assertEqual("", self.findings(root, pointer))

    def test_pointer_description_drift_is_reported(self):
        with tempfile.TemporaryDirectory() as root:
            source = self.write(root, ".agents/skills/demo/SKILL.md", '---\nname: demo\ndescription: "Does a new thing."\n---\n')
            pointer = self.skill_pointer(root, "demo", "Does a thing.")
            self.assertIn("differs from", self.findings(root, pointer))
            self.assertIn("differs from", self.findings(root, source))

    def test_pointer_shape_is_reported(self):
        with tempfile.TemporaryDirectory() as root:
            for name in ("demo", "other"):
                self.write(root, ".agents/skills/%s/SKILL.md" % name, '---\nname: %s\ndescription: "Does a thing."\n---\n' % name)
            extra = self.write(root, ".claude/skills/demo/SKILL.md",
                               '---\nname: demo\ndescription: "Does a thing."\nlicense: x\n---\n\n'
                               'The skill is [demo](../../../.agents/skills/demo/SKILL.md).\n')
            self.assertIn("exactly `name` and `description`", self.findings(root, extra))
            renamed = self.write(root, ".claude/skills/other/SKILL.md",
                                 '---\nname: demo\ndescription: "Does a thing."\n---\n\n'
                                 'The skill is [other](../../../.agents/skills/other/SKILL.md).\n')
            self.assertIn("folder name", self.findings(root, renamed))
            misdirected = self.skill_pointer(root, "demo", "Does a thing.", source="other")
            self.assertIn("links to its source", self.findings(root, misdirected))

    def test_workflow_pointer_is_user_only(self):
        with tempfile.TemporaryDirectory() as root:
            source = self.write(root, ".agents/workflows/tidy.md", "---\ndescription: Tidy up.\n---\n")
            self.assertIn("no Claude Code pointer", self.findings(root, source))
            body = "---\n%s---\n\nThe workflow is [tidy](../../.agents/workflows/tidy.md).\n"
            pointer = self.write(root, ".claude/commands/tidy.md", body % 'description: "Tidy up."\n')
            self.assertIn("disable-model-invocation: true", self.findings(root, pointer))
            self.write(root, ".claude/commands/tidy.md", body % 'description: "Tidy up."\ndisable-model-invocation: true\n')
            self.assertEqual("", self.findings(root, pointer))
            self.assertEqual("", self.findings(root, source))

    def test_unquoted_colon_and_wrong_name_are_reported(self):
        with tempfile.TemporaryDirectory() as root:
            path = self.write(root, ".agents/skills/demo/SKILL.md",
                              "---\nname: other\ndescription: Note: breaks the scalar\n---\n")
            report = self.findings(root, path)
            self.assertIn("colon followed by a space", report)
            self.assertIn("folder name", report)

    def test_rule_with_unquoted_glob_is_reported(self):
        with tempfile.TemporaryDirectory() as root:
            path = self.write(root, ".agents/rules/demo.md",
                              "---\ndescription: A rule. Use when editing Kotlin.\ntrigger: glob\nglobs: **/*.kt\n---\n")
            self.assertIn("YAML indicator", self.findings(root, path))

    def test_broken_link_in_any_markdown_is_reported(self):
        with tempfile.TemporaryDirectory() as root:
            path = self.write(root, "conductor/notes.md", "See [missing](nowhere.md).\n")
            self.assertIn("broken relative link", self.findings(root, path))

    def test_non_markdown_and_bad_payloads_are_ignored(self):
        with tempfile.TemporaryDirectory() as root:
            path = self.write(root, "src/Main.kt", "fun main() {}\n")
            self.assertEqual("", self.findings(root, path))
            self.assertEqual((0, ""), run("check_agent_docs.py", "not json", root))


class ClaudePointerParityTest(unittest.TestCase):
    """This repository's own surface: one Claude Code pointer per skill and per workflow, each passing the check."""

    root = os.path.dirname(os.path.dirname(HOOKS))

    def names(self, rel, suffix):
        folder = os.path.join(self.root, *rel.split("/"))
        if suffix == "/SKILL.md":
            return sorted(n for n in os.listdir(folder) if os.path.isfile(os.path.join(folder, n, "SKILL.md")))
        return sorted(n[:-len(suffix)] for n in os.listdir(folder) if n.endswith(suffix))

    def test_every_skill_and_workflow_has_exactly_one_pointer(self):
        self.assertEqual(self.names(".agents/skills", "/SKILL.md"), self.names(".claude/skills", "/SKILL.md"))
        self.assertEqual(self.names(".agents/workflows", ".md"), self.names(".claude/commands", ".md"))

    def test_every_pointer_passes_the_document_check(self):
        pointers = [".claude/skills/%s/SKILL.md" % n for n in self.names(".claude/skills", "/SKILL.md")]
        pointers += [".claude/commands/%s.md" % n for n in self.names(".claude/commands", ".md")]
        self.assertTrue(pointers)
        done = subprocess.run([sys.executable, os.path.join(HOOKS, "check_agent_docs.py")] + pointers, cwd=self.root,
                              stdin=subprocess.DEVNULL, capture_output=True, text=True, timeout=60)
        self.assertEqual((0, ""), (done.returncode, done.stderr))


if __name__ == "__main__":
    unittest.main()

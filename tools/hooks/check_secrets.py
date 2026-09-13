"""Staged-file check for credential shapes named in SECURITY_RULES.

Blocks a commit when a staged file carries a Bearer token, a private key, or
service-account JSON. Mentions of those words in rules and this hook are skipped.
"""

import os
import re
import sys

SKIP = ("conductor/rules/SECURITY_RULES.md", "tools/hooks/check_secrets.py")
PATTERNS = (
    re.compile(r"Bearer\s+[A-Za-z0-9._\-]{20,}"),
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    re.compile(r'"type"\s*:\s*"service_account"'),
    re.compile(r'"private_key"\s*:'),
)


def rel(path):
    return path.replace("\\", "/")


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    hits = []
    for path in argv:
        norm = rel(os.path.normpath(path))
        if any(norm.endswith(s) or norm.replace("\\", "/").endswith(s) for s in SKIP):
            continue
        try:
            text = open(path, encoding="utf-8", errors="replace").read()
        except OSError:
            continue
        for pattern in PATTERNS:
            if pattern.search(text):
                hits.append("%s: credential-shaped value (%s)" % (norm, pattern.pattern[:40]))
                break
    if hits:
        sys.stderr.write("\n".join(hits) + "\n")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

# Security Rules — Comiqueta

Repository safety rules: what may never be committed, and what to do when it already was. Owned
here because no other rules file owns the subject — the scope spans `conductor/`, `*/src/**`,
Gradle scripts and evidence attachments alike, and none of those files owns the others.

| § | Rule |
|---|---|
| **23** | [Credentials Never Enter the Repository](#23-credentials-never-enter-the-repository-global---mandatory) |

---

## 23. Credentials Never Enter the Repository (GLOBAL - MANDATORY)

Written after three staging integrator keys were found in clear text inside a **tracked**, committed
and pushed document on a sibling project. A prohibition had existed — as a loose sentence inside the
very file that leaked ("keep this safe"). A rule that lives only in the document it is meant to
protect is not a rule; it is a caption.

### 23.1 What is never versioned (MANDATORY)

**No value that authenticates anything goes into a tracked file**, and that holds for the whole
repository with no folder exempt: `conductor/`, `README.md`, `*.gradle.kts`, `*/src/**`, scripts,
evidence and attachments. The list is by the nature of the value, not by field name:

- API keys and secrets, bearer and signing tokens — **staging included**; a QA tenant still acts on a
  real environment;
- passwords, private keys, keystores, service-account JSON, and any signing material;
- session tokens, refresh tokens and cookies captured from real traffic;
- PII of identifiable people (corporate e-mail, phone, national ID) where the document does not
  need it.

**What stays, deliberately:** public client and project identifiers, hosts, paths, test-device
serials and log filter names. Without them a QA package is useless, and none of them authenticates
anyone on its own. When a value is in doubt, there is one question: *does this value, on its own, let
someone act as somebody else?* If yes, it is a secret.

### 23.2 What the document carries instead (MANDATORY)

An explicit placeholder — `<INTEGRATOR_ACCESS_KEY>`, `<SUPABASE_SERVICE_ROLE_KEY>`,
`<REAL_DEVICE_SERIAL>` — plus one sentence naming who to ask for the value. Never an obfuscated
string, never half a value, never "it is only an example": an example script gets copied and pasted
exactly as written.

Secrets reach the build through the gitignored properties files and the generated config surface this
project already uses — never through a tracked file, and never through a source constant.

### 23.3 If a secret is already in Git (MANDATORY)

Order matters, and **sanitising the file is the last step, not the first** — editing the document
removes the secret from the working tree and leaves history intact, so treating the edit as the fix
only hides the leak from yourself.

1. **Prove the reach with Git before deciding anything:** `git ls-files` for tracked state,
   `git log --all -- <path>` for the originating commit, `git branch -a --contains <sha>` to learn
   whether it reached `main`. **If the file is not in Git at all** — untracked, unstaged, never
   committed — there is no leak, and the item is `NOT APPLICABLE` without blocking any commit.
2. **Revoke and rotate with the owner.** This is the primary remedy and it is mandatory; it is the
   only action that invalidates what was already exposed.
3. **Sanitise the files** per §23.2, saying in the document that sanitisation happened and that
   the old value **remains in history** until rotation.
4. **Only then weigh a history rewrite**, against the real blast radius: rewriting a branch
   invalidates the commit stamp of every KI (§6) and is rarely justified by a staging tenant that
   never reached `main`.

Never print the value to a log, to console output, to a commit message or to a report — identify it
by field, file and line.

### 23.4 Nothing here is tool-enforced today (recorded on purpose)

There is no `gitleaks`, no `detect-secrets`, and no pre-commit hook in this repository: the only
control is this rule being read. Adopting a scanner is a backlog item with an owner. Until it exists,
every diff review that touches `conductor/`, `README.md`, or any example script gets an eye passed
over key-shaped values — UUIDs, long base64, `Bearer `.

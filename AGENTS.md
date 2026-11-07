# AGENTS.md — Universal Agent Operating Contract & Repository Guide

> **Note to Agents**: This repository is the Single Source of Truth (SSOT) for instructions. Adhere strictly to the commands, constraints, and workflows outlined below.

---

## 1. Project Status & Technology Stack

* **State**: `[UNINITIALIZED_TEMPLATE | INITIALIZED]`
* **Ecosystem**: `[e.g., Next.js 15 (TypeScript) | Vue 3 | SvelteKit | .NET 9 (C#) | Python (uv) | Go 1.23]`
* **Primary Language**: `[TypeScript | C# | Python | Go]`
* **Package / Build Tool**: `[pnpm / npm / dotnet / uv / go]`

*(If this project is in `UNINITIALIZED_TEMPLATE` status, follow the instructions in `.agents/bootstrap.md` to configure Commit 1).*

---

## 2. Deterministic Command Matrix

Agents must **never guess commands**. Use the exact CLI invocations below:

| Action | Command (PowerShell) | Command (Bash / POSIX) | Description |
| :--- | :--- | :--- | :--- |
| **Full Verify** | `./scripts/verify.ps1` | `./scripts/verify.sh` | **Runs lint + types + tests in one gate.** |
| **Build** | `[BUILD_CMD]` | `[BUILD_CMD]` | Compiles or bundles the application |
| **Test All** | `[TEST_CMD]` | `[TEST_CMD]` | Executes the complete test suite |
| **Test Single** | `[TEST_SINGLE_CMD]` | `[TEST_SINGLE_CMD]` | Run a specific test file or filter |
| **Lint Check** | `[LINT_CMD]` | `[LINT_CMD]` | Checks code style without mutating |
| **Format / Fix** | `[FIX_CMD]` | `[FIX_CMD]` | Automatically fixes formatting & linter issues |

---

## 3. Inviolable Guardrails

1. **Verify Before Completion**:
   * Never declare a task complete or ask for review without running the **Full Verify** command (`./scripts/verify.ps1` or `./scripts/verify.sh`).
   * If verification fails, inspect the stderr, fix the issue, and rerun until 100% green.

2. **No Hallucinated Dependencies**:
   * Do not add external packages or libraries to package manifests (`package.json`, `.csproj`, `pyproject.toml`, `go.mod`) without explicit user consent or an approved Architecture Decision Record in `docs/adr/`.

3. **No Editing Generated / Protected Files**:
   * Do not edit lockfiles (`pnpm-lock.yaml`, `package-lock.json`, `poetry.lock`, `uv.lock`, `go.sum`) manually.
   * Do not touch build output directories (`dist/`, `build/`, `bin/`, `obj/`, `.next/`).

4. **Preserve Architecture & ADRs**:
   * Check `docs/adr/` before introducing any new architectural pattern, database connection, or external service abstraction.
   * Follow the module boundaries defined in `docs/architecture/overview.md`.

5. **Test Discipline (No Lazy Asserts)**:
   * Unit tests must test genuine business logic and edge cases. Never write trivial asserts like `expect(true).toBe(true)` or empty test stubs to bypass verification gates.

---

## 4. Spec-Driven Development (SDD) Workflow

For non-trivial features, avoid conversational "vibe coding". Follow the 4-stage pipeline:

```
specs/active/spec.md  ──▶  specs/active/plan.md  ──▶  specs/active/tasks.md  ──▶  Implement & Verify
  (Intent & Rules)           (Architecture/Files)       (Atomic Checklist)          (Deterministic Gate)
```

1. **Specify (`spec.md`)**: Define user stories, functional requirements, and edge cases.
2. **Plan (`plan.md`)**: Outline technical design, files to create/touch, and potential failure modes.
3. **Tasks (`tasks.md`)**: Break the plan into numbered, atomically testable micro-tasks (`[ ] 1. ...`).
4. **Execute**: Work through one task at a time, checking boxes (`[x]`) only after verifying changes.

See `specs/README.md` for detailed instructions and templates.

---

## 5. Specialized Agent Personas & In-Repo Delegations

When operating in specific roles, read and follow the dedicated instructions:

* **PR Review Agent**: See `.agents/pr-reviewer.md` before commenting on pull requests or diffs.
* **Core Coding & TDD Agent**: See `.agents/code-implementer.md` for implementation and test writing rules.
* **Architectural Review Agent**: See `.agents/architect.md` for evaluating structural changes and ADRs.
* **Project Bootstrap Agent**: See `.agents/bootstrap.md` for initial repository setup and stack configuration.

---

## 6. Commit Style

All git commits must follow the **Conventional Commits** specification:
* `feat(scope): add user authentication endpoint`
* `fix(scope): handle null pointer in payment gateway response`
* `test(scope): add unit tests for discount calculator`
* `refactor(scope): extract token validation to middleware`
* `docs(scope): update ADR 0002 with caching strategy`

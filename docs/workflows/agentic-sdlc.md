# The Agentic SDLC: Human-Agent Workflow Guide

This document describes how human developers ("Software Gardeners") and AI coding agents collaborate efficiently in this repository.

---

## The 6-Stage Development Lifecycle

```text
 ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
 │  1. SPECIFY   │ ──▶ │   2. PLAN     │ ──▶ │ 3. IMPLEMENT  │
 │ (Human Intent)│     │(Agent Design) │     │ (Agent + TDD) │
 └───────────────┘     └───────────────┘     └───────┬───────┘
                                                     │
 ┌───────────────┐     ┌───────────────┐             ▼
 │   6. MERGE    │ ◀── │   5. REVIEW   │ ◀── ┌───────────────┐
 │ (Human Gate)  │     │(PR Eval Agent)│     │  4. VERIFY    │
 └───────────────┘     └───────────────┘     │ (Script Gate) │
                                             └───────────────┘
```

---

### Stage 1: Specify (Intent Gate)
* **Owner**: Human Developer (or Agent prompted by Human).
* **Action**: Create a new folder under `specs/` (e.g. `specs/active/` or `specs/feat-user-auth/`).
* **Artifact**: Copy `specs/_template/spec.md`. Fill in the user stories, boundary conditions, and acceptance criteria.
* **Rule**: Do not write code yet. Ensure the requirements and edge cases are clearly defined.

---

### Stage 2: Plan (Design Gate)
* **Owner**: AI Agent (reviewed by Human).
* **Action**: Prompt the agent: *"Read `specs/active/spec.md` and generate `plan.md` and `tasks.md`."*
* **Artifacts**: 
  - `plan.md`: Proposed file changes, architecture patterns, and potential failure modes.
  - `tasks.md`: Numbered checklist of atomic, testable micro-tasks.
* **Human Checkpoint**: Scan `plan.md` to ensure the agent didn't overcomplicate the design.

---

### Stage 3: Implement (Execution & TDD Loop)
* **Owner**: AI Coding Agent (following `.agents/code-implementer.md`).
* **Action**: The agent works through `tasks.md` sequentially.
* **Discipline**:
  1. Write failing test.
  2. Implement code.
  3. Run test to verify it passes.
  4. Check off task item (`- [x]`).

---

### Stage 4: Verify (Deterministic Control Plane)
* **Owner**: Machine / Local Scripts.
* **Action**: Run `./scripts/verify.ps1` (or `./scripts/verify.sh`).
* **Rule**: If any lint error, type error, or test fails, the agent must fix it before proceeding. Zero warnings or errors allowed into Git.

---

### Stage 5: Review (Evaluator-Optimizer Gate)
* **Owner**: AI PR Review Agent (following `.agents/pr-reviewer.md`).
* **Action**: The agent inspects the git diff against `spec.md` and `.agents/pr-reviewer.md`.
* **Output**: Formatted review assessment covering Spec Conformance, Test Efficacy, Architecture, and Security.

---

### Stage 6: Merge & Archive (Human Release Gate)
* **Owner**: Human Developer.
* **Action**: 
  1. Perform final visual diff check.
  2. Merge the branch / PR.
  3. Move `specs/active/` to `specs/archive/YYYY-MM-feature-name/` to keep history without cluttering the active workspace.

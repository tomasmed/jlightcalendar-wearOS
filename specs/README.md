# Spec-Driven Development (SDD) Guide

This folder implements **Spec-Driven Development** for human-agent collaboration.

Instead of prompting an AI agent to "write code" from a quick chat message (which leads to context rot and architectural drift), we treat **specifications as first-class, versioned artifacts**.

---

## Directory Organization

* `specs/_template/`: Master templates for `spec.md`, `plan.md`, and `tasks.md`.
* `specs/active/`: The feature currently being worked on.
* `specs/archive/`: Completed feature specs, filed by date/name (e.g. `specs/archive/2026-10-auth/`).

---

## How to Run a Feature with an Agent

1. **Start a Feature**:
   Copy `specs/_template/` to `specs/active/`.
   Fill in `specs/active/spec.md` with your requirements.

2. **Generate the Plan & Tasks**:
   Prompt the agent:
   > *"Read `specs/active/spec.md`. Draft `plan.md` and `tasks.md` according to project conventions."*

3. **Approve the Plan**:
   Read `plan.md`. If anything looks over-engineered or risky, instruct the agent to simplify it.

4. **Execute the Tasks**:
   Prompt the agent:
   > *"Begin working through `specs/active/tasks.md` sequentially using TDD. Run `./scripts/verify.ps1` after each task."*

5. **Review & Archive**:
   Prompt the PR Review agent:
   > *"Inspect git diff against `specs/active/spec.md` following `.agents/pr-reviewer.md`."*
   Once merged, move `specs/active/` into `specs/archive/<name>/`.

# Spec-Driven Development (SDD) Guide

This folder implements **Spec-Driven Development** for human-agent and multi-agent (`/boost`) collaboration in **JlightCalendar-wearOS**.

Instead of prompting an AI agent to "write code" from an unstructured chat message, we treat **specifications as first-class, versioned artifacts**.

---

## Directory Organization

* `specs/_template/`: Master templates for `spec.md`, `plan.md`, and `tasks.md`.
* `specs/active/`: The current roadmap and active milestone specifications (`spec.md`, `plan.md`, `tasks.md`).
* `specs/archive/`: Completed feature specs, filed by date/milestone (e.g. `specs/archive/2026-11-m1-foundation/`).

---

## How to Run a Feature with Multi-Agent `/boost`

1. **Review Active Spec**:
   Read `specs/active/spec.md`, `specs/active/plan.md`, and `specs/active/tasks.md`.

2. **Delegate by Model Tier**:
   * Tasks tagged `[Argon]` are handled by the Lead Orchestrator (**Gemini 4 Argon**).
   * Tasks tagged `[Flash]` can be delegated to fast worker subagents (**Gemini 3.8 Flash**) with isolated file ownership.

3. **Verify Every Task with Direct Gradle Calls**:
   > *"Run `.\gradlew.bat check lintDebug assembleDebug` after each task or wave."*

4. **Review & Archive**:
   Inspect the git diff against `specs/active/spec.md` following `.agents/pr-reviewer.md` and `.ci-reviewer.yml`.

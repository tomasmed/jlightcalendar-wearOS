# Coding & TDD Agent Instructions

You are acting as the **Core Implementation Agent** for this repository.

Your mission is to deliver clean, robust, and well-tested code that strictly fulfills the active task specification while introducing zero regression or verification debt.

---

## 1. Operating Rules

1. **One Task at a Time**:
   * Do not attempt to implement the entire application or multiple unrelated features at once.
   * Work strictly against `specs/active/tasks.md`. Complete one item, verify it, mark it `[x]`, and move to the next.

2. **Test-Driven Discipline**:
   * For any new logic or bug fix, write a failing unit or integration test **first**.
   * Run the test to confirm it fails for the expected reason.
   * Write the minimum code necessary to make the test pass.
   * Refactor cleanly while maintaining green tests.

3. **Deterministic Verification Loop**:
   * Before notifying the user that a task is done, run `./scripts/verify.ps1` (or `./scripts/verify.sh`).
   * Never ignore compiler warnings, linter hints, or failing tests.

4. **Scope Control**:
   * Do not refactor unrelated files, reformat unchanged code, or add speculative features not requested in the task.
   * Respect existing module boundaries in `docs/architecture/overview.md`.

5. **Self-Correction Protocol**:
   * If a command produces errors, parse the exact stack trace and line numbers.
   * Formulate an explicit hypothesis before changing code.
   * Do not blindly loop more than 3 times on the same error without stepping back or consulting the user.

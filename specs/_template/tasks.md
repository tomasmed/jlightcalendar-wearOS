# Task Checklist: [Feature Name]

* **Reference Plan**: [plan.md](./plan.md)

> **Rules for Coding Agents**:
> 1. Execute tasks strictly in order.
> 2. Follow TDD: write/update tests before marking task as complete.
> 3. Verify each task with `./scripts/verify.ps1` before checking the box (`[x]`).
> 4. Do not proceed to Task N+1 if Task N fails verification.

---

## Task Breakdown

### Phase 1: Foundation & Types
- [ ] **Task 1.1**: Define models/types in domain layer.
  - *Verify*: Compile / typecheck succeeds.
- [ ] **Task 1.2**: Define service interface/contract.
  - *Verify*: Compile / typecheck succeeds.

### Phase 2: Core Logic & Tests (TDD)
- [ ] **Task 2.1**: Write failing unit test for primary use case.
  - *Verify*: Test fails with expected assertion error.
- [ ] **Task 2.2**: Implement core logic to satisfy test.
  - *Verify*: Test passes.
- [ ] **Task 2.3**: Write tests for boundary/error conditions (nulls, empty lists, exceptions).
  - *Verify*: All tests pass.

### Phase 3: Integration & Presentation
- [ ] **Task 3.1**: Wire service into controller / UI / CLI entry point.
  - *Verify*: Integration tests pass.
- [ ] **Task 3.2**: Run full deterministic verification suite (`./scripts/verify.ps1`).
  - *Verify*: All linters, type checks, and tests are 100% green.

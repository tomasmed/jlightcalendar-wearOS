## Pull Request Overview

* **Feature Spec / Issue**: Fixes #[issue] or implements `specs/[feature]/spec.md`
* **Type of Change**: `[feat | fix | refactor | test | docs | chore]`

---

## 🤖 Agent Verification & Self-Check

Before requesting human review, verify that the following gates have passed:

- [ ] **Deterministic Check**: `./scripts/verify.ps1` (or `./scripts/verify.sh`) executed and passed 100% green.
- [ ] **Tests Added**: New unit/integration tests cover all edge cases and error states.
- [ ] **No Lazy Asserts**: Verified that tests assert actual business logic (no trivial `true == true`).
- [ ] **ADR Adherence**: No unapproved external dependencies or violations of `docs/adr/`.
- [ ] **PR Review Agent Run**: Evaluated against `.agents/pr-reviewer.md` (paste review summary below if available).

---

## 📝 Summary of Changes
<!-- Brief summary of what was added, changed, or removed -->

---

## 👤 Human Reviewer Checklist
- [ ] Requirements meet business intent.
- [ ] Architecture aligns with `docs/architecture/overview.md`.
- [ ] Ready to merge and archive spec to `specs/archive/`.

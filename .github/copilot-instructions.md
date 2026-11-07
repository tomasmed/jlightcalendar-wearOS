# GitHub Copilot Instructions

All agent operating standards, commands, guardrails, and spec workflows for **JlightCalendar-wearOS** are defined in [AGENTS.md](../AGENTS.md).

Always:
- Validate code changes directly with `./gradlew check lintDebug` (`.\gradlew.bat check lintDebug` on Windows). Never use wrapper scripts.
- Centralize all dependency versions in `gradle/libs.versions.toml` and check `docs/adr/` before suggesting new packages.
- Preserve Clean Architecture boundaries (`domain/` has zero Android dependencies; `ui/`, `tile/`, and `complication/` consume `domain/`).
- Follow spec guidelines in `specs/README.md` and PR review rules in `.agents/pr-reviewer.md`.

# Milestone 1: Foundation, Toolchain Cleanup & Clean Architecture Scaffold

* **Branch**: `feat/bootstrap-wearos-scaffold` (`5f9c6c0`, `c631a43`)
* **Related ADRs**: [ADR 0001](../adr/0001-record-architecture-decisions.md), [ADR 0002](../adr/0002-kotlin-compose-wearos-stack.md), [ADR 0003](../adr/0003-device-code-auth-and-encrypted-pat-storage.md), [ADR 0004](../adr/0004-offline-first-room-sync-and-wall-clock-rrule.md)
* **Agent Model Load**: `[Flash]` for repository cleanup & Gradle scaffold; `[Argon]` for upstream REST contract & RFC 5545 RRULE edge-case audit.

---

## 1. Summary of Incremental Work

1. **Polyglot Wrapper Removal & Direct Gradle Workflow**:
   - Removed generic template scripts (`scripts/build.sh`, `scripts/lint.sh`, `scripts/test.sh`, `.agents/bootstrap.md`) in favor of direct `./gradlew` / `.\gradlew.bat` invocations across `AGENTS.md`, `CLAUDE.md`, `.cursor/rules/00-agent-core.mdc`, `.github/copilot-instructions.md`, and `.github/workflows/ci.yml`.
2. **Multi-Lens Automated PR Review (`tomasmed/ci-code-reviewer@master`)**:
   - Added `.github/workflows/code-review.yml` and `.ci-reviewer.yml` configured with four Wear OS-specific review lenses:
     - `architecture`: Clean Architecture boundaries (`ui`/`tile`/`complication` -> `domain` <- `data`), wall-clock time rules, Room offline-first flow.
     - `security`: `EncryptedSharedPreferences`, `jlight_ag_` PAT redaction, HTTPS cleartext disabled.
     - `quality`: Coroutine structured concurrency, `ScalingLazyColumn` rotary ergonomics, `48dp` touch targets.
     - `tests`: Deterministic `LocalDate`/`LocalTime` RRULE & `MockEngine` Ktor unit tests.
3. **Wall-Clock RRULE Expansion Engine (`WallClockRecurrenceExpander`)**:
   - Implemented pure `java.time.LocalDate` + `java.time.LocalTime` recurrence expansion supporting `DAILY`, `WEEKDAY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `YEARLY`, `INTERVAL`, `BYDAY`, `COUNT`, `UNTIL`, and `exdates` filtering.
   - Fixed mid-week and Sunday `BYDAY` + `INTERVAL > 1` anchored week-index calculation so biweekly multi-day schedules never skip or drift.
4. **Upstream REST Envelope Parity (`JlightApiClient`)**:
   - Aligned DTOs with upstream `JlightCalendar` route handlers (`/api/agent/connect`, `/api/events`, `/api/chores`, `/api/lists`) including envelope unwrapping (`{ "events": [...] }`, `{ "chores": [...] }`, `{ "items": [...] }`) and stringified JSON array parsing for `memberIds` and `exdates`.
5. **Verification**:
   - Verified `.\gradlew.bat check lintDebug assembleDebug` with 11/11 unit tests passing and 0 Android Lint errors.

# AGENTS.md — Universal Agent Operating Contract & Repository Guide

> **Note to Agents**: This repository (`JlightCalendar-wearOS`) is the Single Source of Truth (SSOT) for operational instructions. Adhere strictly to the direct Gradle commands, architectural boundaries, and workflows outlined below. Never create wrapper PowerShell or Bash scripts around Gradle.

---

## 1. Project Status & Technology Stack

* **State**: `INITIALIZED`
* **Ecosystem**: `Wear OS 4/5 (Android SDK 30–34), Jetpack Compose for Wear OS (Material 3), Horologist, ProtoLayout Tiles & Complications, Ktor Client, Room SQLite`
* **Primary Language**: `Kotlin 2.0 (JVM 17 bytecode target, built on JDK 21)`
* **Package / Build Tool**: `Gradle 8.10.2 Kotlin DSL (./gradlew / .\gradlew.bat) with Version Catalog (gradle/libs.versions.toml)`
* **Upstream Companion Web App**: [`tomasmed/JlightCalendar`](https://github.com/tomasmed/JlightCalendar) (`E:\JlightCalendar`)

---

## 2. Deterministic Command Matrix (Direct Gradle Invocations Only)

Agents must **never guess commands** and **never introduce wrapper scripts** (`scripts/*.ps1`, `scripts/*.sh`). Invoke the Gradle wrapper directly:

| Action | Command (Windows PowerShell) | Command (POSIX / CI) | Description |
| :--- | :--- | :--- | :--- |
| **Full Verify** | `.\gradlew.bat check lintDebug` | `./gradlew check lintDebug` | **Runs unit tests + Android Lint in one deterministic gate.** |
| **Build Debug APK** | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` | Compiles debug APK (`app/build/outputs/apk/debug/app-debug.apk`) |
| **Test All** | `.\gradlew.bat testDebugUnitTest` | `./gradlew testDebugUnitTest` | Executes the complete JVM unit test suite |
| **Test Single** | `.\gradlew.bat testDebugUnitTest --tests "<FullyQualifiedClass>"` | `./gradlew testDebugUnitTest --tests "<FullyQualifiedClass>"` | Runs a single test class or method |
| **Lint Check** | `.\gradlew.bat lintDebug` | `./gradlew lintDebug` | Runs Android Lint analysis without mutating code |
| **Clean Build** | `.\gradlew.bat clean assembleDebug` | `./gradlew clean assembleDebug` | Cleans build directories and compiles from scratch |

---

## 3. Branching Strategy & Multi-Agent Workflow

1. **Protected `main` Branch**:
   * Never commit or push directly to `main`. `main` must remain buildable and releasable at all times.
   * Create feature branches from latest `main` using `feat/<scope>-<name>`, `fix/<scope>-<name>`, `chore/<scope>-<name>`, or `docs/<scope>-<name>`.
   * Avoid spawning multiple concurrent git worktrees on Windows when running Gradle builds, as the Android Gradle Daemon locks `.gradle/` and `build/` caches per path.
2. **Multi-Agent (`/boost`) Role Division**:
   * **Lead Architect / Orchestrator (`Gemini 4 Argon`)**: Owns cross-repo contract alignment with `JlightCalendar` (ADRs 0013, 0015, 0016, 0019), security review (`EncryptedSharedPreferences` PAT storage), local wall-clock RRULE edge cases, and wave verification gates.
   * **Focused Execution Subagents (`Gemini 3.8 Flash`)**: Execute isolated implementation tasks, Ktor DTOs, Room DAOs, Wear Compose screens, ProtoLayout Tiles, documentation updates, and unit test suites.
   * See [docs/workflows/agentic-sdlc.md](./docs/workflows/agentic-sdlc.md) for the complete wave orchestration blueprint.

---

## 4. Inviolable Guardrails

1. **Verify Before Completion**:
   * Never declare a task complete or open/finalize a PR without running `.\gradlew.bat check lintDebug assembleDebug` (or `./gradlew check lintDebug assembleDebug`).
   * If verification fails, inspect the compiler/test/lint output, fix the root cause, and rerun until 100% green.

2. **No Hallucinated Dependencies**:
   * All dependencies and versions are centralized in `gradle/libs.versions.toml`. Do not add new external libraries without an approved Architecture Decision Record in `docs/adr/`.

3. **No Editing Generated / Protected Files**:
   * Do not manually edit `local.properties`, `.gradle/`, or `**/build/**` outputs.

4. **Preserve Architecture & Upstream Contracts**:
   * Check `docs/adr/` and `docs/architecture/overview.md` before altering layers.
   * **Clean Architecture**: `ui/` (Wear Compose), `tile/` (ProtoLayout), and `complication/` must depend only on `domain/` use-cases and models—never directly on Ktor or Room.
   * **Local Wall-Clock Time Parity**: Event dates (`YYYY-MM-DD`) and times (`HH:mm`) must use `java.time.LocalDate` and `java.time.LocalTime` without converting local event schedules through UTC instants, preserving exact parity with `JlightCalendar` ADR 0013.
   * **Touch & Round Screen Ergonomics**: All interactive tap targets on Wear OS must be at least `48dp x 48dp`, use AMOLED true-black (`#000000`) backgrounds, and support rotary crown/bezel scrolling inside `ScalingLazyColumn`.

5. **Test Discipline (No Lazy Asserts)**:
   * Every domain algorithm, repository sync flow, and recurrence rule must have deterministic unit tests in `app/src/test/`. Never write trivial asserts (`assertTrue(true)`) or hollow mocks.

---

## 5. Spec-Driven Development (SDD) Workflow

For non-trivial features, follow the 4-stage pipeline:

```text
specs/active/spec.md  ──▶  specs/active/plan.md  ──▶  specs/active/tasks.md  ──▶  Implement & Verify
  (Intent & Rules)           (Architecture/Files)       (Atomic Checklist)          (Deterministic Gate)
```

See [specs/README.md](./specs/README.md) and [docs/workflows/agentic-sdlc.md](./docs/workflows/agentic-sdlc.md) for details.

---

## 6. Specialized Agent Personas & In-Repo Skills

* **PR Review Agent**: See [.agents/pr-reviewer.md](./.agents/pr-reviewer.md) and [.ci-reviewer.yml](./.ci-reviewer.yml).
* **Core Coding & TDD Agent**: See [.agents/code-implementer.md](./.agents/code-implementer.md).
* **Architectural Review Agent**: See [.agents/architect.md](./.agents/architect.md).
* **Wear OS Verification Skill**: See [.agents/skills/wearos-verify/SKILL.md](./.agents/skills/wearos-verify/SKILL.md).
* **Galaxy Watch Wireless ADB Skill**: See [.agents/skills/adb-galaxy-watch/SKILL.md](./.agents/skills/adb-galaxy-watch/SKILL.md).

---

## 7. Commit Style

All git commits must follow **Conventional Commits**:
* `feat(auth): implement RFC 8628 device code polling and QR pairing screen`
* `fix(rrule): filter exdates in local wall-clock recurrence expansion`
* `test(domain): add unit tests for biweekly and monthly RRULE expansion`
* `docs(adr): add ADR 0004 for offline-first Room sync and wall-clock time`

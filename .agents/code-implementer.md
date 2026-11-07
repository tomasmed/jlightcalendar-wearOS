# Coding & TDD Agent Instructions (Kotlin & Wear OS)

You are acting as the **Core Implementation Agent** for `JlightCalendar-wearOS`.

Your mission is to deliver clean, idiomatic Kotlin code for Wear OS that strictly fulfills the active task specification while introducing zero regression or verification debt.

---

## 1. Operating Rules

1. **One Task at a Time**:
   * Work strictly against `specs/active/tasks.md`. Complete one item, verify it with Gradle, mark it `[x]`, and move to the next.

2. **Test-Driven Discipline (JVM First)**:
   * Keep domain logic (`dev.tomasmed.jlight.wear.domain`) and data repositories (`dev.tomasmed.jlight.wear.data`) testable on the local JVM without requiring an emulator.
   * For any new RRULE expansion logic, API serialization, or repository state transition, write a unit test in `app/src/test/` first.
   * Run `.\gradlew.bat testDebugUnitTest` (`./gradlew testDebugUnitTest` on POSIX) to confirm it passes.

3. **Deterministic Verification Loop**:
   * Before notifying the user or orchestrator that a task is done, run `.\gradlew.bat check lintDebug assembleDebug` (or `./gradlew check lintDebug assembleDebug`).
   * Never ignore Kotlin compiler warnings, Android Lint errors, or failing tests.

4. **Wear OS & Clean Architecture Guardrails**:
   * Respect module package boundaries in `docs/architecture/overview.md`.
   * Use AMOLED true-black (`#000000`) backgrounds, `ScalingLazyColumn` with rotary scroll support, and minimum `48dp x 48dp` touch targets.
   * Store `jlight_ag_...` Bearer tokens exclusively via `AuthTokenStore` (`EncryptedSharedPreferences`) and never log secrets to Logcat.

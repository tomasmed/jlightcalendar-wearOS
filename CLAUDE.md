# Claude Code Project Guidelines

All operational instructions, guardrails, and commands for **JlightCalendar-wearOS** are maintained in the repository's canonical single source of truth:

👉 Please read and strictly follow: [AGENTS.md](./AGENTS.md)

## Quick CLI Reference (Direct Gradle Wrapper — No Wrapper Scripts)
- **Full Verification Gate**: `.\gradlew.bat check lintDebug` (Windows) or `./gradlew check lintDebug` (POSIX)
- **Build Debug APK**: `.\gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (POSIX)
- **Unit Test Suite**: `.\gradlew.bat testDebugUnitTest` (Windows) or `./gradlew testDebugUnitTest` (POSIX)
- **Feature Specifications**: Check `specs/active/` before writing code
- **Architecture Records**: Check `docs/adr/` and `docs/architecture/overview.md` before altering patterns
- **Role Guidelines & Skills**: Check `.agents/` and `.agents/skills/` for specialized Wear OS workflows

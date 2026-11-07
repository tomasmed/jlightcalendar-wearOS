# Task Checklist: JlightCalendar-wearOS Roadmap

> Each task is tagged with its recommended agent model tier (`[Argon]` for complex architecture/algorithms/security review, `[Flash]` for well-scoped implementation and boilerplate) per [docs/workflows/agentic-sdlc.md](../../docs/workflows/agentic-sdlc.md).

## Milestone 1: Repository Cleanup, Toolchain & Foundation Scaffold
- [x] 1. `[Flash]` Clean generic polyglot template scripts (`scripts/*`, `.agents/bootstrap.md`) and update `AGENTS.md`, `CLAUDE.md`, `.cursor/rules/00-agent-core.mdc`, and `.github/copilot-instructions.md` to use direct `./gradlew` commands.
- [x] 2. `[Flash]` Configure `.ci-reviewer.yml` (4 Wear OS lenses) and GitHub Actions workflows (`.github/workflows/ci.yml`, `.github/workflows/code-review.yml`).
- [x] 3. `[Flash]` Set up JDK 21, Android SDK 34 (`platform-tools`, `platforms;android-34`, `build-tools;34.0.0`), Gradle 8.10.2 wrapper, and `gradle/libs.versions.toml`.
- [x] 4. `[Argon]` Implement baseline Clean Architecture packages (`domain/`, `data/`, `ui/`, `tile/`, `complication/`), `WallClockRecurrenceExpander`, and JVM unit tests (`WallClockRecurrenceExpanderTest`, `OfflineFirstJlightRepositoryTest`), verifying `.\gradlew.bat check lintDebug assembleDebug` passes 100%.

## Milestone 2: Device-Code QR Pairing & Encrypted PAT Storage
- [ ] 5. `[Flash]` Implement Ktor `/api/agent/connect` client methods (Device Code `BOT-XXXX` initiation, 3-second polling loop, and `GLX-XXXXX` Join Code exchange) with `MockEngine` unit tests.
- [ ] 6. `[Argon]` Audit `EncryptedSharedPreferences` key rotation and token revocation handling (`401`/`403` auto-unpair state transition).
- [ ] 7. `[Flash]` Build `PairingScreen` with ZXing QR matrix rendering on round screens and manual Join Code input fallback.

## Milestone 3: Offline-First Room Sync & Mutation Queue
- [ ] 8. `[Flash]` Wire full Room SQLite persistence (`EventDao`, `ChoreDao`, `ListItemDao`) and Ktor sync for `/api/events`, `/api/chores`, and `/api/lists`.
- [ ] 9. `[Flash]` Add `WorkManager` periodic 30-minute background sync worker with Tile and Complication update broadcasts.

## Milestone 4: Interactive Wear Compose Screens
- [ ] 10. `[Flash]` Expand `AgendaScreen` with day grouping (Today / Tomorrow), rotary bezel scrolling, and multi-member OSL color bars.
- [ ] 11. `[Flash]` Build `EventDetailScreen` and `ChoresChecklistScreen` (`PATCH /api/chores` star rewards & `PATCH /api/lists` item toggles) with `48dp x 48dp` touch targets.
- [ ] 12. `[Flash]` Add quick voice event creation (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH` -> `POST /api/events`).

## Milestone 5: Glanceable Tile & Watch Face Complication Polish
- [ ] 13. `[Flash]` Connect `AgendaTileService` to live Room + `WallClockRecurrenceExpander` occurrences with deep-link tap actions into `EventDetailScreen`.
- [ ] 14. `[Flash]` Connect `NextEventComplicationService` (`SHORT_TEXT` & `RANGED_VALUE` countdown) to live repository data.

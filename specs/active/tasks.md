# Task Checklist: JlightCalendar-wearOS Roadmap

> Each task is tagged with its recommended agent model tier (`[Argon]` for complex architecture/algorithms/security review, `[Flash]` for well-scoped implementation and boilerplate) per [docs/workflows/agentic-sdlc.md](../../docs/workflows/agentic-sdlc.md). Incremental engineering logs are recorded in [docs/milestones/README.md](../../docs/milestones/README.md).

## Milestone 1: Repository Cleanup, Toolchain & Foundation Scaffold ([Log](../../docs/milestones/M1-foundation-and-clean-architecture.md))
- [x] 1. `[Flash]` Clean generic polyglot template scripts (`scripts/*`, `.agents/bootstrap.md`) and update `AGENTS.md`, `CLAUDE.md`, `.cursor/rules/00-agent-core.mdc`, and `.github/copilot-instructions.md` to use direct `./gradlew` commands.
- [x] 2. `[Flash]` Configure `.ci-reviewer.yml` (4 Wear OS lenses) and GitHub Actions workflows (`.github/workflows/ci.yml`, `.github/workflows/code-review.yml`).
- [x] 3. `[Flash]` Set up JDK 21, Android SDK 34 (`platform-tools`, `platforms;android-34`, `build-tools;34.0.0`), Gradle 8.10.2 wrapper, and `gradle/libs.versions.toml`.
- [x] 4. `[Argon]` Implement baseline Clean Architecture packages (`domain/`, `data/`, `ui/`, `tile/`, `complication/`), `WallClockRecurrenceExpander`, and JVM unit tests (`WallClockRecurrenceExpanderTest`, `JlightApiClientTest`), verifying `.\gradlew.bat check lintDebug assembleDebug` passes 100%.

## Milestone 2: Device-Code QR Pairing & Encrypted PAT Storage ([Log](../../docs/milestones/M2-device-code-qr-and-encrypted-auth.md))
- [x] 5. `[Flash]` Implement Ktor `/api/agent/connect` client methods (Device Code `BOT-XXXX` initiation, 3-second polling loop, and `GLX-XXXXX` Join Code exchange) with `MockEngine` unit tests.
- [x] 6. `[Argon]` Audit `EncryptedSharedPreferences` key rotation and token revocation handling (`401`/`403` auto-unpair state transition).
- [x] 7. `[Flash]` Build `PairingScreen` with pure-Kotlin ISO 18004 QR matrix rendering on round screens and voice Join Code input fallback.

## Milestone 3: Offline-First Room Sync & Background JobService ([Log](../../docs/milestones/M3-offline-first-room-and-sync.md))
- [x] 8. `[Flash]` Wire full Room SQLite persistence (`JlightWearDatabase`, `JlightWearDao`) and Ktor sync for `/api/events` (including `POST /api/events`), `/api/chores`, and `/api/lists`.
- [x] 9. `[Flash]` Add `JlightPeriodicSyncJobService` periodic 30-minute background sync service with `WearSyncCoordinator` Tile and Complication update broadcasts.

## Milestone 4: Interactive Wear Compose Screens ([Log](../../docs/milestones/M4-interactive-wear-compose-ui.md))
- [x] 10. `[Flash]` Expand `AgendaWatchScreen` with day grouping (Today / Tomorrow), rotary bezel scrolling, and multi-member OSL color dots.
- [x] 11. `[Flash]` Build `EventDetailScreen` and `ChoresChecklistScreen` (`PATCH /api/chores` star rewards & `PATCH /api/lists` item toggles) with `48dp x 48dp` touch targets.
- [x] 12. `[Flash]` Add quick voice event creation (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH` -> `VoiceEventTranscriptParser` -> `POST /api/events`).

## Milestone 5: Glanceable Tile & Watch Face Complication Polish ([Log](../../docs/milestones/M5-glanceable-tile-and-complication.md))
- [x] 13. `[Flash]` Connect `AgendaTileService` to live Room + `WallClockRecurrenceExpander` occurrences with deep-link tap actions into `MainActivity` / `EventDetailScreen`.
- [x] 14. `[Flash]` Connect `NextEventComplicationService` (`SHORT_TEXT` & `RANGED_VALUE` countdown) to live repository data.

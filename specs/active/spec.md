# Feature Specification: JlightCalendar-wearOS Companion App (M1–M5 Roadmap)

* **Status**: Active
* **Created**: 2026-11-07
* **Target Hardware**: Samsung Galaxy Watch 4 / 5 / 6 / 7 / Ultra & Wear OS 4/5 Round Smartwatches (`minSdk = 30`, `targetSdk = 34`)
* **Upstream Host**: [`tomasmed/JlightCalendar`](https://github.com/tomasmed/JlightCalendar)

---

## 1. Problem Statement & User Value

Families using **JlightCalendar** on a kitchen wall kiosk or mobile browser want instant, wrist-level access to their household "Galaxy" schedule, chore stars, and shared shopping/todo lists without pulling out a phone.

---

## 2. User Stories & Acceptance Criteria

### Story 1 (M1): Buildable & Verifiable Clean Architecture Baseline
* **As a** developer or AI coding agent,
* **I want** a pure Kotlin + Wear OS Gradle scaffold with zero wrapper scripts and fast JVM unit tests,
* **So that** `./gradlew check lintDebug assembleDebug` deterministically validates the entire project in CI and locally.

### Story 2 (M2): Zero-Password Watch Pairing via QR Code or Join Code
* **As a** family member wearing a Galaxy Watch,
* **I want** to pair my watch with our household Galaxy by scanning a QR code displayed on my watch face (or entering a short `GLX-XXXXX` Join Code),
* **So that** I never have to type Google OAuth passwords on a tiny circular screen.
* **Acceptance Criteria**:
  * Calls `POST /api/agent/connect` with `{ agentName: "Galaxy Watch", rune: "⌚", requestedScope: "standard" }`.
  * Renders QR code for `<baseUrl>/?claimBot=BOT-XXXX-XXXX` and polls `GET /api/agent/connect?deviceCode=...`.
  * Stores the approved `jlight_ag_...` PAT and `galaxyId` in `EncryptedSharedPreferences`.

### Story 3 (M3): Offline-First Schedule Sync & Local Wall-Clock Recurrence
* **As a** watch wearer on the go (including offline / LTE / Bluetooth disconnected),
* **I want** my recurring household events (`DAILY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY` with `exdates`) cached locally and expanded in local wall-clock time,
* **So that** my watch agenda always matches our home wall kiosk with zero UTC timezone drift.

### Story 4 (M4): Interactive Circular Compose UI (Agenda, Event Details, Chores & Lists)
* **As a** family member,
* **I want** to scroll Today/Tomorrow's agenda using my Galaxy Watch's rotating bezel/crown, inspect multi-member event details, and check off chores or shared list items with `48dp x 48dp` tap targets,
* **So that** I can interact effortlessly on a round screen.

### Story 5 (M5): Glanceable ProtoLayout Tile & Watch Face Complication
* **As a** watch wearer,
* **I want** a right-to-left swipe Tile showing the next 3 upcoming family events and a `SHORT_TEXT` watch face complication showing the next event time/title,
* **So that** I can check family plans in under a second without launching the full app.

# ⌚ JlightCalendar — Wear OS Companion (`JlightCalendar-wearOS`)

**JlightCalendar-wearOS** is the native **Kotlin & Jetpack Compose for Wear OS** smartwatch companion for [**JlightCalendar**](https://github.com/tomasmed/JlightCalendar), the open-source family household calendar, chore tracker, and shared list platform.

Designed specifically for circular AMOLED smartwatches—including **Samsung Galaxy Watch 4 / 5 / 6 / 7 / Ultra** running **Wear OS 4 & 5**—this app brings your family's household **"Galaxy"** right to your wrist with offline-first caching, rotating bezel/crown ergonomics, glanceable Tiles, and Watch Face Complications.

---

## 🌌 Relationship to Upstream `JlightCalendar`

| Repository | Role | Tech Stack | Local Workspace Path |
| :--- | :--- | :--- | :--- |
| [**`tomasmed/JlightCalendar`**](https://github.com/tomasmed/JlightCalendar) | Hosted Web App, Wall Kiosk & REST API Host | Next.js 15, React 19, TypeScript, Turso / libSQL, NextAuth | `E:\JlightCalendar` |
| [**`tomasmed/jlightcalendar-wearOS`**](https://github.com/tomasmed/jlightcalendar-wearOS) | Native Smartwatch Companion App, Tile & Complication | Kotlin 2.0, Compose for Wear OS (Material 3), ProtoLayout, Ktor, Room | `E:\Projects\JlightCalendar-wearOS` |

### Key Shared Concepts
* **Household Galaxies & Inhabitants**: Every family workspace is a **Galaxy** populated by color-coded family members (and `"Everyone"` for household-wide events). The watch renders multi-member color indicators using the upstream **OSL (Fluorescent Light-Spill)** high-contrast palette against true-black (`#000000`) AMOLED backgrounds.
* **Zero-Password Watch Pairing (ADR 0015 / ADR 0003)**: Typing Google OAuth passwords on a 1.4-inch round watch screen is painful. Instead, the watch initiates an RFC 8628-style handshake via `POST /api/agent/connect` and displays a scannable **QR Code** (`/?claimBot=BOT-XXXX`) plus a 1-tap `GLX-XXXXX` Join Code fallback. Approving the watch on your phone or wall kiosk mints a scoped `jlight_ag_...` Personal Access Token stored in `EncryptedSharedPreferences`.
* **Local Wall-Clock Time Parity (ADR 0013 / ADR 0004)**: All event dates (`YYYY-MM-DD`), times (`HH:mm`), and recurrence rules (`RRULE` + `exdates`) are evaluated in **local wall-clock time** (`java.time.LocalDate` / `LocalTime`) without UTC timezone shift, ensuring schedules on your wrist match your wall kiosk in every timezone.

---

## ✨ Core Features & Milestones

1. **M1 — Foundation & Clean Architecture Scaffold**:
   * Single-module `:app` organized into `ui/`, `tile/`, `complication/`, `domain/`, and `data/` layers.
   * Version catalog (`gradle/libs.versions.toml`), JUnit unit test suite, Android Lint, and multi-lens AI PR review (`.ci-reviewer.yml`).
2. **M2 — QR Device-Code Pairing & Encrypted Token Store**:
   * Calls `POST /api/agent/connect` (`{ agentName: "Galaxy Watch", rune: "⌚" }`), renders a scannable QR code on the watch face, polls for approval, and stores `jlight_ag_...` in `EncryptedSharedPreferences`.
3. **M3 — Offline-First Room Sync & Wall-Clock RRULE Engine**:
   * Fetches `/api/events`, `/api/chores`, and `/api/lists` via Ktor Client, caches entities in Room SQLite, and expands `DAILY`, `WEEKLY`, `BIWEEKLY`, and `MONTHLY` recurrences locally so your schedule works even in Airplane mode.
4. **M4 — Interactive Wear Compose UI**:
   * **Today & Tomorrow Agenda Screen** with physical rotating bezel / crown scrolling (`ScalingLazyColumn`).
   * **Event Detail Screen** showing member color pills, time span, location, and recurrence badge.
   * **Chores & Shared Lists Checklist** with `48dp x 48dp` touch targets to complete chores (earning ⭐ stars!) or check off grocery items from your wrist.
5. **M5 — Glanceable ProtoLayout Tile & Watch Face Complication**:
   * **Agenda Tile**: Swipe right-to-left from your watch face to view your next 3 household events at a glance.
   * **Next Event Complication**: Exports `SHORT_TEXT` to any compatible Wear OS watch face slot.

---

## 🛠️ Developer Toolchain & Quick Start

### Prerequisites
* **JDK 21**: Installed at `C:\Program Files\Android\openjdk\jdk-21.0.8` (`JAVA_HOME`).
* **Android SDK (API 34)**: Installed at `C:\Users\tomas\AppData\Local\Android\Sdk` (`ANDROID_HOME`) with `platform-tools`, `platforms;android-34`, and `build-tools;34.0.0`.

### Direct Gradle Commands (No Wrapper Scripts)
All verification and build tasks run directly through the Gradle Wrapper:

```powershell
# 1. Run the full verification gate (Unit Tests + Android Lint)
.\gradlew.bat check lintDebug

# 2. Run JVM unit tests only
.\gradlew.bat testDebugUnitTest

# 3. Build the Debug APK
.\gradlew.bat assembleDebug
```

### Sideloading to a Samsung Galaxy Watch via Wireless ADB
See [.agents/skills/adb-galaxy-watch/SKILL.md](./.agents/skills/adb-galaxy-watch/SKILL.md) for full instructions:
```powershell
adb pair <WATCH_IP>:<PAIR_PORT> <PAIR_CODE>
adb connect <WATCH_IP>:<CONNECT_PORT>
.\gradlew.bat installDebug
adb shell am start -n dev.tomasmed.jlight.wear/.ui.MainActivity
```

---

## 📚 Repository Documentation Map

* [**`AGENTS.md`**](./AGENTS.md) — Universal Agent Operating Contract, command matrix, and guardrails.
* [**`docs/architecture/overview.md`**](./docs/architecture/overview.md) — Clean Architecture layers, upstream `JlightCalendar` REST API mapping, and data flow.
* [**`docs/workflows/agentic-sdlc.md`**](./docs/workflows/agentic-sdlc.md) — Feature-branching strategy and `/boost` multi-agent orchestration plan (**Gemini 4 Argon** vs. **Gemini 3.8 Flash**).
* [**`docs/adr/README.md`**](./docs/adr/README.md) — Index of Architecture Decision Records (`ADR 0001`–`ADR 0004`).
* [**`specs/README.md`**](./specs/README.md) — Spec-Driven Development (SDD) guide and active milestone specifications (`specs/active/`).

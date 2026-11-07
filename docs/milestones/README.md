# Incremental Milestone Engineering Log (`JlightCalendar-wearOS`)

This directory records the step-by-step engineering progress, architectural decisions, verification results, and agentic SDLC delegation across each milestone of `JlightCalendar-wearOS` — the native Kotlin + Jetpack Compose for Wear OS companion app for [`JlightCalendar`](https://github.com/tomasmed/JlightCalendar).

---

## Milestone Index

| Milestone | Title | Branch | Model Routing | Status |
| :--- | :--- | :--- | :--- | :--- |
| **[M1](./M1-foundation-and-clean-architecture.md)** | Repository Cleanup, Toolchain, ADRs & RRULE/Ktor Foundation | `feat/bootstrap-wearos-scaffold` | `[Flash]` scaffold + `[Argon]` RRULE & API parity audit | Completed (`c631a43`) |
| **[M2](./M2-device-code-qr-and-encrypted-auth.md)** | Device-Code QR Pairing, Join-Code Fast Pairing & Auto-Unpair | `feat/wearos-m2-m5-complete` | `[Flash]` QR matrix & UI + `[Argon]` Keystore/401 revocation | Completed |
| **[M3](./M3-offline-first-room-and-sync.md)** | Offline-First Room SQLite Sync, Event Creation & Background Refresh | `feat/wearos-m2-m5-complete` | `[Flash]` DAO/Repository wiring & background refresh | Completed |
| **[M4](./M4-interactive-wear-compose-ui.md)** | Interactive Wear Compose Navigation, Agenda, Chores/Lists & Voice Quick-Add | `feat/wearos-m2-m5-complete` | `[Flash]` Wear Compose Material 3 screens & rotary input | Completed |
| **[M5](./M5-glanceable-tile-and-complication.md)** | Live Glanceable ProtoLayout Tile & Watch Face Complication | `feat/wearos-m2-m5-complete` | `[Flash]` Tile/Complication data binding & deep-linking | Completed |

---

## Upstream Contract Parity (`JlightCalendar`)

Every milestone adheres to the upstream `JlightCalendar` specifications and ADRs:

1. **Local Wall-Clock Recurrence (`ADR 0013` / `ADR 0004`)**:
   - Events use `YYYY-MM-DD` (`java.time.LocalDate`) and `HH:mm` (`java.time.LocalTime`) without UTC `Instant` conversion so recurring schedules never shift across DST or travel timezones.
2. **Agent Handshake & Scoped Bearer PAT (`ADR 0018` / `ADR 0003`)**:
   - Supports both `POST /api/agent/connect` (`BOT-XXXX` Device Code with 3-second polling on `GET /api/agent/connect?deviceCode=...`) and direct household Join Code (`GLX-XXXXX`) self-registration.
   - Bearer PAT (`jlight_ag_...`) is persisted in hardware-backed `EncryptedSharedPreferences` and redacted in `StoredAuthCredentials.toString()`.
3. **True-Black AMOLED & OSL Fluorescent Palette (`ADR 0016` / `ADR 0002`)**:
   - `#000000` background for OLED power efficiency paired with high-contrast OSL member accents (`FluorescentCyan`, `FluorescentEmerald`, `FluorescentAmber`, `FluorescentRose`) and `48dp x 48dp` minimum touch targets.

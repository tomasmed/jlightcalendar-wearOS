# Implementation Plan: JlightCalendar-wearOS (M1–M5)

* **Spec Reference**: [specs/active/spec.md](./spec.md)
* **Architecture Reference**: [docs/architecture/overview.md](../../docs/architecture/overview.md)

---

## 1. Module & Package Layout (`:app`)

```text
app/src/main/java/dev/tomasmed/jlight/wear/
├── domain/
│   ├── model/          # Pure Kotlin domain models (CalendarEvent, EventOccurrence, ChoreItem, SharedListItem, PairingState)
│   ├── repository/     # JlightRepository interface
│   └── rrule/          # WallClockRecurrenceExpander (LocalDate/LocalTime RFC 5545 RRULE + exdates engine)
├── data/
│   ├── auth/           # AuthTokenStore (EncryptedSharedPreferences wrapper + in-memory testable interface)
│   ├── local/          # Room database (JlightWearDatabase, EventEntity, ChoreEntity, ListItemEntity, DAOs)
│   ├── remote/         # Ktor JlightApiClient & kotlinx.serialization DTOs (/api/agent/connect, /api/events, /api/chores, /api/lists)
│   └── repository/     # OfflineFirstJlightRepository
├── ui/
│   ├── MainActivity.kt # Entry activity hosting Wear Compose navigation
│   ├── theme/          # JlightWearTheme (AMOLED #000000 + OSL fluorescent member palette)
│   └── screens/        # AgendaScreen, PairingScreen, EventDetailScreen, ChoresChecklistScreen
├── tile/
│   └── AgendaTileService.kt             # ProtoLayout Material 3 tile for next 3 events
└── complication/
    └── NextEventComplicationService.kt  # SHORT_TEXT watch face complication data source
```

---

## 2. Failure Modes & Mitigations

| Risk / Failure Mode | Mitigation Strategy |
| :--- | :--- |
| **UTC Timezone Drift on Recurring Events** | `WallClockRecurrenceExpander` parses `YYYY-MM-DD` and `HH:mm` strictly into `java.time.LocalDate` and `java.time.LocalTime`. Unit tests assert exact date parity with upstream `JlightCalendar` ADR 0013. |
| **Circular Bezel Clipping on Galaxy Watch** | All scrollable screens use `ScalingLazyColumn` with horizontal and vertical content padding designed for round displays. |
| **PAT Leakage in Logs** | Ktor client and `AuthTokenStore` redact `jlight_ag_...` tokens from `toString()` and never enable `LogLevel.HEADERS` or `LogLevel.ALL` in production. |
| **Offline / Unreachable Server** | Repository reads emit immediately from Room SQLite; network sync failures surface a non-destructive offline status badge while keeping cached schedules interactive. |

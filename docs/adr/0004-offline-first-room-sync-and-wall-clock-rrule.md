# ADR 0004: Offline-First Room Sync & Local Wall-Clock Recurrence Expansion

* **Status**: Accepted
* **Date**: 2026-11-07
* **Deciders**: Maintainers & Lead Architect Agent

## Context and Problem Statement

Smartwatches frequently transition between Wi-Fi, Bluetooth tether, LTE, and standalone offline mode (e.g., outdoors or in Airplane mode). Furthermore, upstream `JlightCalendar` (**ADR 0013**) stores recurring events as a single master row (`date`, `time`, `rrule`, `exdates`) in **local wall-clock time** rather than pre-expanding occurrences in the database or converting local event times to UTC.

## Decision Drivers

* **Instant Launch & Tile Rendering**: Watch UI, ProtoLayout Tiles, and Complications must render in `<100ms` from local storage without blocking on network requests.
* **Exact Recurrence Parity with Web Kiosk**: A weekly event at `09:00` with `exdates: ["2026-11-14"]` must expand to the exact same dates and times on the watch as on the Next.js web app, with zero UTC timezone shift.
* **Pure JVM Testability**: Recurrence expansion is the highest-complexity algorithm in the companion app and must be 100% unit-testable on the JVM without an emulator.

## Decision Outcome

1. **Offline-First Single Source of Truth (Room SQLite)**:
   * `JlightWearDatabase` caches raw event masters (`EventEntity`), chores (`ChoreEntity`), and shared list items (`ListItemEntity`).
   * UI screens, Tiles, and Complications observe Room queries wrapped by domain use-cases.
   * Background synchronization runs via `WorkManager` (periodic 30-minute sync with network constraint) plus pull-to-refresh / on-resume triggers, notifying `TileService.getUpdater(context).requestUpdate(AgendaTileService::class.java)` and `ComplicationDataSourceUpdateRequester` whenever cached data changes.
2. **Local Wall-Clock Recurrence Expander (`WallClockRecurrenceExpander`)**:
   * Implemented in pure Kotlin (`dev.tomasmed.jlight.wear.domain.rrule`) using `java.time.LocalDate`, `java.time.LocalTime`, and `java.time.DayOfWeek`.
   * Supports `FREQ=DAILY`, `FREQ=WEEKLY` (including `INTERVAL=2` biweekly and `BYDAY=MO,TU,WE,TH,FR` weekdays), and `FREQ=MONTHLY` within any requested `[rangeStart, rangeEnd]` window.
   * Filters out any occurrence date present in the event's `exdates` list (`YYYY-MM-DD`) and honours `UNTIL` / `COUNT` bounds.
   * Never converts local event dates/times through `Instant` or `ZoneOffset.UTC`.

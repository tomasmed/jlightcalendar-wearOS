# Milestone 5: Live Glanceable ProtoLayout Tile & Watch Face Complication

* **Related ADR**: [ADR 0002](../adr/0002-kotlin-compose-wearos-stack.md), [ADR 0004](../adr/0004-offline-first-room-sync-and-wall-clock-rrule.md)
* **Agent Model Load**: `[Flash]` for ProtoLayout Tile action bindings and Complication `SHORT_TEXT` / `RANGED_VALUE` providers.

---

## 1. What Was Built

1. **Live ProtoLayout Tile (`AgendaTileService.kt`)**:
   - Queries the next 3 upcoming occurrences across a 48-hour window from `WearSyncCoordinator.provideRepository(applicationContext)`.
   - Renders each occurrence row with a clickable `ActionBuilders.LaunchAction` passing `MainActivity.EXTRA_OCCURRENCE_ID` so tapping an event on the watch carousel opens `EventDetailScreen` directly.
2. **Live Watch Face Complication (`NextEventComplicationService.kt`)**:
   - Supports both `ComplicationType.SHORT_TEXT` and `ComplicationType.RANGED_VALUE` (180-minute countdown progress ring).
   - Selects the next non-expired occurrence for today/tomorrow (`selectNextUpcomingOccurrence`), formats compact watch-face labels (`formatComplicationSummary`), and attaches a `PendingIntent` deep-link into `MainActivity`.

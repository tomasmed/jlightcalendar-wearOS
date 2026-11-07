# Milestone 3: Offline-First Room SQLite Sync, Event Creation & Background Refresh

* **Related ADR**: [ADR 0004](../adr/0004-offline-first-room-sync-and-wall-clock-rrule.md)
* **Agent Model Load**: `[Flash]` for Room singleton wiring, `POST /api/events`, and `JlightPeriodicSyncJobService`.

---

## 1. What Was Built

1. **Reactive Room SQLite Observation & Offline Cache Protection (`JlightWearDatabase.kt` & `OfflineFirstJlightRepository.kt`)**:
   - Added singleton `JlightWearDatabase.getInstance(context)` and galaxy-scoped cleanup/snapshot queries (`clearEventsForGalaxy`, `clearChoresForGalaxy`, `clearListItemsForGalaxy`, `getEventsSnapshot`, `getChoresSnapshot`, `getListItemsSnapshot`).
   - Connected `observeOccurrences`, `observeChores`, and `observeSharedLists` to reactive Room `Flow` queries combined with `WallClockRecurrenceExpander`, strictly scoping any in-memory fallback by `state.galaxyId` so paired galaxies with 0 events never leak unpaired demo events.
   - Enforced `JlightApiException` on any non-2xx HTTP status (`>= 400`) in `JlightApiClient.ensureSuccess` so transient `500`/`502`/`503` server errors fail `syncWithRemote()` cleanly without wiping cached offline Room rows.
   - Passed the watch's local wall-clock `date` (`YYYY-MM-DD`) to `GET /api/chores` (`fetchChores`) and mapped `memberName` so daily chore completion status matches local wall-clock time across all timezones.
2. **Wall-Clock Event Creation (`POST /api/events`)**:
   - Implemented `JlightApiClient.createEvent` and `OfflineFirstJlightRepository.createEvent` formatting local wall-clock `YYYY-MM-DDTHH:mm` payloads and immediately upserting the created `EventEntity` into Room.
3. **Battery-Aware 30-Minute Periodic Sync (`JlightPeriodicSyncJobService.kt` & `WearSyncCoordinator.kt`)**:
   - Scheduled a 30-minute network-constrained `JobService` (`JOB_ID = 42019`) that syncs remote galaxy data in the background and calls `WearSyncCoordinator.requestSurfaceUpdates(context)` to refresh both `AgendaTileService` and `NextEventComplicationService`.


# Architecture Decision Records (ADR) Index

This directory contains the Architecture Decision Records (ADRs) for **JlightCalendar-wearOS**, following the [MADR format](https://adr.github.io/madr/).

When proposing an architectural change or introducing a new library in `gradle/libs.versions.toml`:
1. Copy [template.md](./template.md) to a new file named `NNNN-short-title.md`.
2. Document the context, options considered, and chosen solution (referencing upstream `JlightCalendar` ADRs where applicable).
3. Add the record to the index below.

## Index of Decisions

| ID | Title | Status | Date |
| :--- | :--- | :--- | :--- |
| [0001](./0001-record-architecture-decisions.md) | Record Architecture Decisions | Accepted | 2026-10-07 |
| [0002](./0002-kotlin-compose-wearos-stack.md) | Kotlin 2.0, Jetpack Compose for Wear OS & Clean Architecture Stack | Accepted | 2026-11-07 |
| [0003](./0003-device-code-auth-and-encrypted-pat-storage.md) | Device-Code QR Handshake & Encrypted SharedPreferences PAT Storage | Accepted | 2026-11-07 |
| [0004](./0004-offline-first-room-sync-and-wall-clock-rrule.md) | Offline-First Room Sync & Local Wall-Clock Recurrence Expansion | Accepted | 2026-11-07 |

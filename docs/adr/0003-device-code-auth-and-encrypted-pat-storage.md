# ADR 0003: Device-Code QR Handshake & Encrypted SharedPreferences PAT Storage

* **Status**: Accepted
* **Date**: 2026-11-07
* **Deciders**: Maintainers & Lead Architect Agent

## Context and Problem Statement

`JlightCalendar` authenticates human web sessions via Google OAuth (`NextAuth`) and authenticates external devices/agents via scoped Personal Access Tokens (`jlight_ag_<48_hex_chars>`) as defined in upstream **ADR 0015**. Entering Google account credentials on a 1.4-inch circular watch keyboard is frustrating and error-prone, and Google blocks embedded WebView OAuth flows on Wear OS.

## Decision Drivers

* **Zero Watch Typing**: Users should be able to pair their watch in seconds by scanning a QR code on the watch face with their phone (already logged into `JlightCalendar`).
* **Offline & Kiosk Fallback**: Users should also be able to pair using a short `GLX-XXXXX` Galaxy Join Code if a camera is unavailable.
* **Hardware-Backed Credential Security**: The resulting `jlight_ag_...` Bearer token grants read/write access to household schedules and chores (`standard` scope) and must be encrypted at rest on the watch.

## Decision Outcome

We adopt the upstream `/api/agent/connect` handshake protocol (upstream **ADR 0015**) paired with Android Keystore-backed `EncryptedSharedPreferences`:

1. **Primary Flow (RFC 8628 Device-Code + QR)**:
   * Watch sends `POST /api/agent/connect` with `{ "agentName": "Galaxy Watch", "rune": "⌚", "requestedScope": "standard" }`.
   * Server returns `202 Accepted` with `deviceCode` (`BOT-XXXX-XXXX`) and `claimUrl` (`/?claimBot=BOT-XXXX-XXXX`).
   * Watch renders `<baseUrl>/?claimBot=BOT-XXXX-XXXX` as a high-contrast QR code on the round display alongside the human-readable `BOT-XXXX-XXXX` code, polling `GET /api/agent/connect?deviceCode=...` every 3 seconds.
   * Once the user taps **Approve** on their phone or wall kiosk, the watch receives `{ "status": "approved", "token": "jlight_ag_...", "galaxyId": "..." }`.
2. **Fallback Flow (Galaxy Join Code)**:
   * Watch allows entering a 9-character `GLX-XXXXX` code via `POST /api/agent/connect` for immediate token issuance (`201 Created`).
3. **Encrypted Token Storage (`AuthTokenStore`)**:
   * The minted `jlight_ag_...` token and `galaxyId` are stored in `EncryptedSharedPreferences` (`AES256_SIV` keys / `AES256_GCM` values backed by `MasterKeys.AES256_GCM_SPEC` in Android Keystore).
   * Tokens are never written to plain `SharedPreferences` or logged in Logcat.

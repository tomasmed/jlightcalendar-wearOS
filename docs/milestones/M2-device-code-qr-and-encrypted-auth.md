# Milestone 2: Device-Code QR Pairing, Join-Code Fast Pairing & Auto-Unpair

* **Related ADR**: [ADR 0003](../adr/0003-device-code-auth-and-encrypted-pat-storage.md)
* **Agent Model Load**: `[Flash]` for QR matrix generator & `PairingScreen` Compose UI; `[Argon]` for `401`/`403` token revocation state transitions and `EncryptedSharedPreferences` security audit.

---

## 1. What Was Built

1. **Zero-Dependency ISO/IEC 18004 QR Matrix Generator (`QrCodeMatrixGenerator.kt`)**:
   - Pure Kotlin QR Code Version 1–6 (Byte mode, ECC Level L, Reed-Solomon GF(256) error correction) matrix encoder tailored for `https://<host>/?claimBot=BOT-XXXX-XXXX` URLs on round Wear OS screens.
2. **Pairing Repository & 3-Second Polling Loop (`OfflineFirstJlightRepository.kt` & `PairingScreen.kt`)**:
   - `initiateDevicePairing(baseUrl)` calls `POST /api/agent/connect` and transitions the watch to `PairingSessionState.AwaitingClaim(deviceCode, userCode, claimUrl, expiresAtIso)`.
   - `PairingScreen` renders a high-contrast white-padded `QrCodeBadge` on the circular `#000000` AMOLED canvas and runs a `LaunchedEffect` 3-second polling loop (`pollDevicePairing`) until the human approves the bot claim code on their phone or wall kiosk.
   - Supports direct household Join Code (`GLX-XXXXX`) voice fast-pairing via `pairWithJoinCode`.
3. **Revocation Auto-Unpair (`JlightUnauthorizedException`)**:
   - Any `401 Unauthorized` or `403 Forbidden` response from `/api/events`, `/api/chores`, or `/api/lists` throws `JlightUnauthorizedException`, which is intercepted by `OfflineFirstJlightRepository.runWithAuthGuard` to wipe `AuthTokenStore`, clear cached galaxy tables in Room, and transition immediately to `PairingSessionState.Unpaired`.

# Milestone 2: Device-Code QR Pairing, Join-Code Fast Pairing & Auto-Unpair

* **Related ADR**: [ADR 0003](../adr/0003-device-code-auth-and-encrypted-pat-storage.md)
* **Agent Model Load**: `[Flash]` for QR matrix generator & `PairingScreen` Compose UI; `[Argon]` for `401`/`403` token revocation state transitions and `EncryptedSharedPreferences` security audit.

---

## 1. What Was Built

1. **Zero-Dependency ISO/IEC 18004 QR Matrix Generator (`QrCodeMatrixGenerator.kt`)**:
   - Pure Kotlin QR Code Version 1–9 (Byte mode, ECC Level L, Reed-Solomon GF(256) error correction, and Annex D 18-bit version info blocks for Versions 7–9) supporting URLs up to 230 UTF-8 bytes (`https://<host>/?claimBot=BOT-XXXX-XXXX`) on round Wear OS screens, with explicit capacity bounds checking instead of silent URL truncation.
2. **`PairingViewModel`, Pairing Repository & 3-Second Polling Loop (`PairingViewModel.kt`, `OfflineFirstJlightRepository.kt`, `PairingScreen.kt`)**:
   - `PairingViewModel` orchestrates `startDeviceCodeHandshake()`, `pollDeviceCodeStatus(deviceCode)`, and `pairWithSpokenJoinCode(spokenTranscript)`.
   - `initiateDevicePairing(baseUrl)` calls `POST /api/agent/connect` and transitions the watch to `PairingSessionState.AwaitingClaim(deviceCode, userCode, claimUrl, expiresAtIso)`.
   - `PairingScreen` renders a high-contrast white-padded `QrCodeBadge` on the circular `#000000` AMOLED canvas and runs a `LaunchedEffect` 3-second polling loop (`pollDevicePairing`). If the handshake expires (`410 Gone`), is denied (`403 Forbidden`), or is not found (`404 Not Found`), the repository transitions back to `PairingSessionState.Unpaired` immediately so polling halts and the user can retry.
   - Supports direct household Join Code (`GLX-XXXXX` / `GLX-XXXX-XXXX`) voice fast-pairing with spoken `"dash"`/`"hyphen"` normalization (`JlightApiClient.normalizeJoinCode`).
3. **Revocation Auto-Unpair (`JlightUnauthorizedException`)**:
   - Any `401 Unauthorized` or `403 Forbidden` response from `/api/events`, `/api/chores`, or `/api/lists` throws `JlightUnauthorizedException`, which is intercepted by `OfflineFirstJlightRepository.runWithAuthGuard` to wipe `AuthTokenStore`, clear cached galaxy tables in Room, and transition immediately to `PairingSessionState.Unpaired`.


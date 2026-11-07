package dev.tomasmed.jlight.wear.ui.screens

import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.repository.JlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation state holder for Device-Code QR Pairing (`?claimBot=BOT-XXXX`) and
 * `GLX-XXXXX` / `GLX-XXXX-XXXX` Join-Code fast-pairing (Milestone 2 / ADR 0003).
 */
class PairingViewModel(
    private val repository: JlightRepository,
    val baseUrl: String,
) {
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun observePairingState(): Flow<PairingSessionState> = repository.observePairingState()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    suspend fun startDeviceCodeHandshake(): Result<PairingSessionState.AwaitingClaim> {
        _statusMessage.value = null
        val result = repository.initiateDevicePairing(baseUrl = baseUrl)
        result.onFailure { error ->
            _statusMessage.value = error.message ?: "Failed to start pairing handshake"
        }
        return result
    }

    suspend fun pollDeviceCodeStatus(deviceCode: String): Result<PairingSessionState> {
        val result = repository.pollDevicePairing(
            baseUrl = baseUrl,
            deviceCode = deviceCode,
        )
        result.onSuccess { state ->
            if (state is PairingSessionState.Paired) {
                _statusMessage.value = "Approved!"
            }
        }.onFailure { error ->
            _statusMessage.value = error.message ?: "Pairing session expired or was denied"
        }
        return result
    }

    suspend fun pairWithSpokenJoinCode(spokenTranscript: String): Result<PairingSessionState.Paired> {
        val normalizedCode = JlightApiClient.normalizeJoinCode(spokenTranscript)
        val result = repository.pairWithJoinCode(
            baseUrl = baseUrl,
            joinCode = normalizedCode,
        )
        result.onSuccess {
            _statusMessage.value = "Paired with galaxy!"
        }.onFailure { error ->
            _statusMessage.value = error.message ?: "Join code failed"
        }
        return result
    }
}

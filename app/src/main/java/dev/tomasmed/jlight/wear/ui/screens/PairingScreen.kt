package dev.tomasmed.jlight.wear.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.qr.QrCodeMatrixGenerator
import dev.tomasmed.jlight.wear.ui.theme.JlightOslColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Circular Wear OS Device-Code QR Pairing & Join-Code Fast-Pairing screen (Milestone 2 / ADR 0003).
 *
 * When in [PairingSessionState.AwaitingClaim], renders a high-contrast scannable QR code on a white
 * quiet-zone pad and polls `/api/agent/connect?deviceCode=...` every 3 seconds automatically.
 */
@Composable
fun PairingScreen(
    pairingState: PairingSessionState,
    baseUrl: String,
    statusMessage: String?,
    onStartDeviceHandshake: () -> Unit,
    onPollDeviceHandshake: (String) -> Unit,
    onVoiceJoinCodeRequest: () -> Unit,
    onDismissToDemoAgenda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Automatic 3-second polling loop while awaiting human approval (ADR 0003)
    LaunchedEffect(pairingState, baseUrl) {
        if (pairingState is PairingSessionState.AwaitingClaim) {
            while (true) {
                delay(3_000L)
                onPollDeviceHandshake(pairingState.deviceCode)
            }
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(JlightOslColors.TrueBlack)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "⌚ Pair Galaxy Watch",
                style = MaterialTheme.typography.titleMedium,
                color = JlightOslColors.FluorescentCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (pairingState) {
            is PairingSessionState.AwaitingClaim -> {
                item {
                    QrCodeBadge(
                        payload = pairingState.claimUrl,
                        size = 116.dp,
                    )
                }

                item {
                    Text(
                        text = pairingState.userCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = JlightOslColors.FluorescentEmerald,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Scan QR on phone/kiosk to approve watch PAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is PairingSessionState.Unpaired -> {
                item {
                    Text(
                        text = "Host: $baseUrl",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Button(
                        onClick = onStartDeviceHandshake,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = "Show Pairing QR",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            is PairingSessionState.Paired -> {
                item {
                    Card(
                        onClick = onDismissToDemoAgenda,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Connected Galaxy",
                                style = MaterialTheme.typography.labelSmall,
                                color = JlightOslColors.FluorescentEmerald,
                            )
                            Text(
                                text = pairingState.galaxyId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JlightOslColors.TextPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                onClick = onVoiceJoinCodeRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fast Pair • Join Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.FluorescentAmber,
                    )
                    Text(
                        text = "Speak GLX-XXXXX Code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JlightOslColors.TextPrimary,
                    )
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            item {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentRose,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Card(
                onClick = onDismissToDemoAgenda,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = "Back to Agenda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JlightOslColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun QrCodeBadge(
    payload: String,
    size: Dp = 116.dp,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(payload) {
        QrCodeMatrixGenerator.encode(payload)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val quietZone = 2
            val totalModules = matrix.size + quietZone * 2
            val cellSize = this.size.width / totalModules.toFloat()

            for (r in 0 until matrix.size) {
                for (c in 0 until matrix.size) {
                    if (matrix.isDark(r, c)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(
                                x = (c + quietZone) * cellSize,
                                y = (r + quietZone) * cellSize,
                            ),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f),
                        )
                    }
                }
            }
        }
    }
}

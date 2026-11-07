package dev.tomasmed.jlight.wear.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.tomasmed.jlight.wear.data.sync.JlightPeriodicSyncJobService
import dev.tomasmed.jlight.wear.data.sync.WearSyncCoordinator
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import dev.tomasmed.jlight.wear.domain.repository.JlightRepository
import dev.tomasmed.jlight.wear.domain.voice.VoiceEventTranscriptParser
import dev.tomasmed.jlight.wear.ui.screens.ChoresChecklistScreen
import dev.tomasmed.jlight.wear.ui.screens.EventDetailScreen
import dev.tomasmed.jlight.wear.ui.screens.PairingScreen
import dev.tomasmed.jlight.wear.ui.theme.JlightOslColors
import dev.tomasmed.jlight.wear.ui.theme.JlightWearTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface WearDestination {
    data object Agenda : WearDestination
    data object Pairing : WearDestination
    data object ChoresAndLists : WearDestination
    data class EventDetail(val occurrence: EventOccurrence) : WearDestination
}

private enum class VoiceInputMode {
    CREATE_EVENT,
    JOIN_CODE,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JlightPeriodicSyncJobService.schedulePeriodicSync(this)
        val repository = WearSyncCoordinator.provideRepository(this)
        val deepLinkOccurrenceId = intent?.getStringExtra(EXTRA_OCCURRENCE_ID)

        setContent {
            JlightWearTheme {
                JlightWearRootApp(
                    repository = repository,
                    deepLinkOccurrenceId = deepLinkOccurrenceId,
                    onSurfaceRefreshRequested = {
                        WearSyncCoordinator.requestSurfaceUpdates(this@MainActivity)
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
        const val DEFAULT_HOST_URL = "https://jlightcalendar.vercel.app"
    }
}

@Composable
fun JlightWearRootApp(
    repository: JlightRepository,
    deepLinkOccurrenceId: String? = null,
    onSurfaceRefreshRequested: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val pairingState by repository.observePairingState().collectAsState(initial = PairingSessionState.Unpaired)
    val occurrences by repository.observeOccurrences(
        windowStart = today,
        windowEnd = today.plusDays(1),
    ).collectAsState(initial = emptyList())
    val chores by repository.observeChores().collectAsState(initial = emptyList())
    val sharedLists by repository.observeSharedLists().collectAsState(initial = emptyList())

    var currentDestination by remember { mutableStateOf<WearDestination>(WearDestination.Agenda) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var voiceMode by remember { mutableStateOf(VoiceInputMode.CREATE_EVENT) }
    var consumedDeepLink by remember { mutableStateOf<String?>(null) }

    // Navigate directly to EventDetailScreen when deep-linked from AgendaTileService
    LaunchedEffect(deepLinkOccurrenceId, occurrences) {
        if (!deepLinkOccurrenceId.isNullOrBlank() && consumedDeepLink != deepLinkOccurrenceId) {
            val target = occurrences.firstOrNull {
                it.occurrenceId == deepLinkOccurrenceId || it.masterEventId == deepLinkOccurrenceId
            }
            if (target != null) {
                consumedDeepLink = deepLinkOccurrenceId
                currentDestination = WearDestination.EventDetail(target)
            }
        }
    }

    // Trigger remote sync automatically when watch transitions to Paired state
    LaunchedEffect(pairingState) {
        if (pairingState is PairingSessionState.Paired) {
            repository.syncWithRemote()
            onSurfaceRefreshRequested()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                scope.launch {
                    when (voiceMode) {
                        VoiceInputMode.CREATE_EVENT -> {
                            val parsed = VoiceEventTranscriptParser.parse(spoken, today)
                            val created = repository.createEvent(
                                title = parsed.title,
                                date = parsed.date,
                                startTime = parsed.startTime,
                            )
                            statusMessage = if (created.isSuccess) {
                                onSurfaceRefreshRequested()
                                "Added: ${parsed.title}"
                            } else {
                                "Saved locally: ${parsed.title}"
                            }
                        }

                        VoiceInputMode.JOIN_CODE -> {
                            val normalizedCode = spoken.uppercase().replace(" ", "-")
                            val pairResult = repository.pairWithJoinCode(
                                baseUrl = MainActivity.DEFAULT_HOST_URL,
                                joinCode = normalizedCode,
                            )
                            if (pairResult.isSuccess) {
                                statusMessage = "Paired with galaxy!"
                                currentDestination = WearDestination.Agenda
                                onSurfaceRefreshRequested()
                            } else {
                                statusMessage = pairResult.exceptionOrNull()?.message ?: "Join code failed"
                            }
                        }
                    }
                }
            }
        }
    }

    fun launchSpeechRecognizer(prompt: String, mode: VoiceInputMode) {
        voiceMode = mode
        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
            speechLauncher.launch(intent)
        }.onFailure {
            statusMessage = "Voice input unavailable on this emulator/device."
        }
    }

    when (val dest = currentDestination) {
        is WearDestination.Agenda -> {
            AgendaWatchScreen(
                occurrences = occurrences,
                pairingState = pairingState,
                pendingChoreCount = chores.count { !it.completed },
                statusMessage = statusMessage,
                today = today,
                onSelectOccurrence = { occ -> currentDestination = WearDestination.EventDetail(occ) },
                onOpenChoresAndLists = { currentDestination = WearDestination.ChoresAndLists },
                onOpenPairing = { currentDestination = WearDestination.Pairing },
                onQuickVoiceEvent = {
                    launchSpeechRecognizer(
                        prompt = "Speak event (e.g. Dentist tomorrow at 3 PM)",
                        mode = VoiceInputMode.CREATE_EVENT,
                    )
                },
                onSyncRequested = {
                    scope.launch {
                        val syncRes = repository.syncWithRemote()
                        statusMessage = if (syncRes.isSuccess) {
                            onSurfaceRefreshRequested()
                            "Synced with galaxy"
                        } else {
                            "Offline • Showing cached schedule"
                        }
                    }
                },
            )
        }

        is WearDestination.EventDetail -> {
            EventDetailScreen(
                occurrence = dest.occurrence,
                onBack = { currentDestination = WearDestination.Agenda },
            )
        }

        is WearDestination.ChoresAndLists -> {
            ChoresChecklistScreen(
                chores = chores,
                sharedLists = sharedLists,
                onToggleChore = { chore: ChoreItem ->
                    scope.launch {
                        repository.toggleChoreCompleted(chore.id, !chore.completed)
                        onSurfaceRefreshRequested()
                    }
                },
                onToggleListItem = { item: SharedListItem ->
                    scope.launch {
                        repository.toggleListItemCompleted(item.id, !item.completed)
                        onSurfaceRefreshRequested()
                    }
                },
                onBack = { currentDestination = WearDestination.Agenda },
            )
        }

        is WearDestination.Pairing -> {
            PairingScreen(
                pairingState = pairingState,
                baseUrl = MainActivity.DEFAULT_HOST_URL,
                statusMessage = statusMessage,
                onStartDeviceHandshake = {
                    scope.launch {
                        val res = repository.initiateDevicePairing(MainActivity.DEFAULT_HOST_URL)
                        if (res.isFailure) {
                            statusMessage = res.exceptionOrNull()?.message ?: "Handshake failed"
                        } else {
                            statusMessage = null
                        }
                    }
                },
                onPollDeviceHandshake = { deviceCode ->
                    scope.launch {
                        val res = repository.pollDevicePairing(
                            baseUrl = MainActivity.DEFAULT_HOST_URL,
                            deviceCode = deviceCode,
                        )
                        if (res.getOrNull() is PairingSessionState.Paired) {
                            statusMessage = "Approved!"
                            currentDestination = WearDestination.Agenda
                            onSurfaceRefreshRequested()
                        }
                    }
                },
                onVoiceJoinCodeRequest = {
                    launchSpeechRecognizer(
                        prompt = "Speak Galaxy Join Code (GLX-XXXXX)",
                        mode = VoiceInputMode.JOIN_CODE,
                    )
                },
                onDismissToDemoAgenda = { currentDestination = WearDestination.Agenda },
            )
        }
    }
}

@Composable
fun AgendaWatchScreen(
    occurrences: List<EventOccurrence>,
    modifier: Modifier = Modifier,
    pairingState: PairingSessionState = PairingSessionState.Unpaired,
    pendingChoreCount: Int = 0,
    statusMessage: String? = null,
    today: LocalDate = LocalDate.now(),
    onSelectOccurrence: (EventOccurrence) -> Unit = {},
    onOpenChoresAndLists: () -> Unit = {},
    onOpenPairing: () -> Unit = {},
    onQuickVoiceEvent: () -> Unit = {},
    onSyncRequested: () -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val todayOccurrences = remember(occurrences, today) {
        occurrences.filter { it.occurrenceDate == today }
    }
    val tomorrowOccurrences = remember(occurrences, today) {
        occurrences.filter { it.occurrenceDate == today.plusDays(1) }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(JlightOslColors.TrueBlack),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "⌚ Jlight Galaxy",
                style = MaterialTheme.typography.titleMedium,
                color = JlightOslColors.FluorescentCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
            )
        }

        if (!statusMessage.isNullOrBlank()) {
            item {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentEmerald,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = JlightOslColors.FluorescentCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (todayOccurrences.isEmpty()) {
            item {
                Text(
                    text = "No events remaining today",
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(
                items = todayOccurrences,
                key = { "today-${it.occurrenceId}" },
            ) { occurrence ->
                OccurrenceWatchCard(
                    occurrence = occurrence,
                    timeFormatter = timeFormatter,
                    onClick = { onSelectOccurrence(occurrence) },
                )
            }
        }

        if (tomorrowOccurrences.isNotEmpty()) {
            item {
                Text(
                    text = "TOMORROW",
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            items(
                items = tomorrowOccurrences,
                key = { "tomorrow-${it.occurrenceId}" },
            ) { occurrence ->
                OccurrenceWatchCard(
                    occurrence = occurrence,
                    timeFormatter = timeFormatter,
                    onClick = { onSelectOccurrence(occurrence) },
                )
            }
        }

        item {
            Button(
                onClick = onOpenChoresAndLists,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                val badge = if (pendingChoreCount > 0) " ($pendingChoreCount)" else ""
                Text(
                    text = "★ Chores & Lists$badge",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item {
            Card(
                onClick = onQuickVoiceEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Voice Quick-Add",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.FluorescentCyan,
                    )
                    Text(
                        text = "+ Speak New Event",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JlightOslColors.TextPrimary,
                    )
                }
            }
        }

        item {
            val pairLabel = when (pairingState) {
                is PairingSessionState.Paired -> "Synced • ${pairingState.galaxyId}"
                is PairingSessionState.AwaitingClaim -> "Pending Approval (${pairingState.userCode})"
                is PairingSessionState.Unpaired -> "Pair Watch with Galaxy QR"
            }
            Card(
                onClick = if (pairingState is PairingSessionState.Paired) onSyncRequested else onOpenPairing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (pairingState is PairingSessionState.Paired) "Tap to Refresh" else "Agent Auth",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.FluorescentAmber,
                    )
                    Text(
                        text = pairLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JlightOslColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OccurrenceWatchCard(
    occurrence: EventOccurrence,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val timeLabel = occurrence.startTime?.format(timeFormatter) ?: "All Day"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                occurrence.memberIds.take(3).forEach { member ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(JlightOslColors.colorForMember(member)),
                    )
                }
                Text(
                    text = "$timeLabel • ${occurrence.memberIds.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = occurrence.title,
                style = MaterialTheme.typography.bodyMedium,
                color = JlightOslColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

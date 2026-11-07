package dev.tomasmed.jlight.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.ui.theme.JlightOslColors
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Full event detail view on circular Wear OS AMOLED screen (Milestone 4).
 */
@Composable
fun EventDetailScreen(
    occurrence: EventOccurrence,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
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
                text = occurrence.title,
                style = MaterialTheme.typography.titleMedium,
                color = JlightOslColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            val dateLabel = occurrence.occurrenceDate.format(dateFormatter)
            val timeRangeLabel = when {
                occurrence.startTime != null && occurrence.endTime != null ->
                    "${occurrence.startTime.format(timeFormatter)} – ${occurrence.endTime.format(timeFormatter)}"
                occurrence.startTime != null ->
                    occurrence.startTime.format(timeFormatter)
                else -> "All Day"
            }

            Card(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = dateLabel + if (occurrence.isRecurring) " • ↻ Recurring" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.FluorescentCyan,
                    )
                    Text(
                        text = timeRangeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JlightOslColors.TextPrimary,
                    )
                }
            }
        }

        item {
            Card(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Inhabitants",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.TextSecondary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        occurrence.memberIds.take(4).forEach { member ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(JlightOslColors.colorForMember(member)),
                            )
                        }
                        Text(
                            text = occurrence.memberIds.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = JlightOslColors.FluorescentEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (!occurrence.location.isNullOrBlank()) {
            item {
                Card(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelSmall,
                            color = JlightOslColors.FluorescentAmber,
                        )
                        Text(
                            text = occurrence.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JlightOslColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (!occurrence.description.isNullOrBlank()) {
            item {
                Card(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = JlightOslColors.TextSecondary,
                        )
                        Text(
                            text = occurrence.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JlightOslColors.TextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = "Done",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

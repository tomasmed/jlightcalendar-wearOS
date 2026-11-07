package dev.tomasmed.jlight.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.rrule.WallClockRecurrenceExpander
import dev.tomasmed.jlight.wear.ui.theme.JlightOslColors
import dev.tomasmed.jlight.wear.ui.theme.JlightWearTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JlightWearTheme {
                JlightWearHomeScreen()
            }
        }
    }
}

@Composable
fun JlightWearHomeScreen() {
    val today = remember { LocalDate.now() }
    val sampleOccurrences = remember(today) {
        val sampleEvents = listOf(
            CalendarEvent(
                id = "evt-morning-sync",
                galaxyId = "home-galaxy",
                title = "Family Morning Huddle",
                date = today,
                time = LocalTime.of(8, 30),
                memberIds = listOf("Everyone"),
                rrule = "FREQ=DAILY",
            ),
            CalendarEvent(
                id = "evt-dinner",
                galaxyId = "home-galaxy",
                title = "Taco Night & Chores Check",
                date = today,
                time = LocalTime.of(18, 0),
                memberIds = listOf("Tomas", "Everyone"),
            ),
        )
        WallClockRecurrenceExpander.expandEventsInWindow(
            events = sampleEvents,
            windowStart = today,
            windowEnd = today.plusDays(1),
        )
    }

    AgendaWatchScreen(occurrences = sampleOccurrences)
}

@Composable
fun AgendaWatchScreen(
    occurrences: List<EventOccurrence>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

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
                    .padding(bottom = 4.dp),
            )
        }

        items(
            items = occurrences,
            key = { it.occurrenceId },
        ) { occurrence ->
            Card(
                onClick = { /* Navigates to EventDetailScreen in M4 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val timeLabel = occurrence.startTime?.format(timeFormatter) ?: "All Day"
                    Text(
                        text = "$timeLabel • ${occurrence.memberIds.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JlightOslColors.FluorescentEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    }
}

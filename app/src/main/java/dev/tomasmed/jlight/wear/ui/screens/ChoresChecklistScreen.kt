package dev.tomasmed.jlight.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import dev.tomasmed.jlight.wear.ui.theme.JlightOslColors

/**
 * Interactive Chores & Shared Lists checklist screen for Wear OS (Milestone 4).
 *
 * Enforces `48dp` minimum vertical hit targets and triggers `PATCH /api/chores` (star rewards)
 * and `PATCH /api/lists` mutations.
 */
@Composable
fun ChoresChecklistScreen(
    chores: List<ChoreItem>,
    sharedLists: List<SharedListItem>,
    onToggleChore: (ChoreItem) -> Unit,
    onToggleListItem: (SharedListItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()

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
                text = "★ Chores & Lists",
                style = MaterialTheme.typography.titleMedium,
                color = JlightOslColors.FluorescentAmber,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (chores.isEmpty() && sharedLists.isEmpty()) {
            item {
                Text(
                    text = "All chores & list items complete!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JlightOslColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (chores.isNotEmpty()) {
            item {
                Text(
                    text = "HOUSEHOLD CHORES",
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(
                items = chores,
                key = { "chore-${it.id}" },
            ) { chore ->
                Card(
                    onClick = { onToggleChore(chore) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val statusPrefix = if (chore.completed) "✓ Done" else "○ Tap to complete"
                        Text(
                            text = "$statusPrefix • ${chore.memberId} • ★${chore.stars}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chore.completed) JlightOslColors.FluorescentEmerald else JlightOslColors.FluorescentAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = chore.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (chore.completed) JlightOslColors.TextSecondary else JlightOslColors.TextPrimary,
                            textDecoration = if (chore.completed) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (sharedLists.isNotEmpty()) {
            item {
                Text(
                    text = "SHARED CHECKLIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = JlightOslColors.FluorescentEmerald,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(
                items = sharedLists,
                key = { "list-${it.id}" },
            ) { listItem ->
                Card(
                    onClick = { onToggleListItem(listItem) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val badge = if (listItem.completed) "✓ Checked" else "□ ${listItem.category}"
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = JlightOslColors.FluorescentCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listItem.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (listItem.completed) JlightOslColors.TextSecondary else JlightOslColors.TextPrimary,
                            textDecoration = if (listItem.completed) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
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
                    text = "Back to Agenda",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

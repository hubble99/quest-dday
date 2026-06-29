package com.questdday.ui.screen.today

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.questdday.ui.component.QuestCard
import com.questdday.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayQuestsViewModel,
    onNavigateToCreateQuest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Ticker that forces recomposition of elapsed time every second
    var tickTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tickTrigger++
        }
    }
    val currentEpochSeconds = remember(tickTrigger) {
        Instant.now().epochSecond
    }

    // Date formatting with Indonesian fallback
    val todayDateString = remember {
        try {
            val now = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
            now.format(formatter)
        } catch (e: Exception) {
            LocalDate.now().toString()
        }
    }

    // Collect events (Level Up, Error)
    LaunchedEffect(viewModel) {
        viewModel.levelUpEvent.collect { event ->
            Toast.makeText(
                context,
                "🎉 LEVEL UP! Atribut ID ${event.attributeId} naik dari level ${event.previousLevel} ke ${event.newLevel}!",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.errorEvent.collect { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            ) {
                Text(
                    text = "Today's Quests",
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = todayDateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is TodayQuestsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "Memuat Quest...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is TodayQuestsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.displayLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Button(onClick = { viewModel.initialize() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is TodayQuestsUiState.Success -> {
                    if (state.quests.isEmpty()) {
                        Column(
                            modifier = Modifier
                                  .fillMaxSize()
                                  .padding(Spacing.md),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✨",
                                style = MaterialTheme.typography.displayLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "Tidak ada quest hari ini",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Button(onClick = onNavigateToCreateQuest) {
                                Text("Buat Quest")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(state.quests, key = { it.id }) { quest ->
                                val runningTimer = state.runningTimers.find { it.questId == quest.id }
                                val pendingTimer = state.pendingConfirmationTimers.find { it.questId == quest.id }
                                val attribute = state.attributes[quest.attributeId]
                                val isCompleted = state.completedQuestIds.contains(quest.id)

                                QuestCard(
                                    quest = quest,
                                    attribute = attribute,
                                    isCompleted = isCompleted,
                                    runningTimer = runningTimer,
                                    pendingTimer = pendingTimer,
                                    currentEpochSeconds = currentEpochSeconds,
                                    onCompleteInstant = {
                                        viewModel.completeInstantQuest(quest.id, quest.attributeId ?: 1L)
                                    },
                                    onStartTimer = {
                                        viewModel.startTimer(quest.id, quest.targetDurationSeconds ?: 0)
                                    },
                                    onCancelTimer = {
                                        viewModel.cancelTimer(quest.id)
                                    },
                                    onConfirmTimer = {
                                        viewModel.confirmTimerComplete(
                                            quest.id,
                                            quest.attributeId ?: 1L,
                                            quest.targetDurationSeconds ?: 0
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

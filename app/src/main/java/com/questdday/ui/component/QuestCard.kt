package com.questdday.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.questdday.domain.model.ActiveTimer
import com.questdday.domain.model.Attribute
import com.questdday.domain.model.Quest
import com.questdday.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuestCard(
    quest: Quest,
    attribute: Attribute?,
    isCompleted: Boolean,
    runningTimer: ActiveTimer?,
    pendingTimer: ActiveTimer?,
    currentEpochSeconds: Long,
    onCompleteInstant: () -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onConfirmTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (isCompleted) 0.5f else 1.0f
    
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Title & Attribute details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    
                    val attrText = attribute?.let { "${it.icon ?: "✨"} ${it.displayName}" } ?: "General"
                    Text(
                        text = attrText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                // Right side: Action Button based on Completion Mode & Status
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(Spacing.xs)
                        ) {
                            Text(
                                text = "Selesai ✓",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                            )
                        }
                    } else {
                        when (quest.completionMode) {
                            "instant" -> {
                                IconButton(
                                    onClick = onCompleteInstant,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selesai"
                                    )
                                }
                            }
                            "timer" -> {
                                when {
                                    pendingTimer != null -> {
                                        Button(
                                            onClick = onConfirmTimer,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2E7D32) // Green color
                                            ),
                                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                                        ) {
                                            Text(
                                                text = "Konfirmasi",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    runningTimer != null -> {
                                        Button(
                                            onClick = onCancelTimer,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                                        ) {
                                            Text(
                                                text = "Batal",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = onStartTimer,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                                        ) {
                                            Text(
                                                text = "Mulai",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Default fallback just in case
                                IconButton(
                                    onClick = onCompleteInstant,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selesai"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanded info below for running/pending timers
            if (!isCompleted && quest.completionMode == "timer") {
                when {
                    pendingTimer != null -> {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "Selesai — Konfirmasi",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    runningTimer != null -> {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        val startedSeconds = parseTimestampToEpochSeconds(runningTimer.startedAt)
                        val elapsed = (currentEpochSeconds - startedSeconds).coerceAtLeast(0L)
                        val remaining = (runningTimer.targetDurationSeconds - elapsed).coerceAtLeast(0L)
                        val progress = if (runningTimer.targetDurationSeconds > 0) {
                            (elapsed.toFloat() / runningTimer.targetDurationSeconds).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sisa Waktu: ${formatDuration(remaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseTimestampToEpochSeconds(timestamp: String): Long {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        LocalDateTime.parse(timestamp, formatter)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
    } catch (e: Exception) {
        0L
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

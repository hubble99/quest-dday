package com.questdday.data.local.entity

import com.questdday.domain.model.QuestHistory

fun QuestHistoryEntity.toDomainModel(): QuestHistory {
    return QuestHistory(
        id = id,
        originalQuestId = originalQuestId,
        userId = userId,
        title = title,
        finalStatus = finalStatus,
        totalDaysCompleted = totalDaysCompleted,
        totalExpEarned = totalExpEarned,
        startedAt = startedAt,
        endedAt = endedAt
    )
}

fun QuestHistory.toEntity(): QuestHistoryEntity {
    return QuestHistoryEntity(
        id = id,
        originalQuestId = originalQuestId,
        userId = userId,
        title = title,
        finalStatus = finalStatus,
        totalDaysCompleted = totalDaysCompleted,
        totalExpEarned = totalExpEarned,
        startedAt = startedAt,
        endedAt = endedAt
    )
}

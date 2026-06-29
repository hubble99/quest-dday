package com.questdday.data.local.entity

import com.questdday.domain.model.Quest

fun QuestEntity.toDomainModel(): Quest {
    return Quest(
        id = id,
        userId = userId,
        parentQuestId = parentQuestId,
        attributeId = attributeId,
        title = title,
        description = description,
        isContainer = isContainer == 1,
        completionMode = completionMode,
        targetDurationSeconds = targetDurationSeconds,
        durationType = durationType,
        durationInputType = durationInputType,
        targetDays = targetDays,
        startDate = startDate,
        endDate = endDate,
        absenceMode = absenceMode,
        stackedDurationSeconds = stackedDurationSeconds,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays,
        status = status,
        consecutiveMissedSessions = consecutiveMissedSessions,
        lastCompletedAt = lastCompletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Quest.toEntity(): QuestEntity {
    return QuestEntity(
        id = id,
        userId = userId,
        parentQuestId = parentQuestId,
        attributeId = attributeId,
        title = title,
        description = description,
        isContainer = if (isContainer) 1 else 0,
        completionMode = completionMode,
        targetDurationSeconds = targetDurationSeconds,
        durationType = durationType,
        durationInputType = durationInputType,
        targetDays = targetDays,
        startDate = startDate,
        endDate = endDate,
        absenceMode = absenceMode,
        stackedDurationSeconds = stackedDurationSeconds,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays,
        status = status,
        consecutiveMissedSessions = consecutiveMissedSessions,
        lastCompletedAt = lastCompletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

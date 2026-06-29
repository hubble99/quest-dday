package com.questdday.data.local.entity

import com.questdday.domain.model.QuestLog

fun QuestLogEntity.toDomainModel(): QuestLog {
    return QuestLog(
        id = id,
        questId = questId,
        userId = userId,
        logDate = logDate,
        actualDurationSeconds = actualDurationSeconds,
        expAwarded = expAwarded,
        isEpicFinaleBonus = isEpicFinaleBonus == 1,
        completedAt = completedAt
    )
}

fun QuestLog.toEntity(): QuestLogEntity {
    return QuestLogEntity(
        id = id,
        questId = questId,
        userId = userId,
        logDate = logDate,
        actualDurationSeconds = actualDurationSeconds,
        expAwarded = expAwarded,
        isEpicFinaleBonus = if (isEpicFinaleBonus) 1 else 0,
        completedAt = completedAt
    )
}

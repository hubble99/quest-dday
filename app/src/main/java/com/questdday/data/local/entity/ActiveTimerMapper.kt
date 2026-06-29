package com.questdday.data.local.entity

import com.questdday.domain.model.ActiveTimer

fun ActiveTimerEntity.toDomain(): ActiveTimer {
    return ActiveTimer(
        id = id,
        questId = questId,
        startedAt = startedAt,
        targetDurationSeconds = targetDurationSeconds,
        alarmFiredAt = alarmFiredAt
    )
}

fun ActiveTimer.toEntity(): ActiveTimerEntity {
    return ActiveTimerEntity(
        id = id,
        questId = questId,
        startedAt = startedAt,
        targetDurationSeconds = targetDurationSeconds,
        alarmFiredAt = alarmFiredAt
    )
}

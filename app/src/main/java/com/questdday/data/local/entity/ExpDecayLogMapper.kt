package com.questdday.data.local.entity

import com.questdday.domain.model.ExpDecayLog

fun ExpDecayLogEntity.toDomain(): ExpDecayLog {
    return ExpDecayLog(
        id = id,
        userAttributeStatId = userAttributeStatId,
        inactiveDays = inactiveDays,
        expBefore = expBefore,
        expAfter = expAfter,
        decayRateUsed = decayRateUsed,
        processedAt = processedAt
    )
}

fun ExpDecayLog.toEntity(): ExpDecayLogEntity {
    return ExpDecayLogEntity(
        id = id,
        userAttributeStatId = userAttributeStatId,
        inactiveDays = inactiveDays,
        expBefore = expBefore,
        expAfter = expAfter,
        decayRateUsed = decayRateUsed,
        processedAt = processedAt
    )
}

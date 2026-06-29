package com.questdday.data.local.entity

import com.questdday.domain.model.UserAttributeStat

fun UserAttributeStatEntity.toDomainModel(): UserAttributeStat {
    return UserAttributeStat(
        id = id,
        userId = userId,
        attributeId = attributeId,
        currentLevel = currentLevel,
        currentExp = currentExp,
        lastGainedAt = lastGainedAt,
        updatedAt = updatedAt
    )
}

fun UserAttributeStat.toEntity(): UserAttributeStatEntity {
    return UserAttributeStatEntity(
        id = id,
        userId = userId,
        attributeId = attributeId,
        currentLevel = currentLevel,
        currentExp = currentExp,
        lastGainedAt = lastGainedAt,
        updatedAt = updatedAt
    )
}

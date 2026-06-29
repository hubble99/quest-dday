package com.questdday.data.local.entity

import com.questdday.domain.model.User

fun UserEntity.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        lastActiveAt = lastActiveAt,
        lastEvaluatedDate = lastEvaluatedDate,
        consecutiveInactiveScheduledDays = consecutiveInactiveScheduledDays,
        totalExpEarnedLifetime = totalExpEarnedLifetime,
        hasSeenWelcome = hasSeenWelcome == 1,
        createdAt = createdAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        lastActiveAt = lastActiveAt,
        lastEvaluatedDate = lastEvaluatedDate,
        consecutiveInactiveScheduledDays = consecutiveInactiveScheduledDays,
        totalExpEarnedLifetime = totalExpEarnedLifetime,
        hasSeenWelcome = if (hasSeenWelcome) 1 else 0,
        createdAt = createdAt
    )
}

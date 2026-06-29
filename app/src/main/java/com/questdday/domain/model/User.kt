package com.questdday.domain.model

data class User(
    val id: Long = 0,
    val username: String = "Adventurer",
    val lastActiveAt: String,
    val lastEvaluatedDate: String?,
    val consecutiveInactiveScheduledDays: Int = 0,
    val totalExpEarnedLifetime: Double = 0.0,
    val hasSeenWelcome: Boolean = false,
    val createdAt: String
)

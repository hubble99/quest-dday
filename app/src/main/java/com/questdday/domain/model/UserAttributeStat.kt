package com.questdday.domain.model

data class UserAttributeStat(
    val id: Long = 0,
    val userId: Long,
    val attributeId: Long,
    val currentLevel: Int = 1,
    val currentExp: Double = 0.0,
    val lastGainedAt: String?,
    val updatedAt: String
)

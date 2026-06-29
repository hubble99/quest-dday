package com.questdday.domain.model

data class QuestLog(
    val id: Long = 0,
    val questId: Long,
    val userId: Long,
    val logDate: String,
    val actualDurationSeconds: Int?,
    val expAwarded: Double,
    val isEpicFinaleBonus: Boolean,
    val completedAt: String
)

package com.questdday.domain.model

data class QuestHistory(
    val id: Long = 0,
    val originalQuestId: Long,
    val userId: Long,
    val title: String,
    val finalStatus: String,
    val totalDaysCompleted: Int,
    val totalExpEarned: Double,
    val startedAt: String,
    val endedAt: String
)

package com.questdday.domain.model

data class Quest(
    val id: Long = 0,
    val userId: Long,
    val parentQuestId: Long?,
    val attributeId: Long?,
    val title: String,
    val description: String?,
    val isContainer: Boolean,
    val completionMode: String?,
    val targetDurationSeconds: Int?,
    val durationType: String,
    val durationInputType: String?,
    val targetDays: Int?,
    val startDate: String,
    val endDate: String?,
    val absenceMode: String?,
    val stackedDurationSeconds: Int,
    val scheduleType: String?,
    val scheduleDays: String?,
    val status: String,
    val consecutiveMissedSessions: Int,
    val lastCompletedAt: String?,
    val createdAt: String,
    val updatedAt: String
)

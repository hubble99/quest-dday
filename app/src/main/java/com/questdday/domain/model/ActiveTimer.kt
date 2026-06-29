package com.questdday.domain.model

data class ActiveTimer(
    val id: Long = 0,
    val questId: Long,
    val startedAt: String,
    val targetDurationSeconds: Int,
    val alarmFiredAt: String? = null
)

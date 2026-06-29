package com.questdday.domain.model

data class ExpDecayLog(
    val id: Long = 0,
    val userAttributeStatId: Long,
    val inactiveDays: Int,
    val expBefore: Double,
    val expAfter: Double,
    val decayRateUsed: Double,
    val processedAt: String
)

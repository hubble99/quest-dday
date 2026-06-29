package com.questdday.data.repository

interface ExpRepository {
    suspend fun awardExp(
        userId: Long,
        attributeId: Long,
        amount: Double,
        now: String
    )

    suspend fun applyDecay(
        userId: Long,
        inactiveDays: Int,
        decayRateR: Double,
        now: String
    )

    suspend fun getStat(
        userId: Long,
        attributeId: Long
    ): com.questdday.domain.model.UserAttributeStat?
}

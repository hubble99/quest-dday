package com.questdday.data.repository

import com.questdday.domain.model.AppSetting
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAllSettings(): Flow<List<AppSetting>>
    suspend fun getValue(key: String): String?
    suspend fun setValue(key: String, value: String)
    suspend fun getDecayRateR(): Double?
    suspend fun getDecayGracePeriodDays(): Int?
    suspend fun getFailureThresholdSessions(): Int
    suspend fun getEpicFinaleBonus(): Double
}

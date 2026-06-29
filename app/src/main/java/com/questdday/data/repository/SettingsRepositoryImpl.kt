package com.questdday.data.repository

import com.questdday.data.local.dao.AppSettingDao
import com.questdday.data.local.entity.AppSettingEntity
import com.questdday.data.local.entity.toDomain
import com.questdday.domain.model.AppSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val appSettingDao: AppSettingDao
) : SettingsRepository {

    override fun getAllSettings(): Flow<List<AppSetting>> {
        return appSettingDao.getAllSettings().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getValue(key: String): String? {
        return appSettingDao.getValue(key)
    }

    override suspend fun setValue(key: String, value: String) {
        // AppSettingEntity requires updatedAt, we'll use a simple fallback or just pass current time.
        // Actually, the PRD says updated_at has a default of (datetime('now')). 
        // We'll set it here using a simple approach, but usually, we can just insert it.
        appSettingDao.insertOrUpdate(
            AppSettingEntity(
                key = key,
                value = value,
                updatedAt = java.time.LocalDateTime.now().toString()
            )
        )
    }

    override suspend fun getDecayRateR(): Double? {
        val value = getValue("decay_rate_R")
        if (value.isNullOrBlank()) return null
        return value.toDoubleOrNull()
    }

    override suspend fun getDecayGracePeriodDays(): Int? {
        val value = getValue("decay_grace_period_days")
        if (value.isNullOrBlank()) return null
        return value.toIntOrNull()
    }

    override suspend fun getFailureThresholdSessions(): Int {
        val value = getValue("failure_threshold_sessions")
        return value?.toIntOrNull() ?: 7
    }

    override suspend fun getEpicFinaleBonus(): Double {
        val value = getValue("epic_finale_bonus_exp")
        return value?.toDoubleOrNull() ?: 1000.0
    }
}

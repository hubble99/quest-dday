package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettingValue(key: String): Flow<String?>
    suspend fun updateSetting(key: String, value: String)
    // other operations...
}

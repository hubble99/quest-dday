package com.questdday.data.repository

import com.questdday.data.local.dao.AppSettingDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var appSettingDao: AppSettingDao
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        appSettingDao = mockk(relaxed = true)
        repository = SettingsRepositoryImpl(appSettingDao)
    }

    @Test
    fun `getDecayRateR returns null when value is empty string`() = runBlocking {
        coEvery { appSettingDao.getValue("decay_rate_R") } returns ""

        val result = repository.getDecayRateR()

        assertNull(result)
    }

    @Test
    fun `getDecayRateR returns null when value is non-numeric`() = runBlocking {
        coEvery { appSettingDao.getValue("decay_rate_R") } returns "invalid"

        val result = repository.getDecayRateR()

        assertNull(result)
    }

    @Test
    fun `getFailureThresholdSessions returns 7 when setting not found`() = runBlocking {
        coEvery { appSettingDao.getValue("failure_threshold_sessions") } returns null

        val result = repository.getFailureThresholdSessions()

        assertEquals(7, result)
    }

    @Test
    fun `getEpicFinaleBonus returns 1000_0 as default`() = runBlocking {
        coEvery { appSettingDao.getValue("epic_finale_bonus_exp") } returns null

        val result = repository.getEpicFinaleBonus()

        assertEquals(1000.0, result, 0.0)
    }
}

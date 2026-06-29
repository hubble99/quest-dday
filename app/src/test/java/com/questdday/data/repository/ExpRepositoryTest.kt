package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ExpDecayLogDao
import com.questdday.data.local.dao.UserAttributeStatDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.ExpDecayLogEntity
import com.questdday.data.local.entity.UserAttributeStatEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ExpRepositoryTest {

    private lateinit var appDatabase: AppDatabase
    private lateinit var userAttributeStatDao: UserAttributeStatDao
    private lateinit var userDao: UserDao
    private lateinit var expDecayLogDao: ExpDecayLogDao
    private lateinit var repository: ExpRepositoryImpl

    @Before
    fun setup() {
        appDatabase = mockk(relaxed = true)
        userAttributeStatDao = mockk(relaxed = true)
        userDao = mockk(relaxed = true)
        expDecayLogDao = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionSlot = slot<suspend () -> Any>()
        coEvery {
            appDatabase.withTransaction(capture(transactionSlot))
        } coAnswers {
            transactionSlot.captured.invoke()
        }

        repository = ExpRepositoryImpl(appDatabase, userAttributeStatDao, userDao, expDecayLogDao)
    }

    @Test
    fun `awardExp increments exp correctly without level up`() = runBlocking {
        val currentStat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 1,
            currentLevel = 1,
            currentExp = 50.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStat(1, 1) } returns currentStat

        repository.awardExp(1, 1, 30.0, "now")

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 1, 80.0, 1, "now")
            userDao.addLifetimeExp(1, 30.0)
        }
    }

    @Test
    fun `awardExp triggers level up when exp reaches threshold`() = runBlocking {
        val currentStat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 1,
            currentLevel = 1,
            currentExp = 80.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStat(1, 1) } returns currentStat

        // Target for level 1 is 100.0. After 30.0, exp becomes 110.0
        // Level increases to 2, remaining exp is 10.0
        repository.awardExp(1, 1, 30.0, "now")

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 1, 10.0, 2, "now")
            userDao.addLifetimeExp(1, 30.0)
        }
    }

    @Test
    fun `awardExp handles multiple level ups in single call`() = runBlocking {
        val currentStat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 1,
            currentLevel = 1,
            currentExp = 50.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStat(1, 1) } returns currentStat

        // target(1) = 100.0
        // target(2) = 100 * 2^1.5 = 282.8427
        // Add 500.0 -> total = 550.0
        // - 100.0 -> 450.0 (lvl 2)
        // - 282.8427 -> 167.1573 (lvl 3)
        repository.awardExp(1, 1, 500.0, "now")

        coVerify {
            userAttributeStatDao.updateExpAndLevel(
                userId = 1,
                attributeId = 1,
                exp = match { it in 167.157..167.158 },
                level = 3,
                now = "now"
            )
        }
    }

    @Test
    fun `awardExp does not produce negative exp`() = runBlocking {
        val currentStat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 1,
            currentLevel = 1,
            currentExp = 0.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStat(1, 1) } returns currentStat

        repository.awardExp(1, 1, 0.0, "now")

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 1, 0.0, 1, "now")
        }
    }

    @Test
    fun `applyDecay reduces exp by correct amount`() = runBlocking {
        val stat = UserAttributeStatEntity(
            id = 10,
            userId = 1,
            attributeId = 2,
            currentLevel = 3,
            currentExp = 500.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStatsListByUser(1) } returns listOf(stat)

        repository.applyDecay(
            userId = 1,
            inactiveDays = 3,
            decayRateR = 10.0,
            now = "now"
        )
        // expToDeduct = 10.0 * 2 = 20.0
        // new exp = 480.0

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 2, 480.0, 3, "now")
            expDecayLogDao.insertDecayLog(match { 
                it.expBefore == 500.0 && it.expAfter == 480.0 && it.inactiveDays == 3 && it.decayRateUsed == 10.0
            })
        }
    }

    @Test
    fun `applyDecay floors exp at 0, never negative`() = runBlocking {
        val stat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 2,
            currentLevel = 3,
            currentExp = 10.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStatsListByUser(1) } returns listOf(stat)

        repository.applyDecay(
            userId = 1,
            inactiveDays = 5,
            decayRateR = 10.0,
            now = "now"
        )
        // expToDeduct = 10.0 * 4 = 40.0
        // 10.0 - 40.0 = -30.0 -> floored to 0.0

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 2, 0.0, 3, "now")
        }
    }

    @Test
    fun `applyDecay never reduces current_level`() = runBlocking {
        val stat = UserAttributeStatEntity(
            userId = 1,
            attributeId = 2,
            currentLevel = 5,
            currentExp = 10.0,
            lastGainedAt = null,
            updatedAt = "now"
        )
        coEvery { userAttributeStatDao.getStatsListByUser(1) } returns listOf(stat)

        repository.applyDecay(
            userId = 1,
            inactiveDays = 10,
            decayRateR = 50.0,
            now = "now"
        )
        // heavy decay, should not reduce level

        coVerify {
            userAttributeStatDao.updateExpAndLevel(1, 2, 0.0, 5, "now")
        }
    }

    @Test
    fun `applyDecay inserts entry to exp_decay_log for each stat`() = runBlocking {
        val stat1 = UserAttributeStatEntity(id = 1, userId = 1, attributeId = 1, currentLevel = 1, currentExp = 100.0, lastGainedAt = null, updatedAt = "now")
        val stat2 = UserAttributeStatEntity(id = 2, userId = 1, attributeId = 2, currentLevel = 2, currentExp = 200.0, lastGainedAt = null, updatedAt = "now")
        coEvery { userAttributeStatDao.getStatsListByUser(1) } returns listOf(stat1, stat2)

        repository.applyDecay(1, 3, 10.0, "now")

        coVerify(exactly = 2) { expDecayLogDao.insertDecayLog(any()) }
    }

    @Test
    fun `applyDecay skips execution when inactiveDays is less than 2`() = runBlocking {
        repository.applyDecay(1, 1, 10.0, "now")

        coVerify(exactly = 0) {
            userAttributeStatDao.getStatsListByUser(any())
        }
    }
}

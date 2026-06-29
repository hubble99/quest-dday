package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.AttributeEntity
import com.questdday.data.local.entity.ExpDecayLogEntity
import com.questdday.data.local.entity.UserAttributeStatEntity
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpDecayLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expDecayLogDao: ExpDecayLogDao
    private lateinit var userDao: UserDao
    private lateinit var attributeDao: AttributeDao
    private lateinit var userAttributeStatDao: UserAttributeStatDao

    private val testUserId = 1L
    private val testAttrId = 10L
    private val testStatId = 100L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        expDecayLogDao = db.expDecayLogDao()
        userDao = db.userDao()
        attributeDao = db.attributeDao()
        userAttributeStatDao = db.userAttributeStatDao()

        // Arrange: Insert required FK entities
        userDao.insertUser(UserEntity(
            id = testUserId,
            lastActiveAt = "2026-06-29",
            lastEvaluatedDate = null,
            createdAt = "2026-06-29"
        ))
        attributeDao.insertAttribute(AttributeEntity(
            id = testAttrId, 
            code = "STR", 
            displayName = "Strength",
            icon = null
        ))
        userAttributeStatDao.insertStat(
            UserAttributeStatEntity(
                id = testStatId,
                userId = testUserId,
                attributeId = testAttrId,
                lastGainedAt = null,
                updatedAt = "2026-06-29"
            )
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insertDecayLog_and_getRecentDecayLogs_returns_correctly_ordered_logs`() = runTest {
        // Arrange
        val log1 = ExpDecayLogEntity(
            userAttributeStatId = testStatId,
            inactiveDays = 2,
            expBefore = 100.0,
            expAfter = 80.0,
            decayRateUsed = 10.0,
            processedAt = "2026-06-29 10:00:00"
        )
        val log2 = ExpDecayLogEntity(
            userAttributeStatId = testStatId,
            inactiveDays = 3,
            expBefore = 80.0,
            expAfter = 50.0,
            decayRateUsed = 10.0,
            processedAt = "2026-06-30 10:00:00"
        )

        // Act
        expDecayLogDao.insertDecayLog(log1)
        expDecayLogDao.insertDecayLog(log2)
        val recentLogs = expDecayLogDao.getRecentDecayLogs(limit = 10).first()

        // Assert
        assertEquals(2, recentLogs.size)
        // Ordered by processed_at DESC, so log2 should be first
        assertEquals("2026-06-30 10:00:00", recentLogs[0].processedAt)
        assertEquals("2026-06-29 10:00:00", recentLogs[1].processedAt)
    }
}

package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var questDao: QuestDao
    private lateinit var questLogDao: QuestLogDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        questDao = db.questDao()
        questLogDao = db.questLogDao()
        userDao = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun setupUserAndQuest(): Pair<Long, Long> {
        val user = UserEntity(
            username = "TestUser",
            lastActiveAt = "2023-01-01 10:00:00",
            lastEvaluatedDate = null,
            createdAt = "2023-01-01 10:00:00"
        )
        val userId = userDao.insertUser(user)
        val quest = QuestEntity(
            userId = userId,
            parentQuestId = null,
            attributeId = null,
            title = "Test Quest",
            description = "Desc",
            isContainer = 0,
            completionMode = "instant",
            targetDurationSeconds = null,
            durationType = "endless",
            durationInputType = null,
            targetDays = null,
            startDate = "2023-01-01",
            endDate = null,
            absenceMode = null,
            scheduleType = "daily",
            scheduleDays = null,
            status = "active",
            lastCompletedAt = null,
            createdAt = "2023-01-01 10:00:00",
            updatedAt = "2023-01-01 10:00:00"
        )
        val questId = questDao.insertQuest(quest)
        return Pair(userId, questId)
    }

    @Test
    fun `insertLog_and_getLogForDate_works_correctly`() = runTest {
        val (userId, questId) = setupUserAndQuest()
        
        val log = QuestLogEntity(
            questId = questId,
            userId = userId,
            logDate = "2023-01-01",
            actualDurationSeconds = null,
            expAwarded = 10.0,
            isEpicFinaleBonus = 0,
            completedAt = "now"
        )
        questLogDao.insertLog(log)
        
        val result = questLogDao.getLogForDate(questId, "2023-01-01")
        val nullResult = questLogDao.getLogForDate(questId, "2023-01-02")
        
        assertNotNull(result)
        assertEquals(10.0, result?.expAwarded ?: 0.0, 0.0)
        assertNull(nullResult)
    }

    @Test
    fun `getTotalExpForQuest_sums_correctly_and_returns_zero_when_empty`() = runTest {
        val (userId, questId) = setupUserAndQuest()
        
        val emptySum = questLogDao.getTotalExpForQuest(questId)
        assertEquals(0.0, emptySum, 0.0)
        
        val log1 = QuestLogEntity(
            questId = questId,
            userId = userId,
            logDate = "2023-01-01",
            actualDurationSeconds = null,
            expAwarded = 10.0,
            isEpicFinaleBonus = 0,
            completedAt = "now"
        )
        val log2 = QuestLogEntity(
            questId = questId,
            userId = userId,
            logDate = "2023-01-02",
            actualDurationSeconds = null,
            expAwarded = 15.0,
            isEpicFinaleBonus = 0,
            completedAt = "now"
        )
        questLogDao.insertLog(log1)
        questLogDao.insertLog(log2)
        
        val sum = questLogDao.getTotalExpForQuest(questId)
        assertEquals(25.0, sum, 0.0)
    }
}

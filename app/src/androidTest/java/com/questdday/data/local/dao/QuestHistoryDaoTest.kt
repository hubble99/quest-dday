package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestHistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var questHistoryDao: QuestHistoryDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        questHistoryDao = db.questHistoryDao()
        userDao = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun setupUser(): Long {
        val user = UserEntity(
            username = "TestUser",
            lastActiveAt = "2023-01-01 10:00:00",
            lastEvaluatedDate = null,
            createdAt = "2023-01-01 10:00:00"
        )
        return userDao.insertUser(user)
    }

    @Test
    fun `getHistoryByUser_returns_ordered_by_ended_at_DESC`() = runTest {
        val userId = setupUser()
        
        val history1 = QuestHistoryEntity(
            originalQuestId = 1,
            userId = userId,
            title = "History 1",
            finalStatus = "completed",
            startedAt = "2023-01-01",
            endedAt = "2023-01-02"
        )
        val history2 = QuestHistoryEntity(
            originalQuestId = 2,
            userId = userId,
            title = "History 2",
            finalStatus = "failed_abandoned",
            startedAt = "2023-01-01",
            endedAt = "2023-01-03"
        )
        val history3 = QuestHistoryEntity(
            originalQuestId = 3,
            userId = userId,
            title = "History 3",
            finalStatus = "failed_via_parent",
            startedAt = "2023-01-01",
            endedAt = "2023-01-01"
        )
        
        questHistoryDao.insertHistory(history1)
        questHistoryDao.insertHistory(history2)
        questHistoryDao.insertHistory(history3)
        
        val result = questHistoryDao.getHistoryByUser(userId).first()
        
        assertEquals(3, result.size)
        // Check order DESC by endedAt
        assertEquals("History 2", result[0].title) // 2023-01-03
        assertEquals("History 1", result[1].title) // 2023-01-02
        assertEquals("History 3", result[2].title) // 2023-01-01
    }
}

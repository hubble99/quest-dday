package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var questDao: QuestDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        questDao = db.questDao()
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
    fun `insertQuest_and_getQuestById_returns_correct_data`() = runTest {
        // Arrange
        val userId = setupUser()
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

        // Act
        val id = questDao.insertQuest(quest)
        val result = questDao.getQuestById(id)

        // Assert
        assertNotNull(result)
        assertEquals("Test Quest", result?.title)
    }

    @Test
    fun `getActiveExecutableQuests_excludes_is_container_and_inactive`() = runTest {
        // Arrange
        val userId = setupUser()
        val baseQuest = QuestEntity(
            userId = userId,
            parentQuestId = null,
            attributeId = null,
            title = "Test Quest",
            description = null,
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
            createdAt = "2023-01-01",
            updatedAt = "2023-01-01"
        )
        questDao.insertQuest(baseQuest.copy(title = "Executable Active"))
        questDao.insertQuest(baseQuest.copy(title = "Container Active", isContainer = 1, completionMode = null, scheduleType = null))
        questDao.insertQuest(baseQuest.copy(title = "Executable Completed", status = "completed"))

        // Act
        val result = questDao.getActiveExecutableQuests(userId).first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("Executable Active", result[0].title)
    }

    @Test
    fun `getDailyQuestsForToday_filters_correctly_based_on_dates`() = runTest {
        // Arrange
        val userId = setupUser()
        val baseQuest = QuestEntity(
            userId = userId,
            parentQuestId = null,
            attributeId = null,
            title = "Base",
            description = null,
            isContainer = 0,
            completionMode = "instant",
            targetDurationSeconds = null,
            durationType = "time_bound",
            durationInputType = "date",
            targetDays = null,
            startDate = "2023-01-02",
            endDate = "2023-01-10",
            absenceMode = "shift",
            scheduleType = "daily",
            scheduleDays = null,
            status = "active",
            lastCompletedAt = null,
            createdAt = "2023-01-01",
            updatedAt = "2023-01-01"
        )
        questDao.insertQuest(baseQuest.copy(title = "Valid Today"))
        questDao.insertQuest(baseQuest.copy(title = "Future Start", startDate = "2023-01-10", endDate = "2023-01-20"))
        questDao.insertQuest(baseQuest.copy(title = "Past End", startDate = "2022-01-01", endDate = "2023-01-01"))
        questDao.insertQuest(baseQuest.copy(title = "Endless", startDate = "2022-01-01", endDate = null))

        // Act
        val result = questDao.getDailyQuestsForToday(userId, "2023-01-05").first()

        // Assert
        assertEquals(2, result.size)
        val titles = result.map { it.title }
        assertTrue(titles.contains("Valid Today"))
        assertTrue(titles.contains("Endless"))
    }

    @Test
    fun `deleteQuest_cascades_to_sub_quests`() = runTest {
        // Arrange
        val userId = setupUser()
        val baseQuest = QuestEntity(
            userId = userId,
            parentQuestId = null,
            attributeId = null,
            title = "Base",
            description = null,
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
            createdAt = "2023-01-01",
            updatedAt = "2023-01-01"
        )
        val parentId = questDao.insertQuest(baseQuest.copy(title = "Parent", isContainer = 1, completionMode = null, scheduleType = null))
        val childId = questDao.insertQuest(baseQuest.copy(title = "Child", parentQuestId = parentId))

        // Act
        questDao.deleteQuest(parentId)

        // Assert
        assertNull(questDao.getQuestById(parentId))
        assertNull(questDao.getQuestById(childId))
    }

    @Test
    fun `addStackedDuration_and_resetStackedDuration_work_correctly`() = runTest {
        // Arrange
        val userId = setupUser()
        val quest = QuestEntity(
            userId = userId,
            parentQuestId = null,
            attributeId = null,
            title = "Stack Quest",
            description = null,
            isContainer = 0,
            completionMode = "timer",
            targetDurationSeconds = 60,
            durationType = "time_bound",
            durationInputType = "date",
            targetDays = null,
            startDate = "2023-01-01",
            endDate = "2023-01-10",
            absenceMode = "stack",
            scheduleType = "daily",
            scheduleDays = null,
            status = "active",
            lastCompletedAt = null,
            createdAt = "2023-01-01",
            updatedAt = "2023-01-01"
        )
        val id = questDao.insertQuest(quest)

        // Act 1
        questDao.addStackedDuration(id, 60, "now")
        questDao.addStackedDuration(id, 60, "now")
        val result1 = questDao.getQuestById(id)

        // Assert 1
        assertEquals(120, result1?.stackedDurationSeconds)

        // Act 2
        questDao.resetStackedDuration(id, "now2")
        val result2 = questDao.getQuestById(id)

        // Assert 2
        assertEquals(0, result2?.stackedDurationSeconds)
    }
}

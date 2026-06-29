package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insertUser_and_getUserById_returns_correct_data`() = runTest {
        // Arrange
        val user = UserEntity(
            id = 1L,
            username = "TestUser",
            lastActiveAt = "2023-01-01 10:00:00",
            lastEvaluatedDate = null,
            createdAt = "2023-01-01 10:00:00"
        )

        // Act
        userDao.insertUser(user)
        val result = userDao.getUserById(1L).first()

        // Assert
        assertNotNull(result)
        assertEquals("TestUser", result?.username)
        assertEquals(0, result?.consecutiveInactiveScheduledDays)
        assertEquals(0.0, result?.totalExpEarnedLifetime ?: 0.0, 0.0)
        assertEquals(0, result?.hasSeenWelcome)
    }

    @Test
    fun `updateEvaluationDate_updates_correct_fields_only`() = runTest {
        // Arrange
        val user = UserEntity(
            id = 1L,
            lastActiveAt = "2023-01-01 10:00:00",
            lastEvaluatedDate = null,
            createdAt = "2023-01-01 10:00:00"
        )
        userDao.insertUser(user)

        // Act
        userDao.updateEvaluationDate(1L, "2023-01-02", "2023-01-02 12:00:00")
        val result = userDao.getUserById(1L).first()

        // Assert
        assertEquals("2023-01-02", result?.lastEvaluatedDate)
        assertEquals("2023-01-02 12:00:00", result?.lastActiveAt)
    }

    @Test
    fun `markWelcomeSeen_sets_has_seen_welcome_to_1`() = runTest {
        // Arrange
        val user = UserEntity(
            id = 1L,
            lastActiveAt = "2023-01-01 10:00:00",
            lastEvaluatedDate = null,
            createdAt = "2023-01-01 10:00:00"
        )
        userDao.insertUser(user)

        // Act
        userDao.markWelcomeSeen(1L)
        val result = userDao.getUserById(1L).first()

        // Assert
        assertEquals(1, result?.hasSeenWelcome)
    }
}

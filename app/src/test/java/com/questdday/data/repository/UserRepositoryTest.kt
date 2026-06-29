package com.questdday.data.repository

import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.UserEntity
import com.questdday.data.local.entity.toDomainModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        userDao = mockk(relaxed = true)
        userRepository = UserRepositoryImpl(userDao)
    }

    @Test
    fun `getUser maps entity to domain model correctly`() = runTest {
        // Arrange
        val entity = UserEntity(
            id = 1L,
            username = "Test User",
            lastActiveAt = "2026-06-29T10:00:00Z",
            lastEvaluatedDate = null,
            consecutiveInactiveScheduledDays = 0,
            totalExpEarnedLifetime = 100.0,
            hasSeenWelcome = 1,
            createdAt = "2026-06-20T10:00:00Z"
        )
        every { userDao.getUserById(1L) } returns flowOf(entity)

        // Act
        val result = userRepository.getUser().first()

        // Assert
        assertEquals(entity.toDomainModel(), result)
        assertEquals("Test User", result?.username)
        assertEquals(true, result?.hasSeenWelcome)
    }

    @Test
    fun `markWelcomeSeen calls dao with correct userId`() = runTest {
        // Arrange
        // (Nothing specific to arrange as we use relaxed mockk)

        // Act
        userRepository.markWelcomeSeen()

        // Assert
        coVerify(exactly = 1) { userDao.markWelcomeSeen(1L) }
    }

    @Test
    fun `updateEvaluationDate calls dao with correct parameters`() = runTest {
        // Arrange
        val date = "2026-06-29"
        val now = "2026-06-29T12:00:00Z"

        // Act
        userRepository.updateEvaluationDate(date, now)

        // Assert
        coVerify(exactly = 1) { userDao.updateEvaluationDate(1L, date, now) }
    }

    @Test
    fun `updateInactiveDays calls dao with correct parameters`() = runTest {
        // Arrange
        val days = 5

        // Act
        userRepository.updateInactiveDays(days)

        // Assert
        coVerify(exactly = 1) { userDao.updateInactiveDays(1L, days) }
    }

    @Test
    fun `addLifetimeExp calls dao with correct parameters`() = runTest {
        // Arrange
        val exp = 50.5

        // Act
        userRepository.addLifetimeExp(exp)

        // Assert
        coVerify(exactly = 1) { userDao.addLifetimeExp(1L, exp) }
    }
}

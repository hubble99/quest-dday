package com.questdday.data.repository

import com.questdday.data.local.dao.AttributeDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.entity.AttributeEntity
import com.questdday.data.local.entity.toDomainModel
import com.questdday.domain.exception.QuestValidationException
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

class AttributeRepositoryTest {

    private lateinit var attributeDao: AttributeDao
    private lateinit var questDao: QuestDao
    private lateinit var attributeRepository: AttributeRepositoryImpl

    @Before
    fun setup() {
        attributeDao = mockk(relaxed = true)
        questDao = mockk(relaxed = true)
        attributeRepository = AttributeRepositoryImpl(attributeDao, questDao)
    }

    @Test
    fun `getAllAttributes maps list of entities to domain models`() = runTest {
        // Arrange
        val entity1 = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = "💪", isDefault = 1, sortOrder = 1)
        val entity2 = AttributeEntity(id = 2L, code = "INT", displayName = "Intelligence", icon = "🧠", isDefault = 1, sortOrder = 2)
        every { attributeDao.getAllAttributes() } returns flowOf(listOf(entity1, entity2))

        // Act
        val result = attributeRepository.getAllAttributes().first()

        // Assert
        assertEquals(2, result.size)
        assertEquals(entity1.toDomainModel(), result[0])
        assertEquals(entity2.toDomainModel(), result[1])
    }

    @Test
    fun `getAttributeById maps entity to domain model`() = runTest {
        // Arrange
        val entity = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = "💪", isDefault = 1, sortOrder = 1)
        coEvery { attributeDao.getAttributeById(1L) } returns entity

        // Act
        val result = attributeRepository.getAttributeById(1L)

        // Assert
        assertEquals(entity.toDomainModel(), result)
    }

    @Test(expected = QuestValidationException::class)
    fun `addCustomAttribute throws when name is empty`() = runTest {
        // Act
        attributeRepository.addCustomAttribute(code = "TEST", name = "  ", icon = null)
        // Assert via expected exception
    }

    @Test(expected = QuestValidationException::class)
    fun `addCustomAttribute throws when code already exists`() = runTest {
        // Arrange
        coEvery { attributeDao.countAttributeByCode("STR") } returns 1

        // Act
        attributeRepository.addCustomAttribute(code = "STR", name = "Strength", icon = null)
        // Assert via expected exception
    }

    @Test
    fun `addCustomAttribute succeeds for valid data`() = runTest {
        // Arrange
        coEvery { attributeDao.countAttributeByCode("NEW") } returns 0
        coEvery { attributeDao.insertAttribute(any()) } returns 10L

        // Act
        val result = attributeRepository.addCustomAttribute(code = "NEW", name = "New Attribute", icon = "✨")

        // Assert
        assertEquals(10L, result)
        coVerify(exactly = 1) { attributeDao.insertAttribute(any()) }
    }

    @Test(expected = QuestValidationException::class)
    fun `updateAttribute throws when name is empty`() = runTest {
        // Act
        attributeRepository.updateAttribute(id = 1L, name = "", icon = null)
    }

    @Test
    fun `updateAttribute calls dao with correct parameters`() = runTest {
        // Act
        attributeRepository.updateAttribute(id = 1L, name = "Updated Name", icon = "🌟")

        // Assert
        coVerify(exactly = 1) { attributeDao.updateAttribute(1L, "Updated Name", "🌟") }
    }

    @Test(expected = QuestValidationException::class)
    fun `deleteCustomAttribute throws when attribute is default`() = runTest {
        // Arrange
        val defaultEntity = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = "💪", isDefault = 1, sortOrder = 1)
        coEvery { attributeDao.getAttributeById(1L) } returns defaultEntity

        // Act
        attributeRepository.deleteCustomAttribute(1L)
        // Assert via expected exception
    }

    @Test(expected = QuestValidationException::class)
    fun `deleteCustomAttribute throws when attribute used by active quest`() = runTest {
        // Arrange
        val customEntity = AttributeEntity(id = 2L, code = "CUST", displayName = "Custom", icon = "✨", isDefault = 0, sortOrder = 100)
        coEvery { attributeDao.getAttributeById(2L) } returns customEntity
        coEvery { questDao.countActiveQuestsByAttribute(2L) } returns 1

        // Act
        attributeRepository.deleteCustomAttribute(2L)
        // Assert via expected exception
    }

    @Test
    fun `deleteCustomAttribute succeeds for non-default unused attribute`() = runTest {
        // Arrange
        val customEntity = AttributeEntity(id = 2L, code = "CUST", displayName = "Custom", icon = "✨", isDefault = 0, sortOrder = 100)
        coEvery { attributeDao.getAttributeById(2L) } returns customEntity
        coEvery { questDao.countActiveQuestsByAttribute(2L) } returns 0

        // Act
        attributeRepository.deleteCustomAttribute(2L)

        // Assert
        coVerify(exactly = 1) { attributeDao.deleteCustomAttribute(2L) }
    }
}

package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.AttributeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttributeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var attributeDao: AttributeDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        attributeDao = db.attributeDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `getAllAttributes_returns_sorted_by_sort_order`() = runTest {
        // Arrange
        val attr1 = AttributeEntity(id = 1L, code = "VIT", displayName = "Vitality", icon = null, sortOrder = 2)
        val attr2 = AttributeEntity(id = 2L, code = "STR", displayName = "Strength", icon = null, sortOrder = 1)
        attributeDao.insertAttribute(attr1)
        attributeDao.insertAttribute(attr2)

        // Act
        val result = attributeDao.getAllAttributes().first()

        // Assert
        assertEquals(2, result.size)
        assertEquals("STR", result[0].code)
        assertEquals("VIT", result[1].code)
    }

    @Test
    fun `deleteCustomAttribute_succeeds_for_non_default_attribute`() = runTest {
        // Arrange
        val customAttr = AttributeEntity(id = 1L, code = "CUS", displayName = "Custom", icon = null, isDefault = 0)
        attributeDao.insertAttribute(customAttr)

        // Act
        attributeDao.deleteCustomAttribute(1L)
        val result = attributeDao.getAttributeById(1L)

        // Assert
        assertNull(result)
    }

    @Test
    fun `deleteCustomAttribute_does_nothing_for_default_attribute`() = runTest {
        // Arrange
        val defaultAttr = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = null, isDefault = 1)
        attributeDao.insertAttribute(defaultAttr)

        // Act
        attributeDao.deleteCustomAttribute(1L)
        val result = attributeDao.getAttributeById(1L)

        // Assert
        assertEquals("STR", result?.code)
    }
}

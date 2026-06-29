package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var appSettingDao: AppSettingDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        appSettingDao = db.appSettingDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `getValue_returns_null_for_non_existent_key`() = runTest {
        // Act
        val result = appSettingDao.getValue("non_existent_key")

        // Assert
        assertNull(result)
    }

    @Test
    fun `insertOrUpdate_overwrites_existing_value_for_same_key`() = runTest {
        // Arrange
        val setting1 = AppSettingEntity(key = "theme", value = "dark", updatedAt = "2026-06-29")
        appSettingDao.insertOrUpdate(setting1)

        val setting2 = AppSettingEntity(key = "theme", value = "light", updatedAt = "2026-06-30")

        // Act
        appSettingDao.insertOrUpdate(setting2)
        val result = appSettingDao.getValue("theme")

        // Assert
        assertEquals("light", result)
    }

    @Test
    fun `getAllSettings_returns_all_settings`() = runTest {
        // Arrange
        val setting1 = AppSettingEntity(key = "key1", value = "val1", updatedAt = "2026-06-29")
        val setting2 = AppSettingEntity(key = "key2", value = "val2", updatedAt = "2026-06-29")
        appSettingDao.insertOrUpdate(setting1)
        appSettingDao.insertOrUpdate(setting2)

        // Act
        val settings = appSettingDao.getAllSettings().first()

        // Assert
        assertEquals(2, settings.size)
    }
}

package com.questdday.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.entity.AttributeEntity
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
class UserAttributeStatDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userAttributeStatDao: UserAttributeStatDao
    private lateinit var userDao: UserDao
    private lateinit var attributeDao: AttributeDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userAttributeStatDao = db.userAttributeStatDao()
        userDao = db.userDao()
        attributeDao = db.attributeDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insertStat_with_duplicate_userId_and_attributeId_is_ignored`() = runTest {
        // Arrange
        val user = UserEntity(id = 1L, lastActiveAt = "now", lastEvaluatedDate = null, createdAt = "now")
        userDao.insertUser(user)
        val attr = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = null)
        attributeDao.insertAttribute(attr)

        val stat1 = UserAttributeStatEntity(
            userId = 1L, attributeId = 1L, currentExp = 10.0, lastGainedAt = null, updatedAt = "now"
        )
        val stat2 = UserAttributeStatEntity(
            userId = 1L, attributeId = 1L, currentExp = 20.0, lastGainedAt = null, updatedAt = "now"
        )

        // Act
        val id1 = userAttributeStatDao.insertStat(stat1)
        val id2 = userAttributeStatDao.insertStat(stat2) // Should be ignored
        val result = userAttributeStatDao.getAllStatsByUser(1L).first()

        // Assert
        assertEquals(1, result.size)
        assertEquals(10.0, result[0].currentExp, 0.0)
        assertEquals(-1L, id2) // Room insert with IGNORE returns -1 on conflict
    }

    @Test
    fun `applyDecayToAllStats_does_not_produce_negative_exp`() = runTest {
        // Arrange
        val user = UserEntity(id = 1L, lastActiveAt = "now", lastEvaluatedDate = null, createdAt = "now")
        userDao.insertUser(user)
        val attr1 = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = null)
        val attr2 = AttributeEntity(id = 2L, code = "INT", displayName = "Intelligence", icon = null)
        attributeDao.insertAttribute(attr1)
        attributeDao.insertAttribute(attr2)

        userAttributeStatDao.insertStat(
            UserAttributeStatEntity(userId = 1L, attributeId = 1L, currentExp = 5.0, lastGainedAt = null, updatedAt = "now")
        )
        userAttributeStatDao.insertStat(
            UserAttributeStatEntity(userId = 1L, attributeId = 2L, currentExp = 20.0, lastGainedAt = null, updatedAt = "now")
        )

        // Act
        userAttributeStatDao.applyDecayToAllStats(1L, 10.0, "now2")
        val result = userAttributeStatDao.getAllStatsByUser(1L).first()

        // Assert
        assertEquals(2, result.size)
        val stat1 = result.find { it.attributeId == 1L }
        val stat2 = result.find { it.attributeId == 2L }
        
        assertEquals(0.0, stat1?.currentExp ?: -1.0, 0.0)
        assertEquals(10.0, stat2?.currentExp ?: -1.0, 0.0)
    }

    @Test
    fun `applyDecayToAllStats_updates_all_stats_for_user_in_one_query`() = runTest {
        // Arrange
        val user = UserEntity(id = 1L, lastActiveAt = "now", lastEvaluatedDate = null, createdAt = "now")
        userDao.insertUser(user)
        val attr1 = AttributeEntity(id = 1L, code = "STR", displayName = "Strength", icon = null)
        val attr2 = AttributeEntity(id = 2L, code = "INT", displayName = "Intelligence", icon = null)
        attributeDao.insertAttribute(attr1)
        attributeDao.insertAttribute(attr2)

        userAttributeStatDao.insertStat(
            UserAttributeStatEntity(userId = 1L, attributeId = 1L, currentExp = 50.0, lastGainedAt = null, updatedAt = "now")
        )
        userAttributeStatDao.insertStat(
            UserAttributeStatEntity(userId = 1L, attributeId = 2L, currentExp = 20.0, lastGainedAt = null, updatedAt = "now")
        )

        // Act
        userAttributeStatDao.applyDecayToAllStats(1L, 10.0, "now2")
        val result = userAttributeStatDao.getAllStatsByUser(1L).first()

        // Assert
        val stat1 = result.find { it.attributeId == 1L }
        val stat2 = result.find { it.attributeId == 2L }
        
        assertEquals(40.0, stat1?.currentExp ?: -1.0, 0.0)
        assertEquals(10.0, stat2?.currentExp ?: -1.0, 0.0)
        assertEquals("now2", stat1?.updatedAt)
        assertEquals("now2", stat2?.updatedAt)
    }
}

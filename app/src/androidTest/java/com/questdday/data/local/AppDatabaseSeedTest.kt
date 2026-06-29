package com.questdday.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseSeedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Delete any existing persistent database to ensure onCreate is called
        context.deleteDatabase("quest_dday_database")
        
        // We use the actual getDatabase method because the callback is private inside AppDatabase
        db = AppDatabase.getDatabase(context)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("quest_dday_database")
    }

    @Test
    fun verifySeedDataPopulated() = runTest {
        // We need to perform a query to trigger database creation (and the callback)
        val users = db.userDao().getUserById(1L).first()
        
        // Verify User seed
        assertNotNull("Default user should not be null", users)
        assertEquals("Hero", users?.username)
        assertEquals(0, users?.hasSeenWelcome)

        // Verify Attributes seed
        val attributes = db.attributeDao().getAllAttributes().first()
        assertEquals("Should have 5 default attributes", 5, attributes.size)
        
        val str = attributes.find { it.code == "STR" }
        assertNotNull(str)
        assertEquals("Strength", str?.displayName)
        assertEquals("💪", str?.icon)
        
        // Verify App Settings seed
        val settings = db.appSettingDao().getAllSettings().first()
        assertEquals("Should have 4 default settings", 4, settings.size)
        
        val bonusSetting = db.appSettingDao().getValue("epic_finale_bonus_exp")
        assertEquals("1000", bonusSetting)
        
        val decayR = db.appSettingDao().getValue("decay_rate_R")
        assertEquals("", decayR)

        // Verify User Attribute Stats seed
        val stats = db.userAttributeStatDao().getAllStatsByUser(1L).first()
        assertEquals("Should have 5 default user stats", 5, stats.size)
        
        val strStat = stats.find { it.attributeId == 1L }
        assertNotNull(strStat)
        assertEquals(1, strStat?.currentLevel)
        assertEquals(0.0, strStat?.currentExp ?: -1.0, 0.0)
    }
}

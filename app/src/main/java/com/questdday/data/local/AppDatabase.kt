package com.questdday.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.dao.AppSettingDao
import com.questdday.data.local.dao.AttributeDao
import com.questdday.data.local.dao.ExpDecayLogDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.dao.UserAttributeStatDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.ActiveTimerEntity
import com.questdday.data.local.entity.AppSettingEntity
import com.questdday.data.local.entity.AttributeEntity
import com.questdday.data.local.entity.ExpDecayLogEntity
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.QuestLogEntity
import com.questdday.data.local.entity.UserAttributeStatEntity
import com.questdday.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AttributeEntity::class,
        UserAttributeStatEntity::class,
        QuestEntity::class,
        QuestLogEntity::class,
        QuestHistoryEntity::class,
        ActiveTimerEntity::class,
        ExpDecayLogEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attributeDao(): AttributeDao
    abstract fun userAttributeStatDao(): UserAttributeStatDao
    abstract fun questDao(): QuestDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun questHistoryDao(): QuestHistoryDao
    abstract fun activeTimerDao(): ActiveTimerDao
    abstract fun expDecayLogDao(): ExpDecayLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quest_dday_database"
                )
                .addCallback(AppDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // 1 default user
            db.execSQL("INSERT INTO users (id, username, has_seen_welcome, last_active_at, created_at, consecutive_inactive_scheduled_days, total_exp_earned_lifetime) VALUES (1, 'Hero', 0, datetime('now'), datetime('now'), 0, 0.0)")

            // 5 default attributes
            db.execSQL("INSERT INTO attributes (id, code, display_name, icon, is_default, sort_order) VALUES (1, 'STR', 'Strength', '💪', 1, 1)")
            db.execSQL("INSERT INTO attributes (id, code, display_name, icon, is_default, sort_order) VALUES (2, 'INT', 'Intelligence', '🧠', 1, 2)")
            db.execSQL("INSERT INTO attributes (id, code, display_name, icon, is_default, sort_order) VALUES (3, 'WIS', 'Wisdom', '🦉', 1, 3)")
            db.execSQL("INSERT INTO attributes (id, code, display_name, icon, is_default, sort_order) VALUES (4, 'DEX', 'Dexterity', '🏃', 1, 4)")
            db.execSQL("INSERT INTO attributes (id, code, display_name, icon, is_default, sort_order) VALUES (5, 'VIT', 'Vitality', '❤️', 1, 5)")

            // 4 app settings
            db.execSQL("INSERT INTO app_settings (`key`, value, updated_at) VALUES ('epic_finale_bonus_exp', '1000', datetime('now'))")
            db.execSQL("INSERT INTO app_settings (`key`, value, updated_at) VALUES ('decay_grace_period_days', '', datetime('now'))")
            db.execSQL("INSERT INTO app_settings (`key`, value, updated_at) VALUES ('decay_rate_R', '', datetime('now'))")
            db.execSQL("INSERT INTO app_settings (`key`, value, updated_at) VALUES ('failure_threshold_sessions', '7', datetime('now'))")

            // 5 user attribute stats
            db.execSQL("INSERT INTO user_attribute_stats (id, user_id, attribute_id, current_level, current_exp, updated_at) VALUES (1, 1, 1, 1, 0.0, datetime('now'))")
            db.execSQL("INSERT INTO user_attribute_stats (id, user_id, attribute_id, current_level, current_exp, updated_at) VALUES (2, 1, 2, 1, 0.0, datetime('now'))")
            db.execSQL("INSERT INTO user_attribute_stats (id, user_id, attribute_id, current_level, current_exp, updated_at) VALUES (3, 1, 3, 1, 0.0, datetime('now'))")
            db.execSQL("INSERT INTO user_attribute_stats (id, user_id, attribute_id, current_level, current_exp, updated_at) VALUES (4, 1, 4, 1, 0.0, datetime('now'))")
            db.execSQL("INSERT INTO user_attribute_stats (id, user_id, attribute_id, current_level, current_exp, updated_at) VALUES (5, 1, 5, 1, 0.0, datetime('now'))")
        }
    }
}

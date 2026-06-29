package com.questdday.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.questdday.data.local.dao.AttributeDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.dao.UserAttributeStatDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.AttributeEntity
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
        QuestHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attributeDao(): AttributeDao
    abstract fun userAttributeStatDao(): UserAttributeStatDao
    abstract fun questDao(): QuestDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun questHistoryDao(): QuestHistoryDao
}

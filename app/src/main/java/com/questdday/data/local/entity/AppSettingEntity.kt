package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey(autoGenerate = false)
    val key: String,
    
    val value: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

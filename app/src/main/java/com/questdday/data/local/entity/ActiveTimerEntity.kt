package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_timers",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quest_id"], unique = true)
    ]
)
data class ActiveTimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "quest_id")
    val questId: Long,
    
    @ColumnInfo(name = "started_at")
    val startedAt: String,
    
    @ColumnInfo(name = "target_duration_seconds")
    val targetDurationSeconds: Int,
    
    @ColumnInfo(name = "alarm_fired_at")
    val alarmFiredAt: String? = null
)

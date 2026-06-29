package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_logs",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["quest_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quest_id", "log_date"], name = "idx_quest_logs_quest_date"),
        Index(value = ["user_id", "log_date"], name = "idx_quest_logs_user_date")
    ]
)
data class QuestLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "quest_id")
    val questId: Long,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "log_date")
    val logDate: String,
    
    @ColumnInfo(name = "actual_duration_seconds")
    val actualDurationSeconds: Int?,
    
    @ColumnInfo(name = "exp_awarded", defaultValue = "0.0")
    val expAwarded: Double = 0.0,
    
    @ColumnInfo(name = "is_epic_finale_bonus", defaultValue = "0")
    val isEpicFinaleBonus: Int = 0,
    
    @ColumnInfo(name = "completed_at", defaultValue = "(datetime('now'))")
    val completedAt: String
)

package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"])
    ]
)
data class QuestHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "original_quest_id")
    val originalQuestId: Long,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    val title: String,
    
    @ColumnInfo(name = "final_status")
    val finalStatus: String,
    
    @ColumnInfo(name = "total_days_completed", defaultValue = "0")
    val totalDaysCompleted: Int = 0,
    
    @ColumnInfo(name = "total_exp_earned", defaultValue = "0.0")
    val totalExpEarned: Double = 0.0,
    
    @ColumnInfo(name = "started_at")
    val startedAt: String,
    
    @ColumnInfo(name = "ended_at", defaultValue = "(datetime('now'))")
    val endedAt: String
)

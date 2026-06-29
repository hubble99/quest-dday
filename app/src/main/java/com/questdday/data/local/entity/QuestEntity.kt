package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quests",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_quest_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attribute_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id", "status"], name = "idx_quests_user_status"),
        Index(value = ["parent_quest_id"], name = "idx_quests_parent"),
        Index(value = ["attribute_id"])
    ]
)
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "parent_quest_id")
    val parentQuestId: Long?,
    
    @ColumnInfo(name = "attribute_id")
    val attributeId: Long?,
    
    val title: String,
    
    val description: String?,
    
    @ColumnInfo(name = "is_container", defaultValue = "0")
    val isContainer: Int = 0,
    
    @ColumnInfo(name = "completion_mode")
    val completionMode: String?,
    
    @ColumnInfo(name = "target_duration_seconds")
    val targetDurationSeconds: Int?,
    
    @ColumnInfo(name = "duration_type")
    val durationType: String,
    
    @ColumnInfo(name = "duration_input_type")
    val durationInputType: String?,
    
    @ColumnInfo(name = "target_days")
    val targetDays: Int?,
    
    @ColumnInfo(name = "start_date", defaultValue = "(date('now'))")
    val startDate: String,
    
    @ColumnInfo(name = "end_date")
    val endDate: String?,
    
    @ColumnInfo(name = "absence_mode")
    val absenceMode: String?,
    
    @ColumnInfo(name = "stacked_duration_seconds", defaultValue = "0")
    val stackedDurationSeconds: Int = 0,
    
    @ColumnInfo(name = "schedule_type", defaultValue = "'daily'")
    val scheduleType: String? = "daily",
    
    @ColumnInfo(name = "schedule_days")
    val scheduleDays: String?,
    
    @ColumnInfo(defaultValue = "'active'")
    val status: String = "active",
    
    @ColumnInfo(name = "consecutive_missed_sessions", defaultValue = "0")
    val consecutiveMissedSessions: Int = 0,
    
    @ColumnInfo(name = "last_completed_at")
    val lastCompletedAt: String?,
    
    @ColumnInfo(name = "created_at", defaultValue = "(datetime('now'))")
    val createdAt: String,
    
    @ColumnInfo(name = "updated_at", defaultValue = "(datetime('now'))")
    val updatedAt: String
)

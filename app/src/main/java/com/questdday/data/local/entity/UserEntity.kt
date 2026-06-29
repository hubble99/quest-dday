package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "username")
    val username: String = "Adventurer",
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: String,
    @ColumnInfo(name = "last_evaluated_date")
    val lastEvaluatedDate: String?,
    @ColumnInfo(name = "consecutive_inactive_scheduled_days")
    val consecutiveInactiveScheduledDays: Int = 0,
    @ColumnInfo(name = "total_exp_earned_lifetime")
    val totalExpEarnedLifetime: Double = 0.0,
    @ColumnInfo(name = "has_seen_welcome")
    val hasSeenWelcome: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

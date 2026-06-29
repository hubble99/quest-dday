package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exp_decay_log",
    foreignKeys = [
        ForeignKey(
            entity = UserAttributeStatEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_attribute_stat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpDecayLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_attribute_stat_id")
    val userAttributeStatId: Long,
    
    @ColumnInfo(name = "inactive_days")
    val inactiveDays: Int,
    
    @ColumnInfo(name = "exp_before")
    val expBefore: Double,
    
    @ColumnInfo(name = "exp_after")
    val expAfter: Double,
    
    @ColumnInfo(name = "decay_rate_used")
    val decayRateUsed: Double,
    
    @ColumnInfo(name = "processed_at")
    val processedAt: String
)

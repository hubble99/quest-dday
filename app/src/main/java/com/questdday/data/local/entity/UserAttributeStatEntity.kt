package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_attribute_stats",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attribute_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["user_id", "attribute_id"], unique = true),
        Index(value = ["attribute_id"])
    ]
)
data class UserAttributeStatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "attribute_id")
    val attributeId: Long,
    @ColumnInfo(name = "current_level")
    val currentLevel: Int = 1,
    @ColumnInfo(name = "current_exp")
    val currentExp: Double = 0.0,
    @ColumnInfo(name = "last_gained_at")
    val lastGainedAt: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

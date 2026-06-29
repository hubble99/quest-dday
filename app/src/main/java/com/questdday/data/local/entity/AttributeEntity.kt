package com.questdday.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attributes",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class AttributeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "icon")
    val icon: String?,
    @ColumnInfo(name = "is_default")
    val isDefault: Int = 0,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)

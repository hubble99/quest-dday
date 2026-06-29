package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.AttributeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: AttributeEntity): Long

    @Query("SELECT * FROM attributes ORDER BY sort_order ASC")
    fun getAllAttributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM attributes WHERE id = :id")
    suspend fun getAttributeById(id: Long): AttributeEntity?

    @Query("UPDATE attributes SET display_name = :name, icon = :icon WHERE id = :id")
    suspend fun updateAttribute(id: Long, name: String, icon: String?)

    @Query("DELETE FROM attributes WHERE id = :id AND is_default = 0")
    suspend fun deleteCustomAttribute(id: Long)

    @Query("SELECT COUNT(*) FROM attributes WHERE code = :code")
    suspend fun countAttributeByCode(code: String): Int
}

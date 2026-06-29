package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.UserAttributeStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAttributeStatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStat(stat: UserAttributeStatEntity): Long

    @Query("SELECT * FROM user_attribute_stats WHERE user_id = :userId")
    fun getAllStatsByUser(userId: Long): Flow<List<UserAttributeStatEntity>>

    @Query("SELECT * FROM user_attribute_stats WHERE user_id = :userId AND attribute_id = :attributeId")
    suspend fun getStat(userId: Long, attributeId: Long): UserAttributeStatEntity?

    @Query("SELECT * FROM user_attribute_stats WHERE user_id = :userId")
    suspend fun getStatsListByUser(userId: Long): List<UserAttributeStatEntity>

    @Query("""
        UPDATE user_attribute_stats 
        SET current_exp = :exp, current_level = :level, updated_at = :now
        WHERE user_id = :userId AND attribute_id = :attributeId
    """)
    suspend fun updateExpAndLevel(userId: Long, attributeId: Long, exp: Double, level: Int, now: String)

    @Query("""
        UPDATE user_attribute_stats 
        SET current_exp = MAX(current_exp - :amount, 0), updated_at = :now
        WHERE user_id = :userId
    """)
    suspend fun applyDecayToAllStats(userId: Long, amount: Double, now: String)
}

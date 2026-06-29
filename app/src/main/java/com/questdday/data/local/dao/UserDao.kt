package com.questdday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questdday.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Long): Flow<UserEntity?>

    @Query("UPDATE users SET last_active_at = :now, last_evaluated_date = :date WHERE id = :userId")
    suspend fun updateEvaluationDate(userId: Long, date: String, now: String)

    @Query("UPDATE users SET consecutive_inactive_scheduled_days = :days WHERE id = :userId")
    suspend fun updateInactiveDays(userId: Long, days: Int)

    @Query("UPDATE users SET total_exp_earned_lifetime = total_exp_earned_lifetime + :amount WHERE id = :userId")
    suspend fun addLifetimeExp(userId: Long, amount: Double)

    @Query("UPDATE users SET has_seen_welcome = 1 WHERE id = :userId")
    suspend fun markWelcomeSeen(userId: Long)

    /** One-shot variant for lazy evaluation (inside @Transaction, cannot use Flow). */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserOnce(userId: Long): UserEntity?
}

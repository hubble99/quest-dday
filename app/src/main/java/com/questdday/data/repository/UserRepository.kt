package com.questdday.data.repository

import com.questdday.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(): Flow<User?>
    suspend fun markWelcomeSeen()
    suspend fun updateEvaluationDate(date: String, now: String)
    suspend fun updateInactiveDays(days: Int)
    suspend fun addLifetimeExp(amount: Double)
}

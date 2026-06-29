package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(): Flow<com.questdday.data.local.entity.UserEntity?>
    suspend fun markWelcomeSeen()
}

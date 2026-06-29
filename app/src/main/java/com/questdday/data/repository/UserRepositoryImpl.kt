package com.questdday.data.repository

import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.toDomainModel
import com.questdday.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override fun getUser(): Flow<User?> {
        return userDao.getUserById(userId = 1L).map { it?.toDomainModel() }
    }

    override suspend fun markWelcomeSeen() {
        userDao.markWelcomeSeen(userId = 1L)
    }

    override suspend fun updateEvaluationDate(date: String, now: String) {
        userDao.updateEvaluationDate(userId = 1L, date = date, now = now)
    }

    override suspend fun updateInactiveDays(days: Int) {
        userDao.updateInactiveDays(userId = 1L, days = days)
    }

    override suspend fun addLifetimeExp(amount: Double) {
        userDao.addLifetimeExp(userId = 1L, amount = amount)
    }
}

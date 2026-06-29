package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ExpDecayLogDao
import com.questdday.data.local.dao.UserAttributeStatDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.ExpDecayLogEntity
import com.questdday.util.ExpCalculator
import kotlin.math.max

class ExpRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val userAttributeStatDao: UserAttributeStatDao,
    private val userDao: UserDao,
    private val expDecayLogDao: ExpDecayLogDao
) : ExpRepository {

    override suspend fun awardExp(
        userId: Long,
        attributeId: Long,
        amount: Double,
        now: String
    ) {
        appDatabase.withTransaction {
            val currentStat = userAttributeStatDao.getStat(userId, attributeId)
            if (currentStat != null) {
                var newExp = currentStat.currentExp + amount
                var newLevel = currentStat.currentLevel

                while (newExp >= ExpCalculator.calculateTargetExp(newLevel)) {
                    newExp -= ExpCalculator.calculateTargetExp(newLevel)
                    newLevel++
                }

                userAttributeStatDao.updateExpAndLevel(
                    userId = userId,
                    attributeId = attributeId,
                    exp = newExp,
                    level = newLevel,
                    now = now
                )

                userDao.addLifetimeExp(userId, amount)
            }
        }
    }

    override suspend fun applyDecay(
        userId: Long,
        inactiveDays: Int,
        decayRateR: Double,
        now: String
    ) {
        if (inactiveDays < 2) return

        val expToDeduct = decayRateR * (inactiveDays - 1)

        appDatabase.withTransaction {
            val stats = userAttributeStatDao.getStatsListByUser(userId)

            for (stat in stats) {
                val expBefore = stat.currentExp
                val expAfter = max(expBefore - expToDeduct, 0.0)

                // The task doesn't explicitly restrict updating only when changed, but it says:
                // "5. Insert ke exp_decay_log untuk setiap stat yang berubah"
                // So if expBefore != expAfter, we log it and update it.
                if (expBefore != expAfter) {
                    userAttributeStatDao.updateExpAndLevel(
                        userId = userId,
                        attributeId = stat.attributeId,
                        exp = expAfter,
                        level = stat.currentLevel,
                        now = now
                    )

                    val decayLog = ExpDecayLogEntity(
                        userAttributeStatId = stat.id,
                        inactiveDays = inactiveDays,
                        expBefore = expBefore,
                        expAfter = expAfter,
                        decayRateUsed = decayRateR,
                        processedAt = now
                    )
                    expDecayLogDao.insertDecayLog(decayLog)
                }
            }
        }
    }
}

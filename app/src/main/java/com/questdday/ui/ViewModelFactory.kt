package com.questdday.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.questdday.data.local.AppDatabase
import com.questdday.data.repository.*
import com.questdday.ui.screen.today.TodayQuestsViewModel

class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(application)
        
        val settingsRepository = SettingsRepositoryImpl(db.appSettingDao())
        val expRepository = ExpRepositoryImpl(db, db.userAttributeStatDao(), db.userDao(), db.expDecayLogDao())
        val questRepository = QuestRepositoryImpl(db.questDao(), db.questHistoryDao(), db)
        val timerRepository = TimerRepositoryImpl(db.activeTimerDao(), db.questDao(), db.questLogDao(), db)
        val lazyEvaluationRepository = LazyEvaluationRepositoryImpl(
            db, db.userDao(), db.questDao(), db.questLogDao(), db.questHistoryDao(), db.activeTimerDao(),
            settingsRepository, expRepository
        )
        val questLogRepository = QuestLogRepositoryImpl(db.questLogDao(), db.questHistoryDao())
        val attributeRepository = AttributeRepositoryImpl(db.attributeDao(), db.questDao())

        return when {
            modelClass.isAssignableFrom(TodayQuestsViewModel::class.java) -> {
                TodayQuestsViewModel(
                    application = application,
                    questRepository = questRepository,
                    timerRepository = timerRepository,
                    expRepository = expRepository,
                    lazyEvaluationRepository = lazyEvaluationRepository,
                    settingsRepository = settingsRepository,
                    questLogRepository = questLogRepository,
                    attributeRepository = attributeRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

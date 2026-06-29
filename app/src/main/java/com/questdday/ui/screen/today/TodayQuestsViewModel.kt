package com.questdday.ui.screen.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.questdday.data.repository.QuestRepository
import com.questdday.data.repository.TimerRepository
import com.questdday.data.repository.ExpRepository
import com.questdday.data.repository.LazyEvaluationRepository
import com.questdday.data.repository.SettingsRepository
import com.questdday.data.repository.QuestLogRepository
import com.questdday.domain.model.Quest
import com.questdday.domain.model.ActiveTimer
import com.questdday.util.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class TodayQuestsUiState {
    object Loading : TodayQuestsUiState()
    data class Success(
        val quests: List<Quest>,
        val pendingConfirmationTimers: List<ActiveTimer>,
        val runningTimers: List<ActiveTimer>
    ) : TodayQuestsUiState()
    data class Error(val message: String) : TodayQuestsUiState()
}

data class LevelUpEvent(
    val attributeId: Long,
    val previousLevel: Int,
    val newLevel: Int
)

class TodayQuestsViewModel(
    application: Application,
    private val questRepository: QuestRepository,
    private val timerRepository: TimerRepository,
    private val expRepository: ExpRepository,
    private val lazyEvaluationRepository: LazyEvaluationRepository,
    private val settingsRepository: SettingsRepository,
    private val questLogRepository: QuestLogRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<TodayQuestsUiState>(TodayQuestsUiState.Loading)
    val uiState: StateFlow<TodayQuestsUiState> = _uiState.asStateFlow()

    private val _levelUpEvent = MutableSharedFlow<LevelUpEvent>(extraBufferCapacity = 64)
    val levelUpEvent: SharedFlow<LevelUpEvent> = _levelUpEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val userId = 1L

    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    init {
        initialize()
    }

    fun initialize() {
        _uiState.value = TodayQuestsUiState.Loading
        viewModelScope.launch {
            try {
                // 1. runLayerB (WAJIB panggil runLayerB sebelum runLayerA)
                val layerBResult = lazyEvaluationRepository.runLayerB(userId)
                _uiState.value = TodayQuestsUiState.Success(
                    quests = emptyList(),
                    pendingConfirmationTimers = layerBResult.pendingConfirmationTimers,
                    runningTimers = layerBResult.runningTimers
                )

                // 2. runLayerA
                val today = LocalDate.now()
                lazyEvaluationRepository.runLayerA(userId, today)

                // 3. collect getTodayQuests & timer data
                val todayStr = today.toString()
                combine(
                    questRepository.getTodayQuests(userId, todayStr),
                    timerRepository.getPendingConfirmationTimers(),
                    timerRepository.getRunningTimers()
                ) { quests, pending, running ->
                    TodayQuestsUiState.Success(quests, pending, running)
                }.catch { e ->
                    _uiState.value = TodayQuestsUiState.Error(e.message ?: "Unknown error")
                }.collect { successState ->
                    _uiState.value = successState
                }
            } catch (e: Exception) {
                _uiState.value = TodayQuestsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun completeInstantQuest(questId: Long, attributeId: Long) {
        viewModelScope.launch {
            try {
                val quest = questRepository.getQuestById(questId)
                    ?: throw IllegalArgumentException("Quest not found")

                val todayStr = LocalDate.now().toString()
                val nowStr = now()

                // Ambil level sebelum
                val statBefore = expRepository.getStat(userId, attributeId)
                val levelBefore = statBefore?.currentLevel ?: 1

                val baseExpStr = settingsRepository.getValue("daily_exp_base")
                val baseExp = baseExpStr?.toDoubleOrNull() ?: 15.0

                // 1. Insert quest_log untuk hari ini
                questLogRepository.insertLog(
                    questId = questId,
                    userId = userId,
                    logDate = todayStr,
                    actualDurationSeconds = null,
                    expAwarded = baseExp,
                    isEpicFinaleBonus = false
                )

                // 2. Award EXP via ExpRepository
                expRepository.awardExp(userId, attributeId, baseExp, nowStr)

                // 3. Cek apakah quest time_bound dan hari ini adalah end_date → award epic finale bonus
                if (quest.durationType == "time_bound" && quest.endDate == todayStr) {
                    val bonusExp = settingsRepository.getEpicFinaleBonus()
                    expRepository.awardExp(userId, attributeId, bonusExp, nowStr)
                    
                    // Insert log untuk epic finale bonus
                    questLogRepository.insertLog(
                        questId = questId,
                        userId = userId,
                        logDate = todayStr,
                        actualDurationSeconds = null,
                        expAwarded = bonusExp,
                        isEpicFinaleBonus = true
                    )
                }

                // 4. Emit level up event jika naik level
                val statAfter = expRepository.getStat(userId, attributeId)
                val levelAfter = statAfter?.currentLevel ?: 1
                if (levelAfter > levelBefore) {
                    _levelUpEvent.emit(LevelUpEvent(attributeId, levelBefore, levelAfter))
                }
            } catch (e: Exception) {
                _errorEvent.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun startTimer(questId: Long, targetDurationSeconds: Int) {
        viewModelScope.launch {
            try {
                val success = timerRepository.startTimer(questId, targetDurationSeconds)
                if (!success) {
                    _errorEvent.emit("Timer sudah berjalan untuk quest ini")
                }
            } catch (e: Exception) {
                _errorEvent.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun cancelTimer(questId: Long) {
        viewModelScope.launch {
            try {
                timerRepository.cancelTimer(questId)
                AlarmScheduler.cancelQuestAlarm(getApplication(), questId)
            } catch (e: Exception) {
                _errorEvent.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun confirmTimerComplete(questId: Long, attributeId: Long, actualDurationSeconds: Int) {
        viewModelScope.launch {
            try {
                // Verifikasi keberadaan active timer
                val activeTimer = timerRepository.getTimerByQuestId(questId)
                if (activeTimer == null) {
                    _errorEvent.emit("Tidak ada timer aktif untuk quest ini")
                    return@launch
                }

                val todayStr = LocalDate.now().toString()
                val nowStr = now()

                // Ambil level sebelum
                val statBefore = expRepository.getStat(userId, attributeId)
                val levelBefore = statBefore?.currentLevel ?: 1

                val baseExpStr = settingsRepository.getValue("daily_exp_base")
                val baseExp = baseExpStr?.toDoubleOrNull() ?: 15.0

                // 1. TimerRepository.confirmTimerComplete()
                timerRepository.confirmTimerComplete(
                    questId = questId,
                    userId = userId,
                    logDate = todayStr,
                    actualDurationSeconds = actualDurationSeconds,
                    expAwarded = baseExp
                )

                // 2. Award EXP
                expRepository.awardExp(userId, attributeId, baseExp, nowStr)

                // 3. Cek epic finale bonus
                val quest = questRepository.getQuestById(questId)
                if (quest != null && quest.durationType == "time_bound" && quest.endDate == todayStr) {
                    val bonusExp = settingsRepository.getEpicFinaleBonus()
                    expRepository.awardExp(userId, attributeId, bonusExp, nowStr)

                    // Insert log untuk epic finale bonus
                    questLogRepository.insertLog(
                        questId = questId,
                        userId = userId,
                        logDate = todayStr,
                        actualDurationSeconds = null,
                        expAwarded = bonusExp,
                        isEpicFinaleBonus = true
                    )
                }

                // 4. Emit level up event jika naik level
                val statAfter = expRepository.getStat(userId, attributeId)
                val levelAfter = statAfter?.currentLevel ?: 1
                if (levelAfter > levelBefore) {
                    _levelUpEvent.emit(LevelUpEvent(attributeId, levelBefore, levelAfter))
                }
            } catch (e: Exception) {
                _errorEvent.emit(e.message ?: "Unknown error")
            }
        }
    }
}

package com.questdday.ui.screen.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.questdday.data.repository.AttributeRepository
import com.questdday.data.repository.QuestRepository
import com.questdday.domain.model.Attribute
import com.questdday.domain.model.Quest
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class CreateQuestFormState(
    val title: String = "",
    val description: String = "",
    val selectedAttributeId: Long? = null,
    val isContainer: Boolean = false,
    val completionMode: String? = null,        // 'instant' | 'timer'
    val targetDurationSeconds: Int? = null,
    val durationType: String = "endless",      // 'endless' | 'time_bound'
    val durationInputType: String = "days",    // 'date' | 'days'
    val targetDays: Int? = null,
    val endDate: String? = null,
    val absenceMode: String? = null,           // 'shift' | 'stack'
    val scheduleType: String = "daily",        // 'daily' | 'custom_days'
    val scheduleDays: List<Int> = emptyList(),
    val parentQuestId: Long? = null,
    val parentEndDate: String? = null,         // untuk validasi sub-quest
    val parentQuestTitle: String? = null,      // judul Epic parent untuk banner UI
    val isAwaitingFirstSubQuest: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

class CreateQuestViewModel(
    application: Application,
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository
) : AndroidViewModel(application) {

    private val _formState = MutableStateFlow(CreateQuestFormState())
    val formState: StateFlow<CreateQuestFormState> = _formState.asStateFlow()

    // Temporary storage for Epic quest when creating Epic + sub-quest sequentially
    private var tempEpic: Quest? = null

    val attributes: StateFlow<List<Attribute>> = attributeRepository.getAllAttributes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateTitle(title: String) {
        _formState.update { it.copy(title = title) }
    }

    fun updateDescription(desc: String) {
        _formState.update { it.copy(description = desc) }
    }

    fun selectAttribute(attributeId: Long) {
        _formState.update { it.copy(selectedAttributeId = attributeId) }
    }

    fun setIsContainer(isContainer: Boolean) {
        _formState.update { state ->
            state.copy(
                isContainer = isContainer,
                completionMode = if (isContainer) null else state.completionMode,
                targetDurationSeconds = if (isContainer) null else state.targetDurationSeconds
            )
        }
    }

    fun setCompletionMode(mode: String) {
        _formState.update { it.copy(completionMode = mode) }
    }

    fun setTargetDuration(seconds: Int) {
        _formState.update { it.copy(targetDurationSeconds = seconds) }
    }

    fun setDurationType(type: String) {
        _formState.update { state ->
            state.copy(
                durationType = type,
                absenceMode = if (type == "endless") null else state.absenceMode
            )
        }
    }

    fun setDurationInputType(type: String) {
        _formState.update { it.copy(durationInputType = type) }
    }

    fun setTargetDays(days: Int) {
        _formState.update { it.copy(targetDays = days) }
    }

    fun setEndDate(date: String) {
        _formState.update { it.copy(endDate = date) }
    }

    fun setAbsenceMode(mode: String) {
        _formState.update { it.copy(absenceMode = mode) }
    }

    fun setScheduleType(type: String) {
        _formState.update { it.copy(scheduleType = type) }
    }

    fun toggleScheduleDay(dayOfWeek: Int) {
        _formState.update { state ->
            val updatedDays = if (state.scheduleDays.contains(dayOfWeek)) {
                state.scheduleDays - dayOfWeek
            } else {
                state.scheduleDays + dayOfWeek
            }
            state.copy(scheduleDays = updatedDays.sorted())
        }
    }

    fun setParentQuestId(parentId: Long, parentEndDate: String?, parentTitle: String? = null) {
        _formState.update { state ->
            state.copy(
                parentQuestId = parentId,
                parentEndDate = parentEndDate,
                parentQuestTitle = parentTitle,
                isContainer = false
            )
        }
    }

    suspend fun validateAndSave(): Boolean {
        val state = _formState.value
        val newErrors = mutableMapOf<String, String>()

        // 1. Title validation
        if (state.title.isBlank()) {
            newErrors["title"] = if (state.isContainer) "Epic title cannot be empty" else "Quest title cannot be empty"
        } else if (state.title.length > 100) {
            newErrors["title"] = "Quest title exceeds maximum length of 100 characters"
        }

        // 2. Attribute validation
        if (state.selectedAttributeId == null) {
            newErrors["selectedAttributeId"] = "Quest must have an attribute selected"
        }

        // 3. Conditional validation
        if (state.isContainer) {
            if (state.durationType == "time_bound") {
                if (state.endDate == null && state.targetDays == null) {
                    newErrors["endDate"] = "End date or target days is required"
                }
                if (state.absenceMode == null) {
                    newErrors["absenceMode"] = "Absence mode is required"
                }
            }
        } else {
            // Standalone or sub-quest
            if (state.completionMode != "instant" && state.completionMode != "timer") {
                newErrors["completionMode"] = "Completion mode must be selected"
            } else if (state.completionMode == "timer" && (state.targetDurationSeconds == null || state.targetDurationSeconds <= 0)) {
                newErrors["targetDurationSeconds"] = "Timer duration must be greater than 0"
            }

            if (state.durationType == "time_bound") {
                if (state.durationInputType == "days") {
                    val targetDays = state.targetDays
                    if (targetDays == null || targetDays <= 0) {
                        newErrors["targetDays"] = "Target days must be greater than 0"
                    }
                } else if (state.durationInputType == "date") {
                    val endDateStr = state.endDate
                    if (endDateStr == null) {
                        newErrors["endDate"] = "End date is required"
                    } else {
                        try {
                            val startDate = LocalDate.now()
                            val endDate = LocalDate.parse(endDateStr)
                            if (!endDate.isAfter(startDate)) {
                                newErrors["endDate"] = "End date must be in the future"
                            }
                        } catch (e: Exception) {
                            newErrors["endDate"] = "Invalid end date format"
                        }
                    }
                }

                if (state.absenceMode == null) {
                    newErrors["absenceMode"] = "Absence mode is required"
                }
            }

            if (state.scheduleType == "custom_days" && state.scheduleDays.isEmpty()) {
                newErrors["scheduleDays"] = "At least one day must be selected"
            }

            // Sub-quest specific validation against Parent End Date
            val effectiveParentEndDate = state.parentEndDate
            if ((state.parentQuestId != null || state.isAwaitingFirstSubQuest) && effectiveParentEndDate != null) {
                if (state.durationType == "endless") {
                    newErrors["durationType"] = "Batas waktu sub-quest tidak boleh melebihi batas waktu Epic induk"
                } else {
                    val calculatedSubQuestEndDateStr = if (state.durationInputType == "days") {
                        val targetDays = state.targetDays ?: 0
                        if (targetDays > 0) {
                            LocalDate.now().plusDays(targetDays.toLong()).toString()
                        } else null
                    } else {
                        state.endDate
                    }

                    if (calculatedSubQuestEndDateStr != null) {
                        try {
                            val subQuestEndDate = LocalDate.parse(calculatedSubQuestEndDateStr)
                            val parentEndDate = LocalDate.parse(effectiveParentEndDate)
                            if (subQuestEndDate.isAfter(parentEndDate)) {
                                newErrors["endDate"] = "Batas waktu sub-quest tidak boleh melebihi batas waktu Epic induk"
                            }
                        } catch (e: Exception) {
                            // Ignore parsing issues
                        }
                    }
                }
            }
        }

        if (newErrors.isNotEmpty()) {
            _formState.update { it.copy(errors = newErrors) }
            return false
        }

        _formState.update { it.copy(errors = emptyMap()) }

        try {
            if (state.isContainer) {
                // Sequentially transition to sub-quest mode
                val epic = Quest(
                    userId = 1L,
                    parentQuestId = null,
                    attributeId = state.selectedAttributeId,
                    title = state.title,
                    description = state.description.takeIf { it.isNotBlank() },
                    isContainer = true,
                    completionMode = null,
                    targetDurationSeconds = null,
                    durationType = state.durationType,
                    durationInputType = state.durationInputType,
                    targetDays = state.targetDays,
                    startDate = LocalDate.now().toString(),
                    endDate = if (state.durationType == "time_bound") {
                        if (state.durationInputType == "days") {
                            LocalDate.now().plusDays((state.targetDays ?: 0).toLong()).toString()
                        } else {
                            state.endDate
                        }
                    } else null,
                    absenceMode = if (state.durationType == "time_bound") state.absenceMode else null,
                    stackedDurationSeconds = 0,
                    scheduleType = null,
                    scheduleDays = null,
                    status = "active",
                    consecutiveMissedSessions = 0,
                    lastCompletedAt = null,
                    createdAt = "",
                    updatedAt = ""
                )
                tempEpic = epic

                // Reset state for first sub-quest creation
                _formState.update { currentState ->
                    CreateQuestFormState(
                        parentQuestId = null,
                        parentEndDate = epic.endDate,
                        parentQuestTitle = epic.title,
                        isContainer = false,
                        isAwaitingFirstSubQuest = true,
                        selectedAttributeId = currentState.selectedAttributeId // keep attribute selected for ease of use
                    )
                }
                return false
            } else {
                val questEndDate = if (state.durationType == "time_bound") {
                    if (state.durationInputType == "days") {
                        LocalDate.now().plusDays((state.targetDays ?: 0).toLong()).toString()
                    } else {
                        state.endDate
                    }
                } else null

                val quest = Quest(
                    userId = 1L,
                    parentQuestId = state.parentQuestId,
                    attributeId = state.selectedAttributeId,
                    title = state.title,
                    description = state.description.takeIf { it.isNotBlank() },
                    isContainer = false,
                    completionMode = state.completionMode,
                    targetDurationSeconds = state.targetDurationSeconds,
                    durationType = state.durationType,
                    durationInputType = state.durationInputType,
                    targetDays = state.targetDays,
                    startDate = LocalDate.now().toString(),
                    endDate = questEndDate,
                    absenceMode = if (state.durationType == "time_bound") state.absenceMode else null,
                    stackedDurationSeconds = 0,
                    scheduleType = state.scheduleType,
                    scheduleDays = if (state.scheduleType == "custom_days") state.scheduleDays.joinToString(",") else null,
                    status = "active",
                    consecutiveMissedSessions = 0,
                    lastCompletedAt = null,
                    createdAt = "",
                    updatedAt = ""
                )

                if (state.isAwaitingFirstSubQuest) {
                    val epic = tempEpic ?: throw IllegalStateException("tempEpic is null when awaiting first sub-quest")
                    questRepository.insertEpicWithFirstSubQuest(epic, quest)
                    tempEpic = null
                } else if (state.parentQuestId != null) {
                    questRepository.insertSubQuest(quest, state.parentQuestId)
                } else {
                    questRepository.insertStandaloneQuest(quest)
                }

                // Reset form state on success
                _formState.value = CreateQuestFormState()
                return true
            }
        } catch (e: Exception) {
            _formState.update { it.copy(errors = mapOf("general" to (e.message ?: "Database error occurred"))) }
            return false
        }
    }
}

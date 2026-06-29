package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.toDomainModel
import com.questdday.data.local.entity.toEntity
import com.questdday.domain.exception.QuestValidationException
import com.questdday.domain.model.Quest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestRepositoryImpl(
    private val questDao: QuestDao,
    private val questHistoryDao: QuestHistoryDao,
    private val db: AppDatabase
) : QuestRepository {

    // ------------------------------------------------------------------
    // Timestamp helper — consistent ISO-8601 "YYYY-MM-DD HH:MM:SS"
    // ------------------------------------------------------------------
    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /**
     * Combines daily quests (always included) with custom_days quests filtered
     * by whether [today]'s day-of-week is present in schedule_days CSV.
     *
     * Day-of-week: LocalDate.dayOfWeek.value uses ISO standard (1=Monday, 7=Sunday),
     * which matches the PRD convention (1=Senin..7=Minggu).
     */
    override fun getTodayQuests(userId: Long, today: String): Flow<List<Quest>> {
        val todayDayOfWeek: Int = LocalDate.parse(today).dayOfWeek.value

        val dailyFlow = questDao.getDailyQuestsForToday(userId, today)
        val customFlow = questDao.getCustomScheduleQuests(userId, today)

        return combine(dailyFlow, customFlow) { dailyEntities, customEntities ->
            val dailyQuests = dailyEntities.map { it.toDomainModel() }

            val customQuests = customEntities
                .filter { entity ->
                    val days = parseScheduleDays(entity.scheduleDays)
                    todayDayOfWeek in days
                }
                .map { it.toDomainModel() }

            dailyQuests + customQuests
        }
    }

    override fun getActiveEpicContainers(userId: Long): Flow<List<Quest>> {
        return questDao.getActiveEpicContainers(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getSubQuests(parentId: Long): Flow<List<Quest>> {
        return questDao.getActiveSubQuests(parentId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getQuestById(id: Long): Quest? {
        return questDao.getQuestById(id)?.toDomainModel()
    }

    // ------------------------------------------------------------------
    // Insert operations
    // ------------------------------------------------------------------

    override suspend fun insertStandaloneQuest(quest: Quest): Long {
        validateQuestFields(quest)
        return questDao.insertQuest(quest.toEntity())
    }

    override suspend fun insertEpicWithFirstSubQuest(epic: Quest, subQuest: Quest): Long {
        validateEpicFields(epic)
        validateQuestFields(subQuest)
        validateSubQuestDateAgainstParent(subQuest = subQuest, parentEndDate = epic.endDate)

        return db.withTransaction {
            val epicId = questDao.insertQuest(epic.toEntity())

            // Attach sub-quest to the newly created Epic
            val subQuestWithParent = subQuest.copy(parentQuestId = epicId)
            questDao.insertQuest(subQuestWithParent.toEntity())

            epicId
        }
    }

    override suspend fun insertSubQuest(subQuest: Quest, parentId: Long): Long {
        val parent = questDao.getQuestById(parentId)
            ?: throw QuestValidationException("Parent quest with id $parentId does not exist")

        validateQuestFields(subQuest)
        validateSubQuestDateAgainstParent(subQuest = subQuest, parentEndDate = parent.endDate)

        val subQuestWithParent = subQuest.copy(parentQuestId = parentId)
        return questDao.insertQuest(subQuestWithParent.toEntity())
    }

    // ------------------------------------------------------------------
    // Update operations
    // ------------------------------------------------------------------

    override suspend fun updateQuestStatus(id: Long, status: String) {
        questDao.updateQuestStatus(id = id, status = status, now = now())
    }

    override suspend fun updateMissedSessions(id: Long, sessions: Int) {
        questDao.updateMissedSessions(id = id, sessions = sessions, now = now())
    }

    override suspend fun updateEndDateShift(id: Long, newEndDate: String) {
        questDao.updateEndDate(id = id, newEndDate = newEndDate, now = now())
    }

    override suspend fun addStackedDuration(id: Long, amount: Int) {
        questDao.addStackedDuration(id = id, amount = amount, now = now())
    }

    override suspend fun resetStackedDuration(id: Long) {
        questDao.resetStackedDuration(id = id, now = now())
    }

    override suspend fun deleteQuest(id: Long) {
        questDao.deleteQuest(id)
    }

    // ------------------------------------------------------------------
    // Archive operation
    // ------------------------------------------------------------------

    /**
     * Archives [quest] to quest_history with the given [finalStatus], then deletes
     * it from the quests table. ON DELETE CASCADE in the DB handles sub-quest cleanup.
     * Both operations are wrapped in a single atomic transaction.
     */
    override suspend fun archiveQuestToHistory(
        quest: Quest,
        finalStatus: String,
        totalDaysCompleted: Int,
        totalExpEarned: Double
    ) {
        db.withTransaction {
            val historyEntity = QuestHistoryEntity(
                originalQuestId = quest.id,
                userId = quest.userId,
                title = quest.title,
                finalStatus = finalStatus,
                totalDaysCompleted = totalDaysCompleted,
                totalExpEarned = totalExpEarned,
                startedAt = quest.createdAt,
                endedAt = now()
            )
            questHistoryDao.insertHistory(historyEntity)
            questDao.deleteQuest(quest.id)
        }
    }

    // ------------------------------------------------------------------
    // Private validation helpers
    // ------------------------------------------------------------------

    /**
     * Validates all required fields for a standalone quest or sub-quest.
     * Throws [QuestValidationException] on first violated constraint.
     *
     * Rules (from PRD section 6 & validation-rules skill section 2):
     * - title: required, max 100 chars
     * - attributeId: required
     * - completionMode: required ('instant' or 'timer')
     * - targetDurationSeconds: required > 0 when completionMode = 'timer'
     * - durationType: required ('endless' or 'time_bound')
     * - end_date or target_days: required when durationType = 'time_bound'
     * - absenceMode: required when time_bound, MUST be null when endless
     * - schedule_days: required (non-empty) when scheduleType = 'custom_days'
     * - isContainer must be false for executable quests
     */
    private fun validateQuestFields(quest: Quest) {
        if (quest.isContainer) {
            throw QuestValidationException(
                "Use insertEpicWithFirstSubQuest() for Epic containers"
            )
        }

        if (quest.title.isBlank()) {
            throw QuestValidationException("Quest title cannot be empty")
        }
        if (quest.title.length > 100) {
            throw QuestValidationException(
                "Quest title exceeds maximum length of 100 characters (current: ${quest.title.length})"
            )
        }

        if (quest.attributeId == null) {
            throw QuestValidationException("Quest must have an attribute selected")
        }

        val validCompletionModes = setOf("instant", "timer")
        if (quest.completionMode == null || quest.completionMode !in validCompletionModes) {
            throw QuestValidationException(
                "Quest completion_mode must be 'instant' or 'timer' (got: ${quest.completionMode})"
            )
        }

        if (quest.completionMode == "timer") {
            if (quest.targetDurationSeconds == null || quest.targetDurationSeconds <= 0) {
                throw QuestValidationException(
                    "target_duration_seconds must be > 0 when completion_mode is 'timer'"
                )
            }
        }

        val validDurationTypes = setOf("endless", "time_bound")
        if (quest.durationType !in validDurationTypes) {
            throw QuestValidationException(
                "Quest duration_type must be 'endless' or 'time_bound' (got: ${quest.durationType})"
            )
        }

        if (quest.durationType == "time_bound") {
            if (quest.endDate == null && quest.targetDays == null) {
                throw QuestValidationException(
                    "end_date or target_days is required when duration_type is 'time_bound'"
                )
            }
            if (quest.absenceMode == null) {
                throw QuestValidationException(
                    "absence_mode is required when duration_type is 'time_bound'"
                )
            }
            val validAbsenceModes = setOf("shift", "stack")
            if (quest.absenceMode !in validAbsenceModes) {
                throw QuestValidationException(
                    "absence_mode must be 'shift' or 'stack' (got: ${quest.absenceMode})"
                )
            }
        }

        if (quest.durationType == "endless" && quest.absenceMode != null) {
            throw QuestValidationException(
                "absence_mode must be null when duration_type is 'endless'"
            )
        }

        if (quest.scheduleType == "custom_days") {
            if (quest.scheduleDays.isNullOrBlank()) {
                throw QuestValidationException(
                    "schedule_days must have at least one day selected when schedule_type is 'custom_days'"
                )
            }
            val parsedDays = parseScheduleDays(quest.scheduleDays)
            if (parsedDays.isEmpty()) {
                throw QuestValidationException(
                    "schedule_days must contain at least one valid day (1=Mon..7=Sun)"
                )
            }
        }

        if (quest.scheduleType == "daily" && quest.scheduleDays != null) {
            throw QuestValidationException(
                "schedule_days must be null when schedule_type is 'daily'"
            )
        }
    }

    /**
     * Validates fields specific to an Epic container (is_container = 1).
     * Rules from PRD section 6 & validation-rules skill section 3:
     * - title: required, max 100 chars
     * - attributeId: required (for finale bonus EXP)
     * - isContainer must be true
     * - completionMode: MUST be null
     * - scheduleType: MUST be null
     * - scheduleDays: MUST be null
     * - time_bound: end_date or target_days required
     * - endless: absenceMode must be null
     */
    private fun validateEpicFields(epic: Quest) {
        if (!epic.isContainer) {
            throw QuestValidationException("Epic must have isContainer = true")
        }

        if (epic.title.isBlank()) {
            throw QuestValidationException("Epic title cannot be empty")
        }
        if (epic.title.length > 100) {
            throw QuestValidationException(
                "Epic title exceeds maximum length of 100 characters (current: ${epic.title.length})"
            )
        }

        if (epic.attributeId == null) {
            throw QuestValidationException("Epic must have an attribute selected for finale EXP bonus")
        }

        if (epic.completionMode != null) {
            throw QuestValidationException("Epic container must have completion_mode = null")
        }

        if (epic.scheduleType != null) {
            throw QuestValidationException("Epic container must have schedule_type = null")
        }

        if (epic.scheduleDays != null) {
            throw QuestValidationException("Epic container must have schedule_days = null")
        }

        val validDurationTypes = setOf("endless", "time_bound")
        if (epic.durationType !in validDurationTypes) {
            throw QuestValidationException(
                "Epic duration_type must be 'endless' or 'time_bound' (got: ${epic.durationType})"
            )
        }

        if (epic.durationType == "time_bound") {
            if (epic.endDate == null && epic.targetDays == null) {
                throw QuestValidationException(
                    "end_date or target_days is required when Epic duration_type is 'time_bound'"
                )
            }
            if (epic.absenceMode == null) {
                throw QuestValidationException(
                    "absence_mode is required when Epic duration_type is 'time_bound'"
                )
            }
        }

        if (epic.durationType == "endless" && epic.absenceMode != null) {
            throw QuestValidationException(
                "Epic absence_mode must be null when duration_type is 'endless'"
            )
        }
    }

    /**
     * Validates that [subQuest].endDate does not exceed [parentEndDate].
     * Only enforced when parentEndDate is non-null (parent is time_bound).
     * Uses String comparison which is correct for ISO-8601 "YYYY-MM-DD" format.
     */
    private fun validateSubQuestDateAgainstParent(subQuest: Quest, parentEndDate: String?) {
        if (parentEndDate != null && subQuest.endDate != null) {
            if (subQuest.endDate > parentEndDate) {
                throw QuestValidationException(
                    "Sub-quest end_date (${subQuest.endDate}) must not exceed " +
                        "parent Epic end_date ($parentEndDate)"
                )
            }
        }
    }

    /**
     * Parses a schedule_days CSV string into a list of integers.
     * Input: "1,3,5" → [1, 3, 5]  (1=Monday, 7=Sunday per PRD)
     * Null or blank input → empty list.
     */
    private fun parseScheduleDays(scheduleDays: String?): List<Int> {
        if (scheduleDays.isNullOrBlank()) return emptyList()
        return scheduleDays
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
    }
}

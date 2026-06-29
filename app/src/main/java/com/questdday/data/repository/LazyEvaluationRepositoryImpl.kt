package com.questdday.data.repository

import androidx.room.withTransaction
import com.questdday.data.local.AppDatabase
import com.questdday.data.local.dao.ActiveTimerDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.dao.QuestHistoryDao
import com.questdday.data.local.dao.QuestLogDao
import com.questdday.data.local.dao.UserDao
import com.questdday.data.local.entity.QuestEntity
import com.questdday.data.local.entity.QuestHistoryEntity
import com.questdday.data.local.entity.toDomain
import com.questdday.data.local.entity.toDomainModel
import com.questdday.util.MissedSessionCalculator
import com.questdday.util.ScheduleCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of the lazy evaluation engine.
 *
 * Layer B always runs and returns timer state.
 * Layer A runs once per day inside a single @Transaction, executing Steps 1-6
 * in strict order: missed sessions → cascade failure → absence mode → EXP decay → date update.
 */
class LazyEvaluationRepositoryImpl(
    private val db: AppDatabase,
    private val userDao: UserDao,
    private val questDao: QuestDao,
    private val questLogDao: QuestLogDao,
    private val questHistoryDao: QuestHistoryDao,
    private val activeTimerDao: ActiveTimerDao,
    private val settingsRepository: SettingsRepository,
    private val expRepository: ExpRepository
) : LazyEvaluationRepository {

    // ------------------------------------------------------------------
    // Timestamp helper — consistent ISO-8601 "YYYY-MM-DD HH:MM:SS"
    // ------------------------------------------------------------------
    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    // ------------------------------------------------------------------
    // LAYER B — Always runs, no guard
    // ------------------------------------------------------------------

    override suspend fun runLayerB(userId: Long): LayerBResult {
        // 1. Query active_timers WHERE alarm_fired_at IS NOT NULL → pending confirmation
        val pending = activeTimerDao.getPendingConfirmationTimersOnce()
            .map { it.toDomain() }

        // 2. Query active_timers WHERE alarm_fired_at IS NULL → running timers
        val running = activeTimerDao.getRunningTimersOnce()
            .map { it.toDomain() }

        return LayerBResult(
            pendingConfirmationTimers = pending,
            runningTimers = running
        )
    }

    // ------------------------------------------------------------------
    // LAYER A — Once per day, entire logic in one @Transaction
    // ------------------------------------------------------------------

    override suspend fun runLayerA(userId: Long, today: LocalDate): LayerAResult {
        val user = userDao.getUserOnce(userId)
            ?: throw IllegalStateException("User with id $userId not found")

        val lastEvalDateStr = user.lastEvaluatedDate

        // Guard: already evaluated today → skip
        if (lastEvalDateStr == today.toString()) {
            return LayerAResult.AlreadyEvaluated
        }

        return db.withTransaction {
            // Guard: first time (null) → set today, skip evaluation
            if (lastEvalDateStr == null) {
                userDao.updateEvaluationDate(userId, today.toString(), now())
                return@withTransaction LayerAResult.FirstTime
            }

            val lastEvalDate = LocalDate.parse(lastEvalDateStr)
            val yesterday = today.minusDays(1)

            // STEP 1: Calculate evaluation range (last_evaluated_date+1 to yesterday)
            val rangeStart = lastEvalDate.plusDays(1)
            val rangeEnd = yesterday

            // If rangeStart > rangeEnd, no days to evaluate
            if (rangeStart.isAfter(rangeEnd)) {
                userDao.updateEvaluationDate(userId, today.toString(), now())
                return@withTransaction LayerAResult.Evaluated(
                    failedQuestIds = emptyList(),
                    decayApplied = false
                )
            }

            // STEP 2: Per active quest — update consecutive_missed_sessions
            val activeQuests = questDao.getActiveExecutableQuestsOnce(userId)

            for (quest in activeQuests) {
                updateMissedSessionsForQuest(quest, rangeStart, rangeEnd)
            }

            // STEP 2 parallel: Update users.consecutive_inactive_scheduled_days
            val newInactiveDays = calculateConsecutiveInactiveDays(
                activeQuests = activeQuests,
                existingInactiveDays = user.consecutiveInactiveScheduledDays,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                userId = userId
            )
            userDao.updateInactiveDays(userId, newInactiveDays)

            // STEP 3: CASCADE FAILURE (sub-quest first, then epic parent, then siblings, then standalone)
            val failureThreshold = settingsRepository.getFailureThresholdSessions()
            val failedQuestIds = processCascadeFailure(userId, failureThreshold)

            // STEP 4: ABSENCE MODE for surviving time_bound quests
            processAbsenceMode(userId, rangeStart, rangeEnd)

            // STEP 5: EXP DECAY — skip if decay_rate_R or grace_period_days not configured
            val decayApplied = processExpDecay(userId, newInactiveDays)

            // STEP 6: UPDATE last_evaluated_date = today, last_active_at = now
            userDao.updateEvaluationDate(userId, today.toString(), now())

            return@withTransaction LayerAResult.Evaluated(
                failedQuestIds = failedQuestIds,
                decayApplied = decayApplied
            )
        }
    }

    // ------------------------------------------------------------------
    // STEP 2: Missed session calculation for a single quest
    // ------------------------------------------------------------------

    private suspend fun updateMissedSessionsForQuest(
        quest: QuestEntity,
        rangeStart: LocalDate,
        rangeEnd: LocalDate
    ) {
        val scheduleType = quest.scheduleType ?: "daily"
        val scheduleDays = ScheduleCalculator.parseScheduleDays(quest.scheduleDays)
        val effectiveScheduleDays = if (scheduleType == "daily") (1..7).toList() else scheduleDays

        val questStartDate = LocalDate.parse(quest.startDate)
        // Clamp range to quest's own start_date
        val effectiveStart = if (rangeStart.isBefore(questStartDate)) questStartDate else rangeStart
        // Clamp range to quest's end_date if present
        val questEndDate = quest.endDate?.let { LocalDate.parse(it) }
        val effectiveEnd = if (questEndDate != null && rangeEnd.isAfter(questEndDate)) questEndDate else rangeEnd

        if (effectiveStart.isAfter(effectiveEnd)) return

        // Get scheduled dates in the evaluation range
        val scheduledDates = ScheduleCalculator.getScheduledDatesInRange(
            fromDate = effectiveStart,
            toDate = effectiveEnd,
            scheduleType = scheduleType,
            scheduleDays = effectiveScheduleDays
        )

        if (scheduledDates.isEmpty()) return

        // Get completed dates from DB
        val completedDateStrings = questLogDao.getCompletedDatesForQuest(
            quest.id, effectiveStart.toString(), effectiveEnd.toString()
        )
        val completedDates = completedDateStrings.map { LocalDate.parse(it) }.toSet()

        val trailingMisses = MissedSessionCalculator.calculateConsecutiveMissedSessions(
            scheduledDates, completedDates
        )

        // If there are completions in the range, MissedSessionCalculator resets at that point
        // So trailing misses are counted from the last completion forward
        val effectiveMissed = if (completedDates.isNotEmpty() && trailingMisses < scheduledDates.size) {
            // A completion was found within the range → reset to just trailing misses
            trailingMisses
        } else {
            // No completion found in range → accumulate with existing count
            quest.consecutiveMissedSessions + trailingMisses
        }

        questDao.updateMissedSessions(quest.id, effectiveMissed, now())
    }

    // ------------------------------------------------------------------
    // STEP 2 parallel: Calculate consecutive inactive scheduled days
    // ------------------------------------------------------------------

    private suspend fun calculateConsecutiveInactiveDays(
        activeQuests: List<QuestEntity>,
        existingInactiveDays: Int,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        userId: Long
    ): Int {
        var consecutiveInactive = existingInactiveDays
        var date = rangeStart

        while (!date.isAfter(rangeEnd)) {
            val questsScheduledOnDate = activeQuests.filter { quest ->
                isQuestScheduledOnDate(quest, date)
            }.map { it.toDomainModel() }

            val logsOnDate = questLogDao.getLogsForUserOnDateOnce(userId, date.toString())
                .map { it.toDomainModel() }

            val isInactive = MissedSessionCalculator.isDayInactiveScheduled(
                date, questsScheduledOnDate, logsOnDate
            )

            if (isInactive) {
                consecutiveInactive++
            } else if (questsScheduledOnDate.isNotEmpty()) {
                // Quests scheduled AND at least one completed → reset counter
                consecutiveInactive = 0
            }
            // Free day (no quests scheduled) → counter unchanged

            date = date.plusDays(1)
        }

        return consecutiveInactive
    }

    /**
     * Check if a quest entity is scheduled on the given date,
     * considering its start_date, end_date, schedule_type, and schedule_days.
     */
    private fun isQuestScheduledOnDate(quest: QuestEntity, date: LocalDate): Boolean {
        val questStartDate = LocalDate.parse(quest.startDate)
        if (date.isBefore(questStartDate)) return false

        val questEndDate = quest.endDate?.let { LocalDate.parse(it) }
        if (questEndDate != null && date.isAfter(questEndDate)) return false

        val scheduleType = quest.scheduleType ?: "daily"
        return when (scheduleType) {
            "daily" -> true
            "custom_days" -> {
                val scheduleDays = ScheduleCalculator.parseScheduleDays(quest.scheduleDays)
                ScheduleCalculator.isScheduledOnDate(date, scheduleDays)
            }
            else -> false
        }
    }

    // ------------------------------------------------------------------
    // STEP 3: Cascade failure
    // ------------------------------------------------------------------

    private suspend fun processCascadeFailure(
        userId: Long,
        failureThreshold: Int
    ): List<Long> {
        val failedQuestIds = mutableListOf<Long>()

        // Step 3a: Sub-quests that breached threshold (parent_quest_id IS NOT NULL)
        val failedSubQuests = questDao.getFailedSubQuests(userId, failureThreshold)
        val processedParentIds = mutableSetOf<Long>()

        for (subQuest in failedSubQuests) {
            val parentId = subQuest.parentQuestId ?: continue
            if (parentId in processedParentIds) continue
            processedParentIds.add(parentId)

            // a. Archive the failed sub-quest
            archiveQuest(subQuest, "failed_abandoned")
            failedQuestIds.add(subQuest.id)

            // b. Archive the Epic parent
            val parent = questDao.getQuestById(parentId)
            if (parent != null) {
                archiveQuest(parent, "failed_abandoned")
                failedQuestIds.add(parent.id)

                // c. Archive siblings (all other sub-quests under the same Epic)
                val siblings = questDao.getAllSubQuestsOnce(parentId)
                    .filter { it.id != subQuest.id }

                for (sibling in siblings) {
                    val siblingFinalStatus = when (sibling.status) {
                        "completed" -> "completed"
                        else -> "failed_via_parent" // active or any other status
                    }
                    archiveQuest(sibling, siblingFinalStatus)
                    failedQuestIds.add(sibling.id)
                }

                // d. DELETE Epic parent → CASCADE deletes all sub-quests
                questDao.deleteQuest(parentId)
            }
        }

        // Step 3b: Standalone quests that breached threshold (parent IS NULL)
        val failedStandalone = questDao.getFailedStandaloneQuests(userId, failureThreshold)
        for (quest in failedStandalone) {
            archiveQuest(quest, "failed_abandoned")
            failedQuestIds.add(quest.id)
            questDao.deleteQuest(quest.id)
        }

        return failedQuestIds
    }

    /**
     * Archive a quest entity to quest_history.
     * Does NOT delete the quest — caller handles deletion.
     */
    private suspend fun archiveQuest(quest: QuestEntity, finalStatus: String) {
        val totalCompletions = questLogDao.getTotalCompletionsForQuest(quest.id)
        val totalExp = questLogDao.getTotalExpForQuest(quest.id)

        questHistoryDao.insertHistory(
            QuestHistoryEntity(
                originalQuestId = quest.id,
                userId = quest.userId,
                title = quest.title,
                finalStatus = finalStatus,
                totalDaysCompleted = totalCompletions,
                totalExpEarned = totalExp,
                startedAt = quest.createdAt,
                endedAt = now()
            )
        )
    }

    // ------------------------------------------------------------------
    // STEP 4: Absence mode (Shift / Stack) for surviving time_bound quests
    // ------------------------------------------------------------------

    private suspend fun processAbsenceMode(
        userId: Long,
        rangeStart: LocalDate,
        rangeEnd: LocalDate
    ) {
        // Re-query active quests (some may have been deleted in Step 3)
        val survivingQuests = questDao.getActiveExecutableQuestsOnce(userId)

        for (quest in survivingQuests) {
            if (quest.durationType != "time_bound") continue
            if (quest.absenceMode == null) continue
            if (quest.endDate == null) continue

            val scheduleType = quest.scheduleType ?: "daily"
            val scheduleDays = ScheduleCalculator.parseScheduleDays(quest.scheduleDays)
            val effectiveScheduleDays = if (scheduleType == "daily") (1..7).toList() else scheduleDays

            val questStartDate = LocalDate.parse(quest.startDate)
            val effectiveStart = if (rangeStart.isBefore(questStartDate)) questStartDate else rangeStart

            val scheduledInRange = ScheduleCalculator.getScheduledDatesInRange(
                fromDate = effectiveStart,
                toDate = rangeEnd,
                scheduleType = scheduleType,
                scheduleDays = effectiveScheduleDays
            )

            val completedInRange = questLogDao.getCompletedDatesForQuest(
                quest.id, effectiveStart.toString(), rangeEnd.toString()
            ).map { LocalDate.parse(it) }.toSet()

            val missedInRange = scheduledInRange.count { it !in completedInRange }
            if (missedInRange <= 0) continue

            when (quest.absenceMode) {
                "shift" -> {
                    // Shift end_date via findNextScheduledDay(), not calendar day increment
                    var newEndDate = LocalDate.parse(quest.endDate)
                    for (i in 1..missedInRange) {
                        newEndDate = ScheduleCalculator.findNextScheduledDay(
                            newEndDate, effectiveScheduleDays
                        )
                    }
                    questDao.updateEndDate(quest.id, newEndDate.toString(), now())
                }
                "stack" -> {
                    // Add missed_sessions × target_duration_seconds to stacked_duration_seconds
                    val targetDuration = quest.targetDurationSeconds ?: 0
                    val stackAmount = missedInRange * targetDuration
                    if (stackAmount > 0) {
                        questDao.addStackedDuration(quest.id, stackAmount, now())
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // STEP 5: EXP Decay
    // ------------------------------------------------------------------

    /**
     * Processes EXP decay if configured.
     * SKIP if decay_rate_R or grace_period_days is not configured (null/empty).
     *
     * @return true if decay was applied, false if skipped
     */
    private suspend fun processExpDecay(userId: Long, consecutiveInactiveDays: Int): Boolean {
        val decayRateR = settingsRepository.getDecayRateR() ?: return false
        val gracePeriod = settingsRepository.getDecayGracePeriodDays() ?: return false

        // Decay starts only if inactive >= 2 (grace period = 1 day)
        if (consecutiveInactiveDays < 2) return false

        expRepository.applyDecay(userId, consecutiveInactiveDays, decayRateR, now())
        return true
    }
}

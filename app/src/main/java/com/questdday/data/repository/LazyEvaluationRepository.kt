package com.questdday.data.repository

import com.questdday.domain.model.ActiveTimer
import java.time.LocalDate

/**
 * Engine for lazy evaluation — runs when the app opens.
 *
 * Layer B: Always runs. Returns timer state for UI rendering.
 * Layer A: Runs once per day (guarded by last_evaluated_date).
 *          Performs missed session calculation, cascade failure,
 *          absence mode adjustment, EXP decay, and date tracking.
 */
interface LazyEvaluationRepository {
    /**
     * Layer B — always runs, no guard.
     * Queries active_timers for pending confirmation and running timers.
     */
    suspend fun runLayerB(userId: Long): LayerBResult

    /**
     * Layer A — runs once per day.
     * Guard: skip if last_evaluated_date == today.
     * Steps 1-6 executed inside a single @Transaction.
     */
    suspend fun runLayerA(userId: Long, today: LocalDate): LayerAResult
}

data class LayerBResult(
    val pendingConfirmationTimers: List<ActiveTimer>,
    val runningTimers: List<ActiveTimer>
)

sealed class LayerAResult {
    object AlreadyEvaluated : LayerAResult()
    object FirstTime : LayerAResult()
    data class Evaluated(
        val failedQuestIds: List<Long>,
        val decayApplied: Boolean
    ) : LayerAResult()
}

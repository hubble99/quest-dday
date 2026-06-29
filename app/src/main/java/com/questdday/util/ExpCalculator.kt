package com.questdday.util

import kotlin.math.pow

object ExpCalculator {
    /**
     * Calculates the target EXP required to reach the next level from the given level.
     * Formula: 100 * level^1.5
     */
    fun calculateTargetExp(level: Int): Double {
        return 100.0 * level.toDouble().pow(1.5)
    }
}

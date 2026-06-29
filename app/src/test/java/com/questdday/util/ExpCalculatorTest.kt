package com.questdday.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpCalculatorTest {

    @Test
    fun `calculateTargetExp returns 100_0 for level 1`() {
        val result = ExpCalculator.calculateTargetExp(1)
        assertEquals(100.0, result, 0.0)
    }

    @Test
    fun `calculateTargetExp returns correct value for level 5`() {
        val result = ExpCalculator.calculateTargetExp(5)
        assertEquals(1118.033988749895, result, 0.0001)
    }

    @Test
    fun `calculateTargetExp returns correct value for level 10`() {
        val result = ExpCalculator.calculateTargetExp(10)
        assertEquals(3162.2776601683795, result, 0.0001)
    }
}

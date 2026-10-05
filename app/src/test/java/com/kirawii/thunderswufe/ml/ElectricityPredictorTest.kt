package com.kirawii.thunderswufe.ml

import com.kirawii.thunderswufe.data.database.ElectricityRecord
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ElectricityPredictorTest {
    private fun records(changes: List<Double>, balance: Double = 10.0): List<ElectricityRecord> {
        val start = LocalDateTime.now().minusDays(changes.size.toLong() + 1)
        return changes.mapIndexed { index, change ->
            ElectricityRecord(0, start.plusDays(index.toLong()), balance, change, "A")
        }
    }

    @Test fun previousDayForecastStopsAtZero() {
        val result = ElectricityPredictor(null).predictFutureUsage(records(listOf(1.0, 1.0, 2.0)))
        assertNull(result.error); assertEquals(5, result.daysUntilEmpty)
        assertEquals(2.0, result.predictions.first().predictedUsage, 0.0001)
        assertEquals(0.0, result.predictions[4].remainingBalance, 0.0001)
    }

    @Test fun exponentialSmoothingFavorsRecentDays() {
        val result = ElectricityPredictor(null).predictFutureUsage(
            records(listOf(1.0, 1.0, 5.0)), ModelType.EXPONENTIAL_SMOOTHING)
        assertNull(result.error); assertEquals(4.2, result.predictions.first().predictedUsage, 0.0001)
    }

    @Test fun predictorRequiresThreeCompleteDays() {
        val result = ElectricityPredictor(null).predictFutureUsage(records(listOf(1.0)))
        assertTrue(result.error!!.contains("至少需要 3 天"))
    }

    @Test fun reliabilityUsesLocalBacktestAndStaysBounded() {
        val result = ElectricityPredictor(null).predictFutureUsage(records(List(8) { 2.0 }))
        assertEquals(1f, result.confidence, 0.0001f)
        assertTrue(result.confidence in 0f..1f)
    }
}

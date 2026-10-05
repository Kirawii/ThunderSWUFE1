package com.kirawii.thunderswufe.utils

import com.kirawii.thunderswufe.data.database.ElectricityRecord
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ElectricityAnalyzerTest {
    @Test
    fun normalUsageDoesNotCreateFakeAnomaly() {
        val now = LocalDateTime.now()
        val records = listOf(
            ElectricityRecord(0, now.minusDays(1), 20.0, 1.0, "A"),
            ElectricityRecord(0, now, 19.0, 1.0, "A")
        )

        assertTrue(ElectricityAnalyzer.analyzeUsagePattern(records).isEmpty())
    }
}

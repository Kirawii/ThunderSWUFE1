package com.kirawii.thunderswufe.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvImportTest {
    @Test
    fun parsesValidRowsAndClampsNegativeUsage() {
        val csv = """
            Timestamp,Balance,Change
            2025-01-01 12:00:00,100.5,2.5
            2025-01-01 13:00:00,101.0,-3.0
        """.trimIndent()

        val records = parseRecordsFromCsv(csv.byteInputStream(), "ROOM")

        assertEquals(2, records.size)
        assertEquals(2.5, records[0].change, 0.0001)
        assertEquals(0.0, records[1].change, 0.0001)
        assertEquals("ROOM", records[0].roomNo)
    }
}

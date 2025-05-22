package com.kirawii.thunderswufe.data

import java.time.LocalDateTime

data class ElectricityData(
    val timestamp: LocalDateTime,
    val balance: Double,
    val change: Double,
    val roomNo: String,
    val meterStatus: String
)

data class UserPreferences(
    val roomNo: String,
    val buildingNo: String,
    val areaNo: String,
    val authToken: String
) 
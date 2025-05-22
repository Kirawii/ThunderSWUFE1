package com.kirawii.thunderswufe.utils

import com.kirawii.thunderswufe.data.database.ElectricityRecord
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class ElectricityAnalyzer {
    companion object {
        private const val SUDDEN_CHANGE_THRESHOLD = 5.0 // 突然变化阈值（度）
        private const val HIGH_USAGE_THRESHOLD = 10.0 // 高用电阈值（度/天）
        private const val ANALYSIS_WINDOW_DAYS = 7 // 分析窗口（天）
        
        /**
         * 分析电量记录，检测异常情况
         */
        fun analyzeUsagePattern(records: List<ElectricityRecord>): List<UsageAnomaly> {
            if (records.size < 2) return emptyList()
            
            val anomalies = mutableListOf<UsageAnomaly>()
            val sortedRecords = records.sortedByDescending { it.timestamp }
            
            // 检查突然变化
            for (i in 0 until sortedRecords.size - 1) {
                val current = sortedRecords[i]
                val previous = sortedRecords[i + 1]
                
                val timeDiff = ChronoUnit.HOURS.between(previous.timestamp, current.timestamp)
                if (timeDiff == 0L) continue
                
                val hourlyChange = abs(current.change) / timeDiff
                if (hourlyChange > SUDDEN_CHANGE_THRESHOLD) {
                    anomalies.add(
                        UsageAnomaly(
                            type = AnomalyType.SUDDEN_CHANGE,
                            timestamp = current.timestamp,
                            value = hourlyChange,
                            message = "检测到用电量突然变化：${String.format("%.2f", hourlyChange)}度/小时"
                        )
                    )
                }
            }
            
            // 检查持续高用电
            val recentRecords = sortedRecords.filter {
                ChronoUnit.DAYS.between(it.timestamp, LocalDateTime.now()) <= ANALYSIS_WINDOW_DAYS
            }
            
            if (recentRecords.size >= 2) {
                val totalChange = recentRecords.sumOf { it.change }
                val days = ChronoUnit.DAYS.between(
                    recentRecords.last().timestamp,
                    recentRecords.first().timestamp
                ).toDouble()
                
                if (days > 0) {
                    val dailyAverage = abs(totalChange) / days
                    if (dailyAverage > HIGH_USAGE_THRESHOLD) {
                        anomalies.add(
                            UsageAnomaly(
                                type = AnomalyType.HIGH_USAGE,
                                timestamp = LocalDateTime.now(),
                                value = dailyAverage,
                                message = "近期用电量较高：平均${String.format("%.2f", dailyAverage)}度/天"
                            )
                        )
                    }
                }
            }
            
            return anomalies
        }
        
        /**
         * 生成节能建议
         */
        fun generateSavingTips(anomalies: List<UsageAnomaly>): List<String> {
            val tips = mutableListOf<String>()
            
            if (anomalies.any { it.type == AnomalyType.HIGH_USAGE }) {
                tips.add("• 检查是否有大功率电器长时间运行")
                tips.add("• 建议在用电高峰期（18:00-22:00）减少用电")
                tips.add("• 及时关闭不使用的电器电源")
            }
            
            if (anomalies.any { it.type == AnomalyType.SUDDEN_CHANGE }) {
                tips.add("• 检查是否有电器故障")
                tips.add("• 避免同时使用多个大功率电器")
            }
            
            return tips
        }
    }
}

enum class AnomalyType {
    SUDDEN_CHANGE, // 突然变化
    HIGH_USAGE // 持续高用电
}

data class UsageAnomaly(
    val type: AnomalyType,
    val timestamp: LocalDateTime,
    val value: Double,
    val message: String
) 
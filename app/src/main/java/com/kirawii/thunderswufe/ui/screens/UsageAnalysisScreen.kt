package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.utils.ElectricityAnalyzer
import com.kirawii.thunderswufe.utils.UsageAnomaly
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.format.DateTimeFormatter

@Composable
fun UsageAnalysisScreen(
    modifier: Modifier = Modifier,
    records: List<ElectricityRecord> = emptyList()
) {
    val anomalies = remember(records) {
        ElectricityAnalyzer.analyzeUsagePattern(records)
    }

    val tips = remember(anomalies) {
        ElectricityAnalyzer.generateSavingTips(anomalies)
    }

    val chartData = remember(records) {
        records.map { it.balance }.reversed()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "用电分析",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "用电趋势",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (chartData.isNotEmpty()) {
                        // --- 修改开始 ---
                        // 1. 将 List 转换为 Array
                        val chartDataArray = chartData.toTypedArray()

                        Chart(
                            chart = lineChart(),
                            // 2. 使用展开运算符 *
                            model = entryModelOf(*chartDataArray),
                            startAxis = startAxis(),
                            bottomAxis = bottomAxis(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        // --- 修改结束 ---
                    } else {
                        Text("暂无数据")
                    }
                }
            }
        }

        if (anomalies.isNotEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "异常用电",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        anomalies.forEach { anomaly ->
                            AnomalyItem(anomaly = anomaly)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        if (tips.isNotEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "节能建议",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        tips.forEach { tip ->
                            Text(tip)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomalyItem(
    anomaly: UsageAnomaly,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Column(modifier = modifier) {
        Text(
            text = anomaly.message,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "发生时间：${anomaly.timestamp.format(formatter)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
} 
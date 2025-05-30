package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.utils.ElectricityAnalyzer
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
    var yType by remember { mutableStateOf("余额") }
    var xType by remember { mutableStateOf("日") }
    var timeRange by remember { mutableStateOf("全部") }
    var showAnomalyDialog by remember { mutableStateOf(false) }
    var selectedAnomaly by remember { mutableStateOf<ElectricityAnalyzer.UsageAnomaly?>(null) }

    val filteredRecords = remember(records, timeRange) {
        val now = java.time.LocalDateTime.now()
        when (timeRange) {
            "最近7天" -> records.filter { java.time.temporal.ChronoUnit.DAYS.between(it.timestamp, now) <= 7 }
            "最近30天" -> records.filter { java.time.temporal.ChronoUnit.DAYS.between(it.timestamp, now) <= 30 }
            else -> records
        }
    }

    val chartData = remember(filteredRecords, yType) {
        when (yType) {
            "余额" -> filteredRecords.map { it.balance }.reversed()
            else -> filteredRecords.map { it.change }.reversed()
        }
    }

    val labels = remember(filteredRecords, xType) {
        when (xType) {
            "日" -> filteredRecords.map { it.timestamp.toLocalDate().toString() }.reversed()
            "月" -> filteredRecords.map { it.timestamp.monthValue.toString() + "月" }.reversed()
            "小时" -> filteredRecords.map { it.timestamp.hour.toString() + ":00" }.reversed()
            else -> filteredRecords.map { it.timestamp.toLocalDate().toString() }.reversed()
        }
    }

    val anomalies = remember(filteredRecords) {
        ElectricityAnalyzer.analyzeUsagePattern(filteredRecords)
    }
    val tips = remember(anomalies) {
        ElectricityAnalyzer.generateSavingTips(anomalies)
    }

    val stats = remember(filteredRecords) {
        val total = filteredRecords.sumOf { it.change }
        val avg = if (filteredRecords.isNotEmpty()) total / filteredRecords.size else 0.0
        val max = filteredRecords.maxByOrNull { it.change }?.change ?: 0.0
        val min = filteredRecords.minByOrNull { it.change }?.change ?: 0.0
        mapOf(
            "总用电量(度)" to String.format("%.2f", total),
            "平均每日用电(度)" to String.format("%.2f", avg),
            "最大单日用电(度)" to String.format("%.2f", max),
            "最小单日用电(度)" to String.format("%.2f", min)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "详细用电分析",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Y轴：")
                SegmentedButton(options = listOf("余额", "用电量"), selected = yType) { yType = it }
                Spacer(Modifier.width(16.dp))
                Text("X轴：")
                SegmentedButton(options = listOf("日", "月", "小时"), selected = xType) { xType = it }
                Spacer(Modifier.width(16.dp))
                Text("范围：")
                SegmentedButton(options = listOf("全部", "最近7天", "最近30天"), selected = timeRange) { timeRange = it }
            }
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
                        val chartDataArray = chartData.toTypedArray()
                        Chart(
                            chart = lineChart(),
                            model = entryModelOf(*chartDataArray),
                            startAxis = startAxis(),
                            bottomAxis = bottomAxis(valueFormatter = { x, _ ->
                                val idx = x.toInt().coerceIn(0, labels.lastIndex)
                                labels.getOrElse(idx) { "" }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Text("暂无数据")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                stats.forEach { (k, v) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(k, style = MaterialTheme.typography.bodySmall)
                        Text(v, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                            AnomalyItem(anomaly = anomaly, onClick = {
                                selectedAnomaly = anomaly
                                showAnomalyDialog = true
                            })
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tip, modifier = Modifier.weight(1f))
                                val clipboard = LocalClipboardManager.current
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(tip))
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "复制")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    // 将AlertDialog放在Composable根作用域，避免item作用域问题
    if (showAnomalyDialog && selectedAnomaly != null) {
        AlertDialog(
            onDismissRequest = { showAnomalyDialog = false },
            title = { Text("异常详情") },
            text = {
                Column {
                    Text(selectedAnomaly?.message ?: "")
                    Text("发生时间：" + selectedAnomaly?.timestamp?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    Text("类型：" + (selectedAnomaly?.type?.name ?: ""))
                    Text("数值：" + String.format("%.2f", selectedAnomaly?.value ?: 0.0))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnomalyDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun AnomalyItem(
    anomaly: ElectricityAnalyzer.UsageAnomaly,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val color = when (anomaly.type?.name) {
        "HIGH_USAGE" -> MaterialTheme.colorScheme.error
        "SUDDEN_CHANGE" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = anomaly.message,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            Text(
                text = "发生时间：${anomaly.timestamp.format(formatter)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SegmentedButton(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row {
        options.forEach { option ->
            Button(
                onClick = { onSelect(option) },
                colors = if (option == selected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                else ButtonDefaults.buttonColors()
            ) {
                Text(option)
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
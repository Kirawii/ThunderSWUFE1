package com.kirawii.thunderswufe.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.utils.ElectricityAnalyzer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirawii.thunderswufe.ui.viewmodels.UsageAnalysisViewModel
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModelFactory
import com.kirawii.thunderswufe.ui.viewmodels.UsageAnalysisViewModelFactory
import com.kirawii.thunderswufe.ml.ModelType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UsageAnalysisScreen(
    modifier: Modifier = Modifier,
    usageAnalysisViewModel: UsageAnalysisViewModel = viewModel(
        factory = UsageAnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            settingsViewModel = viewModel(
                factory = SettingsViewModelFactory(LocalContext.current.applicationContext as ThunderApplication)
            )
        )
    ),
    onBack: (() -> Unit)? = null
) {
    var showAnomalyDialog by remember { mutableStateOf(false) }
    var selectedAnomaly by remember { mutableStateOf<ElectricityAnalyzer.UsageAnomaly?>(null) }

    val historicalRecords by usageAnalysisViewModel.historicalRecords.collectAsState()
    val predictionResult by usageAnalysisViewModel.predictionResult.collectAsState()
    val isLoadingPrediction by usageAnalysisViewModel.isLoadingPrediction.collectAsState()

    // 只按时间（日）展示余额
    val filteredRecords = remember(historicalRecords) {
        val seen = mutableSetOf<java.time.LocalDateTime>()
        historicalRecords.filter { seen.add(it.timestamp) }
    }
    val dailyHistory = remember(filteredRecords) {
        filteredRecords.sortedBy { it.timestamp }.groupBy { it.timestamp.toLocalDate() }
            .toSortedMap().entries.toList().takeLast(30)
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
            "总用电量(度)" to String.format(Locale.CHINA, "%.2f", total),
            "平均用电量(度)" to String.format(Locale.CHINA, "%.2f", avg),
            "最大单次用电量(度)" to String.format(Locale.CHINA, "%.2f", max),
            "最小单次用电量(度)" to String.format(Locale.CHINA, "%.2f", min)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (onBack != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Text("详细用电分析", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("历史电量", style = MaterialTheme.typography.titleLarge)
                    EnergyChart(dailyHistory.map { it.value.last().balance },
                        dailyHistory.map { it.key.format(DateTimeFormatter.ofPattern("MM/dd")) }, "每日最后记录")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "每日用电预测",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var modelType by remember { mutableStateOf(ModelType.PREVIOUS_DAY) }
                        SegmentedButton(
                            options = listOf("昨日用量", "指数平滑"),
                            selected = if (modelType == ModelType.PREVIOUS_DAY) "昨日用量" else "指数平滑"
                        ) { selected ->
                            modelType = if (selected == "昨日用量") ModelType.PREVIOUS_DAY else ModelType.EXPONENTIAL_SMOOTHING
                            usageAnalysisViewModel.runPrediction(modelType)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { usageAnalysisViewModel.runPrediction(modelType) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重新预测",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        isLoadingPrediction -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text("正在预测，请稍候...", modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                        predictionResult?.predictions?.isNotEmpty() == true -> {
                            var predYType by remember { mutableStateOf("余额") }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SegmentedButton(options = listOf("余额", "用电量"), selected = predYType) { predYType = it }
                            }
                            val predList = predictionResult!!.predictions.take(30)
                            val yValues = when (predYType) {
                                "余额" -> predList.map { it.remainingBalance }
                                else -> predList.map { it.predictedUsage }
                            }
                            val xLabels = predList.map { it.date.format(DateTimeFormatter.ofPattern("MM-dd")) }
                            EnergyChart(yValues, xLabels, if (predYType == "余额") "预测剩余电量" else "预测每日用电", forecast = true)
                            Text("展示未来最多 30 天", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "近期历史回测稳定度: ${String.format(Locale.CHINA, "%.0f%%", predictionResult!!.confidence * 100)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        !predictionResult?.error.isNullOrBlank() -> {
                            Text("预测失败: ${predictionResult?.error}", color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            Text("暂无预测数据")
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.entries.toList().chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { (k, v) ->
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(k, style = MaterialTheme.typography.labelMedium)
                                    Text(v, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
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
                    Text("数值：" + String.format(Locale.CHINA, "%.2f", selectedAnomaly?.value ?: 0.0))
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
    EnergyChips(options, selected, onSelect)
}

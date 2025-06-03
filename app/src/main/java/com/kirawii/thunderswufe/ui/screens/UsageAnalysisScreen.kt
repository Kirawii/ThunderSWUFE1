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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.format.DateTimeFormatter

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
    val chartData = remember(filteredRecords) { filteredRecords.map { it.balance }.reversed() }
    val labels = remember(filteredRecords) { filteredRecords.map { it.timestamp.toLocalDate().toString() }.reversed() }

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
            "平均用电量(度)" to String.format("%.2f", avg),
            "最大单次用电量(度)" to String.format("%.2f", max),
            "最小单次用电量(度)" to String.format("%.2f", min)
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                    Text("详细用电分析", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "用电预测 (ML)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模型：")
                        var modelType by remember { mutableStateOf(ModelType.LSTM) }
                        SegmentedButton(
                            options = listOf("LSTM", "线性回归"),
                            selected = when (modelType) {
                                ModelType.LSTM -> "LSTM"
                                ModelType.LINEAR_REGRESSION_KERAS, ModelType.SIMPLE_LINEAR -> "线性回归"
                            }
                        ) {
                            modelType = when (it) {
                                "LSTM" -> ModelType.LSTM
                                else -> ModelType.LINEAR_REGRESSION_KERAS
                            }
                            usageAnalysisViewModel.runPrediction(modelType)
                        }
                        Spacer(Modifier.width(16.dp))
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
                                Text("Y轴：")
                                SegmentedButton(options = listOf("余额", "用电量"), selected = predYType) { predYType = it }
                            }
                            val predList = predictionResult!!.predictions
                            val yValues = when (predYType) {
                                "余额" -> predList.map { it.remainingBalance }
                                else -> predList.map { it.predictedUsage }
                            }
                            val xLabels = predList.map { it.date.format(DateTimeFormatter.ofPattern("MM-dd")) }
                            val labelStep = if (xLabels.size > 7) xLabels.size / 7 else 1
                            Chart(
                                chart = lineChart(),
                                model = entryModelOf(*yValues.toTypedArray()),
                                startAxis = startAxis(),
                                bottomAxis = bottomAxis(
                                    valueFormatter = { x, _ ->
                                        val idx = x.toInt().coerceIn(0, xLabels.lastIndex)
                                        if (labelStep == 1 || idx % labelStep == 0 || idx == xLabels.lastIndex) xLabels.getOrElse(idx) { "" } else ""
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Text("Y轴：${if (predYType == "余额") "预测剩余电量(元)" else "预测每日用电量(度)"}", style = MaterialTheme.typography.bodySmall)
                            Text("置信度: ${String.format("%.2f", predictionResult!!.confidence)}", style = MaterialTheme.typography.bodySmall)
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
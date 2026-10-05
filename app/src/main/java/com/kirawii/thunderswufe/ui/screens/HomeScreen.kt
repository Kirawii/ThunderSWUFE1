package com.kirawii.thunderswufe.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModelFactory
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {},
    onAnalysisClick: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(
        application = LocalContext.current.applicationContext as Application,
        settingsViewModel = viewModel(factory = SettingsViewModelFactory(
            LocalContext.current.applicationContext as ThunderApplication))))) {
    val records by homeViewModel.records.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val refreshMessage by homeViewModel.refreshMessage.collectAsState()
    val predictedDays by homeViewModel.predictedDays.collectAsState()
    val predictionError by homeViewModel.predictionError.collectAsState()
    val isPredicting by homeViewModel.isPredicting.collectAsState()
    val latest = records.maxByOrNull { it.timestamp }
    var period by remember { mutableStateOf("日") }
    var metric by remember { mutableStateOf("余额") }
    val groups = remember(records, period) {
        records.sortedBy { it.timestamp }.groupBy {
            when (period) {
                "月" -> it.timestamp.withDayOfMonth(1).toLocalDate().atStartOfDay()
                "小时" -> it.timestamp.truncatedTo(ChronoUnit.HOURS)
                else -> it.timestamp.toLocalDate().atStartOfDay()
            }
        }.toSortedMap().entries.toList().takeLast(if (period == "小时") 48 else 30)
    }
    val values = groups.map { (_, rows) -> if (metric == "余额") rows.last().balance
        else rows.sumOf { it.change.coerceAtLeast(0.0) } }
    val formatter = DateTimeFormatter.ofPattern(when (period) { "月" -> "yy/MM"; "小时" -> "MM/dd HH时"; else -> "MM/dd" })
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text("智能用电") }, actions = {
            IconButton(onClick = onAnalysisClick) { Icon(Icons.Default.Analytics, "详细用电") }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "设置") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = if (latest != null && latest.balance < 10) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("宿舍剩余电量", style = MaterialTheme.typography.labelLarge)
                    Text(latest?.let { String.format(Locale.CHINA, "%.2f 度", it.balance) } ?: "等待首次获取",
                        style = MaterialTheme.typography.displaySmall)
                    Text(when {
                        isPredicting -> "正在估算可用天数…"
                        predictionError != null -> "${predictionError}"
                        predictedDays != null -> "预计还可使用 $predictedDays 天"
                        else -> "积累历史记录后可估算可用天数"
                    }, style = MaterialTheme.typography.bodyMedium)
                    latest?.let { Text("更新于 ${it.timestamp.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = { homeViewModel.refreshData() }, enabled = !isRefreshing) {
                        Text(if (isRefreshing) "正在获取…" else "刷新电量")
                    }
                    refreshMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("用电趋势", style = MaterialTheme.typography.titleLarge)
                    Text("显示最近 ${groups.size} 个有效${period}记录", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EnergyChips(listOf("日", "月", "小时"), period) { period = it }
                    EnergyChips(listOf("余额", "用电量"), metric) { metric = it }
                    EnergyChart(values, groups.map { it.key.format(formatter) }, if (metric == "余额") "剩余电量" else "分时用电")
                    TextButton(onClick = onAnalysisClick) { Text("查看预测与详细分析 →") }
                }
            }
            Text("节能提示", style = MaterialTheme.typography.titleMedium)
            Text("空调温度建议不低于 26℃。离开宿舍时关闭照明和不用的电器。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

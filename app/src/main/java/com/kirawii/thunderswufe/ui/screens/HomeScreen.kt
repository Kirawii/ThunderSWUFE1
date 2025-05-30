package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kirawii.thunderswufe.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf // 确保这个导入存在
// 如果使用 FloatEntry 等特定 Entry 类型，也需要导入，例如：
// import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 建议将 DateTimeFormatter 定义为常量以避免重复创建
private val HOME_SCREEN_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {},
    onAnalysisClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel()
) {
    val records by homeViewModel.records.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能用电") }, // 建议使用 stringResource
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置") // contentDescription 建议使用 stringResource
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues -> // Renamed 'padding' to 'paddingValues' to avoid clash if you use Modifier.padding(padding)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 使用 Scaffold 提供的 paddingValues
                .padding(16.dp), // 额外的内边距
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 电量卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if ((records.firstOrNull()?.balance ?: 100.0) < 10.0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.remaining_electricity),
                        style = MaterialTheme.typography.titleMedium
                    )
                    val percent = ((records.firstOrNull()?.balance ?: 0.0) / 500.0).coerceIn(0.0, 1.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = percent.toFloat(),
                            strokeWidth = 8.dp,
                            color = if (percent < 0.1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = "${records.firstOrNull()?.balance?.let { "%.2f".format(it) } ?: "0.00"}度",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    // 预测信息
                    // TODO: 可用 ViewModel 字段 predictedDays 替换 0
Text(
    text = stringResource(id = R.string.prediction_days, 0),
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.bodyLarge
)
                    // 上次更新时间
                    records.firstOrNull()?.timestamp?.let { ts ->
                        Text(
                            text = stringResource(id = R.string.last_update, ts.format(HOME_SCREEN_DATE_FORMATTER)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { homeViewModel.refreshData() },
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text(if (isRefreshing) stringResource(id = R.string.refreshing) else stringResource(id = R.string.refresh))
                    }
                }
            }

            // 用电趋势图（优化版：支持横坐标粒度和曲线类型切换，动态Y轴）
Card(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    elevation = CardDefaults.cardElevation(4.dp)
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.recent_trend),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 1. 横坐标粒度和曲线类型选择
        var chartMode by remember { mutableStateOf("日") }
        val chartModes = listOf("日", "月", "小时")
        var valueType by remember { mutableStateOf("余额") }
        val valueTypes = listOf("余额", "用电量")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chartModes.forEach { mode ->
                Button(
                    onClick = { chartMode = mode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (chartMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text(mode) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            valueTypes.forEach { type ->
                Button(
                    onClick = { valueType = type },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (valueType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text(type) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (records.isNotEmpty()) {
            // 2. 分组聚合
            val grouped = when (chartMode) {
                "月" -> records.groupBy { it.timestamp.withDayOfMonth(1).toLocalDate() }
                "小时" -> records.groupBy { it.timestamp.truncatedTo(java.time.temporal.ChronoUnit.HOURS) }
                else -> records.groupBy { it.timestamp.toLocalDate() }
            }
            val sortedGroups = grouped.toSortedMap(compareBy { it })
            val chartData = when (valueType) {
                "用电量" -> sortedGroups.values.map { group -> group.sumOf { it.change } }
                else -> sortedGroups.values.map { group -> group.last().balance }
            }
            val labels = sortedGroups.keys.map { date ->
                when (chartMode) {
                    "月" -> if (date is java.time.LocalDate) date.format(DateTimeFormatter.ofPattern("yyyy-MM")) else ""
                    "小时" -> if (date is java.time.LocalDateTime) date.format(DateTimeFormatter.ofPattern("MM-dd HH")) else ""
                    else -> if (date is java.time.LocalDate) date.format(DateTimeFormatter.ofPattern("MM-dd")) else ""
                }
            }
            // 3. 动态Y轴范围
            val minY = chartData.minOrNull() ?: 0.0
            val maxY = chartData.maxOrNull() ?: 0.0
            Chart(
                chart = lineChart(),
                model = entryModelOf(*chartData.toTypedArray()),
                startAxis = startAxis(),
                bottomAxis = bottomAxis(
                    valueFormatter = { x, _ ->
                        val idx = x.toInt().coerceIn(0, labels.lastIndex)
                        labels.getOrElse(idx) { "" }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(id = R.string.no_data))
            }
        }
    }
}


            // 分析按钮
            Button(
                onClick = onAnalysisClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(stringResource(id = R.string.analysis), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
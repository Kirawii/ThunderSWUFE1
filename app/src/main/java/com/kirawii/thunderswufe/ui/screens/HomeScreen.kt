package com.kirawii.thunderswufe.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirawii.thunderswufe.R
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModelFactory
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import androidx.compose.animation.core.snap
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

private val HOME_SCREEN_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd\nHH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {},
    onAnalysisClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            settingsViewModel = viewModel(
                factory = SettingsViewModelFactory(LocalContext.current.applicationContext as ThunderApplication)
            )
        )
    )
) {
    val records by homeViewModel.records.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能用电") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = onAnalysisClick) {
                        Icon(Icons.Default.Analytics, contentDescription = "详细用电")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.content_padding))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_spacing))
        ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if ((records.firstOrNull()?.balance
                                ?: 100.0) < 10.0
                        ) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
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
                        val percent =
                            ((records.firstOrNull()?.balance ?: 0.0) / 500.0).coerceIn(0.0, 1.0)
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
                        val predictedDays by homeViewModel.predictedDays.collectAsState()
                        val predictionError by homeViewModel.predictionError.collectAsState()
                        val isPredicting by homeViewModel.isPredicting.collectAsState()
                        Text(
                            text = when {
                                isPredicting -> "预计可用天数：预测中..."
                                predictionError != null -> "预计可用天数：${predictionError}"
                                predictedDays != null -> stringResource(
                                    id = R.string.prediction_days,
                                    predictedDays ?: 0
                                )

                                else -> "预计可用天数：-"
                            },
                            color = if (predictionError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        records.firstOrNull()?.timestamp?.let { ts ->
                            Text(
                                text = stringResource(
                                    id = R.string.last_update,
                                    ts.format(HOME_SCREEN_DATE_FORMATTER)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { homeViewModel.refreshData() },
                            enabled = !isRefreshing,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text(
                                if (isRefreshing) stringResource(id = R.string.refreshing) else stringResource(
                                    id = R.string.refresh
                                )
                            )
                        }
                    }
                }
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
                        var chartMode by remember { mutableStateOf("日") }
                        val chartModes = listOf("日", "月", "小时")
                        var valueType by remember { mutableStateOf("余额") }
                        val valueTypes = listOf("余额", "用电量")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            chartModes.forEach { mode ->
                                Button(
                                    onClick = { chartMode = mode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (chartMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                                ) { Text(mode) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            valueTypes.forEach { type ->
                                Button(
                                    onClick = { valueType = type },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (valueType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                                ) { Text(type) }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (records.isNotEmpty()) {
                                val grouped = when (chartMode) {
                                    "月" -> records.groupBy {
                                        it.timestamp.withDayOfMonth(1).toLocalDate()
                                    }

                                    "小时" -> records.groupBy { it.timestamp.truncatedTo(java.time.temporal.ChronoUnit.HOURS) }
                                    else -> records.groupBy { it.timestamp.toLocalDate() }
                                }
                                val sortedGroups = grouped.toSortedMap(compareBy { it })

                                val validGroups = sortedGroups.filter { (_, group) ->
                                    group.any { record ->
                                        when (valueType) {
                                            "用电量" -> record.change >= 0
                                            else -> record.balance >= 0
                                        }
                                    }
                                }
                                val entries = validGroups.values.mapIndexed { index, group ->
                                    val value = when (valueType) {
                                        "用电量" -> group.sumOf { it.change }.toFloat()
                                        else -> group.last().balance.toFloat()
                                    }
                                    FloatEntry(index.toFloat(), value)
                                }
                                val labels = validGroups.keys.map { date ->
                                    when (chartMode) {
                                        "月" -> if (date is java.time.LocalDate) date.format(
                                            DateTimeFormatter.ofPattern("yyyy-MM")
                                        ) else ""

                                        "小时" -> if (date is java.time.LocalDateTime) date.format(
                                            DateTimeFormatter.ofPattern("MM-dd HH")
                                        ) else ""

                                        else -> if (date is java.time.LocalDate) date.format(
                                            DateTimeFormatter.ofPattern("MM-dd")
                                        ) else ""
                                    }
                                }
                                val chartModelProducer = remember { ChartEntryModelProducer() }

                                LaunchedEffect(entries) {
                                    chartModelProducer.setEntries(entries)
                                }

                                Chart(
                                    chart = lineChart(),
                                    chartModelProducer = chartModelProducer,
                                    bottomAxis = rememberBottomAxis(
                                        valueFormatter = { x, _ ->
                                            val idx = x.toInt().coerceIn(0, labels.lastIndex)
                                            labels.getOrElse(idx) { "" }
                                        }
                                    ),
                                    endAxis = rememberEndAxis(),
                                    chartScrollSpec = rememberChartScrollSpec(
                                        initialScroll = InitialScroll.End,
                                        autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
                                        isScrollEnabled = true
                                    ),
                                    diffAnimationSpec = snap(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensionResource(R.dimen.chart_height))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensionResource(R.dimen.chart_height)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(id = R.string.no_data))
                                }
                            }
                        }
                        Button(
                            onClick = onAnalysisClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                stringResource(id = R.string.analysis),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        val tips = listOf(
                            "高峰时段尽量减少大功率电器使用，节约用电。",
                            "及时关闭不用的电器，防止待机耗电。",
                            "合理设置空调温度，建议不低于26℃。",
                            "充分利用自然光，减少照明用电。",
                            "定期清理电器灰尘，提高能效。",
                            "合理安排用电时间，避开用电高峰。"
                        )
                        val tip = remember { tips.random() }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "节能建议",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
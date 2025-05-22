package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp) // 间距
                ) {
                    Text(
                        text = "剩余电量", // 建议使用 stringResource
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        // 格式化余额，如果需要的话
                        text = "${records.firstOrNull()?.balance?.let { "%.2f".format(it) } ?: "0.00"}度",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    // 更安全地显示上次更新时间
                    records.firstOrNull()?.timestamp?.let { ts ->
                        Text(
                            text = "上次更新：${ts.format(HOME_SCREEN_DATE_FORMATTER)}", // 建议使用 stringResource
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = {
                            homeViewModel.refreshData()
                        },
                        // modifier = Modifier.padding(top = 8.dp), // 已通过 Column 的 spacedBy 处理
                        enabled = !isRefreshing
                    ) {
                        Text(if (isRefreshing) "刷新中..." else "刷新") // 建议使用 stringResource
                    }
                }
            }

            // 用电趋势图
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 使其占据剩余空间
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "近期用电趋势", // 建议使用 stringResource
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (records.isNotEmpty()) {
                        // 准备图表数据，将 List<Double> 转换为 Array<Double>
                        // 假设 records 是按时间降序（最新在前），reversed() 后变成升序（最早在前），适合图表
                        val chartData = records
                            .map { it.balance } // 获取余额数据
                            .reversed()         // 使数据点从左到右对应时间从早到晚
                            .toTypedArray()     // 转换为 Array<Double>

                        Chart(
                            chart = lineChart(),
                            // 使用展开运算符 * 将 Array<Double> (即 Array<out Number>) 传递给 vararg 参数
                            model = entryModelOf(*chartData),
                            startAxis = startAxis(), // 默认的 Y 轴在左侧
                            bottomAxis = bottomAxis(), // 默认的 X 轴在底部
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) // 或者使用 .aspectRatio(16f/9f) 等
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无数据") // 建议使用 stringResource
                        }
                    }
                }
            }

            // 分析按钮
            Button(
                onClick = onAnalysisClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看详细分析") // 建议使用 stringResource
            }
        }
    }
}
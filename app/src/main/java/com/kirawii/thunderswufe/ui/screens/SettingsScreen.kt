// 在 SettingsScreen.kt
package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager // 不再直接需要
import com.kirawii.thunderswufe.ui.viewmodels.SettingsUiState // 导入 ViewModel 和 UiState
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle // 推荐
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, // 接收 ViewModel
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onImportDone: (() -> Unit)? = null // 导入后回调
) {
    val uiState = viewModel.uiState.value // 直接取 State 对象的 value

    // 用于本地编辑的临时状态，当用户完成编辑后，通过 ViewModel 更新 DataStore
    var localThreshold by remember { mutableStateOf(uiState.threshold) }
    var localRoomNo by remember { mutableStateOf(uiState.roomNo) }
    var localBuildingNo by remember { mutableStateOf(uiState.buildingNo) }
    var localAreaNo by remember { mutableStateOf(uiState.areaNo) }
    // notificationEnabled 直接用 uiState.notificationEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部带返回按钮的栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        OutlinedTextField(
            value = localThreshold,
            onValueChange = { localThreshold = it },
            label = { Text("电量提醒阈值（度）") },
            singleLine = true
        )

        OutlinedTextField(
            value = localRoomNo,
            onValueChange = { localRoomNo = it },
            label = { Text("房间号") },
            singleLine = true
        )

        OutlinedTextField(
            value = localBuildingNo,
            onValueChange = { localBuildingNo = it },
            label = { Text("楼栋号") },
            singleLine = true
        )

        OutlinedTextField(
            value = localAreaNo,
            onValueChange = { localAreaNo = it },
            label = { Text("区域") },
            singleLine = true
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = uiState.notificationEnabled, // 直接使用 ViewModel 中的状态
                onCheckedChange = { enabled ->
                    viewModel.updateNotificationEnabled(enabled) // 通过 ViewModel 更新
                }
            )
            Text("启用电量提醒")
        }

        Button(
            onClick = {
                viewModel.saveAllSettings(
                    threshold = localThreshold,
                    roomNo = localRoomNo,
                    buildingNo = localBuildingNo,
                    areaNo = localAreaNo
                )
                // 可以在这里加一个提示，比如 Toast 或 Snackbar
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        // 导入历史CSV数据
        val context = LocalContext.current
        var showImportResult by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        Text(text = "实验功能：导入历史CSV数据", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            scope.launch {
                showImportResult = "正在导入..."
                showImportResult = importCsvAndInsertDb(context)
                onImportDone?.invoke() // 导入后回调，主界面可自动刷新
            }
        }) {
            Text("导入 assets/balance_data_副本.csv")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(showImportResult)
    }
}
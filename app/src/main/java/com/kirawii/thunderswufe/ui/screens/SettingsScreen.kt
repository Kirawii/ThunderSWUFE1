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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, // 接收 ViewModel
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle() // 观察 UiState

    // 用于本地编辑的临时状态，当用户完成编辑后，通过 ViewModel 更新 DataStore
    var localThreshold by remember(uiState.threshold) { mutableStateOf(uiState.threshold) }
    var localRoomNo by remember(uiState.roomNo) { mutableStateOf(uiState.roomNo) }
    var localBuildingNo by remember(uiState.buildingNo) { mutableStateOf(uiState.buildingNo) }
    var localAreaNo by remember(uiState.areaNo) { mutableStateOf(uiState.areaNo) }
    // notificationEnabled 可以直接从 uiState.notificationEnabled 读取，并通过 viewModel.updateNotificationEnabled 更新

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium
        )

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
    }
}
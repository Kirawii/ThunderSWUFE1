
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
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onImportDone: (() -> Unit)? = null
) {
    val uiState = viewModel.uiState.value
    var localThreshold by remember { mutableStateOf(uiState.threshold) }
    var localRoomNo by remember { mutableStateOf(uiState.roomNo) }
    var localBuildingNo by remember { mutableStateOf(uiState.buildingNo) }
    var localAreaNo by remember { mutableStateOf(uiState.areaNo) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                checked = uiState.notificationEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updateNotificationEnabled(enabled)
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        val context = LocalContext.current
        var showImportResult by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        Text(text = "实验功能：导入历史CSV数据", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            scope.launch {
                showImportResult = "正在导入..."
                showImportResult = importCsvAndInsertDb(context)
                onImportDone?.invoke()
            }
        }) {
            Text("导入 assets/balance_data_副本.csv")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(showImportResult)
    }
}
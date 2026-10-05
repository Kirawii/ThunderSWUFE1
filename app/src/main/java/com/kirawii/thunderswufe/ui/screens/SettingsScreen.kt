
package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    var localAuthToken by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showImportResult by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                showImportResult = "正在导入…"
                showImportResult = context.contentResolver.openInputStream(uri)?.use { input ->
                    importCsvAndInsertDb(context, localRoomNo, input)
                } ?: "导入失败：无法读取文件"
                onImportDone?.invoke()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 顶部栏固定
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
        if (uiState.hasAuthToken) {
            TextButton(onClick = {
                viewModel.clearAuthToken()
                localAuthToken = ""
            }) {
                Text("清除已保存的会话 Token")
            }
        }

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

        OutlinedTextField(
            value = localAuthToken,
            onValueChange = { localAuthToken = it },
            label = { Text(if (uiState.hasAuthToken) "会话 Token（已保存，留空不修改）" else "会话 Token") },
            visualTransformation = PasswordVisualTransformation(),
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
                    areaNo = localAreaNo,
                    authToken = localAuthToken
                )
                localAuthToken = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "实验功能：导入历史CSV数据", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }) {
            Text("选择 CSV 文件")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(showImportResult)
    }
}

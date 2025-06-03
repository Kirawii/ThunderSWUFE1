package com.kirawii.thunderswufe.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel

@Composable
fun ImportCsvScreen(
    context: Context,
    settingsViewModel: SettingsViewModel,
    onImportFinished: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var importStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("一键导入历史CSV数据", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            scope.launch {
                importStatus = "正在导入..."
                val result = importCsvAndInsertDb(context, settingsViewModel.uiState.value.roomNo)
                importStatus = result
                onImportFinished?.invoke()
            }
        }) {
            Text("导入 assets/balance_data_副本.csv")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(importStatus)
    }
}

suspend fun importCsvAndInsertDb(context: Context, roomNo: String): String {
    return try {
        val assetManager = context.assets
        val inputStream = assetManager.open("balance_data_副本.csv")
        val records = parseRecordsFromCsv(inputStream, roomNo)
        insertRecordsToDb(context, records)
        "导入成功：共${records.size}条数据"
    } catch (e: Exception) {
        "导入失败：${e.localizedMessage}"
    }
}

fun parseRecordsFromCsv(inputStream: InputStream, roomNo: String): List<ElectricityRecord> {
    val reader = inputStream.bufferedReader()
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val records = mutableListOf<ElectricityRecord>()
    reader.readLine()
    reader.forEachLine { line ->
        val parts = line.split(",")
        if (parts.size >= 3) {
            val timestamp = LocalDateTime.parse(parts[0].trim(), dateTimeFormatter)
            val balance = parts[1].trim().toDoubleOrNull() ?: 0.0
            val change = parts[2].trim().toDoubleOrNull() ?: 0.0
            records.add(
                ElectricityRecord(
                    0L,
                    timestamp,
                    balance,
                    change,
                    roomNo
                )
            )
        }
    }
    return records
}

suspend fun insertRecordsToDb(context: Context, records: List<ElectricityRecord>) {
    val app = context.applicationContext as com.kirawii.thunderswufe.ThunderApplication
    val dao = app.database.electricityDao()
    kotlinx.coroutines.withContext(Dispatchers.IO) {
        records.forEach { dao.insertRecord(it) }
    }
}

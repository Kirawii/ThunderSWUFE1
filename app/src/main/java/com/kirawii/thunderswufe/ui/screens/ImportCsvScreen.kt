package com.kirawii.thunderswufe.ui.screens

import android.content.Context
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun importCsvAndInsertDb(context: Context, roomNo: String, inputStream: InputStream): String =
    withContext(Dispatchers.IO) {
        if (roomNo.isBlank()) return@withContext "导入失败：请先填写房间号"
        runCatching {
            val records = parseRecordsFromCsv(inputStream, roomNo.trim())
            val dao = (context.applicationContext as ThunderApplication).database.electricityDao()
            dao.insertRecords(records)
            "导入成功：共 ${records.size} 条数据"
        }.getOrElse { "导入失败：${it.message ?: "文件格式不正确"}" }
    }

fun parseRecordsFromCsv(inputStream: InputStream, roomNo: String): List<ElectricityRecord> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return inputStream.bufferedReader().useLines { lines ->
        lines.drop(1).filter { it.isNotBlank() }.mapIndexed { index, line ->
            val parts = line.split(',')
            require(parts.size >= 3) { "第 ${index + 2} 行少于 3 列" }
            val timestamp = LocalDateTime.parse(parts[0].trim(), formatter)
            val balance = parts[1].trim().toDoubleOrNull()
                ?: error("第 ${index + 2} 行余额无效")
            val change = parts[2].trim().toDoubleOrNull()
                ?: error("第 ${index + 2} 行用量无效")
            ElectricityRecord(0L, timestamp, balance, change.coerceAtLeast(0.0), roomNo)
        }.toList()
    }
}

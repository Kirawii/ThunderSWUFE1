package com.kirawii.thunderswufe.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kirawii.thunderswufe.BuildConfig
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.network.NetworkModule
import com.kirawii.thunderswufe.notification.ElectricityNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ElectricityCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val OUTPUT_STATUS = "status"
        const val OUTPUT_MESSAGE = "message"
        const val STATUS_OK = "ok"
        const val STATUS_CONFIGURATION_REQUIRED = "configuration_required"
        const val STATUS_AUTH_REQUIRED = "auth_required"
        const val STATUS_SERVER_ERROR = "server_error"
        const val STATUS_INVALID_RESPONSE = "invalid_response"
    }

    private val app = appContext as ThunderApplication
    private val dao = app.database.electricityDao()
    private val preferences = app.userPreferencesManager
    private val service = NetworkModule.provideElectricityService()
    private val notifications = ElectricityNotificationManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val room = preferences.roomInfo
        val token = preferences.authToken.trim()
        if (room.roomNo.isBlank() || room.buildingNo.isBlank() || room.areaNo.isBlank() || token.isBlank()) {
            return@withContext failure(STATUS_CONFIGURATION_REQUIRED, "请先填写宿舍信息和会话 Token")
        }

        val response = try {
            service.getElectricityInfo(
                authorizationToken = token,
                headers = mapOf(
                    "Accept" to "application/json, text/plain, */*",
                    "Origin" to BuildConfig.BASE_URL.removeSuffix("/"),
                    "Referer" to "${BuildConfig.BASE_URL}easytong_webapp/",
                    "h5req" to "Y",
                    "X-Requested-With" to "cn.com.yunma.school.app",
                    "Cookie" to "etToken=$token"
                ),
                requestFields = mapOf(
                    "RoomNo" to room.roomNo,
                    "BuildingNo" to room.buildingNo,
                    "AreaNo" to room.areaNo,
                    "AccNum" to "0",
                    "FloorNo" to "0",
                    "ItemNum" to "1",
                    "Time" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    "ContentType" to "application/json"
                )
            )
        } catch (_: IOException) {
            return@withContext Result.retry()
        } catch (e: Exception) {
            return@withContext failure(STATUS_SERVER_ERROR, e.message ?: "请求失败")
        }

        when {
            response.code() == 401 || response.code() == 403 ->
                return@withContext failure(STATUS_AUTH_REQUIRED, "会话已失效，请更新 Token")
            response.code() >= 500 -> return@withContext Result.retry()
            !response.isSuccessful ->
                return@withContext failure(STATUS_SERVER_ERROR, "服务器返回 ${response.code()}")
        }

        val balance = response.body()?.balance?.toDoubleOrNull()
            ?: return@withContext failure(STATUS_INVALID_RESPONSE, "服务器未返回有效余额")

        val previous = dao.getLatestRecordByRoom(room.roomNo)
        val usage = previous?.let { (it.balance - balance).coerceAtLeast(0.0) } ?: 0.0
        dao.insertRecord(ElectricityRecord(0L, LocalDateTime.now(), balance, usage, room.roomNo))

        if (preferences.isNotificationEnabled && balance < preferences.lowBalanceThreshold) {
            notifications.showLowBalanceNotification(balance, preferences.lowBalanceThreshold)
        }

        Result.success(workDataOf(OUTPUT_STATUS to STATUS_OK, OUTPUT_MESSAGE to "更新成功"))
    }

    private fun failure(status: String, message: String): Result =
        Result.failure(workDataOf(OUTPUT_STATUS to status, OUTPUT_MESSAGE to message))
}

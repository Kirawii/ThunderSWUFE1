package com.kirawii.thunderswufe.work

import android.content.Context
import android.util.Log
import androidx.work.*
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityDatabase
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.network.ElectricityResponse
import com.kirawii.thunderswufe.network.ElectricityService
import com.kirawii.thunderswufe.network.NetworkModule
import com.kirawii.thunderswufe.notification.ElectricityNotificationManager
import com.kirawii.thunderswufe.utils.ElectricityAnalyzer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Response
import kotlin.coroutines.resumeWithException

class ElectricityCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = ElectricityNotificationManager(context)
    private val app = context.applicationContext as ThunderApplication
    private val database = app.database
    private val preferencesManager = app.userPreferencesManager
    
    private val electricityService: ElectricityService by lazy {
        NetworkModule.provideElectricityService(
            NetworkModule.provideRetrofit(
                NetworkModule.provideOkHttpClient(applicationContext)
            )
        )
    }
    
    companion object {
        private const val WORK_NAME = "electricity_check_worker"
        private const val DEFAULT_THRESHOLD = 10.0
        
        fun setupPeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = PeriodicWorkRequestBuilder<ElectricityCheckWorker>(
                Duration.ofHours(6)
            )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                Duration.ofMinutes(15)
            )
            .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // 获取配置信息
            val threshold = preferencesManager.getLowBalanceThreshold()
            val roomInfo = preferencesManager.getRoomInfo()
            Log.d("ElectricityCheckWorker", "RoomInfo from Prefs: Area='${roomInfo.areaNo}', Building='${roomInfo.buildingNo}', Room='${roomInfo.roomNo}'")
            val authToken = preferencesManager.getAuthToken()
            Log.d("ElectricityCheckWorker", "Auth Token from Prefs: '$authToken'")
            val notificationEnabled = preferencesManager.isNotificationEnabled()
            
            var balance = 0.0
            var isOffline = false
            
            try {
                val call = electricityService.getElectricityInfo(
                    authToken,
                    "Mozilla/5.0 (Linux; Android 15; V2241HA Build/AP3A.240905.015.A2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.111 Mobile Safari/537.36 ZJYXYwebviewbroswer ZJYXYAndroid tourCustomer/yunmaapp.NET/7.1.5/ym-30a974936ba5e48e03b0775175a54a30",
                    "application/json, text/plain, */*",
                    com.kirawii.thunderswufe.BuildConfig.BASE_URL,
                    com.kirawii.thunderswufe.BuildConfig.BASE_URL + "easytong_webapp/",
                    "Y",
                    "\"Android\"",
                    "\"Android WebView\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"",
                    "?1",
                    "cn.com.yunma.school.app",
                    "same-origin",
                    "cors",
                    "empty",
                    "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
                    mapOf(
                        "RoomNo" to roomInfo.roomNo,
                        "BuildingNo" to roomInfo.buildingNo,
                        "AreaNo" to roomInfo.areaNo,
                        "AccNum" to "0",
                        "FloorNo" to "0",
                        "ItemNum" to "1",
                        "Time" to "20241012120009",
                        "Sign" to "a9189f1868aa3272583ec3b9f8702524",
                        "ContentType" to "application/json"
                    )
                )
                val response = suspendCancellableCoroutine<Response<ElectricityResponse>> { cont ->
                    try {
                        val resp = call.execute()
                        cont.resume(resp) {}
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                Log.d("ElectricityCheckWorker", "Response received:")
                Log.d("ElectricityCheckWorker", "  Successful: ${response.isSuccessful}")
                Log.d("ElectricityCheckWorker", "  Code: ${response.code()}")
                Log.d("ElectricityCheckWorker", "  Message: ${response.message()}")
                Log.d("ElectricityCheckWorker", "  Headers: ${response.headers()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("ElectricityCheckWorker", "  Response Body (Success): $responseBody")
                    balance = responseBody?.getBalance()?.toDoubleOrNull() ?: 0.0
                    Log.i("ElectricityCheckWorker", "API call successful. Balance: $balance")

                    val lastRecord = database.electricityDao().getLatestRecordByRoom(roomInfo.roomNo)
                    val previousBalance = lastRecord?.balance ?: 0.0
                    
                    var calculatedChange = 0.0
                    if (lastRecord != null) {
                        calculatedChange = previousBalance - balance
                    }

                    val changeToStore = if (calculatedChange > 0) calculatedChange else 0.0

                    database.electricityDao().insertRecord(
                        ElectricityRecord(0L, LocalDateTime.now(), balance, changeToStore, roomInfo.roomNo)
                    )
                } else {
                    val errorBodyString = response.errorBody()?.string() // Read error body once
                    Log.e("ElectricityCheckWorker", "  Response Body (Error): $errorBodyString")
                    Log.e("ElectricityCheckWorker", "API Error: Code: ${response.code()}, Message: ${response.message()}, Body: $errorBodyString")
                    isOffline = true
                    val lastDbRecord = database.electricityDao().getLatestRecordByRoom(roomInfo.roomNo)
                    balance = lastDbRecord?.balance ?: 0.0
                }
            } catch (e: Exception) {
                Log.e("ElectricityCheckWorker", "Network request or processing failed for room: ${roomInfo.roomNo}", e)
                isOffline = true
                val lastRecord = database.electricityDao().getLatestRecordByRoom(roomInfo.roomNo)
                balance = lastRecord?.balance ?: 0.0
            }
            
            if (notificationEnabled && balance < threshold) {
                notificationManager.showLowBalanceNotification(balance, threshold)
            }
            
            val recentRecords = database.electricityDao().getAllRecords().first()
            val anomalies = ElectricityAnalyzer.analyzeUsagePattern(recentRecords)
            
            if (anomalies.isNotEmpty() && notificationEnabled) {
                val tips = ElectricityAnalyzer.generateSavingTips(anomalies)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 
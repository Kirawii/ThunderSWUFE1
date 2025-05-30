package com.kirawii.thunderswufe.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.notification.ElectricityNotificationManager
import com.kirawii.thunderswufe.ml.ElectricityPredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ElectricityPredictionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val app = appContext as ThunderApplication
    private val notificationManager = ElectricityNotificationManager(appContext)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val records = app.database.electricityDao().getAllRecords().first()

            val predictionResult = app.electricityPredictor.predictFutureUsage(records)
            
            if (predictionResult.error != null) {
                return@withContext Result.failure()
            }

            val notificationEnabled = app.userPreferencesManager.isNotificationEnabled()
            if (!notificationEnabled) {
                return@withContext Result.success()
            }

            predictionResult.daysUntilEmpty?.let { days ->
                if (days <= 3) { // 如果预计3天内电量耗尽
                    val formatter = DateTimeFormatter.ofPattern("MM-dd")
                    val emptyDate = LocalDateTime.now().plusDays(days.toLong()).format(formatter)
                    
                    notificationManager.showPredictionNotification(
                        days,
                        emptyDate,
                        predictionResult.confidence
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
} 
package com.kirawii.thunderswufe

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.kirawii.thunderswufe.data.database.ElectricityDatabase
import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager
import com.kirawii.thunderswufe.ml.ElectricityPredictor
import com.kirawii.thunderswufe.work.ElectricityCheckWorker
import com.kirawii.thunderswufe.work.ElectricityPredictionWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class ThunderApplication : Application(), Configuration.Provider {

    lateinit var database: ElectricityDatabase
        private set

    lateinit var userPreferencesManager: UserPreferencesManager
        private set

    lateinit var electricityPredictor: ElectricityPredictor
        private set

    companion object {
        @Volatile
        private var _instance: ThunderApplication? = null
        val instance: ThunderApplication
            get() = _instance ?: throw IllegalStateException("ThunderApplication 尚未初始化")

        const val ELECTRICITY_CHECK_WORK_NAME = "electricity_check_work"
        const val ELECTRICITY_PREDICTION_WORK_NAME = "electricity_prediction_work"
        private const val DEFAULT_BACKOFF_DELAY_MINUTES = 15L
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        Log.d("ThunderApplication", "onCreate 已开始")

        database = Room.databaseBuilder(
            applicationContext,
            ElectricityDatabase::class.java,
            ElectricityDatabase.DATABASE_NAME
        ).build()
        Log.d("ThunderApplication", "数据库已初始化")

        userPreferencesManager = UserPreferencesManager(applicationContext)
        Log.d("ThunderApplication", "UserPreferencesManager 已初始化")

        Log.d("ThunderApplication", "Initializing UserPreferencesManager defaults (blocking)...")
        runBlocking {
            userPreferencesManager.initializeDefaultsIfNeeded()
        }
        Log.d("ThunderApplication", "UserPreferencesManager defaults initialization COMPLETED (blocking).")

        electricityPredictor = ElectricityPredictor(applicationContext)
        Log.d("ThunderApplication", "ElectricityPredictor 已初始化")

        startBackgroundServices()
        Log.d("ThunderApplication", "onCreate 已完成，后台服务已启动")
    }

    fun startBackgroundServices() {
        Log.i("ThunderApplication", "正在启动后台服务...")
        setupPeriodicElectricityCheckWork()
        setupPeriodicPredictionWork()
    }

    private fun setupPeriodicElectricityCheckWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<ElectricityCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    DEFAULT_BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES
                ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            ELECTRICITY_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.i("ThunderApplication", "周期性电量检查任务已调度，ID: ${workRequest.id}")
    }

    private fun setupPeriodicPredictionWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<ElectricityPredictionWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    DEFAULT_BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES
                ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            ELECTRICITY_PREDICTION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.i("ThunderApplication", "周期性电量预测任务已调度，ID: ${workRequest.id}")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
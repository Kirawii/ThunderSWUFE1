package com.kirawii.thunderswufe;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.kirawii.thunderswufe.data.database.ElectricityDatabase;
import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager;
import com.kirawii.thunderswufe.ml.ElectricityPredictor;
import com.kirawii.thunderswufe.work.ElectricityCheckWorker;
import com.kirawii.thunderswufe.work.ElectricityPredictionWorker;

import java.util.concurrent.TimeUnit;

public class ThunderApplication extends Application implements Configuration.Provider {
    private static volatile ThunderApplication instance;
    public static ThunderApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ThunderApplication 尚未初始化");
        }
        return instance;
    }

    public static final String ELECTRICITY_CHECK_WORK_NAME = "electricity_check_work";
    public static final String ELECTRICITY_PREDICTION_WORK_NAME = "electricity_prediction_work";
    private static final long DEFAULT_BACKOFF_DELAY_MINUTES = 15L;

    private ElectricityDatabase database;
    private UserPreferencesManager userPreferencesManager;
    private ElectricityPredictor electricityPredictor;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.d("ThunderApplication", "onCreate 已开始");

        database = Room.databaseBuilder(
                getApplicationContext(),
                ElectricityDatabase.class,
                ElectricityDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build();
        Log.d("ThunderApplication", "数据库已初始化");

        userPreferencesManager = new UserPreferencesManager(getApplicationContext());
        Log.d("ThunderApplication", "UserPreferencesManager 已初始化");

        Log.d("ThunderApplication", "Initializing UserPreferencesManager defaults (blocking)...");
        userPreferencesManager.initializeDefaultsIfNeeded(); // Java: 同步调用
        Log.d("ThunderApplication", "UserPreferencesManager defaults initialization COMPLETED (blocking).");

        electricityPredictor = new ElectricityPredictor(getApplicationContext());
        Log.d("ThunderApplication", "ElectricityPredictor 已初始化");

        startBackgroundServices();
        Log.d("ThunderApplication", "onCreate 已完成，后台服务已启动");
    }

    public ElectricityDatabase getDatabase() {
        return database;
    }

    public UserPreferencesManager getUserPreferencesManager() {
        return userPreferencesManager;
    }

    public ElectricityPredictor getElectricityPredictor() {
        return electricityPredictor;
    }

    public void startBackgroundServices() {
        Log.i("ThunderApplication", "正在启动后台服务...");
        setupPeriodicElectricityCheckWork();
        setupPeriodicPredictionWork();
    }

    private void setupPeriodicElectricityCheckWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ElectricityCheckWorker.class,
                6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_MINUTES,
                        TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                ELECTRICITY_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.i("ThunderApplication", "周期性电量检查任务已调度，ID: " + workRequest.getId());
    }

    private void setupPeriodicPredictionWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ElectricityPredictionWorker.class,
                1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_MINUTES,
                        TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                ELECTRICITY_PREDICTION_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.i("ThunderApplication", "周期性电量预测任务已调度，ID: " + workRequest.getId());
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}

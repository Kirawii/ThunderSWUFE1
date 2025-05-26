package com.kirawii.thunderswufe;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.kirawii.thunderswufe.data.database.ElectricityDatabase;
import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager;
import com.kirawii.thunderswufe.network.ElectricityService;
import com.kirawii.thunderswufe.network.NetworkModule;
import com.kirawii.thunderswufe.work.ElectricityCheckWorker;
import com.kirawii.thunderswufe.work.ElectricityPredictionWorker;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.TimeUnit;

public class ThunderApplication extends Application implements Configuration.Provider {
    private static volatile ThunderApplication instance;
    private ElectricityDatabase database;
    private UserPreferencesManager userPreferencesManager;
    private ElectricityService electricityService;
    private NetworkModule networkModule;

    public static final String ELECTRICITY_CHECK_WORK_NAME = "electricity_check_work";
    public static final String ELECTRICITY_PREDICTION_WORK_NAME = "electricity_prediction_work";
    private static final long DEFAULT_BACKOFF_DELAY_MINUTES = 15L;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.d("ThunderApplication", "onCreate started");

        initializeDatabase();
        initializeUserPreferences();
        initializeNetwork();
        initializeNetworkModule();
        startBackgroundServices();

        Log.d("ThunderApplication", "onCreate completed, all components initialized");
    }

    private void initializeDatabase() {
        database = Room.databaseBuilder(
                getApplicationContext(),
                ElectricityDatabase.class,
                ElectricityDatabase.DATABASE_NAME
        ).build();
        Log.d("ThunderApplication", "Database initialized");
    }

    private void initializeUserPreferences() {
        userPreferencesManager = new UserPreferencesManager(getApplicationContext());
        Log.d("ThunderApplication", "UserPreferencesManager initialized");

        Log.d("ThunderApplication", "Initializing UserPreferencesManager defaults (blocking)...");
        try {
            Single.fromPublisher(userPreferencesManager.initializeDefaultsIfNeeded())
                    .blockingGet();
            Log.d("ThunderApplication", "UserPreferencesManager defaults initialization COMPLETED");
        } catch (Exception e) {
            Log.e("ThunderApplication", "Error initializing UserPreferencesManager defaults", e);
        }
    }

    private void initializeNetwork() {
        electricityService = NetworkModule.createElectricityService();
        Log.d("ThunderApplication", "Network service initialized");
    }

    private void initializeNetworkModule() {
        networkModule = new NetworkModule();
        Log.d("ThunderApplication", "NetworkModule initialized");
    }

    public void startBackgroundServices() {
        Log.i("ThunderApplication", "Starting background services...");
        setupPeriodicElectricityCheckWork();
        setupPeriodicPredictionWork();
    }

    private void setupPeriodicElectricityCheckWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ElectricityCheckWorker.class,
                6, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_MINUTES,
                        TimeUnit.MINUTES
                )
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                ELECTRICITY_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.i("ThunderApplication", "Periodic electricity check task scheduled, ID: " + workRequest.getId());
    }

    private void setupPeriodicPredictionWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ElectricityPredictionWorker.class,
                1, TimeUnit.DAYS
        )
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_MINUTES,
                        TimeUnit.MINUTES
                )
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                ELECTRICITY_PREDICTION_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.i("ThunderApplication", "Periodic prediction task scheduled, ID: " + workRequest.getId());
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }

    public static ThunderApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ThunderApplication not initialized");
        }
        return instance;
    }

    @NonNull
    public ElectricityDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return database;
    }

    @NonNull
    public UserPreferencesManager getUserPreferencesManager() {
        if (userPreferencesManager == null) {
            throw new IllegalStateException("UserPreferencesManager not initialized");
        }
        return userPreferencesManager;
    }

    @NonNull
    public ElectricityService getElectricityService() {
        if (electricityService == null) {
            throw new IllegalStateException("ElectricityService not initialized");
        }
        return electricityService;
    }

    @NonNull
    public NetworkModule getNetworkModule() {
        if (networkModule == null) {
            throw new IllegalStateException("NetworkModule not initialized");
        }
        return networkModule;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (electricityService != null) {
            electricityService.close();
        }
        Log.i("ThunderApplication", "Application terminated, resources cleaned up");
    }
} 
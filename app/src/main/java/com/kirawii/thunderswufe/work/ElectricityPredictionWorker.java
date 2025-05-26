package com.kirawii.thunderswufe.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.notification.ElectricityNotificationManager;
import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import com.kirawii.thunderswufe.ml.ElectricityPredictor;
import com.kirawii.thunderswufe.ml.PredictionResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ElectricityPredictionWorker extends Worker {
    private final ThunderApplication app;
    private final ElectricityNotificationManager notificationManager;

    public ElectricityPredictionWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
        this.app = (ThunderApplication) context.getApplicationContext();
        this.notificationManager = new ElectricityNotificationManager(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<ElectricityRecord> records = app.getDatabase().electricityDao().getAllRecords().blockingFirst();

            ElectricityPredictor predictor = new ElectricityPredictor(getApplicationContext());
            PredictionResult predictionResult = predictor.predictFutureUsage(records, ElectricityPredictor.ModelType.SIMPLE_LINEAR);

            if (predictionResult.getError() != null) {
                return Result.failure();
            }

            boolean notificationEnabled = app.getUserPreferencesManager().isNotificationEnabled();

            if (!notificationEnabled) {
                return Result.success();
            }

            Integer daysUntilEmpty = predictionResult.getDaysUntilEmpty();
            if (daysUntilEmpty != null && daysUntilEmpty <= 3) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
                String emptyDate = LocalDateTime.now()
                        .plusDays(daysUntilEmpty)
                        .format(formatter);

                notificationManager.showPredictionNotification(
                        daysUntilEmpty,
                        emptyDate,
                        predictionResult.getConfidence()
                );
            }

            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
} 
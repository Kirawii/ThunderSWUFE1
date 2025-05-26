package com.kirawii.thunderswufe.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.kirawii.thunderswufe.ThunderApplication;
import com.kirawii.thunderswufe.notification.ElectricityNotificationManager;
import com.kirawii.thunderswufe.ml.ElectricityPredictor;
import com.kirawii.thunderswufe.ml.PredictionResult;
import io.reactivex.rxjava3.core.Single;
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
            List<ElectricityRecord> records = Single.fromPublisher(
                    app.getDatabase().electricityDao().getAllRecords()
            ).blockingGet();

            PredictionResult predictionResult = app.getElectricityPredictor()
                    .predictFutureUsage(records);

            if (predictionResult.getError() != null) {
                return Result.failure();
            }

            boolean notificationEnabled = Single.fromPublisher(
                    app.getUserPreferencesManager().getNotificationEnabled()
            ).blockingGet();

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
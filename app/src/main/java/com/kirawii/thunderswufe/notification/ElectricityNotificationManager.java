package com.kirawii.thunderswufe.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.kirawii.thunderswufe.R;

public class ElectricityNotificationManager {
    private static final String CHANNEL_ID = "electricity_notification";
    private static final int LOW_BALANCE_ID = 1;
    private static final int PREDICTION_ID = 2;

    private final NotificationManager notificationManager;

    public ElectricityNotificationManager(Context context) {
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "电量提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("宿舍电量提醒通知");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showLowBalanceNotification(double balance, double threshold) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚡ 电量不足提醒")
                .setContentText(String.format("当前剩余电量: %.2f度，低于设定阈值%.2f度", balance, threshold))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(LOW_BALANCE_ID, builder.build());
    }

    public void showPredictionNotification(
            int daysUntilEmpty,
            String emptyDate,
            float confidence
    ) {
        int confidencePercent = (int) (confidence * 100);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ 电量预警")
                .setContentText(String.format("预计将在 %d 天后（%s）电量耗尽", daysUntilEmpty, emptyDate))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format(
                                "预计将在 %d 天后（%s）电量耗尽\n" +
                                "预测准确度：%d%%\n" +
                                "建议及时充值以避免断电",
                                daysUntilEmpty, emptyDate, confidencePercent)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(PREDICTION_ID, builder.build());
    }
} 
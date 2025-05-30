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

    private final Context context;
    private final NotificationManager notificationManager;

    public ElectricityNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
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
        String contentText = "当前剩余电量: " + balance + "度，低于设定阈值" + threshold + "度";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚡ 电量不足提醒")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(LOW_BALANCE_ID, builder.build());
    }

    public void showPredictionNotification(int daysUntilEmpty, String emptyDate, float confidence) {
        int confidencePercent = Math.round(confidence * 100);
        String bigText = "预计将在 " + daysUntilEmpty + " 天后（" + emptyDate + "）电量耗尽\n"
                + "预测准确度：" + confidencePercent + "%\n"
                + "建议及时充值以避免断电";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ 电量预警")
                .setContentText("预计将在 " + daysUntilEmpty + " 天后（" + emptyDate + "）电量耗尽")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(PREDICTION_ID, builder.build());
    }
}

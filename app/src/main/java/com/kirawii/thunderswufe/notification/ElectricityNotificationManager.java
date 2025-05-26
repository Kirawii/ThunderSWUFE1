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
    // 添加一个私有的Context成员变量
    private final Context context; // <-- 新增这一行

    public ElectricityNotificationManager(Context context) {
        this.context = context; // <-- 在构造函数中初始化它
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// 小心这里，之前可能是 Build.VERSION_CODES.O 而不是SDK_SDK_INT
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "电量提醒", // 建议将字符串放在strings.xml中
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("宿舍电量提醒通知"); // 建议将字符串放在strings.xml中
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showLowBalanceNotification(double balance, double threshold) {
        // 现在可以使用成员变量context了
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // 建议使用你自己的应用图标 R.drawable.your_app_icon
                .setContentTitle("⚡ 电量不足提醒") // 建议将字符串放在strings.xml中
                .setContentText(String.format("当前剩余电量: %.2f度，低于设定阈值%.2f度", balance, threshold)) // 建议将格式化字符串放在strings.xml中
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // 点击后自动取消

        notificationManager.notify(LOW_BALANCE_ID, builder.build());
    }

    public void showPredictionNotification(
            int daysUntilEmpty,
            String emptyDate,
            float confidence
    ) {
        int confidencePercent = (int) (confidence * 100);

        // 现在可以使用成员变量context了
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // 建议使用你自己的应用图标 R.drawable.your_app_icon
                .setContentTitle("⚠️ 电量预警") // 建议将字符串放在strings.xml中
                .setContentText(String.format("预计将在 %d 天后（%s）电量耗尽", daysUntilEmpty, emptyDate)) // 建议将格式化字符串放在strings.xml中
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format(
                                "预计将在 %d 天后（%s）电量耗尽\n" +
                                        "预测准确度：%d%%\n" +
                                        "建议及时充值以避免断电", // 建议将格式化字符串放在strings.xml中
                                daysUntilEmpty, emptyDate, confidencePercent)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // 点击后自动取消

        notificationManager.notify(PREDICTION_ID, builder.build());
    }
}
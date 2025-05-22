package com.kirawii.thunderswufe.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kirawii.thunderswufe.R
import kotlin.math.roundToInt

class ElectricityNotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "electricity_notification"
        private const val LOW_BALANCE_ID = 1
        private const val PREDICTION_ID = 2
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "电量提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "宿舍电量提醒通知"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showLowBalanceNotification(balance: Double, threshold: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // 这是一个系统自带的警告图标
            .setContentTitle("⚡ 电量不足提醒")
            .setContentText("当前剩余电量: ${balance}度，低于设定阈值${threshold}度")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(LOW_BALANCE_ID, notification)
    }
    
    fun showPredictionNotification(
        daysUntilEmpty: Int,
        emptyDate: String,
        confidence: Float
    ) {
        val confidencePercent = (confidence * 100).roundToInt()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // 这是一个系统自带的警告图标
            .setContentTitle("⚠️ 电量预警")
            .setContentText("预计将在 $daysUntilEmpty 天后（$emptyDate）电量耗尽")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("预计将在 $daysUntilEmpty 天后（$emptyDate）电量耗尽\n" +
                        "预测准确度：$confidencePercent%\n" +
                        "建议及时充值以避免断电")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(PREDICTION_ID, notification)
    }
} 
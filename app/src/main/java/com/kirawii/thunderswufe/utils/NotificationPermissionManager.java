package com.kirawii.thunderswufe.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

public class NotificationPermissionManager {
    private final Activity activity;
    private ActivityResultLauncher<String> permissionLauncher;

    public NotificationPermissionManager(Activity activity) {
        this.activity = activity;
    }

    public void registerPermissionLauncher(
            ActivityResultLauncher<String> launcher,
            Runnable onGranted,
            Runnable onDenied
    ) {
        this.permissionLauncher = launcher;
    }

    public void checkAndRequestPermission(
            Runnable onGranted,
            Runnable onDenied
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
                onGranted.run();
            } else if (activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
            )) {
                // 显示权限说明对话框
                showPermissionRationaleDialog(onGranted, onDenied);
            } else {
                if (permissionLauncher != null) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        } else {
            // Android 13以下版本默认允许通知
            onGranted.run();
        }
    }

    private void showPermissionRationaleDialog(
            Runnable onGranted,
            Runnable onDenied
    ) {
        // TODO: 显示权限说明对话框
        if (permissionLauncher != null) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
} 